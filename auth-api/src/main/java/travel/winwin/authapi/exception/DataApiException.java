package travel.winwin.authapi.exception;

/** data-api could not be reached, timed out, rejected the call or returned an unusable response. */
public class DataApiException extends RuntimeException {

    public DataApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
