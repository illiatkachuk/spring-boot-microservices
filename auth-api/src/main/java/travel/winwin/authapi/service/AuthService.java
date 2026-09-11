package travel.winwin.authapi.service;

import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import travel.winwin.authapi.entity.User;
import travel.winwin.authapi.exception.EmailAlreadyRegisteredException;
import travel.winwin.authapi.exception.InvalidCredentialsException;
import travel.winwin.authapi.repository.UserRepository;
import travel.winwin.authapi.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // A single constructor needs no @Autowired: Spring passes in the beans it requires.
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public void register(String email, String rawPassword) {
        String normalizedEmail = normalize(email);
        // The UNIQUE constraint on users.email is the final safety net if two requests race past this check.
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }
        // Only the BCrypt hash is stored; the plain password is never saved or logged.
        userRepository.save(new User(normalizedEmail, passwordEncoder.encode(rawPassword)));
    }

    /** Returns a signed JWT if the email/password pair is correct. */
    public String login(String email, String rawPassword) {
        User user = userRepository.findByEmail(normalize(email))
                // matches() re-hashes the given password with the salt stored inside the hash and compares.
                .filter(found -> passwordEncoder.matches(rawPassword, found.getPasswordHash()))
                // Same error for "no such user" and "wrong password", so the API doesn't reveal which emails exist.
                .orElseThrow(InvalidCredentialsException::new);
        return jwtService.generateToken(user);
    }

    /** "A@A.com" and "a@a.com" are the same account. */
    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
