package me.ezra_home.retail_software_solution.cucumber.support.assertions

import me.ezra_home.retail_software_solution.organizations.business.contact.ContactMapper
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactRepository
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.JobTitleMapper
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.JobTitleRepository
import me.ezra_home.retail_software_solution.organizations.business.product_category.ProductCategoryMapper
import me.ezra_home.retail_software_solution.organizations.business.product_category.ProductCategoryRepository
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.UnitGroupMapper
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.UnitGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueMapper
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueRepository
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.AuthorizationPassMapper
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.AuthorizationPassRepository
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationMapper
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OrganizationPersistedResponseBinding(
  private val repository: OrganizationRepository,
  private val mapper: OrganizationMapper,
) : PersistedResponseBinding {
  override val alias = "organization"
  override val scope = SchemaScope.PLATFORM

  override fun responseDtoFor(id: UUID): Any {
    val entity = requireNotNull(repository.findByIdOrNull(id)) { "Organization '$id' not found" }
    return mapper.toResponseDto(mapper.toDomainDto(entity))
  }
}

@Component
class UnitGroupPersistedResponseBinding(
  private val repository: UnitGroupRepository,
  private val mapper: UnitGroupMapper,
) : PersistedResponseBinding {
  override val alias = "unitGroup"
  override val scope = SchemaScope.ORGANIZATION

  override fun responseDtoFor(id: UUID): Any {
    val entity = requireNotNull(repository.findByIdOrNull(id)) { "Unit group '$id' not found" }
    return mapper.toResponseDto(mapper.toDomainDto(entity))
  }
}

@Component
class UnitValuePersistedResponseBinding(
  private val repository: UnitValueRepository,
  private val mapper: UnitValueMapper,
  private val unitValueFetcher: UnitValueFetcher,
) : PersistedResponseBinding {
  override val alias = "unitValue"
  override val scope = SchemaScope.ORGANIZATION

  override fun responseDtoFor(id: UUID): Any {
    val entity = requireNotNull(repository.findByIdOrNull(id)) { "Unit value '$id' not found" }
    val dto = mapper.toDomainDto(entity)
    return mapper.toResponseDto(dto, unitValueFetcher.getUnitName(dto.baseUnit))
  }
}

@Component
class ProductCategoryPersistedResponseBinding(
  private val repository: ProductCategoryRepository,
  private val mapper: ProductCategoryMapper,
) : PersistedResponseBinding {
  override val alias = "category"
  override val scope = SchemaScope.ORGANIZATION

  override fun responseDtoFor(id: UUID): Any {
    val entity = requireNotNull(repository.findByIdOrNull(id)) { "Category '$id' not found" }
    return mapper.toResponseDto(mapper.toDomainDto(entity))
  }
}

@Component
class JobTitlePersistedResponseBinding(
  private val repository: JobTitleRepository,
  private val mapper: JobTitleMapper,
) : PersistedResponseBinding {
  override val alias = "jobTitle"
  override val scope = SchemaScope.ORGANIZATION

  override fun responseDtoFor(id: UUID): Any {
    val entity = requireNotNull(repository.findByIdOrNull(id)) { "Job title '$id' not found" }
    return mapper.toDto(mapper.toDomainDto(entity))
  }
}

@Component
class ContactPersistedResponseBinding(
  private val repository: ContactRepository,
  private val mapper: ContactMapper,
) : PersistedResponseBinding {
  override val alias = "contact"
  override val scope = SchemaScope.ORGANIZATION

  override fun responseDtoFor(id: UUID): Any {
    val entity = requireNotNull(repository.findByIdOrNull(id)) { "Contact '$id' not found" }
    return mapper.toResponseDto(mapper.toDomainDto(entity))
  }
}

@Component
class AuthorizationPassPersistedResponseBinding(
  private val repository: AuthorizationPassRepository,
  private val mapper: AuthorizationPassMapper,
) : PersistedResponseBinding {
  override val alias = "authorizationPass"
  override val scope = SchemaScope.PLATFORM

  override fun responseDtoFor(id: UUID): Any {
    val entity = requireNotNull(repository.findByIdOrNull(id)) { "Authorization pass '$id' not found" }
    return mapper.toResponseDto(entity)
  }
}
