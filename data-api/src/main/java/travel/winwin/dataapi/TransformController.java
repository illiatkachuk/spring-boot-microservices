package travel.winwin.dataapi;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class TransformController {

    /**
     * Reverses the input text: "hello" -> "olleh".
     * InternalTokenFilter has already verified X-Internal-Token before this method is called.
     */
    @PostMapping("/transform")
    public TransformResponse transform(@RequestBody TextRequest request) {
        if (request.text() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text is required");
        }
        // StringBuilder.reverse keeps surrogate pairs (e.g. emoji) intact.
        String reversed = new StringBuilder(request.text()).reverse().toString();
        return new TransformResponse(reversed);
    }
}
