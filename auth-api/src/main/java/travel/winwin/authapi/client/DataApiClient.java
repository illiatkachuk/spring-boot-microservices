package travel.winwin.authapi.client;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import travel.winwin.authapi.exception.DataApiException;

/** HTTP client for the internal data-api service (Service B). */
@Component
public class DataApiClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private static final Logger log = LoggerFactory.getLogger(DataApiClient.class);

    private final RestClient restClient;

    public DataApiClient(RestClient.Builder builder,
                         @Value("${data-api.url}") String baseUrl,
                         @Value("${data-api.internal-token}") String internalToken) {
        // Without timeouts a hung data-api would make /api/process hang forever as well.
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                // Added to every request made by this client; the value comes from the INTERNAL_TOKEN env variable.
                .defaultHeader(INTERNAL_TOKEN_HEADER, internalToken)
                .build();
    }

    /** POST /api/transform with { "text": ... } and return the "result" field of the response. */
    public String transform(String text) {
        TransformResponse response;
        try {
            response = restClient.post()
                    .uri("/api/transform")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TransformRequest(text))
                    .retrieve() // a 4xx/5xx answer from data-api is thrown as a RestClientException
                    .body(TransformResponse.class);
        } catch (RestClientException e) {
            // The message contains the URL and status (e.g. "403 Forbidden"), never our request headers.
            log.error("Call to data-api failed: {}", e.getMessage());
            throw new DataApiException("data-api call failed", e);
        }

        if (response == null || response.result() == null) {
            throw new DataApiException("data-api returned an empty response", null);
        }
        return response.result();
    }

    /** data-api's request/response JSON contract, used only by this client. */
    record TransformRequest(String text) {
    }

    record TransformResponse(String result) {
    }
}
