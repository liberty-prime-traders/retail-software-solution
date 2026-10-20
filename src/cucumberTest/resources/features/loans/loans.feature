@loans
Feature: Loans
  As an authenticated organization user
  I want to record money borrowed from a lender and the repayments against it
  So that loans payable always equals what the business still owes

  Background:
    Given I am authenticated as an organization user
    And the chart of accounts is active with open fiscal periods
    And a lender exists

  Scenario: Recording a loan raises loans payable and the lender's payable balance
    When I record a loan of "100.00" from the lender into cash dated today
    Then the response status should be 200
    And the loan should be "OPEN" with "0.00" repaid and "100.00" outstanding
    And account "002.004" should have a balance of "100.00"
    And account "001.001.001" should have a balance of "100.00"
    And the lender's payable balance should be "100.00" and receivable balance "0.00"
    And loans payable should equal the sum of outstanding loans
    And the loan's ledger groups should have a null source location

  Scenario: Repayments settle the loan, and voiding a repayment reopens it
    Given I record a loan of "100.00" from the lender into cash dated today
    When I repay "60.00" of the loan from cash
    Then the loan should be "OPEN" with "60.00" repaid and "40.00" outstanding
    When I repay "40.00" of the loan from cash
    Then the loan should be "SETTLED" with "100.00" repaid and "0.00" outstanding
    And account "002.004" should have a balance of "0.00"
    And the lender's payable balance should be "100.00" and receivable balance "100.00"
    When I void the latest repayment of the loan
    Then the response status should be 200
    And the loan should be "OPEN" with "60.00" repaid and "40.00" outstanding
    And account "002.004" should have a balance of "40.00"
    And loans payable should equal the sum of outstanding loans

  Scenario: A repayment above the outstanding amount is rejected and names both amounts
    Given I record a loan of "100.00" from the lender into cash dated today
    And I repay "70.00" of the loan from cash
    When I repay "30.01" of the loan from cash
    Then the response status should be 400
    And the error should mention "30.0100"
    And the error should mention "30.0000"
    And the loan should be "OPEN" with "70.00" repaid and "30.00" outstanding

  Scenario: A loan with an active repayment cannot be voided until the repayment is voided
    Given I record a loan of "100.00" from the lender into cash dated today
    And I repay "10.00" of the loan from cash
    When I void the loan
    Then the response status should be 400
    And the error should mention the loan's active repayment
    When I void the latest repayment of the loan
    And I void the loan
    Then the response status should be 200
    And the loan should be "VOIDED" with "0.00" repaid and "0.00" outstanding
    And account "002.004" should have a balance of "0.00"
    And account "001.001.001" should have a balance of "0.00"
    And the lender's payable balance should be "110.00" and receivable balance "110.00"

  Scenario: A voided loan accepts no repayment and cannot be voided again
    Given I record a loan of "100.00" from the lender into cash dated today
    And I void the loan
    When I repay "10.00" of the loan from cash
    Then the response status should be 400
    When I void the loan
    Then the response status should be 400

  Scenario: A repayment cannot be voided twice
    Given I record a loan of "100.00" from the lender into cash dated today
    And I repay "10.00" of the loan from cash
    And I void the latest repayment of the loan
    When I void the latest repayment of the loan
    Then the response status should be 400

  Scenario: A lender must exist, be active and hold the lender type
    Given a contact exists that is not a lender
    When I record a loan of "100.00" from the contact that is not a lender into cash dated today
    Then the response status should be 400
    When I record a loan of "100.00" from an unknown lender into cash dated today
    Then the response status should be 400

  Scenario: An account that cannot hold money is rejected
    When I record a loan of "100.00" from the lender into account "001.001.003" dated today
    Then the response status should be 400

  Scenario: Non-positive amounts are rejected
    When I record a loan of "0.00" from the lender into cash dated today
    Then the response status should be 400
    When I record a loan of "-5.00" from the lender into cash dated today
    Then the response status should be 400

  Scenario: A loan posted into a period with no open fiscal period leaves no loan behind
    When I record a loan of "100.00" from the lender into cash dated "2001-01-15"
    Then the response status should be 400
    And there should be 0 loans
    And no loan ledger groups should exist

  Scenario: A repayment the ledger rejects leaves no repayment behind
    Given I record a loan of "100.00" from the lender into cash dated today
    When I repay "10.00" of the loan from cash dated "2001-01-15"
    Then the response status should be 400
    And the loan should be "OPEN" with "0.00" repaid and "100.00" outstanding
    And account "002.004" should have a balance of "100.00"

  Scenario: Two simultaneous repayments that together exceed the outstanding amount cannot both succeed
    When 5 rounds of two simultaneous repayments of "60.00" are submitted against fresh loans of "100.00"
    Then exactly one repayment should succeed in every round
    And loans payable should equal the sum of outstanding loans

  Scenario: A repayment racing a loan void cannot both succeed
    When 5 rounds of a repayment of "10.00" racing a loan void are submitted against fresh loans of "100.00"
    Then exactly one request should succeed in every round
    And loans payable should equal the sum of outstanding loans

  Scenario: Searching by lender and status
    Given I record a loan of "100.00" from the lender into cash dated today
    And I record a loan of "50.00" from the lender into cash dated today
    And I repay "50.00" of the loan from cash
    When I search the loans for the lender with status "OPEN"
    Then the loan search should return 1 loans
    When I search the loans for the lender with status "SETTLED"
    Then the loan search should return 1 loans
    When I search the loans for the lender with status "VOIDED"
    Then the loan search should return 0 loans
    When I search the loans for an unknown lender
    Then the loan search should return 0 loans

  Scenario: A loan search needs a bounded created range of at most one year
    When I search the loans without a created range
    Then the response status should be 400
    When I search the loans over a range longer than a year
    Then the response status should be 400

  Scenario: Loans payable takes no manual opening balance
    When I POST to /secured/opening-balances with payload:
      """
      { "accountCode": "002.004", "newAmount": 10 }
      """
    Then the response status should be 400
