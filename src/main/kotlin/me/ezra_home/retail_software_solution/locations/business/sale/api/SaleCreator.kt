package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale.SaleAssembler
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLinePreparer
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLinesUpdater
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleStockReserver
import me.ezra_home.retail_software_solution.locations.business.sale.SaleValidator
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentService
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleLineStockRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleStockUpdater
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SaleCreator(
    private val saleRepository: SaleRepository,
    private val saleLineRepository: SaleLineRepository,
    private val saleLinePreparer: SaleLinePreparer,
    private val saleStockReserver: SaleStockReserver,
    private val saleValidator: SaleValidator,
    private val saleAssembler: SaleAssembler,
    private val saleHandlerForKafka: SaleHandlerForKafka,
    private val salePaymentService: SalePaymentService,
    private val saleLinesUpdater: SaleLinesUpdater,
    private val saleStockUpdater: SaleStockUpdater
) {

    fun createDraft(dto: SaleCreateDto): SaleResponseDto {
        val resolvedQuantities = saleLinePreparer.prepareForInsert(dto.linesToAdd)
        val factors = saleLinePreparer.resolveFactors(dto.linesToAdd)
        val sale = SaleEntity(
            contactId = resolveContactId(dto),
            soldBy = dto.soldBy,
            dateSold = dto.dateSold,
            notes = dto.notes
        ).also { saleRepository.saveAndFlush(it) }

        val lines = toLineEntities(sale.id!!, dto.linesToAdd, factors)
        saleLineRepository.saveAll(lines)
        saleStockReserver.reserve(sale.id!!, lines, resolvedQuantities)
        if (dto.payments.isNotEmpty()) {
            val saleTotal = lines.sumOf { Decimals.multiplyScale4(it.quantity, it.unitPrice) }
            salePaymentService.recordPaymentsSubmittedWithSale(sale.id!!, sale.contactId, dto.payments, saleTotal)
        }
        return saleAssembler.buildResponse(sale, lines)
    }

    fun toLineEntities(
        saleId: UUID,
        dtoLines: List<SaleLineCreateDto>,
        factorByProductId: Map<UUID, BigDecimal>
    ): List<SaleLineEntity> =
        dtoLines.map { line ->
            SaleLineEntity(
                saleId,
                line.locationProductId,
                line.quantity,
                line.unitId,
                line.unitPrice,
                factorByProductId[line.locationProductId]!!
            )
        }

    fun updateDraft(dto: SaleUpdateDto): SaleResponseDto {
        val sale = saleRepository.getReferenceById(dto.id)
        SaleValidator.guardIsDraft(sale)
        applyContactId(sale, dto)
        sale.soldBy = dto.soldBy
        sale.dateSold = dto.dateSold
        sale.notes = dto.notes
        saleRepository.save(sale)
        val (lines, _) = saleLinesUpdater.applyLineUpdates(sale.id!!, dto)
        if (dto.payments.isNotEmpty()) {
            val saleTotal = lines.sumOf { Decimals.multiplyScale4(it.quantity, it.unitPrice) }
            salePaymentService.recordPaymentsSubmittedWithSale(sale.id!!, sale.contactId, dto.payments, saleTotal)
        }
        return saleAssembler.buildResponse(sale, lines)
    }

    fun createSale(dto: SaleCreateDto): SaleResponseDto {
        val resolvedQuantities = saleLinePreparer.prepareForCreate(dto)
        val factors = saleLinePreparer.resolveFactors(dto.linesToAdd)
        val contactId = resolveContactId(dto)
        val sale = SaleEntity(
            contactId = contactId,
            soldBy = dto.soldBy ?: SessionContextProvider.getUserId(),
            dateSold = dto.dateSold ?: DateTimes.Offset.Now.organization(),
            notes = dto.notes,
            status = SaleStatus.CONFIRMED
        )
        saleRepository.saveAndFlush(sale)
        val lines: List<SaleLineEntity> = toLineEntities(sale.id!!, dto.linesToAdd, factors)
        saleLineRepository.saveAll(lines)
        if (dto.payments.isNotEmpty()) {
            val saleTotal = lines.sumOf { Decimals.multiplyScale4(it.quantity, it.unitPrice) }
            salePaymentService.recordPaymentsSubmittedWithSale(sale.id!!, contactId, dto.payments, saleTotal)
        }
        runFifoConsumption(lines, resolvedQuantities, sale.referenceNumber!!)
        saleHandlerForKafka.publish(sale, lines)
        return saleAssembler.buildResponse(sale, lines)
    }

    private fun runFifoConsumption(
        lines: List<SaleLineEntity>,
        baseQtyByProductId: Map<UUID, BigDecimal>,
        saleRefNumber: String
    ) {
        val requests = lines.map {
            SaleLineStockRequest(
                saleLineId = it.id!!,
                locationProductId = it.locationProductId,
                quantity = baseQtyByProductId[it.locationProductId]!!,
                unitId = it.unitId,
                conversionFactor = it.conversionFactor
            )
        }
        saleStockUpdater.consumeStock(requests, saleRefNumber)
    }

    fun convertDraftToSale(dto: SaleUpdateDto): SaleResponseDto {
        val sale = saleRepository.getReferenceById(dto.id)
        SaleValidator.guardIsDraft(sale)
        applyContactId(sale, dto)
        sale.soldBy = dto.soldBy ?: sale.soldBy ?: SessionContextProvider.getUserId()
        sale.dateSold = dto.dateSold ?: sale.dateSold ?: DateTimes.Offset.Now.organization()
        sale.notes = dto.notes ?: sale.notes
        val (lines, baseQtyByProductId) = saleLinesUpdater.applyLineUpdates(sale.id!!, dto)
        SaleValidator.guardHasLines(lines)
        val saleTotal = lines.sumOf { Decimals.multiplyScale4(it.quantity, it.unitPrice) }
        if (dto.payments.isNotEmpty()) {
            salePaymentService.recordPaymentsSubmittedWithSale(sale.id!!, sale.contactId, dto.payments, saleTotal)
        }
        if (sale.contactId == SystemContact.WALK_IN.id) {
            saleValidator.guardWalkInPaymentCoverage(dto.id, saleTotal)
        }
        sale.status = SaleStatus.CONFIRMED
        saleRepository.save(sale)
        runFifoConsumption(lines, baseQtyByProductId, sale.referenceNumber!!)
        saleStockReserver.clearBySale(dto.id)
        saleHandlerForKafka.publish(sale, lines)
        return saleAssembler.buildResponse(sale, lines)
    }

    private fun resolveContactId(dto: SaleCreateDto): UUID {
        return if (dto.walkInCustomer) {
            SystemContact.WALK_IN.id
        } else {
            dto.contactId ?: throw RtsGenericException("Customer is required for non-walk-in sales")
        }
    }

    private fun applyContactId(sale: SaleEntity, dto: SaleUpdateDto) {
        when {
            dto.walkInCustomer -> sale.contactId = SystemContact.WALK_IN.id
            dto.contactId != null -> sale.contactId = dto.contactId.orElseThrow {
                RtsGenericException("contactId cannot be null")
            }
        }
    }
}
