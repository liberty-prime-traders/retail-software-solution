package me.ezra_home.retail_software_solution.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.model.util.TableNames

@Entity
@Table(name = TableNames.PAYMENT_METHOD)
class PaymentMethodEntity (

    @Column(name = "name", unique = true, length = 100)
    var name: String,

    @Column(name = "description", length = 1000)
    var description: String? = null,

): AuditableEntity()
