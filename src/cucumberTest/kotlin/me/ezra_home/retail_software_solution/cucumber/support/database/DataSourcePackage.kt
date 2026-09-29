package me.ezra_home.retail_software_solution.cucumber.support.database

import org.springframework.data.jpa.repository.JpaRepository

data class DataSourcePackage(
  val key: String,
  val displayName: String,
  val entityClass: Class<*>,
  val idClass: Class<*>,
  val repository: JpaRepository<Any, Any>,
)
