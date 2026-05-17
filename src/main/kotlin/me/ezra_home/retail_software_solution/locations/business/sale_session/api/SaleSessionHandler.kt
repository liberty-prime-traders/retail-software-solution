package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionAssembler
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionLoader
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleSessionHandler(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionLoader: SaleSessionLoader,
    private val saleSessionAssembler: SaleSessionAssembler,
    private val saleSessionValidator: SaleSessionValidator,
    private val saleDataFetcher: SaleDataFetcher,
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun start(dto: SaleSessionStartDto): SaleSessionResponseDto {
        dto.saleId?.let { saleId ->
            saleSessionStore.findOpenSessionForSale(saleId)?.let { existing ->
                return saleSessionAssembler.buildResponse(existing)
            }
        }
        val sessionId = UUID.randomUUID().toString()
        val locationId = SessionContextProvider.getLocationId()
        val userId = SessionContextProvider.getUserId()
        val session = if (dto.saleId != null) {
            requireDraftStatus(dto.saleId)
            saleSessionLoader.loadFromSale(sessionId, dto.saleId)
        } else {
            saleSessionLoader.newSession(sessionId, locationId, dto.contactId, userId)
        }
        saleSessionValidator.validate(session)
        saleSessionStore.save(session)
        return saleSessionAssembler.buildResponse(session)
    }

    fun abandon(sessionId: String) {
        saleSessionStore.delete(sessionId)
    }

    fun listOpen(): List<SaleSessionSummaryDto> {
        val open = saleSessionStore.listOpen()
        return saleSessionAssembler.buildSummaries(open)
    }

    fun get(sessionId: String): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        val visited = session.visited(SessionContextProvider.getUserId(), java.time.OffsetDateTime.now())
        saleSessionStore.save(visited)
        return saleSessionAssembler.buildResponse(visited)
    }

    private fun requireDraftStatus(saleId: UUID) {
        val header = saleDataFetcher.getHeaderSnapshot(saleId)
        if (header.status != SaleStatus.DRAFT) {
            throw RtsGenericException("Only DRAFT sales can be loaded into a session; sale is ${header.status}")
        }
    }
}
