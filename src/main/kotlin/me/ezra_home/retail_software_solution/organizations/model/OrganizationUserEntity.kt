package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.platform.model.ExpirableAssignmentEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.ORGANIZATION_USER)
class OrganizationUserEntity(

    @Column(name = "join_request_id", updatable = false)
    var joinRequestId: UUID? = null

) : ExpirableAssignmentEntity()
