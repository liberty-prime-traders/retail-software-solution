package me.ezra_home.retail_software_solution.organizations.business.organization_admin

import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.ExpirableUserAssignmentEntity
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.ORGANIZATION_ADMIN)
@HasReference(tableName = TableName.ORGANIZATION_ADMIN)
class OrganizationAdminEntity(adminId: UUID): ExpirableUserAssignmentEntity(userId = adminId)
