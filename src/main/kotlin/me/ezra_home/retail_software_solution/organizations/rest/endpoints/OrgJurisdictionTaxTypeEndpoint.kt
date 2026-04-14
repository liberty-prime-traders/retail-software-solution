package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeInsertDto
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeResponseDto
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeService
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeUpdateDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api.JurisdictionTaxesTreeBuilder
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxRecoveryType
import me.ezra_home.retail_software_solution.util.ui_models.TreeNodeWithData
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/org-tax-types")
@PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
class OrgJurisdictionTaxTypeEndpoint(
    private val orgJurisdictionTaxTypeService: OrgJurisdictionTaxTypeService,
    private val jurisdictionTaxesTreeBuilder: JurisdictionTaxesTreeBuilder
) {

    @GetMapping
    fun getAll(): List<OrgJurisdictionTaxTypeResponseDto> = orgJurisdictionTaxTypeService.getAll()

    @GetMapping("available")
    fun getTree(): List<TreeNodeWithData<UUID, TaxRecoveryType>> = jurisdictionTaxesTreeBuilder.getAvailableTaxTypes()

    @PostMapping
    fun createAll(@RequestBody dtos: List<OrgJurisdictionTaxTypeInsertDto>): List<OrgJurisdictionTaxTypeResponseDto> =
        orgJurisdictionTaxTypeService.createAll(dtos)

    @PutMapping
    fun update(@RequestBody dto: OrgJurisdictionTaxTypeUpdateDto): OrgJurisdictionTaxTypeResponseDto =
        orgJurisdictionTaxTypeService.update(dto)
}
