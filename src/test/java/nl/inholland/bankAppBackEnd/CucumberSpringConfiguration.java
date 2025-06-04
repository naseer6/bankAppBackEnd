package nl.inholland.bankAppBackEnd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@CucumberContextConfiguration
@SpringBootTest(classes = BankAppBackEndApplication.class)
public class CucumberSpringConfiguration {
    // empty class, just needed for Spring context in Cucumber
}
