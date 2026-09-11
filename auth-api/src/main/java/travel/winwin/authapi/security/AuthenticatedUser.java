package travel.winwin.authapi.security;

import java.util.UUID;

/** The logged-in user, taken from a verified JWT and stored in Spring's SecurityContext. */
public record AuthenticatedUser(UUID id, String email) {
}
