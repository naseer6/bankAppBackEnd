package nl.inholland.bankAppBackEnd.services;

import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void register_ShouldEncodePassword_AndSaveUser() {
        User user = new User();
        user.setPassword("plaintext");

        when(passwordEncoder.encode("plaintext")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.register(user);

        assertThat(result.getPassword()).isEqualTo("hashedPassword");
        assertThat(result.getRole()).isEqualTo(User.Role.USER);
        assertThat(result.isApproved()).isFalse();

        verify(userRepository, times(1)).save(user);
    }

    @Test
    void emailExists_ShouldReturnTrue_WhenEmailExists() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(new User()));

        boolean exists = userService.emailExists("test@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    void emailExists_ShouldReturnFalse_WhenEmailNotFound() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        boolean exists = userService.emailExists("notfound@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    void usernameExists_ShouldReturnTrue_WhenUsernameExists() {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(new User()));

        boolean exists = userService.usernameExists("johndoe");

        assertThat(exists).isTrue();
    }

    @Test
    void getUserByUsername_ShouldReturnUser_WhenFound() {
        User user = new User();
        user.setUsername("john");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        Optional<User> result = userService.getUserByUsername("john");

        assertThat(result).isPresent().contains(user);
    }

    @Test
    void getUserByEmail_ShouldReturnUser_WhenFound() {
        User user = new User();
        user.setEmail("john@example.com");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.getUserByEmail("john@example.com");

        assertThat(result).isPresent().contains(user);
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetails_WhenFound() {
        User user = new User();
        user.setUsername("john");
        user.setPassword("hashedPassword");
        user.setRole(User.Role.USER);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        UserDetails userDetails = userService.loadUserByUsername("john");

        assertThat(userDetails.getUsername()).isEqualTo("john");
        assertThat(userDetails.getPassword()).isEqualTo("hashedPassword");
        assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsername_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }
}
