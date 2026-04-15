package me.ezra_home.retail_software_solution.organizations.business.accounting_config

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrgAccountingConfigRepository : JpaRepository<OrgAccountingConfigEntity, UUID>
