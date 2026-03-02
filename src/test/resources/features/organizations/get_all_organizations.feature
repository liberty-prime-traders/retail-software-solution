Feature: Get All Organizations
  As a platform administrator
  I want to retrieve all organizations
  So that I can view all tenants in the system

  @smoke
  Scenario: Retrieve organizations as regular user
    Given I am authenticated as an organization user
    When I get all organizations
    Then the response status should be 403
