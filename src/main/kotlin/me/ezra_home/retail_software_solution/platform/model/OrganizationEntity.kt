package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.AuditableEntity
import me.ezra_home.retail_software_solution.util.model.TableNames

@Entity
@Table(name = TableNames.ORGANIZATION)
class OrganizationEntity(

    @Column(name = "name")
    open var name: String,

    @Column(name = "description")
    open var description: String? = null,

    @Column(name = "subdomain", updatable = false)
    open var subdomain: String? = null,

    @Column(name = "schema_name", length = 100, nullable = false, updatable = false)
    var schemaName: String? = null

): AuditableEntity()
