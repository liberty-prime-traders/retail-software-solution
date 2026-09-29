package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustmentAddDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustmentHandler
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHandler
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHeaderHandler
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHeaderUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLineHandler
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLineRequestDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionPaymentAddDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionPaymentHandler
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionPaymentRemoveDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionPersister
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionResponseDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionRowIdentityDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionStartDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionVoidDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/sale-sessions")
class SaleSessionEndpoint(
    private val saleSessionHandler: SaleSessionHandler,
    private val saleSessionHeaderHandler: SaleSessionHeaderHandler,
    private val saleSessionLineHandler: SaleSessionLineHandler,
    private val saleSessionAdjustmentHandler: SaleSessionAdjustmentHandler,
    private val saleSessionPaymentHandler: SaleSessionPaymentHandler,
    private val saleSessionPersister: SaleSessionPersister,
) {

    @PostMapping
    fun start(@RequestBody sessionStartDto: SaleSessionStartDto): SaleSessionResponseDto =
        saleSessionHandler.start(sessionStartDto)

    @GetMapping("unsaved")
    fun listOpenSessions(
        @RequestParam(required = false, defaultValue = "false") mineOnly: Boolean
    ): List<SaleSessionSummaryDto> = saleSessionHandler.getSessionsForUnsavedSales(mineOnly)

    @GetMapping("{sessionId}")
    fun acquireSession(@PathVariable sessionId: UUID): SaleSessionResponseDto =
        saleSessionHandler.acquireSession(sessionId)

    @DeleteMapping("unsaved/{sessionId}")
    fun abandon(@PathVariable sessionId: UUID): ResponseEntity<Unit> {
        saleSessionHandler.abandon(sessionId)
        return ResponseEntity.ok().build()
    }

    @PutMapping("{sessionId}/lines")
    fun applyLineChanges(
        @PathVariable sessionId: UUID,
        @RequestBody lineRequestDto: SaleSessionLineRequestDto,
    ): SaleSessionResponseDto = saleSessionLineHandler.applyLineChanges(sessionId, lineRequestDto)

    @DeleteMapping("{sessionId}/lines")
    fun removeLine(
        @PathVariable sessionId: UUID,
        @RequestBody rowIdentityDto: SaleSessionRowIdentityDto,
    ): SaleSessionResponseDto = saleSessionLineHandler.removeLine(sessionId, rowIdentityDto)

    @PostMapping("{sessionId}/adjustments")
    fun addAdjustment(
        @PathVariable sessionId: UUID,
        @RequestBody adjustmentAddDto: SaleSessionAdjustmentAddDto,
    ): SaleSessionResponseDto = saleSessionAdjustmentHandler.add(sessionId, adjustmentAddDto)

    @DeleteMapping("{sessionId}/adjustments")
    fun removeAdjustment(
        @PathVariable sessionId: UUID,
        @RequestBody rowIdentityDto: SaleSessionRowIdentityDto,
    ): SaleSessionResponseDto = saleSessionAdjustmentHandler.remove(sessionId, rowIdentityDto)

    @PostMapping("{sessionId}/payments")
    fun addPayment(
        @PathVariable sessionId: UUID,
        @RequestBody paymentAddDto: SaleSessionPaymentAddDto,
    ): SaleSessionResponseDto = saleSessionPaymentHandler.add(sessionId, paymentAddDto)

    @DeleteMapping("{sessionId}/payments")
    fun removePayment(
        @PathVariable sessionId: UUID,
        @RequestBody paymentRemoveDto: SaleSessionPaymentRemoveDto,
    ): SaleSessionResponseDto = saleSessionPaymentHandler.remove(sessionId, paymentRemoveDto)

    @PutMapping("{sessionId}/header")
    fun updateHeader(
        @PathVariable sessionId: UUID,
        @RequestBody headerUpdateDto: SaleSessionHeaderUpdateDto,
    ): SaleSessionResponseDto = saleSessionHeaderHandler.update(sessionId, headerUpdateDto)

    @PostMapping("{sessionId}/draft")
    fun saveDraft(@PathVariable sessionId: UUID): SaleSessionResponseDto =
        saleSessionPersister.saveDraft(sessionId)

    @PostMapping("{sessionId}/confirm")
    fun confirm(@PathVariable sessionId: UUID): SaleSessionResponseDto =
        saleSessionPersister.confirm(sessionId)

    @PostMapping("{sessionId}/void")
    fun voidSale(
        @PathVariable sessionId: UUID,
        @RequestBody saleSessionVoidDto: SaleSessionVoidDto,
    ): SaleSessionResponseDto = saleSessionPersister.voidSale(sessionId, saleSessionVoidDto)
}
