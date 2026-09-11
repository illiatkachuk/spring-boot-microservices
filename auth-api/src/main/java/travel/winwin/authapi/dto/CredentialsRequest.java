package travel.winwin.authapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of both /api/auth/register and /api/auth/login: { "email": "...", "password": "..." }. */
public record CredentialsRequest(
        @NotBlank @Email @Size(max = 255) String email,
        // BCrypt only uses the first 72 bytes of a password, and Spring Security rejects longer ones.
        @NotBlank @Size(max = 72) String password) {

    /** A record's default toString() prints every field - keep the password out of any log line. */
    @Override
    public String toString() {
        return "CredentialsRequest[email=" + email + ", password=***]";
    }
}
