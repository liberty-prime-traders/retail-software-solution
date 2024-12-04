package me.ezra_home.retail_software_solution.business.sysuser

import me.ezra_home.retail_software_solution.model.entity.SysUserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SysUserRepository : JpaRepository<SysUserEntity, UUID>
