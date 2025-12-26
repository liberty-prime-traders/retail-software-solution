package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.platform.model.ExpirableAssignmentEntity
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.listeners.ReferenceNumberEntityListener
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.ORGANIZATION_ADMIN)
@HasReference(tableName = TableName.ORGANIZATION_ADMIN)
@EntityListeners(ReferenceNumberEntityListener::class)
class OrganizationAdminEntity(adminId: UUID): ExpirableAssignmentEntity(userId = adminId)
