package me.ezra_home.retail_software_solution.organizations.business.organization_user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.ExpirableUserAssignmentEntity
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.ORGANIZATION_USER)
@HasReference(tableName = TableName.ORGANIZATION_USER)
class OrganizationUserEntity(

    @Column(name = "join_request_id", updatable = false)
    var joinRequestId: UUID? = null,

) : ExpirableUserAssignmentEntity()
