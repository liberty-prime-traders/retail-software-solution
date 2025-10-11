package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.listeners.OrganizationReferenceNumberEntityListener
import me.ezra_home.retail_software_solution.util.model.AuditableEntity
import me.ezra_home.retail_software_solution.util.model.TableNames

@Entity
@Table(name = TableNames.PAYMENT_METHOD)
@EntityListeners(OrganizationReferenceNumberEntityListener::class)
class PaymentMethodEntity (

    @Column(name = "name", unique = true, length = 100)
    var name: String? = null,

    @Column(name = "description", length = 1000)
    var description: String? = null,

    @Column(name = "reference_number", unique = true)
    var referenceNumber: String? = null,

): AuditableEntity()
