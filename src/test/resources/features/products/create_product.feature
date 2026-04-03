Feature: Create Product
  As an authenticated organization user
  I want to create a product
  So that it can be used in inventory flows

  Background:
    Given I am authenticated as an organization user
    And a category exists
    And a product group exists
    And a unit group exists
    And a unit exists

  @smoke
  Scenario: Authenticated organization user creates product successfully
    When I create a product with name "Laptop Pro 14" and description "For office use"
    Then the response status should be 200
    And the response should contain field "id"
    And the response field "productName" should be "Laptop Pro 14"

  @publishes-to-kafka
  Scenario: Product creation publishes catalog event to Kafka
    And I am subscribed to the catalog events topic
    When I create a product with name "Kafka Product" and description "Event validation"
    Then the response status should be 200
    And a catalog event should be published for table "PRODUCT"
    And the catalog event should reference the created resource
