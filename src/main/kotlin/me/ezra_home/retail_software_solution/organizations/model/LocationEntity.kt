package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.organizations.business.location.LocationType
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited

@Audited
@Entity
@Table(name = TableNames.LOCATION)
@HasReference(tableName = TableName.LOCATION)
class LocationEntity(

    @Column(name = "location_type", nullable = false)
    var locationType: LocationType,

    @Column(name = "name", length = 100)
    var name: String,

    @Column(name = "description", length = 1000)
    var description: String? = null,

    @Column(name = "schema_name", length = 100, nullable = false, updatable = false)
    var schemaName: String? = null

): HasReferenceEntity()
