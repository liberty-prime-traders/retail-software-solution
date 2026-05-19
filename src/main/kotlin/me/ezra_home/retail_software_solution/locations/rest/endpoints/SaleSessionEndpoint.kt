package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustmentAddDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustmentHandler
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionCommitHandler
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHandler
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHeaderHandler
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHeaderUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLineAddDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLineHandler
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLineUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionPaymentAddDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionPaymentHandler
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionPaymentRemoveDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionResponseDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionRowIdentityDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionStartDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionSummaryDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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
    private val saleSessionCommitHandler: SaleSessionCommitHandler,
) {

    @PostMapping
    fun start(@RequestBody dto: SaleSessionStartDto): SaleSessionResponseDto =
        saleSessionHandler.start(dto)

    @GetMapping
    fun listOpenSessions(): List<SaleSessionSummaryDto> = saleSessionHandler.listOpenSessions()

    @GetMapping("{sessionId}")
    fun acquireSession(@PathVariable sessionId: UUID): SaleSessionResponseDto =
        saleSessionHandler.acquireSession(sessionId)

    @DeleteMapping("{sessionId}")
    fun abandon(@PathVariable sessionId: UUID): ResponseEntity<Unit> {
        saleSessionHandler.abandon(sessionId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("{sessionId}/lines")
    fun addLine(
        @PathVariable sessionId: UUID,
        @RequestBody dto: SaleSessionLineAddDto,
    ): SaleSessionResponseDto = saleSessionLineHandler.addLine(sessionId, dto)

    @PutMapping("{sessionId}/lines")
    fun updateLine(
        @PathVariable sessionId: UUID,
        @RequestBody dto: SaleSessionLineUpdateDto,
    ): SaleSessionResponseDto = saleSessionLineHandler.updateLine(sessionId, dto)

    @DeleteMapping("{sessionId}/lines")
    fun removeLine(
        @PathVariable sessionId: UUID,
        @RequestBody dto: SaleSessionRowIdentityDto,
    ): SaleSessionResponseDto = saleSessionLineHandler.removeLine(sessionId, dto)

    @PostMapping("{sessionId}/adjustments")
    fun addAdjustment(
        @PathVariable sessionId: UUID,
        @RequestBody dto: SaleSessionAdjustmentAddDto,
    ): SaleSessionResponseDto = saleSessionAdjustmentHandler.add(sessionId, dto)

    @DeleteMapping("{sessionId}/adjustments")
    fun removeAdjustment(
        @PathVariable sessionId: UUID,
        @RequestBody dto: SaleSessionRowIdentityDto,
    ): SaleSessionResponseDto = saleSessionAdjustmentHandler.remove(sessionId, dto)

    @PostMapping("{sessionId}/payments")
    fun addPayment(
        @PathVariable sessionId: UUID,
        @RequestBody dto: SaleSessionPaymentAddDto,
    ): SaleSessionResponseDto = saleSessionPaymentHandler.add(sessionId, dto)

    @DeleteMapping("{sessionId}/payments")
    fun removePayment(
        @PathVariable sessionId: UUID,
        @RequestBody dto: SaleSessionPaymentRemoveDto,
    ): SaleSessionResponseDto = saleSessionPaymentHandler.remove(sessionId, dto)

    @PatchMapping("{sessionId}/header")
    fun updateHeader(
        @PathVariable sessionId: UUID,
        @RequestBody dto: SaleSessionHeaderUpdateDto,
    ): SaleSessionResponseDto = saleSessionHeaderHandler.update(sessionId, dto)

    @PostMapping("{sessionId}/draft")
    fun saveDraft(@PathVariable sessionId: UUID): SaleSessionResponseDto =
        saleSessionCommitHandler.saveDraft(sessionId)

    @PostMapping("{sessionId}/confirm")
    fun confirm(@PathVariable sessionId: UUID): SaleSessionResponseDto =
        saleSessionCommitHandler.confirm(sessionId)
}
