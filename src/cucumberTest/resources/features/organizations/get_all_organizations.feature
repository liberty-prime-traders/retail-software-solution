Feature: Get All Organizations
  As a platform administrator
  I want to retrieve all organizations
  So that I can view all tenants in the system

  @smoke
  Scenario: Regular organization user cannot retrieve all organizations (403)
    Given I am authenticated as an organization user
    When I get all organizations
    Then the response status should be 403

  @smoke
  Scenario: Unauthenticated user cannot retrieve all organizations (403)
    Given I am not authenticated
    When I get all organizations
    Then the response status should be 403
