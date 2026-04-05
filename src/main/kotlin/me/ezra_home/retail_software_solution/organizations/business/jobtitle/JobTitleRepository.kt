package me.ezra_home.retail_software_solution.organizations.business.jobtitle

import me.ezra_home.retail_software_solution.organizations.model.JobTitleEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
internal interface JobTitleRepository: JpaRepository<JobTitleEntity, UUID>
