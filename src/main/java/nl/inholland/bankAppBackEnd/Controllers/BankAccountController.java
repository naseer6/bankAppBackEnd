package nl.inholland.bankAppBackEnd.Controllers;

import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.UserRepository;
import nl.inholland.bankAppBackEnd.services.BankAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/accounts")
public class BankAccountController {

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/create")
    public Object createAccount(@RequestParam Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            return "❌ User not found.";
        }

        User user = userOpt.get();

        // Check if user is approved
        if (!user.isApproved()) {
            return "❌ Account creation is only allowed for approved users.";
        }

        if (!user.getRole().equals(User.Role.USER)) {
            return "❌ Account can only be created for users with USER role.";
        }

        // If the user is approved, create the bank account
        BankAccount account = bankAccountService.createAccountForUser(user);
        return account;
    }
}

