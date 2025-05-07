package nl.inholland.bankAppBackEnd.Controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public String home() {
        return "✅ Backend is running! Try /api/users/register or /api/users/login";
    }
}
