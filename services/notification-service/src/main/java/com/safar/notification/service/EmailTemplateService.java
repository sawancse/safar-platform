package com.safar.notification.service;

import com.safar.notification.dto.EmailContext;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailTemplateService {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateService.class);

    private final TemplateEngine templateEngine;
    private final JavaMailSender mailSender;

    @Value("${notification.from-email}")
    private String fromEmail;

    @Value("${notification.from-name:Safar}")
    private String fromName;

    public EmailTemplateService(TemplateEngine templateEngine, JavaMailSender mailSender) {
        this.templateEngine = templateEngine;
        this.mailSender = mailSender;
    }

    public String render(String templateName, EmailContext ctx) {
        Context thymeleafCtx = new Context();
        thymeleafCtx.setVariable("ctx", ctx);
        return templateEngine.process("email/" + templateName, thymeleafCtx);
    }

    public void sendHtmlEmail(String to, String subject, String templateName, EmailContext ctx) {
        if (to == null || to.isBlank()) {
            log.warn("Skipping email — no recipient for template {}", templateName);
            return;
        }
        try {
            String html = render(templateName, ctx);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent HTML email [{}] to {}", templateName, to);
        } catch (Exception e) {
            log.error("Failed to send HTML email [{}] to {}: {}", templateName, to, e.getMessage(), e);
            throw new RuntimeException("HTML email failed: " + templateName, e);
        }
    }

    /**
     * Build subject line with chapter prefix for journey narrative emails.
     */
    public String buildChapterSubject(int chapter, String cityName, String baseSubject) {
        return String.format("Chapter %d of Your %s Journey — %s", chapter, cityName, baseSubject);
    }
}
