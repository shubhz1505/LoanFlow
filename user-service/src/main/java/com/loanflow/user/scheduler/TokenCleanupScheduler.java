package com.loanflow.user.scheduler;

import com.loanflow.user.repository.RefreshTokenRepository;
import com.loanflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository         userRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredAndRevoked();
        log.info("Token cleanup: deleted {} expired/revoked tokens", deleted);
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void unlockExpiredLockouts() {
        var expired = userRepository.findExpiredLocks(LocalDateTime.now());
        expired.forEach(user -> {
            userRepository.unlockUser(user.getId());
            log.info("Auto-unlocked account: userId={}", user.getId());
        });
    }
}