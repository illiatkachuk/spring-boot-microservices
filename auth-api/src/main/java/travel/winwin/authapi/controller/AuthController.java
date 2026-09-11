package travel.winwin.authapi.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import travel.winwin.authapi.dto.CredentialsRequest;
import travel.winwin.authapi.dto.TokenResponse;
import travel.winwin.authapi.service.AuthService;

/** Public endpoints (permitAll in SecurityConfig). Controllers stay thin: validate input, call the service. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody CredentialsRequest request) {
        authService.register(request.email(), request.password());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody CredentialsRequest request) {
        return new TokenResponse(authService.login(request.email(), request.password()));
    }
}
