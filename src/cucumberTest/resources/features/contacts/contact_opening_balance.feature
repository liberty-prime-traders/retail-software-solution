@contact-opening-balance
Feature: Contact opening balance
  As an authenticated organization user
  I want to declare a contact's opening receivable and payable
  So that the contact list reports the latest revision per subledger direction

  Background:
    Given I am authenticated as an organization user
    And a contact exists

  Scenario: The listed opening balance follows the latest revision of each direction
    When I set the contact's opening balances to payable "40.00" and receivable "100.00"
    And I set the contact's opening balances to payable "40.00" and receivable "90.00"
    Then the response status should be 200
    And the contact's listed net opening balance should be "50.00" with effect "ADDS"

  Scenario: A negative opening balance is rejected
    When I set the contact's opening balances to payable "0.00" and receivable "-1.00"
    Then the response status should be 400
