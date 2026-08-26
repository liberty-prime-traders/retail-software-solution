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
      val schema = detectSchema(repository, entityClass)
      
      // Warn about unsupported location schema during discovery
      if (schema == Schema.LOCATION) {
        // Location schema entities exist but database assertions aren't yet implemented
        // Skip registering location entities to prevent misleading test errors
        return@forEach
      }
      
      @Suppress("UNCHECKED_CAST")
      val pkg = DataSourcePackage(
        key = key,
        displayName = displayNameFor(entityClass.simpleName),
        entityClass = entityClass,
        idClass = idClass,
        repository = repository as JpaRepository<Any, Any>,
        schema = schema,
      )
      packages[key] = pkg
      packages.putIfAbsent(keyFor(beanName), pkg)
    }
  }

  private fun detectSchema(repository: JpaRepository<*, *>, entityClass: Class<*>): Schema {
    // Try repository package first (works for most cases)
    val repoPackage = repository.javaClass.packageName
    val repoSchema = detectSchemaFromPackage(repoPackage)
    if (repoSchema != null) return repoSchema
    
    // Fallback to entity package (handles proxy cases)
    val entityPackage = entityClass.packageName
    return detectSchemaFromPackage(entityPackage)
      ?: error(
        "Unable to detect schema for repository: ${repository.javaClass.simpleName}. " +
          "Repository package: $repoPackage, Entity package: $entityPackage. " +
          "Expected package to contain '.platform.', '.organizations.', or '.locations.'"
      )
  }

  private fun detectSchemaFromPackage(packageName: String): Schema? {
    // Skip packages outside project namespace (prevents matching external libraries)
    if (!packageName.startsWith("me.ezra_home.retail_software_solution")) {
      return null
    }
    
    return when {
      packageName.contains(".platform.") -> Schema.PLATFORM
      packageName.contains(".organizations.") -> Schema.ORGANIZATION
      packageName.contains(".locations.") -> Schema.LOCATION
      else -> null
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
