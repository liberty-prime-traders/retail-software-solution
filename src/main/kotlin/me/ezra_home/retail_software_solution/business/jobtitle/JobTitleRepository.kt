package me.ezra_home.retail_software_solution.business.jobtitle

import java.util.UUID
import me.ezra_home.retail_software_solution.model.entity.JobTitleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JobTitleRepository: JpaRepository<JobTitleEntity, UUID>