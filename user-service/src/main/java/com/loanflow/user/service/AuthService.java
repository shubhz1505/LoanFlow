package com.loanflow.user.service;

import com.loanflow.commons.enums.KycStatus;
import com.loanflow.commons.enums.UserRole;
import com.loanflow.commons.exceptions.LoanFlowException;
import com.loanflow.user.dto.request.LoginRequest;
import com.loanflow.user.dto.request.RegisterRequest;
import com.loanflow.user.dto.response.AuthResponse;
import com.loanflow.user.entity.RefreshToken;
import com.loanflow.user.entity.User;
import com.loanflow.user.repository.UserRepository;
import com.loanflow.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository       userRepository;
    private final JwtService           jwtService;
    private final PasswordEncoder      passwordEncoder;
    private final AuthenticationManager authenticationManager;

    private static final int    MAX_FAILED_ATTEMPTS    = 5;
    private static final int    LOCK_DURATION_MINUTES  = 30;

    // ══════════════════════════════════════════════════════════
    // REGISTRATION
    // ══════════════════════════════════════════════════════════

    @Transactional
    public AuthResponse register(RegisterRequest request,
                                 String deviceInfo,
                                 String ipAddress) {

        /**
         * Check uniqueness BEFORE trying to insert.
         * Yes, the DB unique constraint also catches duplicates.
         * But DB constraint throws a generic DataIntegrityViolationException
         * that's hard to parse into a clean user-facing message.
         *
         * This explicit check gives us: "Email already registered"
         * instead of: "Duplicate entry for key 'uq_users_email'"
         *
         * Race condition: two requests check simultaneously → both pass →
         * second insert fails with DataIntegrityViolationException.
         * We catch that in GlobalExceptionHandler and return 409 Conflict.
         * Belt-and-suspenders approach.
         */
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new LoanFlowException(
                    "Email already registered: " + request.getEmail(),
                    "EMAIL_ALREADY_EXISTS",
                    HttpStatus.CONFLICT);
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new LoanFlowException(
                    "Phone number already registered",
                    "PHONE_ALREADY_EXISTS",
                    HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase().trim())
                /**
                 * passwordEncoder.encode() uses BCrypt with cost factor 12.
                 * Cost factor 12 = 2^12 = 4096 iterations.
                 * Takes ~300ms to hash on modern hardware.
                 * That's intentional — makes brute force attacks slow.
                 *
                 * If attacker gets your DB dump:
                 * 1 billion BCrypt hashes/second is impossible.
                 * Real rate: ~100 hashes/second per GPU.
                 * 8-char password space: 96^8 = 7.2 quadrillion combinations.
                 * Time to crack: 7.2 * 10^15 / 100 = 72 trillion seconds.
                 * That's 2.3 million years. BCrypt works.
                 */
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .role(UserRole.APPLICANT)
                .kycStatus(KycStatus.NOT_INITIATED)
                .build();

        userRepository.save(user);
        log.info("New user registered: email={} userId={}", user.getEmail(), user.getId());

        return buildAuthResponse(user, deviceInfo, ipAddress);
    }

    // ══════════════════════════════════════════════════════════
    // LOGIN
    // ══════════════════════════════════════════════════════════

    @Transactional
    public AuthResponse login(LoginRequest request,
                              String deviceInfo,
                              String ipAddress) {

        // Load user first — needed for lockout check before auth attempt
        User user = userRepository.findByEmail(
                        request.getEmail().toLowerCase().trim())
                .orElseThrow(() ->
                        /**
                         * IMPORTANT: Same exception message for wrong email AND wrong password.
                         * "Invalid email or password" — not "Email not found."
                         *
                         * Why? User enumeration attack prevention.
                         * If we say "Email not found" for wrong email:
                         *   Attacker knows email doesn't exist → tries next one
                         *   Can enumerate all registered emails
                         *   Sells email list or uses for phishing
                         *
                         * "Invalid email or password" reveals nothing.
                         */
                        new BadCredentialsException("Invalid email or password"));

        // Check account lock BEFORE authentication attempt
        if (!user.isAccountNonLocked()) {
            if (user.getLockedUntil() != null
                    && LocalDateTime.now().isBefore(user.getLockedUntil())) {
                throw new LockedException(
                        "Account locked until " + user.getLockedUntil() +
                                ". Too many failed login attempts.");
            } else {
                // Lock period expired — auto-unlock
                user.setAccountNonLocked(true);
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        }

        try {
            /**
             * authenticationManager.authenticate() does:
             * 1. Calls UserDetailsServiceImpl.loadUserByUsername(email)
             * 2. Compares provided password with stored BCrypt hash
             * 3. Throws BadCredentialsException if mismatch
             * 4. Returns Authentication object if match
             *
             * We use AuthenticationManager (not PasswordEncoder.matches directly)
             * because it goes through Spring Security's full auth pipeline,
             * including disabled/locked account checks we configured in SecurityConfig.
             */
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase().trim(),
                            request.getPassword()));

        } catch (BadCredentialsException e) {
            handleFailedLoginAttempt(user);
            // Same message always — don't leak which part was wrong
            throw new BadCredentialsException("Invalid email or password");
        }

        // Successful login — reset failed attempts
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User logged in: userId={} ip={}", user.getId(), ipAddress);
        return buildAuthResponse(user, deviceInfo, ipAddress);
    }

    // ══════════════════════════════════════════════════════════
    // TOKEN REFRESH
    // ══════════════════════════════════════════════════════════

    @Transactional
    public AuthResponse refreshToken(String refreshTokenValue,
                                     String deviceInfo,
                                     String ipAddress) {

        // rotateRefreshToken handles: validation, reuse detection, rotation
        RefreshToken newRefreshToken = jwtService.rotateRefreshToken(
                refreshTokenValue, deviceInfo, ipAddress);

        User user = userRepository.findById(newRefreshToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = jwtService.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresInMs(900000L)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .kycStatus(user.getKycStatus().name())
                .build();
    }

    // ══════════════════════════════════════════════════════════
    // LOGOUT
    // ══════════════════════════════════════════════════════════

    @Transactional
    public void logout(String refreshTokenValue) {
        /**
         * Logout = revoke the refresh token.
         * The access token is still technically valid until it expires (15 min).
         *
         * Options to invalidate access token immediately:
         * 1. Redis blocklist: store tokenId in Redis with TTL=15min
         *    Gateway checks blocklist on every request.
         *    Cost: Redis lookup on every request.
         *
         * 2. Short expiry (5 min): accept 5-min window of risk.
         *    No Redis needed. Simple.
         *
         * 3. Stateful tokens: every token validated against DB.
         *    Defeats purpose of JWT (stateless).
         *
         * LoanFlow uses option 2 (15 min expiry, accept the window).
         * For banking-grade: implement Redis blocklist.
         */
        jwtService.revokeToken(refreshTokenValue);
        log.info("User logged out — refresh token revoked");
    }

    // ══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════

    private void handleFailedLoginAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountNonLocked(false);
            user.setLockedUntil(
                    LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            log.warn("Account locked: userId={} attempts={}", user.getId(), attempts);
        }

        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user,
                                           String deviceInfo,
                                           String ipAddress) {
        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = jwtService.generateRefreshToken(
                user, deviceInfo, ipAddress);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresInMs(900000L)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .kycStatus(user.getKycStatus().name())
                .build();
    }
}