Feature: Admin Transfer

  Scenario: Admin successfully transfers money between two accounts
    Given an admin is authenticated
    And there is a source account with IBAN "NL01INHO0000000001" and a destination account with IBAN "NL01INHO0000000002"
    And the transfer amount is 100.0
    When the admin initiates a transfer
    Then the transfer should be successful

