package nl.inholland.bankAppBackEnd.DTOs;

public class DashboardStatsDTO {
    private int pendingApprovals;
    private int totalUsers;
    private int activeAccounts;
    private int todayTransactions;

    // Getters and setters

    public DashboardStatsDTO(int pendingApprovals, int totalUsers, int activeAccounts, int todayTransactions) {
        this.pendingApprovals = pendingApprovals;
        this.totalUsers = totalUsers;
        this.activeAccounts = activeAccounts;
        this.todayTransactions = todayTransactions;
    }
}
