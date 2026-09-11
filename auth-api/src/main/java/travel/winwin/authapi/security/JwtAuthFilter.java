package travel.winwin.authapi.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads "Authorization: Bearer &lt;token&gt;", verifies the JWT and, if it is valid, marks the request as authenticated.
 *
 * <p>The filter never rejects a request on its own. It only fills the SecurityContext; the rules in
 * SecurityConfig decide afterwards, and a protected endpoint without an authenticated user gets 401.
 *
 * <p>Deliberately not a {@code @Component}: Spring Boot would then also register it as a plain servlet filter
 * outside the security chain. SecurityConfig creates it and places it at the right spot in the chain instead.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                AuthenticatedUser user = jwtService.parseToken(token);
                // No credentials and no roles: a valid token is all this API requires.
                var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                // Log the reason, never the token itself.
                log.debug("Ignoring invalid JWT: {}", e.getClass().getSimpleName());
            }
        }

        filterChain.doFilter(request, response);
    }
}
