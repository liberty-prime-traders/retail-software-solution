package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import me.ezra_home.retail_software_solution.util.model.AuditableEntity
import me.ezra_home.retail_software_solution.util.enums.LocationType
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.LOCATION)
class LocationEntity(

    @NotNull
    @Column(name = "organization_id", nullable = false)
    var organizationId: UUID? = null,

    @NotNull
    @Column(name = "location_type", nullable = false)
    var locationType: LocationType? = null,

    @Column(name = "name", length = 100)
    var name: String? = null,

    @Column(name = "description", length = 1000)
    var description: String? = null,

    @NotNull
    @Column(name = "schema_name", length = 100, nullable = false)
    var schemaName: String? = null

): AuditableEntity()
