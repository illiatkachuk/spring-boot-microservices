package travel.winwin.authapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * UserDetailsServiceAutoConfiguration is excluded because we authenticate users ourselves (AuthService + JWT).
 * Left enabled, it would create a demo in-memory user and print its generated password to the log.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class AuthApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApiApplication.class, args);
    }
}
