Feature: User Registration

  Scenario: Valid registration
    Given the user is on the registration page
    When the user enters valid registration details
    Then the user account should be created

