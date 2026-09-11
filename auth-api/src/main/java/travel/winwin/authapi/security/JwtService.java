package travel.winwin.authapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import travel.winwin.authapi.entity.User;

/** Creates and verifies JWTs signed with HMAC-SHA256 using the JWT_SECRET key. */
@Service
public class JwtService {

    private static final String EMAIL_CLAIM = "email";

    private final SecretKey signingKey;
    private final Duration tokenLifetime;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        // Throws WeakKeyException at startup if the secret is shorter than 32 bytes (256 bits).
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.tokenLifetime = Duration.ofMinutes(expirationMinutes);
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(EMAIL_CLAIM, user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(tokenLifetime)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Checks the signature and expiration, then returns the user the token was issued to.
     *
     * @throws JwtException if the token is malformed, tampered with, expired or unsigned
     */
    public AuthenticatedUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AuthenticatedUser(UUID.fromString(claims.getSubject()), claims.get(EMAIL_CLAIM, String.class));
    }
}
