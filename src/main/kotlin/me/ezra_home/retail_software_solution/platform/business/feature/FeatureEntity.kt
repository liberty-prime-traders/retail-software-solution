package me.ezra_home.retail_software_solution.platform.business.feature

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import me.ezra_home.retail_software_solution.platform.business.feature.api.FeatureConverter
import me.ezra_home.retail_software_solution.util.model.BaseEntity
import me.ezra_home.retail_software_solution.util.model.TableNames

@Entity
@Table(name = TableNames.FEATURE)
class FeatureEntity(

    @Convert(converter = FeatureConverter::class)
    @Column(name = "feature", nullable = false, updatable = false)
    var feature: Feature,

    @Column(name = "description", nullable = false)
    var description: String

) : BaseEntity()
