package me.ezra_home.retail_software_solution.cucumber.support.database

import jakarta.annotation.PostConstruct
import org.springframework.context.ApplicationContext
import org.springframework.core.ResolvableType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/**
 * Discovers every `JpaRepository` bean and registers a [DataSourcePackage] keyed by the entity
 * simple name (uppercased, with `Entity` stripped). Aliases let features reference an entity by a
 * shorter or alternate name when the auto-key is ambiguous or verbose.
 */
@Component
class DataAccessHelper(private val applicationContext: ApplicationContext) {

  private val packages = mutableMapOf<String, DataSourcePackage>()

  @PostConstruct
  fun discover() {
    applicationContext.getBeansOfType(JpaRepository::class.java).forEach { (beanName, repository) ->
      val resolvedRepo = ResolvableType.forClass(repository::class.java).`as`(JpaRepository::class.java)
      val entityClass = resolvedRepo.getGeneric(0).resolve() ?: return@forEach
      val idClass = resolvedRepo.getGeneric(1).resolve() ?: return@forEach

      val key = keyFor(entityClass.simpleName)
      @Suppress("UNCHECKED_CAST")
      val pkg = DataSourcePackage(
        key = key,
        displayName = displayNameFor(entityClass.simpleName),
        entityClass = entityClass,
        idClass = idClass,
        repository = repository as JpaRepository<Any, Any>,
      )
      packages[key] = pkg
      packages.putIfAbsent(keyFor(beanName), pkg)
    }
  }

  private fun displayNameFor(simpleName: String): String =
    simpleName.removeSuffix("Entity").replace(CAMEL_BOUNDARY, "_")

  fun alias(name: String, repositoryClass: KClass<out JpaRepository<*, *>>) {
    val repository = applicationContext.getBean(repositoryClass.java)
    val pkg = packages.values.firstOrNull { it.repository === repository }
      ?: error("Repository ${repositoryClass.simpleName} is not registered in DataAccessHelper")
    packages[keyFor(name)] = pkg
  }

  fun get(name: String): DataSourcePackage =
    packages[keyFor(name)]
      ?: error(
        "No JpaRepository registered for '$name'. " +
          "Known keys: ${packages.keys.sorted().joinToString()}",
      )

  fun find(name: String): DataSourcePackage? = packages[keyFor(name)]

  private fun keyFor(raw: String): String =
    raw.uppercase()
      .replace(" ", "")
      .replace("_", "")
      .replace("ENTITY", "")
      .replace("REPOSITORY", "")

  private companion object {
    val CAMEL_BOUNDARY = Regex("(?<=[a-z])(?=[A-Z])")
  }
}
