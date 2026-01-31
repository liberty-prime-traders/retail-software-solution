package me.ezra_home.retail_software_solution.util.model

import me.ezra_home.retail_software_solution.util.enums.HasCode
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel

enum class TableName(override val code: String, val schemaLevel: SchemaLevel) : HasCode {

  SYS_USER(TableNames.SYS_USER, SchemaLevel.PLATFORM),
  ORGANIZATION(TableNames.ORGANIZATION, SchemaLevel.PLATFORM),
  RESERVED_SUBDOMAIN(TableNames.RESERVED_SUBDOMAIN, SchemaLevel.PLATFORM),
  DB_VERSION(TableNames.DB_VERSION, SchemaLevel.PLATFORM),
  DB_MIGRATION(TableNames.DB_MIGRATION, SchemaLevel.PLATFORM),
  TABLE_REGISTRY(TableNames.TABLE_REGISTRY, SchemaLevel.PLATFORM),

  PRODUCT(TableNames.PRODUCT, SchemaLevel.ORGANIZATION),
  PRODUCT_GROUP(TableNames.PRODUCT_GROUP, SchemaLevel.ORGANIZATION),
  PRODUCT_CATEGORY(TableNames.PRODUCT_CATEGORY, SchemaLevel.ORGANIZATION),
  CONTACT(TableNames.CONTACT, SchemaLevel.ORGANIZATION),
  TAG(TableNames.TAG, SchemaLevel.ORGANIZATION),
  ADDRESS(TableNames.ADDRESS, SchemaLevel.ORGANIZATION),
  LOCATION(TableNames.LOCATION, SchemaLevel.ORGANIZATION),
  JOB_TITLE(TableNames.JOB_TITLE, SchemaLevel.ORGANIZATION),
  UNIT_GROUP(TableNames.UNIT_GROUP, SchemaLevel.ORGANIZATION),
  UNIT_VALUE(TableNames.UNIT_VALUE, SchemaLevel.ORGANIZATION),
  ORGANIZATION_ADMIN(TableNames.ORGANIZATION_ADMIN, SchemaLevel.ORGANIZATION),
  PAYMENT_METHOD(TableNames.PAYMENT_METHOD, SchemaLevel.ORGANIZATION),
  ORGANIZATION_JOIN_REQUEST(TableNames.ORGANIZATION_JOIN_REQUEST, SchemaLevel.ORGANIZATION),
  ORGANIZATION_USER(TableNames.ORGANIZATION_USER, SchemaLevel.ORGANIZATION),
  PRODUCT_TAG(TableNames.PRODUCT_TAG, SchemaLevel.ORGANIZATION),
  ORG_TABLE_REGISTRY(TableNames.ORG_TABLE_REGISTRY, SchemaLevel.ORGANIZATION),
  ORGANIZATION_AUDIT(TableNames.ORGANIZATION_AUDIT, SchemaLevel.ORGANIZATION),
  PRODUCT_AUDIT(TableNames.PRODUCT_AUDIT, SchemaLevel.ORGANIZATION),

  LOCATION_PRODUCT(TableNames.LOCATION_PRODUCT, SchemaLevel.LOCATION),
  SYNC_LOG(TableNames.SYNC_LOG, SchemaLevel.LOCATION),

  ADDRESS_AUDIT(TableNames.ADDRESS_AUDIT, SchemaLevel.ORGANIZATION),
  CONTACT_AUDIT(TableNames.CONTACT_AUDIT, SchemaLevel.ORGANIZATION),
  JOB_TITLE_AUDIT(TableNames.JOB_TITLE_AUDIT, SchemaLevel.ORGANIZATION),
  UNIT_GROUP_AUDIT(TableNames.UNIT_GROUP_AUDIT, SchemaLevel.ORGANIZATION),
  UNIT_VALUE_AUDIT(TableNames.UNIT_VALUE_AUDIT, SchemaLevel.ORGANIZATION),
  LOCATION_AUDIT(TableNames.LOCATION_AUDIT, SchemaLevel.ORGANIZATION),

  PAYMENT_METHOD_AUDIT(TableNames.PAYMENT_METHOD_AUDIT, SchemaLevel.ORGANIZATION),
  TAG_AUDIT(TableNames.TAG_AUDIT, SchemaLevel.ORGANIZATION),
  PRODUCT_CATEGORY_AUDIT(TableNames.PRODUCT_CATEGORY_AUDIT, SchemaLevel.ORGANIZATION),
  PRODUCT_GROUP_AUDIT(TableNames.PRODUCT_GROUP_AUDIT, SchemaLevel.ORGANIZATION),

  LOCATION_PRODUCT_AUDIT(TableNames.LOCATION_PRODUCT_AUDIT, SchemaLevel.LOCATION);

  val tableName: String
    get() = code

  companion object {
    private val tableNameSet: Set<String> = entries.map { it.code }.toSet()
    fun exists(name: String?): Boolean = name in tableNameSet
  }
}
