package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.BaseEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.DB_VERSION)
class DbVersionEntity(

    @Column(name = "version_number", updatable = false, insertable = false)
    val versionNumber: String,

    @Column(name = "sequence_number")
    var sequenceNumber: Long? = null,

    @Column(name = "prev_version_id", updatable = false, insertable = false)
    val prevVersionId: UUID? = null,

    @Column(name = "activated_on")
    var activatedOn: OffsetDateTime? = null

) : BaseEntity()
