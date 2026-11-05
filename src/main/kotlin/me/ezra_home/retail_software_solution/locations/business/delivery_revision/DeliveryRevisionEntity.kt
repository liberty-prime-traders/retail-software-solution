package me.ezra_home.retail_software_solution.locations.business.delivery_revision

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.DELIVERY_REVISION)
@HasReference(tableName = TableName.DELIVERY_REVISION)
class DeliveryRevisionEntity(

  @Column(name = "revised_line_id", nullable = false, updatable = false, unique = true)
  var revisedLineId: UUID,

  @Column(name = "replacement_line_id", nullable = false, updatable = false, unique = true)
  var replacementLineId: UUID,

  @Column(name = "reason", nullable = false, updatable = false, columnDefinition = "text")
  var reason: String

) : ImmutableEntity()
