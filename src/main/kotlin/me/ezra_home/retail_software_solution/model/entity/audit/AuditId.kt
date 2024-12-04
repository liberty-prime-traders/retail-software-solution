package me.ezra_home.retail_software_solution.model.entity.audit

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.Objects
import java.util.UUID

@Embeddable
class AuditId(
    @NotNull
    @Column(name = "entity_id", nullable = false)
    open var entityId: UUID? = null,

    @Size(max = 100)
    @NotNull
    @Column(name = "entity_name", nullable = false, length = 100)
    open var entityName: String? = null

) {
    fun isEqual(that: AuditId?): Boolean =
        that != null && Objects.equals(entityId, that.entityId) && Objects.equals(entityName, that.entityName)
}
