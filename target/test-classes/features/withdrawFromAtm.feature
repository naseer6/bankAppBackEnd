Feature: Withdraw from ATM

  Scenario: Successful ATM withdrawal
    Given a user "alice" with an active account "NL01INHO0000000001" and balance 1000
    When the user withdraws 200 euros from the ATM
    Then the withdrawal is successful
    And the account balance should be 800

  Scenario: ATM withdrawal exceeding balance
    Given a user "bob" with an active account "NL01INHO0000000002" and balance 100
    When the user withdraws 200 euros from the ATM
    Then the withdrawal fails with message "Insufficient balance"
    And the account balance should be 100
