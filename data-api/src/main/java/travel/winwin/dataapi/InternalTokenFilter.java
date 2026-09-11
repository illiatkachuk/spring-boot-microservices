package travel.winwin.dataapi;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lets a request through only if it carries the shared secret in the X-Internal-Token header.
 *
 * <p>This is a servlet filter, so it runs before Spring MVC picks a controller or parses the body:
 * a caller without the right token gets 403 no matter which URL or payload it sends.
 * Spring Boot registers any {@code @Component} filter for all URLs automatically.
 */
@Component
public class InternalTokenFilter extends OncePerRequestFilter {

    static final String HEADER_NAME = "X-Internal-Token";

    private static final Logger log = LoggerFactory.getLogger(InternalTokenFilter.class);

    private final byte[] expectedToken;

    public InternalTokenFilter(@Value("${internal.token}") String internalToken) {
        if (internalToken.isBlank()) {
            // Fail at startup instead of accepting requests with an empty header.
            throw new IllegalStateException("INTERNAL_TOKEN must not be blank");
        }
        this.expectedToken = internalToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String providedToken = request.getHeader(HEADER_NAME);

        if (providedToken == null || !isExpectedToken(providedToken)) {
            // Log only the fact of rejection, never the header value.
            log.warn("Rejected {} {}: missing or invalid {} header",
                    request.getMethod(), request.getRequestURI(), HEADER_NAME);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExpectedToken(String providedToken) {
        // Unlike String.equals, MessageDigest.isEqual doesn't stop at the first differing byte,
        // so response timing can't be used to guess the token byte by byte.
        return MessageDigest.isEqual(expectedToken, providedToken.getBytes(StandardCharsets.UTF_8));
    }
}
