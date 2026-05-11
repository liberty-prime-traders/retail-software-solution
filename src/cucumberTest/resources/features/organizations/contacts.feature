Feature: Contacts
  As an authenticated organization user
  I want to manage contacts

  Background:
    Given I am authenticated as an organization user

  @regression
  Scenario: Authenticated user creates an INDIVIDUAL contact successfully
    When I send a POST request to "/secured/contacts" with body:
      """
      {
        "contactType": "CUSTOMER",
        "identityType": "INDIVIDUAL",
        "firstName": "Jane",
        "lastName": "Smith",
        "email": "jane.smith@example.com",
        "phone": "0987654321",
        "address": "456 Jane St"
      }
      """
    Then the response status should be 200
    And the response should contain field "id"
    And the response should contain field "createdBy"
    And the response should contain field "createdOn"
    And the response should contain field "referenceNumber"
    And the response should match json:
      """
      {
        "contactType": "CUSTOMER",
        "identityType": "INDIVIDUAL",
        "firstName": "Jane",
        "lastName": "Smith",
        "companyName": null,
        "fullName": "Jane Smith",
        "email": "jane.smith@example.com",
        "phone": "0987654321",
        "address": "456 Jane St",
        "creditLimit": null,
        "notes": null,
        "status": "ACTIVE",
        "systemDefined": false
      }
      """

  @regression
  Scenario: Authenticated user creates an ORGANIZATION contact successfully
    When I send a POST request to "/secured/contacts" with body:
      """
      {
        "contactType": "SUPPLIER",
        "identityType": "ORGANIZATION",
        "companyName": "Tech Corp",
        "email": "contact@techcorp.com",
        "phone": "1112223333",
        "address": "Innovate Ave"
      }
      """
    Then the response status should be 200
    And the response should contain field "id"
    And the response should contain field "createdBy"
    And the response should contain field "createdOn"
    And the response should contain field "referenceNumber"
    And the response should match json:
      """
      {
        "contactType": "SUPPLIER",
        "identityType": "ORGANIZATION",
        "companyName": "Tech Corp",
        "firstName": null,
        "lastName": null,
        "fullName": "Tech Corp",
        "email": "contact@techcorp.com",
        "phone": "1112223333",
        "address": "Innovate Ave",
        "creditLimit": null,
        "notes": null,
        "status": "ACTIVE",
        "systemDefined": false
      }
      """

  @regression
  Scenario: Authenticated user views list of contacts
    Given a contact exists
    When I send a GET request to "/secured/contacts"
    Then the response status should be 200
    And the response should contain field "0.createdBy"
    And the response should contain field "0.createdOn"
    And the response should contain field "0.referenceNumber"
    And the response should match json:
      """
      [
        {
          "id": "#contact->0",
          "contactType": "CUSTOMER",
          "identityType": "INDIVIDUAL",
          "firstName": "John",
          "lastName": "Doe",
          "companyName": null,
          "fullName": "John Doe",
          "email": "john.doe@example.com",
          "phone": "1234567890",
          "address": "123 Test St",
          "creditLimit": null,
          "notes": null,
          "status": "ACTIVE",
          "systemDefined": false
        }
      ]
      """

  @regression
  Scenario: Authenticated user updates a contact
    Given a contact exists
    When I send a PUT request to "/secured/contacts" with body:
      """
      {
        "id": "#contact->0",
        "firstName": "John Updated",
        "notes": "Some updated notes"
      }
      """
    Then the response status should be 200
    And the response should contain field "createdBy"
    And the response should contain field "createdOn"
    And the response should contain field "referenceNumber"
    And the response should match json:
      """
      {
        "id": "#contact->0",
        "contactType": "CUSTOMER",
        "identityType": "INDIVIDUAL",
        "firstName": "John Updated",
        "lastName": "Doe",
        "companyName": null,
        "fullName": "John Updated Doe",
        "email": "john.doe@example.com",
        "phone": "1234567890",
        "address": "123 Test St",
        "creditLimit": null,
        "notes": "Some updated notes",
        "status": "ACTIVE",
        "systemDefined": false
      }
      """

  @regression
  Scenario: Authenticated user deletes a contact
    Given a contact exists
    When I send a DELETE request to "/secured/contacts/#contact->0"
    Then the response status should be 204

  @regression
  Scenario: Duplicate contact identity is rejected
    Given a contact exists
    When I send a POST request to "/secured/contacts" with body:
      """
      {
        "contactType": "CUSTOMER",
        "identityType": "INDIVIDUAL",
        "firstName": "John",
        "lastName": "Doe"
      }
      """
    Then the response status should be 400
    And the response error message should be "A Contact with person 'John Doe' already exists"
    And the response should contain field "timestamp"
    And the response should match json:
      """
      {
        "message": "A Contact with person 'John Doe' already exists",
        "body": null
      }
      """

  @regression
  Scenario: INDIVIDUAL contact requires first name
    When I send a POST request to "/secured/contacts" with body:
      """
      {
        "contactType": "CUSTOMER",
        "identityType": "INDIVIDUAL",
        "lastName": "MissingFirstName"
      }
      """
    Then the response status should be 400
    And the response error message should be "Identity type INDIVIDUAL requires first name"
    And the response should contain field "timestamp"
    And the response should match json:
      """
      {
        "message": "Identity type INDIVIDUAL requires first name",
        "body": null
      }
      """

  @regression
  Scenario: ORGANIZATION contact requires company name
    When I send a POST request to "/secured/contacts" with body:
      """
      {
        "contactType": "SUPPLIER",
        "identityType": "ORGANIZATION"
      }
      """
    Then the response status should be 400
    And the response error message should be "Identity type ORGANIZATION requires company name"
    And the response should contain field "timestamp"
    And the response should match json:
      """
      {
        "message": "Identity type ORGANIZATION requires company name",
        "body": null
      }
      """

  @regression
  Scenario: Unauthenticated user cannot view contacts
    Given I am not authenticated
    When I send a GET request to "/secured/contacts"
    Then the response status should be 403

  @regression
  Scenario: Unauthenticated user cannot create a contact
    Given I am not authenticated
    When I send a POST request to "/secured/contacts" with body:
      """
      {
        "contactType": "CUSTOMER",
        "identityType": "INDIVIDUAL",
        "firstName": "Unauthorized"
      }
      """
    Then the response status should be 403

  @regression
  Scenario: Unauthenticated user cannot update a contact
    Given a contact exists
    And I am not authenticated
    When I send a PUT request to "/secured/contacts" with body:
      """
      {
        "id": "#contact->0",
        "firstName": "Hacked"
      }
      """
    Then the response status should be 403

  @regression
  Scenario: Unauthenticated user cannot delete a contact
    Given a contact exists
    And I am not authenticated
    When I send a DELETE request to "/secured/contacts/#contact->0"
    Then the response status should be 403
