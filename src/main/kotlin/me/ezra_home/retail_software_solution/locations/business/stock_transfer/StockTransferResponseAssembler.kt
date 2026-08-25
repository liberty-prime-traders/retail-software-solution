package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.configuration.session.withLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.api.StockTransferDispatchResponseDto
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.api.StockTransferReceiptResponseDto
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.api.StockTransferResponse
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDomainDto
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.toSummaryDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class StockTransferResponseAssembler(
    private val stockTransferSchemaGateway: StockTransferSchemaGateway,
    private val locationService: LocationService
) {

    fun build(orderDomainDto: StockTransferOrderDomainDto): StockTransferResponse {
        val sourceLocationSchema = locationService.getSchemaByLocationId(orderDomainDto.sourceLocationId)
        val destinationLocationSchema = locationService.getSchemaByLocationId(orderDomainDto.destinationLocationId)

        val receiptSnapshot = locationService.withLocationSchema(destinationLocationSchema) {
            stockTransferSchemaGateway.readReceiptSnapshot(orderDomainDto.referenceNumber)
        }

        val dispatchWithLines = locationService.withLocationSchema(sourceLocationSchema) {
            stockTransferSchemaGateway.buildDispatchWithLines(
                orderRef = orderDomainDto.referenceNumber,
                isDraft = orderDomainDto.status == StockTransferStatus.DRAFT,
                confirmedRefs = receiptSnapshot?.confirmedDispatchLineRefs ?: emptySet()
            )
        }

        val receiptResponseDto = receiptSnapshot?.let { header ->
            StockTransferReceiptResponseDto(
                id = header.id,
                referenceNumber = header.referenceNumber,
                status = header.status,
                receivedById = header.receivedById,
                receivedAt = header.receivedAt,
                notes = header.notes,
                lines = dispatchWithLines.lines.filter { it.quantityReceived != null }
            )
        }

        return assemble(
            orderDomainDto = orderDomainDto,
            dispatch = StockTransferDispatchResponseDto(
                id = dispatchWithLines.dispatch.id,
                referenceNumber = dispatchWithLines.dispatch.referenceNumber,
                status = dispatchWithLines.dispatch.status,
                dispatchedById = dispatchWithLines.dispatch.dispatchedById,
                dispatchedAt = dispatchWithLines.dispatch.dispatchedAt,
                notes = dispatchWithLines.dispatch.notes,
                lines = dispatchWithLines.lines
            ),
            receipt = receiptResponseDto
        )
    }

    fun buildDispatchOnly(
        orderDomainDto: StockTransferOrderDomainDto,
        dispatchEntity: StockTransferDispatchEntity,
        lines: List<ReconciledTransferLine> = emptyList()
    ): StockTransferResponse = assemble(
        orderDomainDto = orderDomainDto,
        dispatch = StockTransferDispatchResponseDto(
            id = dispatchEntity.id!!,
            referenceNumber = dispatchEntity.requiredReference(),
            status = dispatchEntity.status,
            dispatchedById = dispatchEntity.dispatchedById,
            dispatchedAt = dispatchEntity.dispatchedAt,
            notes = dispatchEntity.notes,
            lines = lines
        ),
        receipt = null
    )

    fun buildDispatchAndReceipt(
        orderDomainDto: StockTransferOrderDomainDto,
        receiptEntity: StockTransferReceiptEntity,
        receiptLines: List<StockTransferReceiptLineEntity>
    ): StockTransferResponse {
        val sourceLocationSchema = locationService.getSchemaByLocationId(orderDomainDto.sourceLocationId)
        val confirmedRefs = receiptLines.map { it.stockTransferDispatchLineRef }.toSet()

        val dispatchWithLines = locationService.withLocationSchema(sourceLocationSchema) {
            stockTransferSchemaGateway.buildDispatchWithLines(
                orderRef = orderDomainDto.referenceNumber,
                isDraft = false,
                confirmedRefs = confirmedRefs
            )
        }

        return assemble(
            orderDomainDto = orderDomainDto,
            dispatch = StockTransferDispatchResponseDto(
                id = dispatchWithLines.dispatch.id,
                referenceNumber = dispatchWithLines.dispatch.referenceNumber,
                status = dispatchWithLines.dispatch.status,
                dispatchedById = dispatchWithLines.dispatch.dispatchedById,
                dispatchedAt = dispatchWithLines.dispatch.dispatchedAt,
                notes = dispatchWithLines.dispatch.notes,
                lines = dispatchWithLines.lines
            ),
            receipt = StockTransferReceiptResponseDto(
                id = receiptEntity.id!!,
                referenceNumber = receiptEntity.requiredReference(),
                status = receiptEntity.status,
                receivedById = receiptEntity.receivedById,
                receivedAt = receiptEntity.receivedAt,
                notes = receiptEntity.notes,
                lines = dispatchWithLines.lines.filter { it.quantityReceived != null }
            )
        )
    }

    private fun assemble(
        orderDomainDto: StockTransferOrderDomainDto,
        dispatch: StockTransferDispatchResponseDto,
        receipt: StockTransferReceiptResponseDto?
    ) = StockTransferResponse(
        summary = orderDomainDto.toSummaryDto(
            sourceLocationName = locationService.getById(orderDomainDto.sourceLocationId).name,
            destinationLocationName = locationService.getById(orderDomainDto.destinationLocationId).name
        ),
        dispatch = dispatch,
        receipt = receipt,
        perspective = getPerspective(orderDomainDto.sourceLocationId)
    )

    private fun getPerspective(sourceLocationId: UUID): StockTransferPerspective  {
        return if (SessionContextProvider.getLocationId() == sourceLocationId) {
            StockTransferPerspective.OUTGOING
        } else {
            StockTransferPerspective.INCOMING
        }
    }
}
