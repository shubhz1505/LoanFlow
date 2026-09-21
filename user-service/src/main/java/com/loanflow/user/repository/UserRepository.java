package com.loanflow.user.repository;

import com.loanflow.commons.enums.KycStatus;
import com.loanflow.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    List<User> findByKycStatus(KycStatus kycStatus);

    @Query("SELECT u FROM User u WHERE u.accountNonLocked = false AND u.lockedUntil < :now")
    List<User> findExpiredLocks(LocalDateTime now);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.accountNonLocked = true, u.lockedUntil = null WHERE u.id = :userId")
    void unlockUser(String userId);
}