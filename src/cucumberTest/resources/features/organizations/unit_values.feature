Feature: Unit Values
  As an authenticated organization user
  I want to manage unit values

  Background:
    Given I am authenticated as an organization user

  @regression
  Scenario: Authenticated user creates a unit value successfully
    Given a unit group exists
    When I send a POST request to "/secured/unitvalues" with body:
      """
      {
        "name": "Liter",
        "code": "L",
        "description": "Metric literal",
        "unitGroupId": "#unitGroup->0"
      }
      """
    Then the response status should be 200
    And the response should match the persisted unitValue

  @regression
  Scenario: Authenticated user views list of all unit values
    Given a unit group exists
    And a unit exists
    When I send a GET request to "/secured/unitvalues"
    Then the response status should be 200
    And the response should contain at least 1 items
    And the response item 0 should match the persisted unitValue identified by "#unitValue->0"

  @regression
  Scenario: Authenticated user views list of unit values by group
    Given a unit group exists
    And a unit exists
    When I send a GET request to "/secured/unitvalues?unitGroupId=#unitGroup->0"
    Then the response status should be 200
    And the response should contain at least 1 items
    And the response item 0 should match the persisted unitValue identified by "#unitValue->0"

  @regression
  Scenario: Authenticated user updates a unit value
    Given a unit group exists
    And a unit exists
    When I send a PUT request to "/secured/unitvalues" with body:
      """
      {
        "id": "#unitValue->0",
        "name": "Updated Unit Name",
        "code": "UUN",
        "description": "Updated Description"
      }
      """
    Then the response status should be 200
    And the response should match the persisted unitValue

  @regression
  Scenario: Authenticated user deletes a unit value
    Given a unit group exists
    And a unit exists
    When I send a DELETE request to "/secured/unitvalues/#unitValue->0"
    Then the response status should be 204
    When I send a GET request to "/secured/unitvalues"
    Then the response status should be 200
    And the response should be an empty list

  @regression
  Scenario: Duplicate unit value name is rejected
    Given a unit group exists
    And a unit exists
    When I send a POST request to "/secured/unitvalues" with body:
      """
      {
        "name": "Test Unit",
        "code": "TUV",
        "unitGroupId": "#unitGroup->0"
      }
      """
    Then the response status should be 400
    And the response error message should be "A unit value with the name Test Unit already exists"

  @regression
  Scenario: Unit value requires a conversion factor when base unit is provided
    Given a unit group exists
    And a unit exists
    When I send a POST request to "/secured/unitvalues" with body:
      """
      {
        "name": "Second Unit",
        "code": "SU",
        "unitGroupId": "#unitGroup->0",
        "baseUnit": "#unitValue->0"
      }
      """
    Then the response status should be 400
    And the response error message should be "A unit value with a base unit must have a conversion factor"

  @regression
  Scenario: Unit value requires a base unit when conversion factor is provided
    Given a unit group exists
    And a unit exists
    When I send a POST request to "/secured/unitvalues" with body:
      """
      {
        "name": "Second Unit",
        "code": "SU",
        "unitGroupId": "#unitGroup->0",
        "conversionFactor": 1.5
      }
      """
    Then the response status should be 400
    And the response error message should be "A unit value with a conversion factor must have a base unit"

  @regression
  Scenario: Unauthenticated user cannot view unit values
    Given I am not authenticated
    When I send a GET request to "/secured/unitvalues"
    Then the response status should be 403
