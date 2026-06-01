package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSaveResult
import me.ezra_home.retail_software_solution.locations.business.sale.api.ConfirmedSalePersister
import me.ezra_home.retail_software_solution.locations.business.sale.api.DraftSalePersister
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdater
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleVoidCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionAssembler
import me.ezra_home.retail_software_solution.locations.business.sale_session.SessionToSaveRequestMapper
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionTotalsCalculator
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleSessionPersister(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionAssembler: SaleSessionAssembler,
    private val saleSessionValidator: SaleSessionValidator,
    private val saleSessionTotalsCalculator: SaleSessionTotalsCalculator,
    private val sessionToSaveRequestMapper: SessionToSaveRequestMapper,
    private val draftSalePersister: DraftSalePersister,
    private val confirmedSalePersister: ConfirmedSalePersister,
    private val saleUpdater: SaleUpdater,
) {

    fun saveDraft(sessionId: UUID): SaleSessionResponseDto {
        val saleSession = loadAndValidate(sessionId)
        val saleSaveRequest = sessionToSaveRequestMapper.toSaleSaveRequest(saleSession)
        val saleSaveResult = draftSalePersister.saveDraft(saleSaveRequest)
        val saleSessionAfterSave = applySaleSaveResult(saleSession, saleSaveResult)
        val saleSessionWithTotals = saleSessionTotalsCalculator.recompute(saleSessionAfterSave)
        saleSessionStore.save(saleSessionWithTotals)
        return saleSessionAssembler.buildResponse(saleSessionWithTotals)
    }

    fun confirm(sessionId: UUID): SaleSessionResponseDto {
        val saleSession = loadAndValidate(sessionId)
        saleSessionValidator.guardNonEmptyLines(saleSession)
        saleSessionValidator.guardWalkInFullyCovered(saleSession)
        saleSessionValidator.guardPaymentsWithinTotal(saleSession)
        val saleSaveRequest = sessionToSaveRequestMapper.toSaleSaveRequest(saleSession)
        val saleSaveResult = confirmedSalePersister.confirm(saleSaveRequest)
        val saleSessionAfterSave = applySaleSaveResult(saleSession, saleSaveResult)
        val saleSessionResponseDto = saleSessionAssembler.buildResponse(saleSessionAfterSave)
        saleSessionStore.delete(sessionId)
        return saleSessionResponseDto
    }

    fun voidSale(sessionId: UUID, saleSessionVoidDto: SaleSessionVoidDto): SaleSessionResponseDto {
        val saleSession = saleSessionStore.load(sessionId)
        val saleId = saleSession.saleId
        if (saleId == null) {
            saleSessionStore.delete(sessionId)
            return saleSessionAssembler.buildResponse(saleSession)
        }
        val saleSummary = saleUpdater.voidSale(SaleVoidCreateDto(saleId, saleSessionVoidDto.reason))
        val now = DateTimes.Offset.Now.organization()
        val saleSessionAfterVoid = saleSession.copy(
            originalStatus = saleSummary.status,
            lastUpdatedAt = now,
            lastAccessedById = SessionContextProvider.getUserId(),
            lastAccessedAt = now,
        )
        val saleSessionResponseDto = saleSessionAssembler.buildResponse(saleSessionAfterVoid)
        saleSessionStore.delete(sessionId)
        return saleSessionResponseDto
    }

    private fun loadAndValidate(sessionId: UUID): SaleSession {
        val saleSession = saleSessionStore.load(sessionId)
        saleSessionValidator.guardMutable(saleSession)
        val saleSessionWithTotals = saleSessionTotalsCalculator.recompute(saleSession)
        saleSessionValidator.validate(saleSessionWithTotals)
        return saleSessionWithTotals
    }

    private fun applySaleSaveResult(saleSession: SaleSession, saleSaveResult: SaleSaveResult): SaleSession {
        val now = DateTimes.Offset.Now.organization()
        val userId = SessionContextProvider.getUserId()
        return saleSession.copy(
            saleId = saleSaveResult.saleId,
            saleVersion = saleSaveResult.newVersion,
            originalStatus = saleSaveResult.saleStatus,
            lastUpdatedAt = now,
            lastAccessedAt = now,
            lastAccessedById = userId,
            header = saleSession.header.copy(
                referenceNumber = saleSaveResult.saleReferenceNumber,
                dateSold = saleSaveResult.dateSold,
                soldById = saleSaveResult.soldById,
            ),
            saleLines = saleSession.saleLines.map { saleSessionLine ->
                val newSaleLineId = saleSaveResult.saleLineIdsByClientKey[saleSessionLine.identity.key()]
                if (newSaleLineId != null && !saleSessionLine.identity.isPersisted()) {
                    saleSessionLine.copy(identity = SessionIdentity.persisted(newSaleLineId))
                } else saleSessionLine
            },
            saleAdjustments = saleSession.saleAdjustments.map { saleSessionAdjustment ->
                val newSaleAdjustmentId = saleSaveResult.saleAdjustmentIdsByClientKey[saleSessionAdjustment.identity.key()]
                val updatedIdentity = if (newSaleAdjustmentId != null && !saleSessionAdjustment.identity.isPersisted()) {
                    SessionIdentity.persisted(newSaleAdjustmentId)
                } else saleSessionAdjustment.identity
                val updatedRelatedSaleLineIdentity = saleSessionAdjustment.relatedSaleLineIdentity?.let { relatedSaleLineIdentity ->
                    val newSaleLineId = saleSaveResult.saleLineIdsByClientKey[relatedSaleLineIdentity.key()]
                    if (newSaleLineId != null && !relatedSaleLineIdentity.isPersisted()) {
                        SessionIdentity.persisted(newSaleLineId)
                    } else relatedSaleLineIdentity
                }
                saleSessionAdjustment.copy(identity = updatedIdentity, relatedSaleLineIdentity = updatedRelatedSaleLineIdentity)
            },
            salePayments = saleSession.salePayments.map { saleSessionPayment ->
                val persistedSalePayment = saleSaveResult.persistedSalePaymentsByClientKey[saleSessionPayment.identity.key()]
                if (persistedSalePayment != null && !saleSessionPayment.identity.isPersisted()) {
                    saleSessionPayment.copy(
                        identity = SessionIdentity.persisted(persistedSalePayment.id),
                        paymentDate = persistedSalePayment.paymentDate,
                    )
                } else saleSessionPayment
            },
        )
    }
}
