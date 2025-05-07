package nl.inholland.bankAppBackEnd.Controllers;

import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User saved = userService.register(user);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace(); // 👈 This logs the real error in console
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
    public String login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        return userService.login(username, password)
                .map(user -> "Login successful! Welcome, " + user.getUsername())
                .orElse("Invalid username or password");
    }
}
