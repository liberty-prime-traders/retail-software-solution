package me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductUnitRequestDto
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentCategory
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentDecreaseStockRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentIncreaseStockRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentMovementDetails
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentStockUpdater
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.StockAdjustmentCategoryRules
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.StockAdjustmentEntity
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.StockAdjustmentHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.StockAdjustmentRepository
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.StockAdjustmentValidator
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.StockMovementReasonService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import me.ezra_home.retail_software_solution.util.business.reference_number.ReferenceNumberGenerator
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnLocationSchema
class StockAdjustmentService(
    private val stockAdjustmentRepository: StockAdjustmentRepository,
    private val stockAdjustmentStockUpdater: StockAdjustmentStockUpdater,
    private val stockAdjustmentHandlerForKafka: StockAdjustmentHandlerForKafka,
    private val locationProductService: LocationProductService,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val stockMovementReasonService: StockMovementReasonService,
    private val fiscalPeriodService: FiscalPeriodService,
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val referenceNumberGenerator: ReferenceNumberGenerator,
    private val userQualifier: UserQualifier
) {

    fun adjust(stockAdjustmentCreateDto: StockAdjustmentCreateDto): StockAdjustmentResponseDto {
        val direction = StockAdjustmentValidator.guardCategoryRequest(stockAdjustmentCreateDto)
        val locationProductId = stockAdjustmentCreateDto.locationProductId
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.Now.organization())
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, setOf(locationProductId))
        locationProductService.guardAllActive(listOf(locationProductId))
        if (StockAdjustmentCategoryRules.requiresReason(stockAdjustmentCreateDto.category)) {
            stockMovementReasonService.guardReason(direction.movementType, stockAdjustmentCreateDto.stockMovementReasonId)
        }

        val storageUnitsPerUnit = locationProductDataFetcher.getStorageUnitsPerUnit(
            listOf(LocationProductUnitRequestDto(locationProductId, stockAdjustmentCreateDto.unitId))
        ).getValue(locationProductId)
        val storageQuantity = ExactQuantity.ofUserProvided(stockAdjustmentCreateDto.userProvidedQuantity, storageUnitsPerUnit)
        val referenceNumber = referenceNumberGenerator.generateSingle(TableName.STOCK_ADJUSTMENT)
        val adjustmentDetails = StockAdjustmentMovementDetails(
            category = stockAdjustmentCreateDto.category,
            reasonId = stockAdjustmentCreateDto.stockMovementReasonId,
            note = StringUtils.getValueOrNull(stockAdjustmentCreateDto.note)
        )

        val productLabel = locationProductDataFetcher.findSummaryByIds(setOf(locationProductId))[locationProductId]?.label
            ?: locationProductId.toString()

        val totalCost = when (direction) {
            StockAdjustmentDirection.INCREASE -> applyIncrease(
                stockAdjustmentCreateDto, storageQuantity, storageUnitsPerUnit, referenceNumber, adjustmentDetails
            )
            StockAdjustmentDirection.DECREASE -> stockAdjustmentStockUpdater.decreaseStock(
                StockAdjustmentDecreaseStockRequest(
                    locationProductId = locationProductId,
                    productLabel = productLabel,
                    storageQuantity = storageQuantity,
                    userProvidedQuantity = stockAdjustmentCreateDto.userProvidedQuantity,
                    unitId = stockAdjustmentCreateDto.unitId,
                    storageUnitsPerUnit = storageUnitsPerUnit,
                    documentReference = referenceNumber,
                    adjustmentDetails = adjustmentDetails
                )
            ).totalCost
        }

        val savedStockAdjustment = stockAdjustmentRepository.save(
            StockAdjustmentEntity(
                locationProductId = locationProductId,
                category = stockAdjustmentCreateDto.category,
                direction = direction,
                userProvidedQuantity = stockAdjustmentCreateDto.userProvidedQuantity,
                unitId = stockAdjustmentCreateDto.unitId,
                storageUnitsPerUnit = storageUnitsPerUnit,
                storageQuantity = storageQuantity.toStorageQuantity(),
                totalCost = totalCost,
                stockMovementReasonId = stockAdjustmentCreateDto.stockMovementReasonId,
                note = adjustmentDetails.note
            ).also { it.referenceNumber = referenceNumber }
        )
        stockAdjustmentHandlerForKafka.publish(savedStockAdjustment)
        return toResponseDto(savedStockAdjustment, productLabel)
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun getAll(): List<StockAdjustmentResponseDto> {
        val stockAdjustments = stockAdjustmentRepository.findAll()
        val labelsByLocationProductId = locationProductDataFetcher
            .findSummaryByIds(stockAdjustments.map { stockAdjustment -> stockAdjustment.locationProductId }.toSet())
            .mapValues { locationProductSummary -> locationProductSummary.value.label }
        return stockAdjustments.map { stockAdjustment ->
            toResponseDto(
                stockAdjustment,
                labelsByLocationProductId[stockAdjustment.locationProductId] ?: stockAdjustment.locationProductId.toString()
            )
        }
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun getCategoryDescriptors(): List<StockAdjustmentCategoryDescriptor> {
        val reasonOptions = stockMovementReasonService.getAll().map { reason ->
            StockAdjustmentReasonOption(
                id = reason.id,
                name = reason.name,
                allowedDirections = StockAdjustmentDirection.entries.filter { direction -> direction.movementType in reason.movementTypes }
            )
        }.filter { reasonOption -> reasonOption.allowedDirections.isNotEmpty() }
        return StockAdjustmentCategory.entries.map { category ->
            StockAdjustmentCategoryDescriptor(
                category = category,
                displayName = StockAdjustmentCategoryRules.displayName(category),
                allowedDirections = StockAdjustmentCategoryRules.allowedDirections(category),
                unitCostDirections = StockAdjustmentCategoryRules.unitCostDirections(category),
                requiresReason = StockAdjustmentCategoryRules.requiresReason(category),
                requiresNote = StockAdjustmentCategoryRules.requiresNote(category),
                reasons = if (StockAdjustmentCategoryRules.requiresReason(category)) reasonOptions else emptyList()
            )
        }
    }

    private fun applyIncrease(
        stockAdjustmentCreateDto: StockAdjustmentCreateDto,
        storageQuantity: ExactQuantity,
        storageUnitsPerUnit: Long,
        referenceNumber: String,
        adjustmentDetails: StockAdjustmentMovementDetails
    ): BigDecimal {
        val totalCost = Decimals.Scale4.multiply(stockAdjustmentCreateDto.unitCost!!, stockAdjustmentCreateDto.userProvidedQuantity)
        stockAdjustmentStockUpdater.increaseStock(
            StockAdjustmentIncreaseStockRequest(
                locationProductId = stockAdjustmentCreateDto.locationProductId,
                storageQuantity = storageQuantity,
                userProvidedQuantity = stockAdjustmentCreateDto.userProvidedQuantity,
                unitId = stockAdjustmentCreateDto.unitId,
                storageUnitsPerUnit = storageUnitsPerUnit,
                totalCost = totalCost,
                documentReference = referenceNumber,
                adjustmentDetails = adjustmentDetails
            )
        )
        return totalCost
    }

    private fun toResponseDto(stockAdjustmentEntity: StockAdjustmentEntity, productLabel: String) = StockAdjustmentResponseDto(
        referenceNumber = stockAdjustmentEntity.requiredReference(),
        locationProductId = stockAdjustmentEntity.locationProductId,
        productLabel = productLabel,
        category = stockAdjustmentEntity.category,
        direction = stockAdjustmentEntity.direction,
        userProvidedQuantity = stockAdjustmentEntity.userProvidedQuantity,
        unitId = stockAdjustmentEntity.unitId,
        totalCost = stockAdjustmentEntity.totalCost,
        stockMovementReasonId = stockAdjustmentEntity.stockMovementReasonId,
        note = stockAdjustmentEntity.note,
        adjustedBy = userQualifier.getCreatorFullName(stockAdjustmentEntity.requiredCreatedById()),
        adjustedAt = stockAdjustmentEntity.requiredCreatedOn()
    )
}
