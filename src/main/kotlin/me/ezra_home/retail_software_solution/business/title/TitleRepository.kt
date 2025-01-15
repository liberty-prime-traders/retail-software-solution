package me.ezra_home.retail_software_solution.business.title

import java.util.UUID
import me.ezra_home.retail_software_solution.model.entity.TitleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TitleRepository: JpaRepository<TitleEntity, UUID>