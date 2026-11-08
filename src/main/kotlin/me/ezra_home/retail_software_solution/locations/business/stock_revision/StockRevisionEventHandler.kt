package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.time.LocalDate
import java.util.UUID

abstract class StockRevisionEventHandler(
    private val stockRevisionRepository: StockRevisionRepository
) : EventReissueHandler {

    override fun reissue(sourceDocumentId: UUID) {
        republish(
            stockRevisionRepository.findById(sourceDocumentId)
                .orElseThrow { RtsGenericException("Stock revision $sourceDocumentId not found") }
        )
    }

    protected abstract fun republish(stockRevision: StockRevisionEntity)

    protected fun sourceContext() = EventSourceContext.LocationLevel(
        orgSchema = SessionContextProvider.getOrganizationSchema(),
        locationSchema = SessionContextProvider.getLocationSchema()
    )

    protected fun postingDate(): LocalDate = DateTimes.Local.Now.organization()
}
