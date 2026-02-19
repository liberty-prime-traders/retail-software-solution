package me.ezra_home.retail_software_solution.messaging.kafka.catalog

import me.ezra_home.retail_software_solution.configuration.session.ServiceAccountContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.sync_services.SyncServiceRegistry
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.util.enums.ServiceAccount
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class CatalogEventHandler(
    private val organizationCache: OrganizationCache,
    private val locationCache: LocationCache,
    private val syncServiceRegistry: SyncServiceRegistry,
    private val catalogEventProducer: CatalogEventProducer
) {

    fun consume(event: CatalogChangedEvent) {
        try {
            ServiceAccountContext.runWithServiceAccount(ServiceAccount.CATALOG_SYNC) {
                val organization = organizationCache.getAllOrganizations()
                    .find { it.schemaName == event.sourceSchema }
                    ?: throw RtsGenericException("Organization with schema ${event.sourceSchema} not found")
                SessionContextProvider.initOrganization(organization)

                locationCache.getAllLocations().forEach { location ->
                    SessionContextProvider.initLocation(location)
                    syncServiceRegistry.getService(event.tableName).syncSingle(event.entityId)
                }
            }
        } finally {
            SessionContextProvider.clear()
        }
    }

    fun publish(tableName: TableName, entityId: UUID) {
        val sourceSchema = SessionContextProvider.getSession().organizationSchemaName
            ?: throw RtsGenericException("Organization schema name not found in session")

        catalogEventProducer.publish(
            CatalogChangedEvent(
                eventId = UUID.randomUUID(),
                sourceSchema = sourceSchema,
                timestamp = Instant.now(),
                correlationId = null,
                tableName = tableName,
                entityId = entityId
            )
        )
    }
}
