package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.platform.model.ExpirableAssignmentEntity
import me.ezra_home.retail_software_solution.util.listeners.OrganizationReferenceNumberEntityListener
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.ORGANIZATION_USER)
@EntityListeners(OrganizationReferenceNumberEntityListener::class)
class OrganizationUserEntity(

    @Column(name = "join_request_id", updatable = false)
    var joinRequestId: UUID? = null,

) : ExpirableAssignmentEntity()
