package me.ezra_home.retail_software_solution.platform.business.authorization_pass

import me.ezra_home.retail_software_solution.platform.business.authorization_pass.AuthorizationPassEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AuthorizationPassRepository : JpaRepository<AuthorizationPassEntity, UUID> {
    fun findByCode(code: UUID): AuthorizationPassEntity?
}
