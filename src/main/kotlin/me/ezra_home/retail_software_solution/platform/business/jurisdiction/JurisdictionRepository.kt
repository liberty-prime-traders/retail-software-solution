package me.ezra_home.retail_software_solution.platform.business.jurisdiction

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JurisdictionRepository : JpaRepository<JurisdictionEntity, UUID>
