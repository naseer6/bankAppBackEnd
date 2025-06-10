Feature: Deposit into ATM

  Scenario: User deposits money into their account via ATM
    Given the user is on the registration page
    When the user enters valid registration details
    Then the user account should be created

    Given the user is on the login page
    When the user enters valid credentials
    Then the user should be redirected to the dashboard

    Given the user has a bank account with IBAN "ATMDEPOSIT123" and balance 100.0
    When the user deposits 50.0 into the ATM for IBAN "ATMDEPOSIT123"
    Then the ATM deposit should be successful and the new balance should be 150.0
