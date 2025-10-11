package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.enums.CategoryType
import me.ezra_home.retail_software_solution.util.listeners.OrganizationReferenceNumberEntityListener
import me.ezra_home.retail_software_solution.util.model.AuditableEntity

@Entity
@Table(name = TableNames.CATEGORY)
@EntityListeners(OrganizationReferenceNumberEntityListener::class)
class CategoryEntity(

    @Column(name = "category_type", nullable = false)
    var categoryType: CategoryType? = null,

    @Column(name = "category_name", nullable = false)
    var categoryName: String? = null,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "reference_number", unique = true)
    var referenceNumber: String? = null
): AuditableEntity()
