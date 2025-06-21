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

    /**
     * Generates a valid Dutch IBAN number following the format NLxxINHO0xxxxxxxxx.
     * Ensures each IBAN is unique within the system.
     * @return A unique IBAN number
     */
    private String generateIban() {
        Random random = new Random();
        String countryCode = "NL";
        String bankCode = "INHO0"; // Bank code for Inholland Bank

        // Generate check digits and account number
        String checkDigits = String.format("%02d", random.nextInt(100));
        String accountNumber = String.format("%09d", random.nextInt(1000000000));

        // Combine all parts to form IBAN
        String iban = countryCode + checkDigits + bankCode + accountNumber;

        // Check if this IBAN already exists, if so generate a new one
        if (bankAccountRepository.findByIban(iban).isPresent()) {
            return generateIban(); // Recursive call to generate a unique IBAN
        }

        return iban;
    }

    /**
     * Creates a checking account for a user with specified limits.
     * @param user Account owner
     * @param absoluteLimit Minimum balance allowed
     * @param dailyLimit Maximum daily transfer amount
     * @return Created checking account
     */
    public BankAccount createCheckingAccount(User user, Double absoluteLimit, Double dailyLimit) {
        BankAccount checking = new BankAccount();
        checking.setIban(generateIban());
        checking.setBalance(0.0);
        checking.setOwner(user);
        checking.setType(BankAccount.AccountType.CHECKING);
        checking.setAbsoluteLimit(absoluteLimit);
        checking.setDailyLimit(dailyLimit);

        return bankAccountRepository.save(checking);
    }

    /**
     * Creates a savings account for a user.
     * @param user Account owner
     * @return Created savings account
     */
    public BankAccount createSavingsAccount(User user) {
        BankAccount savings = new BankAccount();
        savings.setIban(generateIban());
        savings.setBalance(0.0);
        savings.setOwner(user);
        savings.setType(BankAccount.AccountType.SAVINGS);
        savings.setAbsoluteLimit(0.0);
        savings.setDailyLimit(0.0); // Savings accounts typically don't allow external transfers

        return bankAccountRepository.save(savings);
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
        BankAccount account = getAccountById(accountId);
        return Optional.of(BankAccountDTO.fromEntity(account));
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
//    @Transactional
//    public void closeAccount(Long accountId) {
//        BankAccount account = bankAccountRepository.findById(accountId)
//                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));
//
//        // Check if the account has zero balance
//        if (account.getBalance() != 0.0) {
//            throw new IllegalStateException("Cannot close account with non-zero balance");
//        }
//
//        account.setActive(false);
//        bankAccountRepository.save(account);
//    }

    /**
     * Get dashboard statistics
     */
    public DashboardStatsDTO getDashboardStats() {
        int pendingApprovals = (int) userRepository.findAll()
                .stream()
                .filter(user -> !user.isApproved() && user.getRole() == User.Role.CUSTOMER)
                .count();

        int totalUsers = (int) userRepository.count();
        int activeAccounts = getActiveAccountsCount();
        // Call the method on the instance, not statically
        int todayTransactions = transactionService.getTodayTransactionsCount();

        return new DashboardStatsDTO(pendingApprovals, totalUsers, activeAccounts, todayTransactions);
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

        if (currentUser.getRole() == User.Role.CUSTOMER &&
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

    public List<BankAccount> getAccountsByOwner(User user) {
        return bankAccountRepository.findAllByOwner(user);
    }



    /**
     * Finds all bank accounts in the system.
     * @return List of all bank accounts
     */
    public List<BankAccount> getAllAccounts() {
        return bankAccountRepository.findAll();
    }

    /**
     * Finds all accounts owned by a specific user.
     * @param user Account owner
     * @return List of accounts owned by the user
     */
    public List<BankAccount> getAccountsByUser(User user) {
        return bankAccountRepository.findAllByOwner(user);
    }

    /**
     * Finds an account by its ID.
     * @param id Account ID
     * @return BankAccount if found
     * @throws ResourceNotFoundException if account not found
     */
    public BankAccount getAccountById(Long id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    /**
     * Finds an account by its IBAN.
     * @param iban IBAN to search for
     * @return BankAccount if found
     * @throws ResourceNotFoundException if account not found
     */
    public BankAccount getAccountByIban(String iban) {
        return bankAccountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with IBAN: " + iban));
    }

    /**
     * Sets the absolute limit for an account.
     * @param accountId Account ID
     * @param limit New absolute limit
     * @return Updated account
     */
    public BankAccount setAbsoluteLimit(Long accountId, Double limit) {
        BankAccount account = getAccountById(accountId);
        account.setAbsoluteLimit(limit);
        return bankAccountRepository.save(account);
    }

    /**
     * Sets the daily limit for an account.
     * @param accountId Account ID
     * @param limit New daily limit
     * @return Updated account
     */
    public BankAccount setDailyLimit(Long accountId, Double limit) {
        BankAccount account = getAccountById(accountId);
        account.setDailyLimit(limit);
        return bankAccountRepository.save(account);
    }

    /**
     * Closes an account by deleting it from the system.
     * Checks that the account has zero balance before closing.
     * @param accountId Account ID
     * @throws IllegalStateException if account has non-zero balance
     */
    public void closeAccount(Long accountId) {
        BankAccount account = getAccountById(accountId);

        if (account.getBalance() != 0) {
            throw new IllegalStateException("Account must have zero balance before closing");
        }

        bankAccountRepository.delete(account);
    }

    /**
     * Searches for accounts by user name.
     * Used for finding IBANs based on customer name.
     * @param name Name to search for
     * @return List of matching accounts with owner and IBAN information
     */
    public List<AccountSearchResultDTO> searchAccountsByName(String name) {
        List<User> users = userRepository.findByNameContainingIgnoreCase(name);

        List<AccountSearchResultDTO> results = new ArrayList<>();
        for (User user : users) {
            if (user.isApproved() && user.getRole() == User.Role.CUSTOMER) {
                List<BankAccount> accounts = bankAccountRepository.findAllByOwner(user);
                for (BankAccount account : accounts) {
                    if (account.getType() == BankAccount.AccountType.CHECKING) {
                        results.add(new AccountSearchResultDTO(
                            user.getName(),
                            account.getIban(),
                            account.getType().toString()
                        ));
                    }
                }
            }
        }
        return results;
    }

    /**
     * Checks if a transaction would exceed limits and updates account balances if valid.
     * @param fromAccount Source account
     * @param toAccount Destination account
     * @param amount Transfer amount
     * @throws IllegalStateException if limits would be exceeded
     */
    @Transactional
    public void validateAndProcessTransfer(BankAccount fromAccount, BankAccount toAccount, Double amount) {
        // Reset daily limits if needed
        fromAccount.resetDailySpentIfNewDay();

        // Check limits
        if (fromAccount.wouldViolateAbsoluteLimit(amount)) {
            throw new IllegalStateException("Transfer would exceed absolute limit");
        }

        if (fromAccount.wouldExceedDailyLimit(amount)) {
            throw new IllegalStateException("Transfer would exceed daily limit");
        }

        // Process transfer
        fromAccount.setBalance(fromAccount.getBalance() - amount);
        toAccount.setBalance(toAccount.getBalance() + amount);

        // Update daily spent
        fromAccount.addToDailySpent(amount);

        // Save changes
        bankAccountRepository.save(fromAccount);
        bankAccountRepository.save(toAccount);
    }
}
