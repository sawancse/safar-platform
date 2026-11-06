package com.safar.payment.repository;

import com.safar.payment.entity.SettlementLine;
import com.safar.payment.entity.SettlementPlan;
import com.safar.payment.entity.enums.RecipientType;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * open-in-view is off in production, and controllers return SettlementPlan as JSON, so the
 * lazy {@code lines} collection must already be loaded when the repository call returns
 * (otherwise Jackson throws "failed to lazily initialize a collection ... no Session").
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SettlementPlanRepositoryTest {

    @Autowired SettlementPlanRepository planRepo;
    @Autowired SettlementLineRepository lineRepo;
    @Autowired EntityManager em;

    private UUID bookingId;
    private UUID planId;

    private void seed() {
        bookingId = UUID.randomUUID();
        SettlementPlan plan = planRepo.save(SettlementPlan.builder()
                .bookingId(bookingId).totalAmountPaise(354000L).build());
        planId = plan.getId();
        lineRepo.save(SettlementLine.builder().plan(plan).recipientType(RecipientType.HOST)
                .recipientId(UUID.randomUUID()).amountPaise(246000L).build());
        lineRepo.save(SettlementLine.builder().plan(plan).recipientType(RecipientType.PLATFORM)
                .amountPaise(108000L).build());
        em.flush();
        em.clear(); // next reads come from the DB, not the first-level cache
    }

    @Test
    void findByBookingIdLoadsLines() {
        seed();
        SettlementPlan p = planRepo.findByBookingId(bookingId).orElseThrow();
        em.detach(p);
        assertTrue(Hibernate.isInitialized(p.getLines()));
        assertEquals(2, p.getLines().size());
    }

    @Test
    void findByIdLoadsLines() {
        seed();
        SettlementPlan p = planRepo.findById(planId).orElseThrow();
        em.detach(p);
        assertTrue(Hibernate.isInitialized(p.getLines()));
        assertEquals(2, p.getLines().size());
    }

    @Test
    void findAllLoadsLinesWithoutDuplicatingPlans() {
        seed();
        List<SettlementPlan> all = planRepo.findAll();
        assertEquals(1, all.size());
        all.forEach(em::detach);
        assertTrue(Hibernate.isInitialized(all.get(0).getLines()));
        assertEquals(2, all.get(0).getLines().size());
    }
}
