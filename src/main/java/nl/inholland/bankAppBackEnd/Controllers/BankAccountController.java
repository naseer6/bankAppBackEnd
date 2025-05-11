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
        Optional<User> user = userRepository.findById(userId);

        if (user.isEmpty()) {
            return "❌ User not found.";
        }

        if (!user.get().getRole().equals(User.Role.USER)) {
            return "❌ Account can only be created for USER roles.";
        }

        BankAccount account = bankAccountService.createAccountForUser(user.get());
        return account;
    }
}
