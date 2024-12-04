package me.ezra_home.retail_software_solution.model.entity.audit

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import me.ezra_home.retail_software_solution.model.enums.Status
import me.ezra_home.retail_software_solution.model.util.TableNames
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Generated
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.AUDIT)
class AuditEntity(

    @EmbeddedId
    open var id: AuditId? = null,

    @NotNull
    @Column(name = "created_by_id", nullable = false)
    open var createdById: UUID? = null,

    @Column(name = "last_updated_by_id")
    open var lastUpdatedById: UUID? = null,

    @Generated
    @ColumnDefault("now()")
    @Column(name = "created_on", updatable = false)
    open var createdOn: OffsetDateTime? = null,

    @Column(name = "last_updated_on")
    open var lastUpdatedOn: OffsetDateTime? = null,

    @Column(name = "active_ind")
    open var activeInd: Boolean? = null,

    @Column(name = "predecessor_of_id")
    open var predecessorOfId: UUID? = null,

    @Size(max = 5)
    @Column(name = "status", length = 5)
    open var status: Status? = null

)
