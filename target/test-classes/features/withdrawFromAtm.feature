Feature: Withdraw from ATM

  Scenario: User withdraws money from their account via ATM
    Given the user is on the registration page
    When the user enters valid registration details
    Then the user account should be created

    Given the user is on the login page
    When the user enters valid credentials
    Then the user should be redirected to the dashboard

    Given the user has a bank account with IBAN "ATMWITHDRAW123" and balance 200.0
    When the user withdraws 80.0 from the ATM for IBAN "ATMWITHDRAW123"
    Then the ATM withdrawal should be successful and the new balance should be 120.0
