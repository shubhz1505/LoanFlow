package com.loanflow.user.entity;

import com.loanflow.commons.enums.KycStatus;
import com.loanflow.commons.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uq_users_phone", columnNames = "phone")
        },
        indexes = {
                @Index(name = "idx_users_email",      columnList = "email"),
                @Index(name = "idx_users_kyc_status", columnList = "kyc_status"),
                @Index(name = "idx_users_role",       columnList = "role")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /**
     * BCrypt hashed password — never store plaintext.
     * BCrypt output is always 60 characters but we use 255
     * to future-proof against stronger algorithms (Argon2 = longer hash).
     */
    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    private String city;
    private String state;
    private String pincode;

    @Column(name = "pan_reference", length = 20)
    private String panReference;

    @Column(name = "aadhaar_reference", length = 20)
    private String aadhaarReference;

    /**
     * @Enumerated(EnumType.STRING) stores "APPLICANT", "LOAN_OFFICER" etc.
     *
     * Never use EnumType.ORDINAL (stores 0, 1, 2 as integers).
     * With ORDINAL: if you add a new role in the middle of the enum,
     * all subsequent ordinal values shift → data corruption.
     * With STRING: adding a role anywhere is safe.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 30)
    private KycStatus kycStatus;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "account_non_locked", nullable = false)
    @Builder.Default
    private boolean accountNonLocked = true;

    // KYC lifecycle tracking
    @Column(name = "kyc_submitted_at")
    private LocalDateTime kycSubmittedAt;

    @Column(name = "kyc_verified_at")
    private LocalDateTime kycVerifiedAt;

    @Column(name = "kyc_rejection_reason", columnDefinition = "TEXT")
    private String kycRejectionReason;

    @Column(name = "kyc_attempt_count", nullable = false)
    @Builder.Default
    private Integer kycAttemptCount = 0;

    // Login security
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    /**
     * @CreationTimestamp: Hibernate sets this once on INSERT.
     * updatable = false: Hibernate ignores this field on UPDATE.
     * Even if you accidentally set createdAt in code,
     * Hibernate won't overwrite the original value in DB.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * @PrePersist: Called by JPA before first INSERT.
     * We set UUID here (not in @GeneratedValue) because:
     * 1. We can set the ID BEFORE saving (useful for event correlation)
     * 2. UUID generation strategy is under our control
     * 3. Works the same whether using JPA or not
     *
     * We also set defaults here as safety net
     * (Lombok @Builder.Default handles it too, but belt-and-suspenders
     * is correct approach for financial systems)
     */
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.kycStatus == null) {
            this.kycStatus = KycStatus.NOT_INITIATED;
        }
        if (this.role == null) {
            this.role = UserRole.APPLICANT;
        }
    }
}