package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited

@Audited
@Entity
@Table(name = TableNames.ORGANIZATION)
@HasReference(tableName = TableName.ORGANIZATION)
internal class OrganizationEntity(

    @Column(name = "name")
    var name: String,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "private", nullable = false)
    var hidden: Boolean = false,

    @Column(name = "current_db_version_id")
    var currentDbVersionId: UUID? = null,

    @NotAudited
    @Column(name = "creation_pass_id", updatable = false)
    var creationPassId: UUID? = null,

    @NotAudited
    @Column(name = "subdomain", updatable = false)
    var subdomain: String? = null,

    @NotAudited
    @Column(name = "schema_name", length = 100, nullable = false, updatable = false)
    var schemaName: String? = null,

    @Column(name = "timezone", length = 50)
    var timezone: String? = null

): HasReferenceEntity()
