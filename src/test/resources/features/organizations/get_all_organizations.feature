Feature: Get All Organizations
  As a platform administrator
  I want to retrieve all organizations
  So that I can view all tenants in the system

  @smoke @simple
  Scenario: Successfully retrieve all organizations
    Given I am authenticated as an organization admin
    When I get all organizations
    Then the response status should be 200

  @simple
  Scenario: Retrieve organizations without authentication
    Given I am not authenticated
    When I get all organizations
    Then the response status should be 401

  @simple
  Scenario: Retrieve organizations as regular user
    Given I am authenticated as an organization user
    When I get all organizations
    Then the response status should be 403
