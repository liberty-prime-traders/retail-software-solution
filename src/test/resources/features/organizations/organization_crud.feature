Feature: Organization CRUD Operations
  As a platform administrator
  I want to perform CRUD operations on organizations
  So that I can manage tenants in the system

  Background:
    Given I am authenticated as an organization admin

  @smoke
  Scenario: Get all organizations
    When I get all organizations
    Then the response status should be 200

  @smoke
  Scenario: Create a new organization successfully
    When I create an organization with name "Acme Corp" and subdomain "acme"
    Then the response status should be 201
    And the response should contain field "id"
    And the response should contain field "name"
    And the response should contain field "subdomain"
    And the response field "name" should be "Acme Corp"
    And the response field "subdomain" should be "acme"

  Scenario: Create organization without authentication
    Given I am not authenticated
    When I create an organization with name "Test Org" and subdomain "test"
    Then the response status should be 401

  Scenario: Update an organization
    When I update the current organization with name "Updated Corp"
    Then the response status should be 200
    And the response field "name" should be "Updated Corp"

  Scenario: Delete an organization
    When I delete the current organization
    Then the response status should be 204

  @regression
  Scenario: Launch organization by domain
    When I launch organization with domain "acme"
    Then the response status should be 200
    And the response should contain field "organization"

  Scenario: Get organization locations
    Given an organization exists with id "123e4567-e89b-12d3-a456-426614174000"
    When I get locations for organization "123e4567-e89b-12d3-a456-426614174000"
    Then the response status should be 200
