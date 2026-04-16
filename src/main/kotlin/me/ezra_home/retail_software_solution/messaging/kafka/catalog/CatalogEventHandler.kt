package me.ezra_home.retail_software_solution.messaging.kafka.catalog

import me.ezra_home.retail_software_solution.configuration.session.ServiceAccountContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.sync_services.SyncServiceRegistry
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSessionSetup
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.util.enums.ServiceAccount
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class CatalogEventHandler(
    private val eventSessionSetup: EventSessionSetup,
    private val locationService: LocationService,
    private val syncServiceRegistry: SyncServiceRegistry,
    private val catalogEventProducer: CatalogEventProducer
) {

    fun consume(event: CatalogChangedEvent) {
        ServiceAccountContext.runWithServiceAccount(ServiceAccount.CATALOG_SYNC) {
            eventSessionSetup.initFromEvent(event)

            locationService.getAllLocationDtos().forEach { location ->
                SessionContextProvider.initLocation(location)
                syncServiceRegistry.getService(event.tableName).syncSingle(event.entityId)
            }
        }
    }

    fun publish(tableName: TableName, entityId: UUID) {
        val orgSchema = SessionContextProvider.getOrganizationSchema()

        catalogEventProducer.publish(
            CatalogChangedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.OrgLevel(orgSchema),
                timestamp = Instant.now(),
                correlationId = null,
                tableName = tableName,
                entityId = entityId
            )
        )
    }
}
