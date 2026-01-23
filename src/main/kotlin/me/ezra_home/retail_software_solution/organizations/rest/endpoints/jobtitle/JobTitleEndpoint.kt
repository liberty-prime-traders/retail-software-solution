package me.ezra_home.retail_software_solution.organizations.rest.endpoints.jobtitle

import me.ezra_home.retail_software_solution.organizations.business.jobtitle.JobTitleService
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.dto.JobTitleInsertDto
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.dto.JobTitleResponseDto
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.dto.JobTitleUpdateDto
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@CrossOrigin
@RestController
@RequestMapping("secured/job-title")
class JobTitleEndpoint(
    private val titleService: JobTitleService,
    private val jobTitleService: JobTitleService
) {

    @PostMapping
    fun createJobTitle(@RequestBody titleInsertDto: JobTitleInsertDto): JobTitleResponseDto =
        titleService.createJobTitle(titleInsertDto)

    @PutMapping
    fun updateJobTitle(@RequestBody titleDto: JobTitleUpdateDto): JobTitleResponseDto =
        titleService.updateJobTitle(titleDto)

    @GetMapping
    fun getAllJobTitles(): Collection<JobTitleResponseDto> =
        titleService.getAllJobTitles()

    @DeleteMapping("{id}")
    fun deleteJobTitle(@PathVariable id: UUID?): ResponseEntity<HttpStatusCode> {
        jobTitleService.deleteJobTitle(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
