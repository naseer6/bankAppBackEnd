Feature: Approve User

  Scenario: Admin approves a pending user
    Given there is a pending user with username "pendinguser"
    When the admin approves the user with username "pendinguser"
    Then the user with username "pendinguser" should be approved
    And a bank account should be created for the user
