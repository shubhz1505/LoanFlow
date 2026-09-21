package com.loanflow.user.security;

import com.loanflow.user.entity.RefreshToken;
import com.loanflow.user.entity.User;
import com.loanflow.user.repository.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Constructor injection instead of @Autowired field injection.
     * Why constructor injection?
     * 1. Fields are final — immutable after construction (thread-safe)
     * 2. Easier to unit test — just call new JwtService(mockRepo)
     * 3. Fails fast at startup if dependency missing (not at first use)
     * 4. No reflection needed (Spring uses constructor directly)
     */
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry-ms}") long accessTokenExpiryMs,
            @Value("${jwt.refresh-token-expiry-ms}") long refreshTokenExpiryMs,
            RefreshTokenRepository refreshTokenRepository) {

        /**
         * Keys.hmacShaKeyFor() creates an HMAC-SHA key from the secret bytes.
         * HMAC-SHA256 requires minimum 256-bit (32-byte) key.
         * Our secret is 64+ chars = 64+ bytes → satisfies requirement.
         *
         * Why not RS256 (asymmetric)?
         * RS256: private key signs → public key verifies
         * Advantage: Gateway can verify without knowing the private key
         * Disadvantage: Key management complexity, slower signing
         *
         * HS256: same secret key signs AND verifies
         * Advantage: Simple, fast
         * Disadvantage: Every service that verifies needs the secret
         * Solution: Only Gateway verifies. Services trust Gateway headers.
         * → HS256 is correct choice for our architecture.
         */
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // ══════════════════════════════════════════════════════════
    // ACCESS TOKEN
    // ══════════════════════════════════════════════════════════

    public String generateAccessToken(User user) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiryMs);

        return Jwts.builder()
                /**
                 * subject = the "who" this token is for.
                 * We use userId (UUID) not email.
                 * Why not email? Email can change. UUID never changes.
                 * If user updates email, old tokens still valid until expiry.
                 * subject = userId → always resolves to correct user.
                 */
                .subject(user.getId())

                /**
                 * Custom claims — additional data embedded in token.
                 * Gateway extracts these and stamps as headers.
                 * Downstream services read headers — no DB lookup needed.
                 *
                 * What to put in claims:
                 * ✅ Role — needed for authorization in every service
                 * ✅ KYC status — Loan Service checks this before accepting application
                 * ✅ Email — Notification Service needs it for emails
                 *
                 * What NOT to put in claims:
                 * ❌ Password (obviously)
                 * ❌ Full address (PII — token can be decoded client-side)
                 * ❌ Credit score (sensitive financial data)
                 * ❌ Large objects — token is sent in every request header
                 *    Keep it under 4KB (HTTP header size limit)
                 */
                .claim("email",     user.getEmail())
                .claim("role",      user.getRole().name())
                .claim("kycStatus", user.getKycStatus().name())
                .claim("fullName",  user.getFullName())

                .issuedAt(now)
                .expiration(expiry)

                /**
                 * signWith(secretKey): signs the token with HMAC-SHA256.
                 * The signature prevents tampering.
                 * If anyone modifies the payload, signature verification fails.
                 * Token is REJECTED even if expiry hasn't passed.
                 */
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            /**
             * JwtException covers:
             * - ExpiredJwtException: token past expiry
             * - MalformedJwtException: not a valid JWT structure
             * - SignatureException: signature doesn't match (tampered)
             * - UnsupportedJwtException: wrong algorithm
             *
             * We log WARN not ERROR — invalid tokens are normal
             * (expired tokens happen constantly in production).
             * ERROR would flood your alert system.
             */
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ══════════════════════════════════════════════════════════
    // REFRESH TOKEN
    // ══════════════════════════════════════════════════════════

    @Transactional
    public RefreshToken generateRefreshToken(User user,
                                             String deviceInfo,
                                             String ipAddress) {
        RefreshToken token = RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                /**
                 * Token value: UUID v4 = 122 bits of randomness.
                 * Unguessable by brute force.
                 * Not a JWT — no expiry embedded, no claims.
                 * It's an opaque reference that maps to DB record.
                 * DB record has the expiry.
                 *
                 * Why not make refresh token a JWT?
                 * If refresh token is JWT with embedded expiry:
                 *   You cannot revoke it before expiry.
                 *   Stolen token valid until expiry (7 days of access).
                 *
                 * With DB-backed opaque token:
                 *   Set revoked=true in DB → token invalid immediately.
                 *   Zero-delay revocation.
                 */
                .token(UUID.randomUUID().toString())
                .userId(user.getId())
                .expiresAt(LocalDateTime.now()
                        .plusSeconds(refreshTokenExpiryMs / 1000))
                .revoked(false)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .build();

        return refreshTokenRepository.save(token);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(String oldTokenValue,
                                           String deviceInfo,
                                           String ipAddress) {

        RefreshToken oldToken = refreshTokenRepository
                .findByToken(oldTokenValue)
                .orElseThrow(() ->
                        new RuntimeException("Refresh token not found"));

        /**
         * REUSE DETECTION — the critical security check.
         *
         * Scenario: Token stolen.
         * 1. Attacker uses stolen token → gets new token pair
         * 2. Old token marked revoked in DB
         * 3. Real user tries to use their (now revoked) token
         * 4. We detect: token exists but revoked=true
         * 5. This means: token was already used by someone else
         * 6. Action: revoke ALL tokens for this user
         * 7. Force re-login → attacker immediately locked out
         *
         * This is called "Refresh Token Rotation with Reuse Detection"
         * Standard security practice (OAuth 2.0 security BCP).
         */
        if (oldToken.isRevoked()) {
            log.warn("SECURITY ALERT: Refresh token reuse detected for user {}. " +
                    "Revoking all tokens.", oldToken.getUserId());
            refreshTokenRepository.revokeAllByUserId(oldToken.getUserId());
            throw new RuntimeException(
                    "Refresh token already used. Possible token theft. " +
                            "Please login again.");
        }

        if (oldToken.isExpired()) {
            throw new RuntimeException("Refresh token expired. Please login again.");
        }

        // Revoke old token
        oldToken.setRevoked(true);
        oldToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(oldToken);

        // Issue new refresh token
        User user = new User();
        user.setId(oldToken.getUserId());
        return generateRefreshToken(user, deviceInfo, ipAddress);
    }

    @Transactional
    public void revokeToken(String tokenValue) {
        refreshTokenRepository.findByToken(tokenValue)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    token.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(token);
                });
    }
}