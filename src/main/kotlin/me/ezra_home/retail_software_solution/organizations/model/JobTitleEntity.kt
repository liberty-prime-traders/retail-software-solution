package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.listeners.ReferenceNumberEntityListener
import me.ezra_home.retail_software_solution.util.model.AuditableEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.model.TableName

@Entity
@Table(name = TableNames.JOB_TITLE)
@HasReference(tableName = TableName.JOB_TITLE)
@EntityListeners(ReferenceNumberEntityListener::class)
class JobTitleEntity(
    @Column(name = "value")
    var value: String,

    @Column(name = "reference_number", unique = true)
    var referenceNumber: String? = null
) : AuditableEntity()
