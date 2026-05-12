Feature: Job Titles
  As an authenticated organization user
  I want to manage job titles

  Background:
    Given I am authenticated as an organization user

  @regression
  Scenario: Authenticated user creates a job title successfully
    When I send a POST request to "/secured/job-title" with body:
      """
      {
        "value": "Software Engineer"
      }
      """
    Then the response status should be 200
    And the response should match the persisted jobTitle

  @regression
  Scenario: Authenticated user views list of job titles
    Given a job title exists
    When I send a GET request to "/secured/job-title"
    Then the response status should be 200
    And the response should contain 1 items
    And the response item 0 should match the persisted jobTitle identified by "#jobTitle->0"

  @regression
  Scenario: Authenticated user updates a job title
    Given a job title exists
    When I send a PUT request to "/secured/job-title" with body:
      """
      {
        "id": "#jobTitle->0",
        "value": "Senior Engineer"
      }
      """
    Then the response status should be 200
    And the response should match the persisted jobTitle

  @regression
  Scenario: Authenticated user deletes a job title
    Given a job title exists
    When I send a DELETE request to "/secured/job-title/#jobTitle->0"
    Then the response status should be 204

  @regression
  Scenario: Duplicate job title value is rejected on create
    Given a job title exists
    When I send a POST request to "/secured/job-title" with body:
      """
      {
        "value": "Test Job Title"
      }
      """
    Then the response status should be 400
    And the response error message should be "A job title with the value Test Job Title already exists."

  @regression
  Scenario: Duplicate job title value is rejected on update
    Given a job title exists
    When I send a POST request to "/secured/job-title" with body:
      """
      {
        "value": "Product Manager"
      }
      """
    Then the response status should be 200
    When I send a PUT request to "/secured/job-title" with body:
      """
      {
        "id": "#jobTitle->0",
        "value": "Product Manager"
      }
      """
    Then the response status should be 400
    And the response error message should be "A job title with the value Product Manager already exists."

  @regression
  Scenario: Unauthenticated user cannot view job titles
    Given I am not authenticated
    When I send a GET request to "/secured/job-title"
    Then the response status should be 403

  @regression
  Scenario: Unauthenticated user cannot create a job title
    Given I am not authenticated
    When I send a POST request to "/secured/job-title" with body:
      """
      {
        "value": "Hacker"
      }
      """
    Then the response status should be 403

  @regression
  Scenario: Unauthenticated user cannot update a job title
    Given a job title exists
    And I am not authenticated
    When I send a PUT request to "/secured/job-title" with body:
      """
      {
        "id": "#jobTitle->0",
        "value": "Hacked Title"
      }
      """
    Then the response status should be 403

  @regression
  Scenario: Unauthenticated user cannot delete a job title
    Given a job title exists
    And I am not authenticated
    When I send a DELETE request to "/secured/job-title/#jobTitle->0"
    Then the response status should be 403
