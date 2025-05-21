package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.enums.CategoryType
import me.ezra_home.retail_software_solution.util.model.AuditableEntity

@Entity
@Table(name = TableNames.PRODUCT)
class ProductEntity(

    @Column(name = "name", nullable = false)
    var productName: String? = null,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "category_id", nullable = false)
    var categoryId: UUID? = null,

    ): AuditableEntity()
