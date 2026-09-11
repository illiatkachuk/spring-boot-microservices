package travel.winwin.authapi.exception;

import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import travel.winwin.authapi.dto.ApiError;

/** Turns exceptions thrown anywhere in the controllers/services into HTTP status codes with a JSON body. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleEmailAlreadyRegistered(EmailAlreadyRegisteredException e) {
        return new ApiError(e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError handleInvalidCredentials(InvalidCredentialsException e) {
        return new ApiError(e.getMessage());
    }

    /** data-api is down, timed out or rejected our internal token: the failure is upstream, hence 502 Bad Gateway. */
    @ExceptionHandler(DataApiException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiError handleDataApiFailure(DataApiException e) {
        return new ApiError("Processing service is unavailable");
    }

    /** @Valid failed: say which fields are wrong, but never echo submitted values (one of them may be a password). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return new ApiError(details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleUnreadableBody(HttpMessageNotReadableException e) {
        return new ApiError("Request body is missing or is not valid JSON");
    }
}
