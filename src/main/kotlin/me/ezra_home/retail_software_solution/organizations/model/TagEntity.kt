package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.enums.CategoryType
import me.ezra_home.retail_software_solution.util.enums.CategoryTypeConverter
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited

@Audited
@Entity
@Table(name = TableNames.TAG)
@HasReference(tableName = TableName.TAG)
class TagEntity(

    @Column(name = "category")
    @Convert(converter = CategoryTypeConverter::class)
    var category: CategoryType,

    @Column(name = "tag_name", nullable = false)
    var tagName: String,

    @Column(name = "description")
    var description: String? = null

): HasReferenceEntity()
