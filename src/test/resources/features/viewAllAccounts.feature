Feature: View All Accounts
  As an administrator
  I want to view all bank accounts in the system
  So that I can monitor and manage them effectively

  Scenario: Admin successfully retrieves all bank accounts
    Given the system has bank accounts
    When the admin requests to view all accounts
    Then all bank accounts in the system are returned
    And the response status is 200 OK

  Scenario: No bank accounts exist in the system
    Given the system has no bank accounts
    When the admin requests to view all accounts
    Then an empty list of accounts is returned
    And the response status is 200 OK

  Scenario: Unauthorized user attempts to view all accounts
    Given a user is not authenticated
    When the user requests to view all accounts
    Then the request is denied
    And the response status is 401 Unauthorized

  Scenario: Regular user attempts to view all accounts
    Given a user with role "USER" is authenticated
    When the user requests to view all accounts
    Then the request is denied
    And the response status is 403 Forbidden
