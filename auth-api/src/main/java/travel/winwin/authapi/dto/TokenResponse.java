package travel.winwin.authapi.dto;

/** Response of /api/auth/login: { "token": "..." }. */
public record TokenResponse(String token) {

    @Override
    public String toString() {
        return "TokenResponse[token=***]";
    }
}
