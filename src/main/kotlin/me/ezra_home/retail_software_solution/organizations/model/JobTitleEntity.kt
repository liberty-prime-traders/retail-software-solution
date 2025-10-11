package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.listeners.OrganizationReferenceNumberEntityListener
import me.ezra_home.retail_software_solution.util.model.AuditableEntity
import me.ezra_home.retail_software_solution.util.model.TableNames

@Entity
@Table(name = TableNames.JOB_TITLE)
@EntityListeners(OrganizationReferenceNumberEntityListener::class)
class JobTitleEntity(
    @Column(name = "value")
    var value: String? = null,

    @Column(name = "reference_number", unique = true)
    var referenceNumber: String? = null,
) : AuditableEntity()
