package me.ezra_home.retail_software_solution.business.audit.auditmanager


import me.ezra_home.retail_software_solution.model.enums.DatabaseAction
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID


data class TransientAuditEntity(
    val action: DatabaseAction,
    val auditTime: OffsetDateTime,
    val auditUserOktaId: String,
    val entityName: String,
    val entityId: UUID
) {

    fun matchesWith(other: TransientAuditEntity?): Boolean {
        return Objects.equals(entityName, other?.entityName)
                && Objects.equals(entityId, other?.entityId)
    }

}
