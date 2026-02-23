Feature: Product Management
  As a store manager
  I want to manage products
  So that I can maintain my inventory

  Background:
    Given I am authenticated as an organization admin

  @smoke
  Scenario: Create a new product
    When I create a product with name "Laptop" and description "High-performance laptop"
    Then the response status should be 201
    And the response should contain field "id"
    And the response field "productName" should be "Laptop"
    And the response field "status" should be "ACTIVE"

  Scenario: Create product without authentication
    Given I am not authenticated
    When I create a product with name "Laptop" and description "Test"
    Then the response status should be 401
