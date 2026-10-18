package me.ezra_home.retail_software_solution.organizations.business.fund_movement.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.FundMovementResponseBuilder
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.search.FundMovementSearchExecutor
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.search.FundMovementSearchQueryBuilder
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.search.FundMovementSearchRowMapper
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.search.FundMovementSearchValidator
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class FundMovementSearchService(
    private val fundMovementSearchExecutor: FundMovementSearchExecutor,
    private val fundMovementResponseBuilder: FundMovementResponseBuilder
) {

    fun search(pageRequest: PageRequest<FundMovementSearchParameters, String>): PageResponse<FundMovementResponse, String> {
        val fundMovementSearchParameters = pageRequest.parameters.sanitized()
        FundMovementSearchValidator.guardValidParameters(fundMovementSearchParameters)
        FundMovementSearchValidator.guardValidPageSize(pageRequest.requestedSize)
        val cursor = KeysetSearchCursor.decode(pageRequest.previousCursor)

        val predicate = FundMovementSearchQueryBuilder.buildPredicate(fundMovementSearchParameters)
        val sqlQuery = FundMovementSearchQueryBuilder.buildListQuery(predicate, cursor)
        val rawRows = fundMovementSearchExecutor.execute(sqlQuery, pageRequest.requestedSize + 1, setTimeout = true)
            .map { FundMovementSearchRowMapper.fromTuple(it) }
        val hasMore = rawRows.size > pageRequest.requestedSize
        val pageRows = if (hasMore) rawRows.take(pageRequest.requestedSize) else rawRows

        val contents = fundMovementResponseBuilder.buildResponses(pageRows)

        val currentCursor = pageRows.lastOrNull()
            ?.let { KeysetSearchCursor(it.fundMovementDto.createdOn, it.fundMovementDto.id).encode() }
            ?: pageRequest.previousCursor

        return PageResponse(currentCursor = currentCursor, hasMore = hasMore, contents = contents)
    }
}
