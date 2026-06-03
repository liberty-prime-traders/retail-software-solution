Feature: Unit Groups
  As an authenticated organization user
  I want to manage unit groups

  Background:
    Given I am authenticated as an organization user

  @regression
  Scenario: Authenticated user creates a unit group successfully
    When I POST to secured/unitgroups with payload:
      """
      {
        "name": "Volume",
        "description": "Measurement of volume"
      }
      """
    Then the response status should be 200
    And response contains details:
      """
      {
        "id": "^[0-9a-f-]{36}$",
        "name": "Volume",
        "description": "Measurement of volume",
        "createdBy": "^.+$",
        "createdOn": "^.+$",
        "referenceNumber": "^UNGR\\d+$",
        "systemDefined": false
      }
      """

  @regression
  Scenario: Authenticated user views list of unit groups
    Given a unit group exists
    When I GET from secured/unitgroups
    Then the response status should be 200
    And the response should contain 1 items
    And response contains item with details:
      """
      {
        "id": "#unitGroup->0",
        "name": "Test Unit Group",
        "createdBy": "^.+$",
        "createdOn": "^.+$",
        "referenceNumber": "^UNGR\\d+$",
        "systemDefined": false
      }
      """

  @regression
  Scenario: Authenticated user updates a unit group
    Given a unit group exists
    When I PUT to secured/unitgroups with payload:
      """
      {
        "id": "#unitGroup->0",
        "name": "Updated Unit Group Name",
        "description": "Updated Description"
      }
      """
    Then the response status should be 200
    And response contains details:
      """
      {
        "id": "#unitGroup->0",
        "name": "Updated Unit Group Name",
        "description": "Updated Description",
        "createdBy": "^.+$",
        "createdOn": "^.+$",
        "referenceNumber": "^UNGR\\d+$",
        "systemDefined": false
      }
      """

  @regression
  Scenario: Authenticated user deletes a unit group
    Given a unit group exists
    When I DELETE from secured/unitgroups/#unitGroup->0
    Then the response status should be 204
    When I GET from secured/unitgroups
    Then the response status should be 200
    And the response should be an empty list

  @regression
  Scenario: Duplicate unit group name is rejected
    Given a unit group exists
    When I POST to secured/unitgroups with payload:
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
    When I GET from secured/unitgroups
    Then the response status should be 403
