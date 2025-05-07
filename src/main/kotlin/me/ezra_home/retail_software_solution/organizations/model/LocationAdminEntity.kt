package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.platform.model.BaseAdminEntity
import java.util.UUID

@Entity
@Table(name = TableNames.LOCATION_ADMIN)
class LocationAdminEntity(

    @Column(name = "location_id", updatable = false)
    var locationId: UUID? = null,

): BaseAdminEntity()
