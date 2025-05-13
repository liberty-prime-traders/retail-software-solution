package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.enums.JoinRequestStatus
import me.ezra_home.retail_software_solution.util.enums.JoinRequestStatusConverter
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import java.util.UUID

@Entity
@Table(name = TableNames.ORGANIZATION_JOIN_REQUEST)
class OrganizationJoinRequestEntity(

    @Column(name = "subdomain", nullable = false, length = 63)
    var subdomain: String,

    @Column(name = "status", nullable = false, length = 5)
    @Convert(converter = JoinRequestStatusConverter::class)
    var status: JoinRequestStatus,

    @Column(name = "organization_id", nullable = true, updatable = false)
    var organizationId: UUID?,

    ) : HasCreatorEntity()
