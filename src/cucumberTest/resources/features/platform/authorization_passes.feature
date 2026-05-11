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
    And the response field "id" should not be null
    And the response field "referenceNumber" should not be null
    And the response field "assignedTo" should not be null
    And the response field "createdBy" should not be null
    And the response field "createdOn" should not be null
    And the response should match json:
      """
      {
        "passType": "CREATE_ORGANIZATION",
        "passStatus": "ACTIVE",
        "maxUseCount": 10,
        "usedCount": 0,
        "expiresOn": null
      }
      """

  @regression
  Scenario: Platform admin views all authorization passes
    Given an authorization pass exists
    When I send a GET request to "/secured/authorization-passes"
    Then the response status should be 200
    And the response field "0.referenceNumber" should not be null
    And the response field "0.assignedTo" should not be null
    And the response field "0.createdBy" should not be null
    And the response field "0.createdOn" should not be null
    And the response should match json:
      """
      [
        {
          "id": "#authorizationPass->0",
          "passType": "CREATE_ORGANIZATION",
          "maxUseCount": 5,
          "usedCount": 0,
          "passStatus": "ACTIVE",
          "expiresOn": null
        }
      ]
      """

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
    And the response field "referenceNumber" should not be null
    And the response field "assignedTo" should not be null
    And the response field "createdBy" should not be null
    And the response field "createdOn" should not be null
    And the response should match json:
      """
      {
        "id": "#authorizationPass->0",
        "passType": "CREATE_ORGANIZATION",
        "maxUseCount": 20,
        "usedCount": 0,
        "passStatus": "ACTIVE",
        "expiresOn": null
      }
      """

  @regression
  Scenario: Platform admin revokes an authorization pass
    Given an authorization pass exists
    When I send a PUT request to "/secured/authorization-passes/#authorizationPass->0/revoke"
    Then the response status should be 200
    And the response field "referenceNumber" should not be null
    And the response field "assignedTo" should not be null
    And the response field "createdBy" should not be null
    And the response field "createdOn" should not be null
    And the response should match json:
      """
      {
        "id": "#authorizationPass->0",
        "passType": "CREATE_ORGANIZATION",
        "maxUseCount": 5,
        "usedCount": 0,
        "passStatus": "REVOKED",
        "expiresOn": null
      }
      """

  @regression
  Scenario: Unauthenticated user cannot view authorization passes
    Given I am not authenticated
    When I send a GET request to "/secured/authorization-passes"
    Then the response status should be 403
    And the response should contain field "timestamp"
    And the response should match json:
      """
      {
        "status": 403,
        "error": "Forbidden",
        "message": "Access Denied",
        "path": "/secured/authorization-passes"
      }
      """

  @regression
  Scenario: Organization user cannot view authorization passes
    Given I am authenticated as an organization user
    When I send a GET request to "/secured/authorization-passes"
    Then the response status should be 403
    And the response body should be empty
