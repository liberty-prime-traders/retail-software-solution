package me.ezra_home.retail_software_solution.organizations.business.tag

import me.ezra_home.retail_software_solution.organizations.model.TagEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TagRepository: JpaRepository<TagEntity, UUID>
