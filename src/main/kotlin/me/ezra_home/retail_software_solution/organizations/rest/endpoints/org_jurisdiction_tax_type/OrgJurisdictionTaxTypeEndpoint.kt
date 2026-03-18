package me.ezra_home.retail_software_solution.organizations.rest.endpoints.org_jurisdiction_tax_type

import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.OrgJurisdictionTaxTypeService
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.dto.OrgJurisdictionTaxTypeInsertDto
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.dto.OrgJurisdictionTaxTypeResponseDto
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.dto.OrgJurisdictionTaxTypeUpdateDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.JurisdictionTaxTypeService
import me.ezra_home.retail_software_solution.util.ui_models.TreeNode
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/org-jurisdiction-tax-types")
@PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
class OrgJurisdictionTaxTypeEndpoint(
    private val orgJurisdictionTaxTypeService: OrgJurisdictionTaxTypeService,
    private val jurisdictionTaxTypeService: JurisdictionTaxTypeService
) {

    @GetMapping
    fun getAll(): List<OrgJurisdictionTaxTypeResponseDto> = orgJurisdictionTaxTypeService.getAll()

    @GetMapping("available")
    fun getTree(): List<TreeNode<UUID>> = jurisdictionTaxTypeService.getAvailableTaxTypes()

    @PostMapping
    fun createAll(@RequestBody dtos: List<OrgJurisdictionTaxTypeInsertDto>): List<OrgJurisdictionTaxTypeResponseDto> =
        orgJurisdictionTaxTypeService.createAll(dtos)

    @PutMapping
    fun update(@RequestBody dto: OrgJurisdictionTaxTypeUpdateDto): OrgJurisdictionTaxTypeResponseDto =
        orgJurisdictionTaxTypeService.update(dto)
}
