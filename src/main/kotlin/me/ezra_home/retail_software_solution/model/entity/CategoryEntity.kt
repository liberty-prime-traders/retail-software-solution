package me.ezra_home.retail_software_solution.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.model.util.TableNames

@Entity
@Table(name = TableNames.CATEGORY)
class CategoryEntity(

    @Column(name = "category_type")
    open var category_type: String? = null,

    @Column(name = "category_name")
    open var category_name: String? = null,

    @Column(name = "description")
    open var description: String? = null,

): AuditableEntity()
