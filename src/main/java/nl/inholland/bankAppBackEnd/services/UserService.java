package nl.inholland.bankAppBackEnd.services;

import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Registers a new user with encrypted password.
     * By default, customers start as unapproved, while employees are approved automatically.
     * @param user User to register
     * @return Saved user entity
     */
    public User register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword())); // Encrypt password
        if (user.getRole() == User.Role.CUSTOMER) {
            user.setApproved(false); // Customers need approval
        } else {
            user.setApproved(true); // Employees are approved by default
        }
        return userRepository.save(user);
    }

    /**
     * Updates an existing user.
     * @param user User to update
     * @return Updated user entity
     */
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Checks if an email already exists in the system.
     * @param email Email to check
     * @return True if email exists, false otherwise
     */
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    /**
     * Finds a user by email.
     * @param email Email to search for
     * @return User if found, null otherwise
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * Checks if a username already exists in the system.
     * @param username Username to check
     * @return True if username exists, false otherwise
     */
    public boolean usernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    /**
     * Gets a user by username.
     * @param username Username to search for
     * @return Optional containing the user if found
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Gets a user by email.
     * @param email Email to search for
     * @return Optional containing the user if found
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Save a user entity.
     * @param user User to save
     * @return Saved user entity
     */
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Loads a user by username or email for Spring Security authentication.
     * @param usernameOrEmail Username or email to search for
     * @return UserDetails for Spring Security
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Try to find by username first, then by email
        Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(usernameOrEmail);
        }

        User user = userOpt.orElseThrow(() ->
            new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail));

        // Only allow approved users to authenticate
        if (!user.isApproved()) {
            throw new UsernameNotFoundException("User account not approved yet");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), // Use email as the principal
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }

    /**
     * Deletes a user by username.
     * @param username Username of the user to delete
     */
    public void deleteUserByUsername(String username) {
        userRepository.findByUsername(username).ifPresent(userRepository::delete);
    }

    /**
     * Finds a user by ID.
     * @param id ID to search for
     * @return Optional containing the user if found
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Finds all unapproved customers.
     * @return List of unapproved customers
     */
    public List<User> findUnapprovedCustomers() {
        return userRepository.findAll().stream()
                .filter(user -> !user.isApproved() && user.getRole() == User.Role.CUSTOMER)
                .toList();
    }

    /**
     * Finds all users.
     * @return List of all users
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Approves a user account.
     * @param user User to approve
     * @return Updated user entity
     */
    public User approveUser(User user) {
        user.setApproved(true);
        return userRepository.save(user);
    }

    public boolean bsnNumberExists(String bsnNumber) {
        return userRepository.findByBsnNumber(bsnNumber).isPresent();
    }

    /**
     * Verifies if a plain text password matches the encoded password.
     * @param plainPassword The plain text password to check
     * @param encodedPassword The encoded password to check against
     * @return true if the password matches, false otherwise
     */
    public boolean verifyPassword(String plainPassword, String encodedPassword) {
        return passwordEncoder.matches(plainPassword, encodedPassword);
    }
}
