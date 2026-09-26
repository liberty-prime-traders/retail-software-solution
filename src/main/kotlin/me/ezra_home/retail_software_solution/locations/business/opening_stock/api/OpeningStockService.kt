package me.ezra_home.retail_software_solution.locations.business.opening_stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.opening_stock.OpeningStockEntity
import me.ezra_home.retail_software_solution.locations.business.opening_stock.OpeningStockHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.opening_stock.OpeningStockRepository
import me.ezra_home.retail_software_solution.locations.business.opening_stock.OpeningStockValidator
import me.ezra_home.retail_software_solution.locations.business.stock.api.OpeningStockLineStockRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.OpeningStockStockUpdater
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class OpeningStockService(
    private val openingStockRepository: OpeningStockRepository,
    private val openingStockStockUpdater: OpeningStockStockUpdater,
    private val locationProductService: LocationProductService,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val fiscalPeriodService: FiscalPeriodService,
    private val openingStockHandlerForKafka: OpeningStockHandlerForKafka,
    private val userQualifier: UserQualifier
) {

    fun declareInitialStock(lines: List<OpeningStockLineDto>): List<OpeningStockResponseDto> {
        OpeningStockValidator.guardNotEmpty(lines)
        OpeningStockValidator.guardNoDuplicateProducts(lines)

        val productIds = lines.map { it.locationProductId }
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.Now.organization())
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, productIds)

        val labelsByProductId = locationProductDataFetcher.findSummaryByIds(productIds).mapValues { it.value.label }
        OpeningStockValidator.guardPositiveQuantitiesAndCosts(lines, labelsByProductId)
        locationProductService.guardAllActive(productIds)

        val alreadyDeclared = openingStockRepository.findByLocationProductIdIn(productIds)
            .map { it.locationProductId }.toSet()
        OpeningStockValidator.guardNotAlreadyDeclared(alreadyDeclared, productIds, labelsByProductId)

        val savedOpeningStocks = openingStockRepository.saveAll(
            lines.map { line ->
                OpeningStockEntity(
                    locationProductId = line.locationProductId,
                    quantity = line.quantity,
                    unitCost = line.unitCost
                )
            }
        )
        val savedByProductId = savedOpeningStocks.associateBy { it.locationProductId }

        openingStockStockUpdater.recordOpeningStock(
            lines.map { line ->
                val openingStock = savedByProductId.getValue(line.locationProductId)
                OpeningStockLineStockRequest(
                    locationProductId = line.locationProductId,
                    quantity = line.quantity,
                    unitId = line.unitId,
                    unitCost = line.unitCost,
                    externalReferenceNumber = openingStock.requiredReference()
                )
            }
        )

        savedOpeningStocks.forEach { openingStock -> openingStockHandlerForKafka.publish(openingStock) }
        return savedOpeningStocks.map { toResponseDto(it, labelsByProductId.getValue(it.locationProductId)) }
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun getAll(): List<OpeningStockResponseDto> {
        val all = openingStockRepository.findAll()
        val labelsByProductId = locationProductDataFetcher.findSummaryByIds(all.map { it.locationProductId }).mapValues { it.value.label }
        return all.map { toResponseDto(it, labelsByProductId[it.locationProductId] ?: it.locationProductId.toString()) }
    }

    private fun toResponseDto(openingStock: OpeningStockEntity, productLabel: String) = OpeningStockResponseDto(
        referenceNumber = openingStock.requiredReference(),
        locationProductId = openingStock.locationProductId,
        productLabel = productLabel,
        quantity = openingStock.quantity,
        unitCost = openingStock.unitCost,
        declaredBy = userQualifier.getCreatorFullName(openingStock.requiredCreatedById()),
        declaredAt = openingStock.requiredCreatedOn()
    )
}
