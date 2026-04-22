Feature: Authorization Passes
  As a platform admin
  I want to manage authorization passes

  Background:
    Given the authorization pass table is clean
    And I am authenticated as a platform admin

  @regression
  Scenario: Platform admin issues an authorization pass successfully
    When I send a POST request to "/secured/authorization-passes" with body:
      """
      {
        "passType": "CREATE_ORGANIZATION",
        "maxUseCount": 10,
        "assignedToId": "22222222-2222-2222-2222-222222222222"
      }
      """
    Then the response status should be 200
    And the response should contain field "id"
    And the response field "referenceNumber" should not be null
    And the response field "passType" should be "CREATE_ORGANIZATION"
    And the response field "passStatus" should be "ACTIVE"

  @regression
  Scenario: Platform admin views all authorization passes
    Given an authorization pass exists
    When I send a GET request to "/secured/authorization-passes"
    Then the response status should be 200
    And the response should contain at least 1 items

  @regression
  Scenario: Platform admin updates an authorization pass
    Given an authorization pass exists
    When I send a PUT request to "/secured/authorization-passes" with body:
      """
      {
        "id": "#authorizationPass->0",
        "maxUseCount": 20
      }
      """
    Then the response status should be 200
    And the response field "id" should be "#authorizationPass->0"
    And the response field "maxUseCount" should be "20"

  @regression
  Scenario: Platform admin revokes an authorization pass
    Given an authorization pass exists
    When I send a PUT request to "/secured/authorization-passes/#authorizationPass->0/revoke"
    Then the response status should be 200
    And the response field "passStatus" should be "REVOKED"

  @regression
  Scenario: Unauthenticated user cannot view authorization passes
    Given I am not authenticated
    When I send a GET request to "/secured/authorization-passes"
    Then the response status should be 403

  @regression
  Scenario: Organization user cannot view authorization passes
    Given I am authenticated as an organization user
    When I send a GET request to "/secured/authorization-passes"
    Then the response status should be 403
