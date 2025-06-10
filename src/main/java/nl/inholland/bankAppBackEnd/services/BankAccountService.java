package nl.inholland.bankAppBackEnd.services;

import jakarta.transaction.Transactional;
import nl.inholland.bankAppBackEnd.DTOs.AccountSearchResultDTO;
import nl.inholland.bankAppBackEnd.DTOs.BankAccountDTO;
import nl.inholland.bankAppBackEnd.DTOs.DashboardStatsDTO;
import nl.inholland.bankAppBackEnd.exceptions.ResourceNotFoundException;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import nl.inholland.bankAppBackEnd.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BankAccountService {

    @Autowired
    private BankAccountRepository bankAccountRepository;
    @Autowired
    private UserRepository userRepository;



    private final TransactionService transactionService;

    @Autowired
    public BankAccountService(
            BankAccountRepository bankAccountRepository,
            UserRepository userRepository,
            TransactionService transactionService) { // Update constructor
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService; // Inject TransactionService
    }

    public BankAccount createAccountForUser(User user) {
        BankAccount account = new BankAccount();
        account.setOwner(user);
        account.setBalance(0.0);
        account.setIban(generateIban());
        account.setAbsoluteLimit(0.0); // Default absolute limit
        account.setDailyLimit(1000.0); // Default daily limit

        return bankAccountRepository.save(account);
    }

    public List<BankAccount> createAccountsForUser(User user) {
        return createAccountsForUserWithLimits(user, 0.0, 1000.0);
    }

    public List<BankAccount> createAccountsForUserWithLimits(User user, Double absoluteLimit, Double dailyLimit) {
        BankAccount checking = new BankAccount();
        checking.setIban(generateIban());
        checking.setBalance(0.0);
        checking.setOwner(user);
        checking.setType(BankAccount.AccountType.CHECKING);
        checking.setAbsoluteLimit(absoluteLimit);
        checking.setDailyLimit(dailyLimit);


        BankAccount savings = new BankAccount();
        savings.setIban(generateIban());
        savings.setBalance(0.0);
        savings.setOwner(user);
        savings.setType(BankAccount.AccountType.SAVINGS);
        savings.setAbsoluteLimit(absoluteLimit);
        savings.setDailyLimit(0.0); // Savings accounts typically don't allow transfers


        bankAccountRepository.save(checking);
        bankAccountRepository.save(savings);

        return List.of(checking, savings);
    }

    private String generateIban() {
        // Generate a unique IBAN - check if it already exists
        String iban;
        do {
            iban = "NL" + String.format("%02d", new Random().nextInt(100)) +
                    "INHO" + String.format("%010d", new Random().nextInt(1000000000));
        } while (bankAccountRepository.findByIban(iban).isPresent());

        return iban;
    }

    public List<BankAccount> getByOwner(User owner) {
        return bankAccountRepository.findAllByOwner(owner);
    }

    public void save(BankAccount account) {
        bankAccountRepository.save(account);
    }

    public List<BankAccount> getAllAccounts() {
        return bankAccountRepository.findAll();
    }

    public Optional<BankAccount> getAccountById(Long accountId) {
        return bankAccountRepository.findById(accountId);
    }



    public int getActiveAccountsCount() {
        return (int) bankAccountRepository.findAll()
                .stream()
                .filter(BankAccount::isActive)
                .count();
    }

    public Map<String, Object> getLimitsForUser(String iban, User currentUser) {
        BankAccount account = bankAccountRepository.findByIban(iban)
                .orElseThrow(() -> new NoSuchElementException("❌ Account not found"));

        if (currentUser.getRole() == User.Role.USER &&
                !account.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("❌ You can only view your own account limits");
        }

        Map<String, Object> limits = new HashMap<>();
        limits.put("iban", account.getIban());
        limits.put("absoluteLimit", account.getAbsoluteLimit());
        limits.put("dailyLimit", account.getDailyLimit());
        limits.put("dailySpent", account.getDailySpent());
        limits.put("remainingDailyLimit", account.getRemainingDailyLimit());
        limits.put("balance", account.getBalance());
        limits.put("availableBalance", Math.max(0, account.getBalance() - account.getAbsoluteLimit()));

        return limits;
    }




    /**
     * Find accounts by owner name (case-insensitive partial match)
     *
     * @param name The name to search for
     * @return List of accounts matching the search criteria
     * @throws IllegalArgumentException if name is null or empty
     * @throws ResourceNotFoundException if no users or accounts found
     */
    public List<AccountSearchResultDTO> findAccountsByOwnerName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name parameter cannot be empty");
        }

        // Get users matching the name
        List<User> matchingUsers = userRepository.findByNameContainingIgnoreCase(name);

        if (matchingUsers.isEmpty()) {
            throw new ResourceNotFoundException("No users found with name: " + name);
        }

        // Use Java 8 streams to efficiently process the data
        List<AccountSearchResultDTO> results = matchingUsers.stream()
                .flatMap(user -> bankAccountRepository.findAllByOwner(user).stream()
                        .map(account -> new AccountSearchResultDTO(
                                user.getName(),
                                account.getIban(),
                                account.getType().toString()
                        )))
                .collect(Collectors.toList());

        if (results.isEmpty()) {
            throw new ResourceNotFoundException("No accounts found for users with name: " + name);
        }

        return results;
    }

    /**
     * Get all accounts as DTOs
     */
    public List<BankAccountDTO> getAllAccountDTOs() {
        return getAllAccounts().stream()
                .map(BankAccountDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get account DTO by ID
     */
    public Optional<BankAccountDTO> getAccountDTOById(Long accountId) {
        return getAccountById(accountId).map(BankAccountDTO::fromEntity);
    }

    /**
     * Check if account exists
     */
    public boolean accountExists(Long accountId) {
        return bankAccountRepository.existsById(accountId);
    }

    /**
     * Close an account with better error handling
     */
    @Transactional
    public void closeAccount(Long accountId) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));

        // Check if the account has zero balance
        if (account.getBalance() != 0.0) {
            throw new IllegalStateException("Cannot close account with non-zero balance");
        }

        account.setActive(false);
        bankAccountRepository.save(account);
    }

    /**
     * Get dashboard statistics
     */
    public DashboardStatsDTO getDashboardStats() {
        int pendingApprovals = (int) userRepository.findAll()
                .stream()
                .filter(user -> !user.isApproved() && user.getRole() == User.Role.USER)
                .count();

        int totalUsers = (int) userRepository.count();
        int activeAccounts = getActiveAccountsCount();
        // Call the method on the instance, not statically
        int todayTransactions = transactionService.getTodayTransactionsCount();

        return new DashboardStatsDTO(pendingApprovals, totalUsers, activeAccounts, todayTransactions);
    }
}


