package com.safar.chef.entity;

import com.safar.chef.entity.enums.ChefBookingStatus;
import com.safar.chef.entity.enums.MealType;
import com.safar.chef.entity.enums.ServiceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "chef_bookings", schema = "chefs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChefBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_ref", unique = true, length = 20)
    private String bookingRef;

    @Column(name = "chef_id", nullable = false)
    private UUID chefId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "chef_name")
    private String chefName;

    @Column(name = "customer_name")
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type")
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type")
    private MealType mealType;

    @Column(name = "service_date")
    private LocalDate serviceDate;

    @Column(name = "service_time")
    private String serviceTime;

    @Column(name = "guests_count")
    private Integer guestsCount;

    @Column(name = "number_of_meals")
    @Builder.Default
    private Integer numberOfMeals = 1;

    @Column(name = "menu_id")
    private UUID menuId;

    @Column(name = "menu_name")
    private String menuName;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String city;

    private String locality;

    private String pincode;

    @Column(name = "total_amount_paise")
    private Long totalAmountPaise;

    @Column(name = "platform_fee_paise")
    private Long platformFeePaise;

    @Column(name = "chef_earnings_paise")
    private Long chefEarningsPaise;

    @Enumerated(EnumType.STRING)
    private ChefBookingStatus status;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "rating_given")
    private Integer ratingGiven;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
