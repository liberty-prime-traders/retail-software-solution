Feature: Catalog Sync
  As a system
  I want products created at the org level to be synced to location catalogs
  So that location inventory reflects the current catalog

  @consumes-from-kafka
  Scenario: Product creation is consumed and synced to location catalog
    Given I am authenticated as an organization user
    And a category exists
    And a product group exists
    And a unit group exists
    And a unit exists
    When I create a product with name "Kafka Synced Product" and description "Consumer flow validation"
    Then the response status should be 200
    And the location catalog should contain product "Kafka Synced Product"
