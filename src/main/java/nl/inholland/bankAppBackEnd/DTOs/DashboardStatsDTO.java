package nl.inholland.bankAppBackEnd.DTOs;

public class DashboardStatsDTO {
    private int pendingApprovals;
    private int totalUsers;
    private int activeAccounts;
    private int todayTransactions;

    // Constructor
    public DashboardStatsDTO(int pendingApprovals, int totalUsers, int activeAccounts, int todayTransactions) {
        this.pendingApprovals = pendingApprovals;
        this.totalUsers = totalUsers;
        this.activeAccounts = activeAccounts;
        this.todayTransactions = todayTransactions;
    }

    // Getters only - removed setters as they're not being used
    public int getPendingApprovals() {
        return pendingApprovals;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public int getActiveAccounts() {
        return activeAccounts;
    }

    public int getTodayTransactions() {
        return todayTransactions;
    }
}
