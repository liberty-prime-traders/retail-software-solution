package me.ezra_home.retail_software_solution.organizations.business.org_table_registry

import me.ezra_home.retail_software_solution.organizations.model.OrgTableRegistryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrgTableRegistryRepository: JpaRepository<OrgTableRegistryEntity, UUID>
