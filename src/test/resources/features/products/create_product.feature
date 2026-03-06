Feature: Create Product
  As an authenticated organization user
  I want to create a product
  So that it can be used in inventory flows

  @smoke @products @auth
  Scenario: Authenticated organization user creates product successfully
    Given I am authenticated as an organization user
    When I create a product with name "Laptop Pro 14" and description "For office use"
    Then the response status should be 200
    And the response should contain field "id"
    And the response field "productName" should be "Laptop Pro 14"

  @products @auth @negative
  Scenario: Unauthenticated user cannot create product (403)
    Given I am not authenticated
    When I create a product with name "Unauthorized Product" and description "Should fail"
    Then the response status should be 403
