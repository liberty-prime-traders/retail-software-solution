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

  @products @kafka
  Scenario: Product creation publishes catalog event to Kafka
    Given I am authenticated as an organization user
    And I am subscribed to the catalog events topic
    When I create a product with name "Kafka Product" and description "Event validation"
    Then the response status should be 200
    And a catalog event should be published for table "PRODUCT"
    And the catalog event should reference the created resource

  @products @kafka @kafka-consumer
  Scenario: Product creation is consumed and synced to location catalog
    Given I am authenticated as a platform admin
    And a location exists for catalog sync
    And I am authenticated as an organization user
    And I use the catalog sync location context
    When I create a product with name "Kafka Synced Product" and description "Consumer flow validation"
    Then the response status should be 200
    And the created product should be synced to location catalog
