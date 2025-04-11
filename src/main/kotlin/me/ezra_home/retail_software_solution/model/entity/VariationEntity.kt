package me.ezra_home.retail_software_solution.model.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import me.ezra_home.retail_software_solution.model.util.TableNames
import org.hibernate.annotations.ColumnDefault
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = TableNames.VARIATION)
class VariationEntity (
    @Column(name = "name", nullable = false)
    var name: String? = null,

    @Column(name = "description")
    var description: String? = null

): AuditableEntity()
