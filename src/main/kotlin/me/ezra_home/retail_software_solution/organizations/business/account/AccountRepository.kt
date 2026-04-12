package me.ezra_home.retail_software_solution.organizations.business.account

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AccountRepository : JpaRepository<AccountEntity, UUID> {

    fun findAllByParentAccountCode(parentAccountCode: String): List<AccountEntity>
}
