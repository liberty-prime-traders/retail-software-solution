package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionAssembler
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionLoader
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleSessionHandler(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionLoader: SaleSessionLoader,
    private val saleSessionAssembler: SaleSessionAssembler,
    private val saleSessionValidator: SaleSessionValidator,
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun start(dto: SaleSessionStartDto): SaleSessionResponseDto {
        dto.saleId?.let { saleId ->
            saleSessionStore.findOpenSessionForSale(saleId)?.let { existing ->
                return saleSessionAssembler.buildResponse(existing)
            }
        }
        val sessionId = UUID.randomUUID()
        val session = if (dto.saleId != null) {
            saleSessionLoader.loadFromSale(sessionId, dto.saleId)
        } else {
            val locationId = SessionContextProvider.getLocationId()
            val userId = SessionContextProvider.getUserId()
            saleSessionLoader.newSession(sessionId, locationId, dto.contactId, userId)
        }
        saleSessionValidator.validate(session)
        saleSessionStore.save(session)
        return saleSessionAssembler.buildResponse(session)
    }

    fun abandon(sessionId: UUID) {
        saleSessionStore.delete(sessionId)
    }

    fun listOpenSessions(): List<SaleSessionSummaryDto> {
        return saleSessionAssembler.buildSummaries( saleSessionStore.listOpenSessions())
    }

    fun acquireSession(sessionId: UUID): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        val visited = session.markVisited(SessionContextProvider.getUserId())
        saleSessionStore.save(visited)
        return saleSessionAssembler.buildResponse(visited)
    }
}
