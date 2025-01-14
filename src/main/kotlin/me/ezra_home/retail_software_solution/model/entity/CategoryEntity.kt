package me.ezra_home.retail_software_solution.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.model.enums.CategoryType
import me.ezra_home.retail_software_solution.model.util.TableNames

@Entity
@Table(name = TableNames.CATEGORY)
class CategoryEntity(

    @Column(name = "category_type")
    var categoryType: CategoryType? = null,

    @Column(name = "category_name")
    var categoryName: String? = null,

    @Column(name = "description")
    var description: String? = null,

): AuditableEntity()
