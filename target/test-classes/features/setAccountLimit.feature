Feature: Set Account Limit

  Scenario: Admin sets absolute and daily limit on a user's account
    Given the user has a bank account with IBAN "NL01INHO0000000001" and balance 500.0
    When the admin sets the absolute limit to 100.0 and daily limit to 300.0 for IBAN "NL01INHO0000000001"
    Then the account with IBAN "NL01INHO0000000001" should have absolute limit 100.0 and daily limit 300.0

  Scenario: Admin sets only the daily limit on a user's account
    Given the user has a bank account with IBAN "NL01INHO0000000002" and balance 200.0
    When the admin sets the daily limit to 150.0 for IBAN "NL01INHO0000000002"
    Then the account with IBAN "NL01INHO0000000002" should have daily limit 150.0

  Scenario: Admin sets only the absolute limit on a user's account
    Given the user has a bank account with IBAN "NL01INHO0000000003" and balance 1000.0
    When the admin sets the absolute limit to 250.0 for IBAN "NL01INHO0000000003"
    Then the account with IBAN "NL01INHO0000000003" should have absolute limit 250.0

