package nl.inholland.bankAppBackEnd.Controllers;

import nl.inholland.bankAppBackEnd.config.JwtUtil;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userService.emailExists(user.getEmail())) {
            return ResponseEntity.status(409).body("❌ Email is already in use");
        }

        if (userService.usernameExists(user.getUsername())) {
            return ResponseEntity.status(409).body("❌ Username is already in use");
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
        String username = creds.get("username");
        String password = creds.get("password");

        Optional<User> optionalUser = userService.getUserByUsername(username);

        if (optionalUser.isEmpty() || !optionalUser.get().getPassword().equals(password)) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        User user = optionalUser.get();

        String token = jwtUtil.generateToken(user.getUsername());

        // Send token no matter if approved or not
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


    @GetMapping("/test")
    public String test() {
        return "✅ Security config is working";
    }

    @GetMapping("/foo")
    public String foo() {
        return "foo";
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
}
