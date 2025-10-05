package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.model.AuditableEntity
import java.util.UUID

@Entity
@Table(name = TableNames.PRODUCT)
class ProductEntity(

    @Column(name = "name", nullable = false)
    var productName: String? = null,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "category_id", nullable = false)
    var categoryId: UUID? = null,

    @Column(name = "reference_number", unique = true)
    var referenceNumber: String? = null,

): AuditableEntity()
