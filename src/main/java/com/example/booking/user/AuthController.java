package com.example.booking.user;

import com.example.booking.security.JwtService;
import com.example.booking.user.dto.LoginRequest;
import com.example.booking.user.dto.RegisterRequest;
import com.example.booking.user.dto.TokenResponse;
import com.example.booking.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(UserService userService,
                         AuthenticationManager authenticationManager,
                         JwtService jwtService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        User user = userService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        // Throws BadCredentialsException (→ 401 ProblemDetail) on failure.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        User user = userService.getByEmail(req.email());
        String token = jwtService.issue(user.getEmail(), user.getRole().name());
        return TokenResponse.bearer(token, jwtService.expiresInSeconds());
    }
}

/*
 * ============================================================================
 * FILE ROLE: REST endpoints for registration and login.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - POST /api/auth/register: validates the body, creates a user, returns 201.
 *   - POST /api/auth/login: authenticates the credentials, then issues a JWT.
 *
 * TECHNICAL CONCEPTS
 *   - @RestController + @RequestMapping expose HTTP endpoints returning JSON.
 *   - @Valid triggers Bean Validation on the request record BEFORE the method
 *     runs; failures become a 400 via GlobalExceptionHandler.
 *   - Login delegates to the AuthenticationManager (DAO provider + BCrypt); a bad
 *     password throws BadCredentialsException -> 401. On success JwtService mints
 *     the token. These endpoints are public (permitAll in SecurityConfig).
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * These are the two "front-desk windows" for accounts:
 *   - REGISTER: make a new account. It checks your details and creates the user.
 *   - LOGIN:    prove who you are. It checks your password and, if correct, hands
 *               you your pass (the token) to use from then on.
 *
 * Both windows are open to everyone - which makes sense, because you obviously
 * cannot already be logged in when you are trying to register or log in.
 * ============================================================================
 */
