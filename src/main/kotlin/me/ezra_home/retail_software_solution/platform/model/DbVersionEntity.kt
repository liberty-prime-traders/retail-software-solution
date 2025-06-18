package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.DB_VERSION)
class DbVersionEntity(

    @Column(name = "version_number", nullable = false, length = 15)
    val versionNumber: String,

    @Column(name = "sequence_number", nullable = false)
    val sequenceNumber: Long,

    @Column(name = "prev_version_id", updatable = false)
    val prevVersionId: UUID? = null,

    @Column(name = "activated_on")
    var activatedOn: OffsetDateTime? = null

) : HasCreatorEntity()
