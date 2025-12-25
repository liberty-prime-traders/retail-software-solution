package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.PRODUCT)
class ProductEntity(

    @Column(name = "name", nullable = false)
    var productName: String,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "category_id", nullable = false)
    var categoryId: UUID,

    @Column(name = "base_unit_id")
    var baseUnitId: UUID,
    
    @Column(name = "cursor", insertable = false, updatable = false)
    var cursor: Long

): HasCreatorEntity()
