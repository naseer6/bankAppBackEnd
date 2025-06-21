package nl.inholland.bankAppBackEnd.Controllers;

import nl.inholland.bankAppBackEnd.config.JwtUtil;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication controller for handling user registration and login.
 * Provides endpoints for customer and employee registration and login, issuing JWT tokens on successful authentication.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Registers a new user (customer or employee).
     * Approval is set based on the role: customers require approval, employees and admins are approved by default.
     * @param user User registration details
     * @return Success message or error
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userService.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body("Email already in use.");
        }
        // Set approval based on role
        if (user.getRole() == User.Role.CUSTOMER) {
            user.setApproved(false); // Customers require approval
        } else {
            user.setApproved(true); // Employees/Admins are approved by default
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.save(user);
        return ResponseEntity.ok("Registration successful. Awaiting approval if customer.");
    }

    /**
     * Authenticates a user and issues a JWT token.
     * Only approved users can log in.
     * @param loginRequest Login credentials
     * @return JWT token if authentication is successful
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        User user = userService.findByEmail(loginRequest.get("email"));
        if (user == null) {
            return ResponseEntity.status(401).body("Invalid credentials.");
        }
        if (!user.isApproved()) {
            return ResponseEntity.status(403).body("Account not approved yet.");
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.get("email"),
                        loginRequest.get("password")
                )
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwt = jwtUtil.generateToken(userDetails);
        Map<String, String> response = new HashMap<>();
        response.put("token", jwt);
        return ResponseEntity.ok(response);
    }
}
