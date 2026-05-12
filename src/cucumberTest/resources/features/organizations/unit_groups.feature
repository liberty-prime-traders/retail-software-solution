Feature: Unit Groups
  As an authenticated organization user
  I want to manage unit groups

  Background:
    Given I am authenticated as an organization user

  @regression
  Scenario: Authenticated user creates a unit group successfully
    When I send a POST request to "/secured/unitgroups" with body:
      """
      {
        "name": "Volume",
        "description": "Measurement of volume"
      }
      """
    Then the response status should be 200
    And the response should match the persisted unitGroup

  @regression
  Scenario: Authenticated user views list of unit groups
    Given a unit group exists
    When I send a GET request to "/secured/unitgroups"
    Then the response status should be 200
    And the response should contain 1 items
    And the response item 0 should match the persisted unitGroup identified by "#unitGroup->0"

  @regression
  Scenario: Authenticated user updates a unit group
    Given a unit group exists
    When I send a PUT request to "/secured/unitgroups" with body:
      """
      {
        "id": "#unitGroup->0",
        "name": "Updated Unit Group Name",
        "description": "Updated Description"
      }
      """
    Then the response status should be 200
    And the response should match the persisted unitGroup

  @regression
  Scenario: Authenticated user deletes a unit group
    Given a unit group exists
    When I send a DELETE request to "/secured/unitgroups/#unitGroup->0"
    Then the response status should be 204
    When I send a GET request to "/secured/unitgroups"
    Then the response status should be 200
    And the response should be an empty list

  @regression
  Scenario: Duplicate unit group name is rejected
    Given a unit group exists
    When I send a POST request to "/secured/unitgroups" with body:
      """
      {
        "name": "Test Unit Group"
      }
      """
    Then the response status should be 400
    And the response error message should be "An UnitGroup using the name 'Test Unit Group' already exists"

  @regression
  Scenario: Unauthenticated user cannot view unit groups
    Given I am not authenticated
    When I send a GET request to "/secured/unitgroups"
    Then the response status should be 403
