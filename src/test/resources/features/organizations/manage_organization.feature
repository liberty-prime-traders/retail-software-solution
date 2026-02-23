Feature: Organization Management
  As a platform administrator
  I want to manage organizations
  So that I can control tenant access

  Background:
    Given I am authenticated as an organization admin

  @smoke
  Scenario: Create a new organization
    When I create an organization with name "Test Org" and subdomain "testorg"
    Then the response status should be 201
    And the response should contain field "id"
    And the response field "name" should be "Test Org"

  Scenario: Get all organizations
    When I get all organizations
    Then the response status should be 200
