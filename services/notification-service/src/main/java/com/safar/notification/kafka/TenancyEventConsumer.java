package com.safar.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.notification.dto.EmailContext;
import com.safar.notification.service.EmailContextBuilder;
import com.safar.notification.service.EmailTemplateService;
import com.safar.notification.service.InAppNotificationService;
import com.safar.notification.service.SmsService;
import com.safar.notification.service.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Consumes PG tenancy lifecycle events and sends emails + in-app notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenancyEventConsumer {

    private final EmailTemplateService emailTemplateService;
    private final InAppNotificationService inAppNotificationService;
    private final SmsService smsService;
    private final UserClient userClient;
    private final ObjectMapper objectMapper;

    @Value("${app.base-url:https://ysafar.com}")
    private String baseUrl;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @KafkaListener(
            topics = {"tenancy.created", "tenancy.invoice.generated", "tenancy.invoice.overdue",
                      "tenancy.rent.reminder", "tenancy.rent.reminder.urgent",
                      "tenancy.rent.reminder.advance",
                      "tenancy.agreement.host-signed", "tenancy.agreement.active",
                      "tenancy.notice"},
            groupId = "notification-tenancy-group"
    )
    public void onTenancyEvent(String message,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            switch (topic) {
                case "tenancy.created"                -> handleTenancyCreated(message);
                case "tenancy.invoice.generated"       -> handleInvoiceGenerated(message);
                case "tenancy.invoice.overdue"          -> handleInvoiceOverdue(message);
                case "tenancy.rent.reminder"            -> handleRentReminder(message, false);
                case "tenancy.rent.reminder.urgent"     -> handleRentReminder(message, true);
                case "tenancy.rent.reminder.advance"    -> handleAdvanceRentReminder(message);
                case "tenancy.agreement.host-signed"    -> handleAgreementPendingSign(message);
                case "tenancy.agreement.active"         -> handleAgreementActive(message);
                case "tenancy.notice"                   -> handleNoticeGiven(message);
                default -> log.warn("Unhandled tenancy topic: {}", topic);
            }
        } catch (Exception e) {
            log.error("Error handling tenancy event on topic {}: {}", topic, e.getMessage(), e);
        }
    }

    // ── tenancy.created → Welcome email ──

    private void handleTenancyCreated(String message) {
        JsonNode node = tryParseJson(message);
        if (node == null) return;

        String tenantId = node.path("tenantId").asText("");
        UserClient.UserInfo tenant = userClient.getUser(tenantId);
        if (tenant == null || tenant.email() == null || tenant.email().isBlank()) return;

        EmailContext ctx = new EmailContext();
        ctx.setGuestName(tenant.name());
        ctx.setGuestEmail(tenant.email());
        ctx.setTenancyRef(node.path("tenancyRef").asText(""));
        ctx.setRentAmount(formatPaise(node.path("monthlyRentPaise").asLong()));
        ctx.setMoveInDate(formatDate(node.path("moveInDate").asText("")));
        ctx.setPropertyName(node.path("listingId").asText("Your PG"));
        ctx.setRoomDescription(node.path("sharingType").asText("") + " - Bed " + node.path("bedNumber").asText(""));
        ctx.setNoticePeriodDays(String.valueOf(node.path("noticePeriodDays").asInt(30)));

        String subject = "Welcome to Your PG - " + ctx.getTenancyRef();
        emailTemplateService.sendHtmlEmail(tenant.email(), subject, "tenancy-welcome", ctx);

        inAppNotificationService.create(UUID.fromString(tenantId),
                "PG Confirmed", "Your PG tenancy " + ctx.getTenancyRef() + " is now active. Monthly rent: " + ctx.getRentAmount(),
                "TENANCY_CREATED", node.path("id").asText(""), "TENANCY");

        log.info("Sent tenancy welcome email to {} for {}", tenant.email(), ctx.getTenancyRef());
    }

    // ── tenancy.rent.reminder.advance → 7-day advance reminder (before invoice) ──

    private void handleAdvanceRentReminder(String message) {
        JsonNode node = tryParseJson(message);
        if (node == null) return;

        String tenantId = node.path("tenantId").asText("");
        if (tenantId.isBlank()) return;

        UserClient.UserInfo tenant = userClient.getUser(tenantId);
        if (tenant == null) return;

        String rentAmount = formatPaise(node.path("totalMonthlyPaise").asLong());
        String billingDate = formatDate(node.path("nextBillingDate").asText(""));
        String tenancyRef = node.path("tenancyRef").asText("");

        // Email
        if (tenant.email() != null && !tenant.email().isBlank()) {
            EmailContext ctx = new EmailContext();
            ctx.setGuestName(tenant.name());
            ctx.setTenancyRef(tenancyRef);
            ctx.setRentAmount(rentAmount);
            ctx.setDueDate(billingDate);
            ctx.setDaysUntilDue(7);

            String subject = "Rent Reminder - Payment Due in 7 Days";
            emailTemplateService.sendHtmlEmail(tenant.email(), subject, "tenancy-rent-reminder-advance", ctx);
            log.info("Sent 7-day advance rent email to {} for {}", tenant.email(), tenancyRef);
        }

        // SMS
        if (tenant.phone() != null && !tenant.phone().isBlank()) {
            smsService.sendRentAdvanceReminder(tenant.phone(), tenant.name(), rentAmount, billingDate);
        }

        // In-app
        inAppNotificationService.create(UUID.fromString(tenantId),
                "Rent Due Next Week",
                "Your monthly rent of " + rentAmount + " is due on " + billingDate + ". Please keep funds ready.",
                "TENANCY_RENT_ADVANCE", node.path("tenancyId").asText(""), "TENANCY");
    }

    // ── tenancy.invoice.generated → Invoice notification ──

    private void handleInvoiceGenerated(String message) {
        JsonNode node = tryParseJson(message);
        if (node == null) return;

        String tenancyId = node.path("tenancyId").asText("");
        String tenantId = node.path("tenantId").asText("");
        if (tenantId.isBlank()) {
            log.debug("No tenantId in invoice event for tenancy {}, skipping", tenancyId);
            return;
        }

        UserClient.UserInfo tenant = userClient.getUser(tenantId);
        if (tenant == null) return;

        String invoiceNumber = node.path("invoiceNumber").asText("");
        String rentAmount = formatPaise(node.path("grandTotalPaise").asLong());
        String dueDate = formatDate(node.path("dueDate").asText(""));

        // Email
        if (tenant.email() != null && !tenant.email().isBlank()) {
            EmailContext ctx = new EmailContext();
            ctx.setGuestName(tenant.name());
            ctx.setInvoiceNumber(invoiceNumber);
            ctx.setRentAmount(rentAmount);
            ctx.setDueDate(dueDate);
            ctx.setTenancyRef(node.path("tenancyRef").asText(""));

            int month = node.path("billingMonth").asInt();
            int year = node.path("billingYear").asInt();
            String monthName = month > 0 ? LocalDate.of(year, month, 1).getMonth().toString() : "";

            String subject = "Rent Invoice " + invoiceNumber + " - " + capitalize(monthName) + " " + year;
            emailTemplateService.sendHtmlEmail(tenant.email(), subject, "tenancy-invoice", ctx);
            log.info("Sent invoice email to {} for {}", tenant.email(), invoiceNumber);
        }

        // SMS
        if (tenant.phone() != null && !tenant.phone().isBlank()) {
            smsService.sendInvoiceGenerated(tenant.phone(), tenant.name(), rentAmount, dueDate, invoiceNumber);
        }

        // In-app
        inAppNotificationService.create(UUID.fromString(tenantId),
                "New Invoice", "Rent invoice " + invoiceNumber + " for " + rentAmount + " due by " + dueDate,
                "TENANCY_INVOICE", node.path("id").asText(""), "TENANCY_INVOICE");
    }

    // ── tenancy.rent.reminder / .urgent → Payment reminder ──

    private void handleRentReminder(String message, boolean urgent) {
        JsonNode node = tryParseJson(message);
        if (node == null) return;

        String tenantId = node.path("tenantId").asText("");
        if (tenantId.isBlank()) return;

        UserClient.UserInfo tenant = userClient.getUser(tenantId);
        if (tenant == null) return;

        String invoiceNumber = node.path("invoiceNumber").asText("");
        String rentAmount = formatPaise(node.path("grandTotalPaise").asLong());
        String dueDate = formatDate(node.path("dueDate").asText(""));

        // Email
        if (tenant.email() != null && !tenant.email().isBlank()) {
            EmailContext ctx = new EmailContext();
            ctx.setGuestName(tenant.name());
            ctx.setTenancyRef(node.path("tenancyRef").asText(""));
            ctx.setInvoiceNumber(invoiceNumber);
            ctx.setRentAmount(rentAmount);
            ctx.setDueDate(dueDate);
            ctx.setDaysUntilDue(node.path("daysUntilDue").asInt());

            String template = urgent ? "tenancy-rent-reminder-urgent" : "tenancy-rent-reminder";
            String subject = urgent
                    ? "Rent Due Tomorrow - " + invoiceNumber
                    : "Rent Due in 5 Days - " + invoiceNumber;

            emailTemplateService.sendHtmlEmail(tenant.email(), subject, template, ctx);
            log.info("Sent {} rent reminder email to {} for {}", urgent ? "urgent" : "5-day", tenant.email(), invoiceNumber);
        }

        // SMS
        if (tenant.phone() != null && !tenant.phone().isBlank()) {
            if (urgent) {
                smsService.sendRentUrgentReminder(tenant.phone(), tenant.name(), rentAmount, dueDate);
            } else {
                smsService.sendRentReminder(tenant.phone(), tenant.name(), rentAmount, dueDate, invoiceNumber);
            }
        }

        // In-app
        inAppNotificationService.create(UUID.fromString(tenantId),
                urgent ? "Rent Due Tomorrow" : "Rent Reminder",
                "Your rent of " + rentAmount + " is due " + (urgent ? "tomorrow" : "in 5 days"),
                "TENANCY_RENT_REMINDER", node.path("invoiceId").asText(""), "TENANCY_INVOICE");
    }

    // ── tenancy.invoice.overdue → Overdue notice ──

    private void handleInvoiceOverdue(String message) {
        JsonNode node = tryParseJson(message);
        if (node == null) return;

        String tenancyId = node.path("tenancyId").asText("");
        String tenantId = node.path("tenantId").asText("");
        if (tenantId.isBlank()) {
            log.debug("No tenantId in overdue event for tenancy {}, skipping", tenancyId);
            return;
        }

        UserClient.UserInfo tenant = userClient.getUser(tenantId);
        if (tenant == null) return;

        String invoiceNumber = node.path("invoiceNumber").asText("");
        String rentAmount = formatPaise(node.path("grandTotalPaise").asLong());
        String penaltyAmount = formatPaise(node.path("latePenaltyPaise").asLong());

        // Email
        if (tenant.email() != null && !tenant.email().isBlank()) {
            EmailContext ctx = new EmailContext();
            ctx.setGuestName(tenant.name());
            ctx.setInvoiceNumber(invoiceNumber);
            ctx.setRentAmount(rentAmount);
            ctx.setDueDate(formatDate(node.path("dueDate").asText("")));
            ctx.setPenaltyAmount(penaltyAmount);
            ctx.setTenancyRef(node.path("tenancyRef").asText(""));

            String subject = "Overdue: Rent Payment Required - " + invoiceNumber;
            emailTemplateService.sendHtmlEmail(tenant.email(), subject, "tenancy-invoice-overdue", ctx);
            log.info("Sent overdue notice email to {} for {}", tenant.email(), invoiceNumber);
        }

        // SMS
        if (tenant.phone() != null && !tenant.phone().isBlank()) {
            smsService.sendRentOverdue(tenant.phone(), tenant.name(), rentAmount, penaltyAmount);
        }

        // In-app
        inAppNotificationService.create(UUID.fromString(tenantId),
                "Rent Overdue", "Your rent of " + rentAmount + " is overdue. Late penalty: " + penaltyAmount,
                "TENANCY_INVOICE_OVERDUE", node.path("id").asText(""), "TENANCY_INVOICE");
    }

    // ── tenancy.agreement.host-signed → Agreement ready for tenant sign ──

    private void handleAgreementPendingSign(String message) {
        JsonNode node = tryParseJson(message);
        if (node == null) return;

        String tenantEmail = node.path("tenantEmail").asText("");
        String tenantName = node.path("tenantName").asText("");
        String tenancyId = node.path("tenancyId").asText("");
        if (tenantEmail.isBlank()) return;

        EmailContext ctx = new EmailContext();
        ctx.setGuestName(tenantName);
        ctx.setAgreementNumber(node.path("agreementNumber").asText(""));
        ctx.setAgreementUrl(baseUrl + "/pg-dashboard");
        ctx.setPropertyName(node.path("propertyAddress").asText(""));
        ctx.setRoomDescription(node.path("roomDescription").asText(""));
        ctx.setRentAmount(formatPaise(node.path("monthlyRentPaise").asLong()));
        ctx.setMoveInDate(formatDate(node.path("moveInDate").asText("")));

        String subject = "Your Rental Agreement is Ready - Please Review & Sign";
        emailTemplateService.sendHtmlEmail(tenantEmail, subject, "tenancy-agreement-review", ctx);

        if (!tenancyId.isBlank()) {
            // Try to find tenant userId for in-app notification
            String tenantId = node.path("tenantId").asText("");
            if (tenantId.isBlank()) {
                // Agreement event has tenancyId but might not have tenantId directly
                // We'll only send in-app if we have the ID
                log.debug("No tenantId in agreement event, skipping in-app notification");
            } else {
                inAppNotificationService.create(UUID.fromString(tenantId),
                        "Agreement Ready", "Your rental agreement is ready for review and signature",
                        "TENANCY_AGREEMENT_PENDING", tenancyId, "TENANCY");
            }
        }

        log.info("Sent agreement review email to {} for {}", tenantEmail, ctx.getAgreementNumber());
    }

    // ── tenancy.agreement.active → Both signed confirmation ──

    private void handleAgreementActive(String message) {
        JsonNode node = tryParseJson(message);
        if (node == null) return;

        String tenantEmail = node.path("tenantEmail").asText("");
        String tenantName = node.path("tenantName").asText("");
        if (tenantEmail.isBlank()) return;

        EmailContext ctx = new EmailContext();
        ctx.setGuestName(tenantName);
        ctx.setAgreementNumber(node.path("agreementNumber").asText(""));
        ctx.setAgreementUrl(baseUrl + "/pg-dashboard");
        ctx.setPropertyName(node.path("propertyAddress").asText(""));
        ctx.setRentAmount(formatPaise(node.path("monthlyRentPaise").asLong()));

        String subject = "Rental Agreement Signed Successfully - " + ctx.getAgreementNumber();
        emailTemplateService.sendHtmlEmail(tenantEmail, subject, "tenancy-agreement-signed", ctx);

        // In-app notification
        String tenantId = node.path("tenantId").asText("");
        if (!tenantId.isBlank()) {
            inAppNotificationService.create(UUID.fromString(tenantId),
                    "Agreement Active", "Your rental agreement " + ctx.getAgreementNumber() + " is now active",
                    "TENANCY_AGREEMENT_ACTIVE", node.path("tenancyId").asText(""), "TENANCY");
        }

        log.info("Sent agreement signed confirmation to {} for {}", tenantEmail, ctx.getAgreementNumber());
    }

    // ── tenancy.notice → Notice period started ──

    private void handleNoticeGiven(String message) {
        JsonNode node = tryParseJson(message);
        if (node == null) return;

        String tenantId = node.path("tenantId").asText("");
        if (tenantId.isBlank()) return;

        UserClient.UserInfo tenant = userClient.getUser(tenantId);
        if (tenant == null || tenant.email() == null || tenant.email().isBlank()) return;

        EmailContext ctx = new EmailContext();
        ctx.setGuestName(tenant.name());
        ctx.setTenancyRef(node.path("tenancyRef").asText(""));
        ctx.setMoveOutDate(formatDate(node.path("moveOutDate").asText("")));
        ctx.setNoticePeriodDays(String.valueOf(node.path("noticePeriodDays").asInt(30)));

        String subject = "Notice Period Started - " + ctx.getTenancyRef();
        emailTemplateService.sendHtmlEmail(tenant.email(), subject, "tenancy-notice", ctx);

        inAppNotificationService.create(UUID.fromString(tenantId),
                "Notice Period", "Your notice period has started. Move-out date: " + ctx.getMoveOutDate(),
                "TENANCY_NOTICE", node.path("id").asText(""), "TENANCY");

        log.info("Sent notice period email to {} for {}", tenant.email(), ctx.getTenancyRef());
    }

    // ── Helpers ──

    private JsonNode tryParseJson(String message) {
        try {
            return objectMapper.readTree(message);
        } catch (Exception e) {
            log.warn("Failed to parse tenancy event JSON: {}", e.getMessage());
            return null;
        }
    }

    private String formatPaise(long paise) {
        return EmailContextBuilder.formatPaiseToRupeesWithSymbol(paise);
    }

    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return "";
        try {
            return LocalDate.parse(isoDate).format(DATE_FMT);
        } catch (Exception e) {
            return isoDate;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
