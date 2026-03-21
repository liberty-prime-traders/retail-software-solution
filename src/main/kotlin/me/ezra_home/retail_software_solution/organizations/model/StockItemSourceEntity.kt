package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.organizations.business.inventory.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.inventory.StockItemSourceConverter
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames

@Entity
@Table(name = TableNames.STOCK_ITEM_SOURCE)
@HasReference(tableName = TableName.STOCK_ITEM_SOURCE)
class StockItemSourceEntity(

  @Convert(converter = StockItemSourceConverter::class)
  @Column(name = "code", nullable = false, length = 20, unique = true, updatable = false)
  var code: StockItemSource,

  @Column(name = "name", nullable = false, length = 100)
  var name: String,

  @Column(name = "description", columnDefinition = "TEXT")
  var description: String? = null,

  @Column(name = "system_defined", nullable = false, updatable = false)
  val systemDefined: Boolean = false

) : HasReferenceEntity()
