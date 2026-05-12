Feature: Product Categories
  As an authenticated organization user
  I want to manage product categories

  Background:
    Given I am authenticated as an organization user

  @regression
  Scenario: Authenticated user creates a product category successfully
    When I send a POST request to "/secured/product-category" with body:
      """
      {
        "categoryName": "Electronics",
        "description": "Electronic goods and accessories"
      }
      """
    Then the response status should be 200
    And the response should match the persisted category

  @regression
  Scenario: Authenticated user views list of product categories
    Given a category exists
    When I send a GET request to "/secured/product-category"
    Then the response status should be 200
    And the response should contain 1 items
    And the response item 0 should match the persisted category identified by "#category->0"

  @regression
  Scenario: Authenticated user updates a product category
    Given a category exists
    When I send a PUT request to "/secured/product-category" with body:
      """
      {
        "id": "#category->0",
        "categoryName": "Updated Category Name",
        "description": "Updated description"
      }
      """
    Then the response status should be 200
    And the response should match the persisted category

  @regression
  Scenario: Authenticated user deletes a product category
    Given a category exists
    When I send a DELETE request to "/secured/product-category/#category->0"
    Then the response status should be 204

  @regression
  Scenario: Duplicate category name is rejected on create
    Given a category exists
    When I send a POST request to "/secured/product-category" with body:
      """
      {
        "categoryName": "Test Category"
      }
      """
    Then the response status should be 400
    And the response error message should be "A category with the name Test Category already exists."

  @regression
  Scenario: Duplicate category name is rejected on update
    Given a category exists
    When I send a POST request to "/secured/product-category" with body:
      """
      {
        "categoryName": "Clothing"
      }
      """
    Then the response status should be 200
    When I send a PUT request to "/secured/product-category" with body:
      """
      {
        "id": "#category->0",
        "categoryName": "Clothing"
      }
      """
    Then the response status should be 400
    And the response error message should be "A category with the name Clothing already exists."

  @regression
  Scenario: Unauthenticated user cannot view product categories
    Given I am not authenticated
    When I send a GET request to "/secured/product-category"
    Then the response status should be 403

  @regression
  Scenario: Unauthenticated user cannot create product categories
    Given I am not authenticated
    When I send a POST request to "/secured/product-category" with body:
      """
      {
        "categoryName": "New Category"
      }
      """
    Then the response status should be 403

  @regression
  Scenario: Unauthenticated user cannot update product categories
    Given a category exists
    And I am not authenticated
    When I send a PUT request to "/secured/product-category" with body:
      """
      {
        "id": "#category->0",
        "categoryName": "Updated Name"
      }
      """
    Then the response status should be 403

  @regression
  Scenario: Unauthenticated user cannot delete product categories
    Given a category exists
    And I am not authenticated
    When I send a DELETE request to "/secured/product-category/#category->0"
    Then the response status should be 403
