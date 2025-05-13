package nl.inholland.bankAppBackEnd.Controllers;

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

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userService.emailExists(user.getEmail())) {
            return ResponseEntity.status(409).body("❌ Email is already in use");
        }

        if (userService.usernameExists(user.getUsername())) {
            return ResponseEntity.status(409).body("❌ Username is already in use");
        }

        try {
            user.setApproved(false); // Default to unapproved
            User saved = userService.register(user);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Error: " + e.getMessage());
        }
    }

    @GetMapping("/test")
    public String test() {
        return "✅ Security config is working";
    }

    @GetMapping("/foo")
    public String foo() {
        return "foo";
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        Optional<User> optionalUser = userService.login(username, password);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            // Instead of rejecting login, let them login but show they are unapproved
            if (!user.isApproved()) {
                return ResponseEntity.ok("Login successful! However, your account is awaiting approval by an admin.");
            }

            // If approved, continue with login flow (send JWT or session token here)
            return ResponseEntity.ok("Login successful!");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }


    // Use Principal to get the current authenticated user
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // User is not authenticated
        }

        String username = principal.getName(); // Get the username from the Principal object
        Optional<User> userOpt = userService.getUserByUsername(username); // Find the user by username

        if (userOpt.isPresent()) {
            return ResponseEntity.ok(userOpt.get()); // Return user details if found
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // Return Not Found if user doesn't exist
        }
    }
}
