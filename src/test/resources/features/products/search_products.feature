Feature: Product Search
  As a store employee
  I want to search for products
  So that I can quickly find items

  Background:
    Given I am authenticated as an organization user
    And the following products exist:
      | name        | description      | status |
      | MacBook Pro | Apple laptop     | ACTIVE |
      | iPhone 15   | Apple smartphone | ACTIVE |

  @regression
  Scenario: Search by text
    When I search for products with text "MacBook"
    Then the response status should be 200
    And the response should contain 1 items

  Scenario: Search with no results
    When I search for products with text "NonExistent"
    Then the response status should be 200
    And the response should be an empty list
