package me.ezra_home.retail_software_solution.organizations.business.product_tax_assignment

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.product_tax_assignment.dto.ProductTaxAssignmentInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_tax_assignment.dto.ProductTaxAssignmentResponseDto
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.TaxRateCache
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ProductTaxAssignmentService(
    private val productTaxAssignmentMapper: ProductTaxAssignmentMapper,
    private val productTaxAssignmentRepository: ProductTaxAssignmentRepository,
    private val taxRateCache: TaxRateCache
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getByProduct(productId: UUID): Collection<ProductTaxAssignmentResponseDto> =
        productTaxAssignmentRepository.findByProductId(productId)
            .map { productTaxAssignmentMapper.toResponseDto(it) }

    fun assign(dto: ProductTaxAssignmentInsertDto): ProductTaxAssignmentResponseDto {
        if (dto.endDate != null && dto.startDate.isAfter(dto.endDate))
            throw RtsGenericException("startDate must not be after endDate")
        val taxRate = taxRateCache.getAll().firstOrNull { it.id == dto.taxRateId }
            ?: throw RtsGenericException("Tax rate not found")

        val overlapping = productTaxAssignmentRepository.findOverlapping(
            productId = dto.productId,
            orgJurisdictionTaxTypeId = taxRate.orgJurisdictionTaxTypeId,
            from = dto.startDate,
            to = dto.endDate,
            excludeId = null
        )
        if (overlapping.isNotEmpty()) {
            throw RtsGenericException("An overlapping tax assignment already exists for this product and tax type")
        }

        val entity = productTaxAssignmentMapper.toEntity(dto)
        productTaxAssignmentRepository.save(entity)
        return productTaxAssignmentMapper.toResponseDto(entity)
    }

    fun close(id: UUID, endDate: LocalDate): ProductTaxAssignmentResponseDto {
        val entity = productTaxAssignmentRepository.findById(id)
            .orElseThrow { UpdatingNonExistingRecordException() }
        val today = DateTimes.Local.Now.organization()
        if (entity.startDate > today) {
            throw RtsGenericException("Use delete to remove a pending assignment")
        }
        entity.endDate = endDate
        productTaxAssignmentRepository.save(entity)
        return productTaxAssignmentMapper.toResponseDto(entity)
    }

    fun updatePending(id: UUID, startDate: LocalDate, endDate: LocalDate?): ProductTaxAssignmentResponseDto {
        val entity = productTaxAssignmentRepository.findById(id)
            .orElseThrow { UpdatingNonExistingRecordException() }
        if (endDate != null && startDate.isAfter(endDate))
            throw RtsGenericException("startDate must not be after endDate")
        val today = DateTimes.Local.Now.organization()
        if (entity.startDate <= today) {
            throw RtsGenericException("Only pending assignments can be edited")
        }
        val taxRate = taxRateCache.getAll().firstOrNull { it.id == entity.taxRateId }
            ?: throw RtsGenericException("Tax rate not found")
        val overlapping = productTaxAssignmentRepository.findOverlapping(
            productId = entity.productId,
            orgJurisdictionTaxTypeId = taxRate.orgJurisdictionTaxTypeId,
            from = startDate,
            to = endDate,
            excludeId = id
        )
        if (overlapping.isNotEmpty()) {
            throw RtsGenericException("An overlapping tax assignment already exists for this product and tax type")
        }
        entity.startDate = startDate
        entity.endDate = endDate
        productTaxAssignmentRepository.save(entity)
        return productTaxAssignmentMapper.toResponseDto(entity)
    }

    fun deletePending(id: UUID) {
        val entity = productTaxAssignmentRepository.findById(id)
            .orElseThrow { UpdatingNonExistingRecordException() }
        val today = DateTimes.Local.Now.organization()
        if (entity.startDate <= today) {
            throw RtsGenericException("Only pending assignments can be deleted")
        }
        productTaxAssignmentRepository.deleteById(id)
    }
}
