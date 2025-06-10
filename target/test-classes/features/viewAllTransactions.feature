Feature: View all transactions (admin)

  Scenario: Admin views all transactions
    Given I am logged in as an admin
    When I request all transactions
    Then I should receive a list of all transactions

  Scenario: Admin filters transactions by IBAN
    Given I am logged in as an admin
    When I request all transactions filtered by IBAN "NL01INHO0000000001"
    Then I should receive a list of transactions for IBAN "NL01INHO0000000001"
