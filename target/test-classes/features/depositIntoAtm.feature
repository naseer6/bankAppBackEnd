Feature: Deposit into ATM

  Scenario: Successful ATM deposit
    Given a user "carol" with an active account "NL01INHO0000000003" and balance 500
    When the user deposits 300 euros into the ATM
    Then the deposit is successful
    And the account balance should be 800

  Scenario: ATM deposit exceeding limit
    Given a user "dave" with an active account "NL01INHO0000000004" and balance 1000
    When the user deposits 2500 euros into the ATM
    Then the deposit fails with message "ATM deposit limit is €2000 per transaction"
    And the account balance should be 1000
