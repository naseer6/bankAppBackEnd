package nl.inholland.bankAppBackEnd.Controllers;

import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.UserRepository;
import nl.inholland.bankAppBackEnd.services.BankAccountService;
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

    @Autowired
    private BankAccountService bankAccountService;


    @PostMapping("/approve/{userId}")
    public ResponseEntity<String> approveUser(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (user.getRole() == User.Role.USER && !user.isApproved()) {
                user.setApproved(true);
                userRepository.save(user);

                // 🏦 Create bank account after approval
                bankAccountService.createAccountsForUser(user);


                return ResponseEntity.ok("User approved and bank account created.");
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("User is already approved or not eligible.");
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
