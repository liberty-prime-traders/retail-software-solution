package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.listeners.ReferenceNumberEntityListener
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.model.TableName

@Entity
@Table(name = TableNames.ORGANIZATION)
@HasReference(tableName = TableName.ORGANIZATION)
@EntityListeners(ReferenceNumberEntityListener::class)
class OrganizationEntity(

    @Column(name = "name")
    var name: String,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "subdomain", updatable = false)
    var subdomain: String? = null,

    @Column(name = "schema_name", length = 100, nullable = false, updatable = false)
    var schemaName: String? = null,

    @Column(name = "reference_number", unique = true)
    var referenceNumber: String? = null

): HasCreatorEntity()
