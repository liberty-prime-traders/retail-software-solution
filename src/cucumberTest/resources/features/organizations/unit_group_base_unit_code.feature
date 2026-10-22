@unit-group-base-unit-code
Feature: Unit group base unit code
  As an authenticated organization user
  I want to choose the code of a custom unit group's base unit
  So that products can reference it without reproducing the generated code

  Background:
    Given I am authenticated as an organization user

  Scenario: A bulk import creates the root unit with the explicit code and resolves both reference styles
    When I POST to /secured/unitgroups/bulk with payload:
      """
      {
        "unitGroups": [
          {
            "name": "Eggs",
            "baseUnitName": "Sachet",
            "baseUnitCode": "egg-root",
            "unitValues": [
              {"name": "Half Dozen", "code": "egg-hdz", "baseUnitCode": "egg-root", "unitsOfBasePerUnit": 6},
              {"name": "Dozen", "code": "egg-dz", "baseUnitCode": "__piece__", "unitsOfBasePerUnit": 12}
            ]
          }
        ]
      }
      """
    Then the response status should be 200
    When I GET to /secured/unitvalues
    Then the response field "find { it.code == 'egg-root' }.name" should be "Sachet"
    And the response field "find { it.code == 'egg-hdz' }.baseUnitName" should be "Sachet"
    And the response field "find { it.code == 'egg-dz' }.baseUnitName" should be "Sachet"

  Scenario: A bulk import without a base unit code keeps the generated root unit code
    When I POST to /secured/unitgroups/bulk with payload:
      """
      {"unitGroups": [{"name": "Milk Crates", "unitValues": []}]}
      """
    Then the response status should be 200
    When I GET to /secured/unitvalues
    Then the response field "find { it.code == 'milk-crates-base' }.name" should be "Piece"

  Scenario: Creating a single unit group creates the root unit with the explicit code
    When I POST to /secured/unitgroups with payload:
      """
      {"name": "Boxes", "baseUnitName": "Sachet", "baseUnitCode": "box-root"}
      """
    Then the response status should be 200
    When I GET to /secured/unitvalues
    Then the response field "find { it.code == 'box-root' }.name" should be "Sachet"

  Scenario: A bulk import rejects a base unit code that already exists in the database
    When I POST to /secured/unitgroups with payload:
      """
      {"name": "Boxes", "baseUnitCode": "taken-root"}
      """
    Then the response status should be 200
    When I POST to /secured/unitgroups/bulk with payload:
      """
      {"unitGroups": [{"name": "Eggs", "baseUnitCode": "taken-root"}]}
      """
    Then the response status should be 400
    And the response error should contain "baseUnitCode 'taken-root' already exists in the database"

  Scenario: Creating a single unit group rejects a base unit code that already exists in the database
    When I POST to /secured/unitgroups with payload:
      """
      {"name": "Boxes", "baseUnitCode": "taken-root"}
      """
    Then the response status should be 200
    When I POST to /secured/unitgroups with payload:
      """
      {"name": "Crates", "baseUnitCode": "taken-root"}
      """
    Then the response status should be 400
    And the response error should contain "taken-root"
