package me.ezra_home.retail_software_solution.organizations.business.fund_movement.search

import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementSearchParameters
import me.ezra_home.retail_software_solution.util.queries.SearchGuards

object FundMovementSearchValidator {

    private const val MAX_ACCOUNT_CODES = 50
    private const val MAX_REFERENCES = 20
    private const val MIN_PAGE_SIZE = 1
    private const val MAX_PAGE_SIZE = 500

    fun guardValidParameters(fundMovementSearchParameters: FundMovementSearchParameters) {
        SearchGuards.guardBoundedRange(
            fundMovementSearchParameters.createdFrom,
            fundMovementSearchParameters.createdBefore,
            FundMovementSearchParameterNames.CREATED_FROM,
            FundMovementSearchParameterNames.CREATED_BEFORE
        )
        SearchGuards.guardMaxSize(fundMovementSearchParameters.accountCodes.size, MAX_ACCOUNT_CODES, "account codes")
        SearchGuards.guardMaxSize(fundMovementSearchParameters.references.size, MAX_REFERENCES, "references")

        SearchGuards.guardRangeOrder(
            fundMovementSearchParameters.createdFrom,
            fundMovementSearchParameters.createdBefore,
            "createdFrom must not be after createdBefore"
        ) { from, before -> from.isAfter(before) }

        SearchGuards.guardRangeOrder(
            fundMovementSearchParameters.movementDateFrom,
            fundMovementSearchParameters.movementDateBefore,
            "movementDateFrom must not be after movementDateBefore"
        ) { from, before -> from.isAfter(before) }
    }

    fun guardValidPageSize(requestedSize: Int) {
        SearchGuards.guardPageSize(requestedSize, MIN_PAGE_SIZE, MAX_PAGE_SIZE)
    }
}
