package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.purchase_movement_context_backfill.api.PurchaseMovementContextBackfiller
import me.ezra_home.retail_software_solution.locations.business.sale_cogs_backfill.api.SaleLineCogsBackfiller
import me.ezra_home.retail_software_solution.organizations.business.org_profile.api.OrgProfileService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
@RequestMapping("secured/admin-data")
@PreAuthorize("@rtsPermissions.isOrganizationAdmin()")
class AdminDataEndpoint(
    private val orgProfileService: OrgProfileService,
    private val saleLineCogsBackfiller: SaleLineCogsBackfiller,
    private val purchaseMovementContextBackfiller: PurchaseMovementContextBackfiller,
) {

    @PostMapping("seed-defaults")
    fun seedDefaults(): ResponseEntity<HttpStatus> {
        orgProfileService.applySeedDefaults()
        return ResponseEntity(HttpStatus.OK)
    }

    @PostMapping("sale-line-cogs/backfill")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun backfillSaleLineCogs() = saleLineCogsBackfiller.startBackfillAllLocations()

    @PostMapping("purchase-movement-context/backfill")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun backfillPurchaseMovementContext() = purchaseMovementContextBackfiller.startBackfillAllLocations()
}
