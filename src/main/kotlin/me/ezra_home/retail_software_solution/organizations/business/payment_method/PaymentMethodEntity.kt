package me.ezra_home.retail_software_solution.organizations.business.payment_method

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited

@Audited
@Entity
@Table(name = TableNames.PAYMENT_METHOD)
@HasReference(tableName = TableName.PAYMENT_METHOD)
class PaymentMethodEntity (

    @Column(name = "name", unique = true, length = 100)
    var name: String? = null,

    @Column(name = "description", length = 1000)
    var description: String? = null

): HasReferenceEntity()
