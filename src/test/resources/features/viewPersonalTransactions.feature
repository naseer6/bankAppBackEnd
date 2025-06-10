Feature: View personal transactions

  Scenario: User views their personal transactions
    Given I am logged in as a user
    When I request my personal transactions
    Then I should receive a list of my transactions

  Scenario: User views their personal transactions with pagination
    Given I am logged in as a user
    When I request my personal transactions with page 0 and size 5
    Then I should receive a paginated list of my transactions with size 5
