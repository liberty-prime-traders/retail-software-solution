package me.ezra_home.retail_software_solution.organizations.business.feature

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import me.ezra_home.retail_software_solution.platform.business.feature.api.FeatureConverter
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.ORGANIZATION_FEATURE)
class OrganizationFeatureEntity(

    @Column(name = "feature_code", nullable = false, updatable = false)
    @Convert(converter = FeatureConverter::class)
    var feature: Feature,

    @Column(name = "status", nullable = false)
    var status: OrganizationFeatureStatus,

    @Column(name = "enabled_on")
    var enabledOn: OffsetDateTime? = null,

    @Column(name = "enabled_by")
    var enabledBy: UUID? = null,

    @Column(name = "disabled_on")
    var disabledOn: OffsetDateTime? = null,

    @Column(name = "disabled_by")
    var disabledBy: UUID? = null

) : HasCreatorEntity()
