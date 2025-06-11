package nl.inholland.bankAppBackEnd.Controllers;
import nl.inholland.bankAppBackEnd.repository.UserRepository;
import nl.inholland.bankAppBackEnd.config.JwtUtil;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userService.emailExists(user.getEmail())) {
            return ResponseEntity.status(409).body("❌ Email is already in use");
        }

        if (userService.usernameExists(user.getUsername())) {
            return ResponseEntity.status(409).body("❌ Username is already in use");
        }
        if (userRepository.findByBsnNumber(user.getBsnNumber()).isPresent()) {
            return ResponseEntity.status(409).body("❌ BSN number is already in use");
        }
        try {
            user.setApproved(false); // All users start unapproved
            User saved = userService.register(user);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Error: " + e.getMessage());
        }


    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> creds) {
        String email = creds.get("email");
        String password = creds.get("password");

        Optional<User> optionalUser = userService.getUserByEmail(email); // ✅ changed from getUserByUsername

        if (optionalUser.isEmpty() || !passwordEncoder.matches(password, optionalUser.get().getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        User user = optionalUser.get();
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().toString()); // still generating based on username

        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", user.getUsername(),
                "role", user.getRole(),
                "approved", user.isApproved(),
                "message", user.isApproved()
                        ? "Login successful"
                        : "Login successful, but your account is not yet approved"
        ));
    }


    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        String username = principal.getName();
        Optional<User> userOpt = userService.getUserByUsername(username);

        if (userOpt.isPresent()) {
            return ResponseEntity.ok(userOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    @PostMapping("/login-email")
    public ResponseEntity<?> loginByEmail(@RequestBody Map<String, String> creds) {
        String email = creds.get("email");
        String password = creds.get("password");

        System.out.println("📥 Login attempt with email: " + email);

        Optional<User> optionalUser = userService.getUserByEmail(email);

        if (optionalUser.isEmpty()) {
            System.out.println("❌ No user found for email: " + email);
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        User user = optionalUser.get();
        System.out.println("✅ Found user: " + user.getUsername());
        System.out.println("🔒 Encrypted password from DB: " + user.getPassword());

        if (!passwordEncoder.matches(password, user.getPassword())) {
            System.out.println("❌ Password mismatch for user: " + user.getUsername());
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().toString());
        System.out.println("🎟️ Generated JWT: " + token);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", user.getUsername(),
                "role", user.getRole(),
                "approved", user.isApproved(),
                "message", user.isApproved()
                        ? "Login successful"
                        : "Login successful, but your account is not yet approved"
        ));
    }




}
