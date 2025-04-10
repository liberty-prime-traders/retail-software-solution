package me.ezra_home.retail_software_solution.locations.business.jobtitle

import me.ezra_home.retail_software_solution.locations.model.JobTitleEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JobTitleRepository: JpaRepository<JobTitleEntity, UUID>
