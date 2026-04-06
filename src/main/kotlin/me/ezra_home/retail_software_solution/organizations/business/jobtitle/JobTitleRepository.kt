package me.ezra_home.retail_software_solution.organizations.business.jobtitle

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JobTitleRepository: JpaRepository<JobTitleEntity, UUID>
