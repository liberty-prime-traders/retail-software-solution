package me.ezra_home.retail_software_solution.locations.business.location_user

import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ExpirableUserAssignmentEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames

@Entity
@Table(name = TableNames.LOCATION_USER)
@HasReference(tableName = TableName.LOCATION_USER)
class LocationUserEntity : ExpirableUserAssignmentEntity()
