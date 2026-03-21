package com.safar.user.service;

import com.safar.user.dto.PaymentMethodDto;
import com.safar.user.dto.PaymentMethodRequest;
import com.safar.user.entity.PaymentMethod;
import com.safar.user.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private static final int MAX_PAYMENT_METHODS = 10;

    private final PaymentMethodRepository repository;

    public List<PaymentMethodDto> getAll(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public PaymentMethodDto create(UUID userId, PaymentMethodRequest req) {
        if (repository.countByUserId(userId) >= MAX_PAYMENT_METHODS) {
            throw new IllegalStateException("Maximum of " + MAX_PAYMENT_METHODS + " payment methods allowed");
        }

        // If setting as default, clear existing defaults
        if (Boolean.TRUE.equals(req.isDefault())) {
            repository.clearDefaults(userId);
        }

        // Auto-generate label if not provided
        String label = req.label();
        if (label == null || label.isBlank()) {
            label = generateLabel(req);
        }

        PaymentMethod entity = PaymentMethod.builder()
                .userId(userId)
                .type(req.type())
                .label(label)
                .isDefault(Boolean.TRUE.equals(req.isDefault()))
                .upiId(req.upiId())
                .cardLast4(req.cardLast4())
                .cardNetwork(req.cardNetwork())
                .cardHolder(req.cardHolder())
                .cardExpiry(req.cardExpiry())
                .bankName(req.bankName())
                .bankAccountLast4(req.bankAccountLast4())
                .build();

        // If this is the first payment method, make it default
        if (repository.countByUserId(userId) == 0) {
            entity.setIsDefault(true);
        }

        return toDto(repository.save(entity));
    }

    @Transactional
    public PaymentMethodDto update(UUID userId, UUID methodId, PaymentMethodRequest req) {
        PaymentMethod entity = repository.findById(methodId)
                .filter(m -> m.getUserId().equals(userId))
                .orElseThrow(() -> new NoSuchElementException("Payment method not found"));

        if (Boolean.TRUE.equals(req.isDefault())) {
            repository.clearDefaults(userId);
        }

        entity.setType(req.type());
        entity.setLabel(req.label() != null && !req.label().isBlank() ? req.label() : generateLabel(req));
        entity.setIsDefault(Boolean.TRUE.equals(req.isDefault()));
        entity.setUpiId(req.upiId());
        entity.setCardLast4(req.cardLast4());
        entity.setCardNetwork(req.cardNetwork());
        entity.setCardHolder(req.cardHolder());
        entity.setCardExpiry(req.cardExpiry());
        entity.setBankName(req.bankName());
        entity.setBankAccountLast4(req.bankAccountLast4());

        return toDto(repository.save(entity));
    }

    @Transactional
    public void setDefault(UUID userId, UUID methodId) {
        PaymentMethod entity = repository.findById(methodId)
                .filter(m -> m.getUserId().equals(userId))
                .orElseThrow(() -> new NoSuchElementException("Payment method not found"));

        repository.clearDefaults(userId);
        entity.setIsDefault(true);
        repository.save(entity);
    }

    @Transactional
    public void delete(UUID userId, UUID methodId) {
        PaymentMethod entity = repository.findById(methodId)
                .filter(m -> m.getUserId().equals(userId))
                .orElseThrow(() -> new NoSuchElementException("Payment method not found"));
        repository.delete(entity);
    }

    private String generateLabel(PaymentMethodRequest req) {
        return switch (req.type()) {
            case "UPI" -> req.upiId() != null ? "UPI - " + req.upiId() : "UPI";
            case "CREDIT_CARD" -> (req.cardNetwork() != null ? req.cardNetwork() : "Card")
                    + (req.cardLast4() != null ? " •••• " + req.cardLast4() : "");
            case "DEBIT_CARD" -> "Debit"
                    + (req.cardNetwork() != null ? " " + req.cardNetwork() : "")
                    + (req.cardLast4() != null ? " •••• " + req.cardLast4() : "");
            case "NET_BANKING" -> req.bankName() != null ? req.bankName() + " Net Banking" : "Net Banking";
            default -> req.type();
        };
    }

    private PaymentMethodDto toDto(PaymentMethod m) {
        return new PaymentMethodDto(
                m.getId(), m.getType(), m.getLabel(), m.getIsDefault(),
                m.getUpiId(), m.getCardLast4(), m.getCardNetwork(),
                m.getCardHolder(), m.getCardExpiry(),
                m.getBankName(), m.getBankAccountLast4(),
                m.getCreatedAt()
        );
    }
}
