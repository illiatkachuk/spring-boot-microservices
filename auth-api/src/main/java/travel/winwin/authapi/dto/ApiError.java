package travel.winwin.authapi.dto;

/** Error body returned by GlobalExceptionHandler: { "error": "..." }. */
public record ApiError(String error) {
}
