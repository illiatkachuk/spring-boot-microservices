package travel.winwin.authapi.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import travel.winwin.authapi.dto.ProcessRequest;
import travel.winwin.authapi.dto.ProcessResponse;
import travel.winwin.authapi.security.AuthenticatedUser;
import travel.winwin.authapi.service.ProcessService;

@RestController
@RequestMapping("/api")
public class ProcessController {

    private final ProcessService processService;

    public ProcessController(ProcessService processService) {
        this.processService = processService;
    }

    /**
     * Protected endpoint: without a valid JWT, Spring Security answers 401 before this method is reached.
     * {@code @AuthenticationPrincipal} injects the AuthenticatedUser that JwtAuthFilter put into the SecurityContext.
     */
    @PostMapping("/process")
    public ProcessResponse process(@AuthenticationPrincipal AuthenticatedUser user,
                                   @Valid @RequestBody ProcessRequest request) {
        return new ProcessResponse(processService.process(user.id(), request.text()));
    }
}
