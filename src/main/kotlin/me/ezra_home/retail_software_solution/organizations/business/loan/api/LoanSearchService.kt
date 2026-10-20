package me.ezra_home.retail_software_solution.organizations.business.loan.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.loan.LoanMapper
import me.ezra_home.retail_software_solution.organizations.business.loan.LoanRepository
import me.ezra_home.retail_software_solution.organizations.business.loan.LoanResponseBuilder
import me.ezra_home.retail_software_solution.organizations.business.loan.search.LoanSearchExecutor
import me.ezra_home.retail_software_solution.organizations.business.loan.search.LoanSearchQueryBuilder
import me.ezra_home.retail_software_solution.organizations.business.loan.search.LoanSearchRowMapper
import me.ezra_home.retail_software_solution.organizations.business.loan.search.LoanSearchValidator
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class LoanSearchService(
    private val loanSearchExecutor: LoanSearchExecutor,
    private val loanRepository: LoanRepository,
    private val loanMapper: LoanMapper,
    private val loanResponseBuilder: LoanResponseBuilder,
    private val contactService: ContactService
) {

    fun search(pageRequest: PageRequest<LoanSearchParameters, String>): PageResponse<LoanResponse, String> {
        val loanSearchParameters = pageRequest.parameters.sanitized()
        LoanSearchValidator.guardValidParameters(loanSearchParameters)
        LoanSearchValidator.guardValidPageSize(pageRequest.requestedSize)
        val cursor = KeysetSearchCursor.decode(pageRequest.previousCursor)

        val predicate = LoanSearchQueryBuilder.buildPredicate(loanSearchParameters)
        val sqlQuery = LoanSearchQueryBuilder.buildListQuery(predicate, cursor)
        val rawRows = loanSearchExecutor.execute(sqlQuery, pageRequest.requestedSize + 1, setTimeout = true)
            .map { LoanSearchRowMapper.fromTuple(it) }
        val hasMore = rawRows.size > pageRequest.requestedSize
        val pageRows = if (hasMore) rawRows.take(pageRequest.requestedSize) else rawRows

        val loanDtosById = loanRepository.findAllById(pageRows.map { it.id })
            .map { loanMapper.toDto(it) }
            .associateBy { it.id }
        val contents = loanResponseBuilder.buildResponses(pageRows.map { loanDtosById.getValue(it.id) }, contactService.getContactsById())

        val currentCursor = pageRows.lastOrNull()
            ?.let { KeysetSearchCursor(it.createdOn, it.id).encode() }
            ?: pageRequest.previousCursor

        return PageResponse(currentCursor = currentCursor, hasMore = hasMore, contents = contents)
    }
}
