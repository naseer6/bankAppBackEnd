package nl.inholland.bankAppBackEnd.Controllers;

import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    // Approve a user by admin
    @PostMapping("/approve/{userId}")
    public ResponseEntity<String> approveUser(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Check if user is a regular user and not already approved
            if (user.getRole() == User.Role.USER && !user.isApproved()) {
                user.setApproved(true); // Mark user as approved
                userRepository.save(user);
                return ResponseEntity.ok("User approved successfully.");
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User is already approved or is not eligible for approval.");
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }
    }

    @GetMapping("/unapproved-users")
    public ResponseEntity<?> getUnapprovedUsers() {
        List<User> unapprovedUsers = userRepository.findAll()
                .stream()
                .filter(user -> !user.isApproved() && user.getRole() == User.Role.USER)
                .toList();

        return ResponseEntity.ok(unapprovedUsers);
    }

}
