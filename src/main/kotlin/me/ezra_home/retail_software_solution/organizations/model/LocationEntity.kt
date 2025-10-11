package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import me.ezra_home.retail_software_solution.util.enums.LocationType
import me.ezra_home.retail_software_solution.util.listeners.LocationReferenceNumberEntityListener
import me.ezra_home.retail_software_solution.util.listeners.OrganizationReferenceNumberEntityListener
import me.ezra_home.retail_software_solution.util.model.AuditableEntity
import me.ezra_home.retail_software_solution.util.model.TableNames

@Entity
@Table(name = TableNames.LOCATION)
@EntityListeners(LocationReferenceNumberEntityListener::class)
class LocationEntity(

    @NotNull
    @Column(name = "location_type", nullable = false)
    var locationType: LocationType? = null,

    @Column(name = "name", length = 100)
    var name: String? = null,

    @Column(name = "description", length = 1000)
    var description: String? = null,

    @NotNull
    @Column(name = "schema_name", length = 100, nullable = false, updatable = false)
    var schemaName: String? = null,

    @Column(name = "reference_number", unique = true)
    var referenceNumber: String? = null

): AuditableEntity()
