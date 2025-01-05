package me.ezra_home.retail_software_solution.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import me.ezra_home.retail_software_solution.model.enums.LocationType
import me.ezra_home.retail_software_solution.model.util.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.LOCATION)
class LocationEntity : AuditableEntity() {
    @NotNull
    @Column(name = "organization_id", nullable = false)
    var organizationId: UUID? = null

    @NotNull
    @Column(name = "location_type", nullable = false)
    var locationType: LocationType? = null

    @Size(max = 100)
    @Column(name = "name", length = 100)
    var name: String? = null

    @Size(max = 1000)
    @Column(name = "description", length = 1000)
    var description: String? = null
}
