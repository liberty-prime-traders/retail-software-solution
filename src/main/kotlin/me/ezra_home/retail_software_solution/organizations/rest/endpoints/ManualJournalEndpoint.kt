package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.manual_journal.api.ManualJournalCreateDto
import me.ezra_home.retail_software_solution.organizations.business.manual_journal.api.ManualJournalResponseDto
import me.ezra_home.retail_software_solution.organizations.business.manual_journal.api.ManualJournalReversalCreateDto
import me.ezra_home.retail_software_solution.organizations.business.manual_journal.api.ManualJournalDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.manual_journal.api.ManualJournalService
import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import me.ezra_home.retail_software_solution.util.annotations.RequiresFeature
import me.ezra_home.retail_software_solution.util.enums.RtsPermissionNames
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/manual-journals")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class ManualJournalEndpoint(
    private val manualJournalService: ManualJournalService,
    private val manualJournalDataFetcher: ManualJournalDataFetcher
) {

    @PostMapping
    @PreAuthorize("hasAuthority('${RtsPermissionNames.CREATE_MANUAL_JOURNAL}')")
    fun create(@RequestBody manualJournalCreateDto: ManualJournalCreateDto): ManualJournalResponseDto =
        manualJournalService.create(manualJournalCreateDto)

    @PostMapping("{referenceNumber}/reversal")
    @PreAuthorize("hasAuthority('${RtsPermissionNames.REVERSE_MANUAL_JOURNAL}')")
    fun reverse(
        @PathVariable referenceNumber: String,
        @RequestBody manualJournalReversalCreateDto: ManualJournalReversalCreateDto
    ): ManualJournalResponseDto = manualJournalService.reverse(referenceNumber, manualJournalReversalCreateDto)

    @GetMapping
    @PreAuthorize("hasAuthority('${RtsPermissionNames.VIEW_MANUAL_JOURNAL}')")
    fun getAll(): List<ManualJournalResponseDto> = manualJournalDataFetcher.getAll()
}
