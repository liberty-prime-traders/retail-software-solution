package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited

@Entity
@Table(name = TableNames.ORGANIZATION)
@HasReference(tableName = TableName.ORGANIZATION)
class OrganizationEntity(

    @Audited
    @Column(name = "name")
    var name: String,

    @Audited
    @Column(name = "description")
    var description: String? = null,

    @Column(name = "subdomain", updatable = false)
    var subdomain: String? = null,

    @Column(name = "schema_name", length = 100, nullable = false, updatable = false)
    var schemaName: String? = null

): HasReferenceEntity()
