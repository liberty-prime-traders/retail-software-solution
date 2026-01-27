package me.ezra_home.retail_software_solution.configuration.datasource

object HibernateEnversProperties {

  val enversPropertyMap: Map<String, Any> = mapOf(
    "hibernate.integration.envers.enabled" to true,
    "org.hibernate.envers.audit_strategy_validity_store_revend_timestamp" to true,
    "org.hibernate.envers.store_data_at_delete" to true,
    "org.hibernate.envers.audit_strategy" to "org.hibernate.envers.strategy.ValidityAuditStrategy"
  )
}
