package me.ezra_home.retail_software_solution.rest.endpoints.jobtitle

import me.ezra_home.retail_software_solution.business.jobtitle.JobTitleService
import me.ezra_home.retail_software_solution.business.jobtitle.dto.JobTitleInsertDto
import me.ezra_home.retail_software_solution.business.jobtitle.dto.JobTitleResponseDto
import me.ezra_home.retail_software_solution.business.jobtitle.dto.JobTitleUpdateDto
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@CrossOrigin
@RestController
@RequestMapping("secured/jobtitle")
class JobTitleEndpoint(private val titleService: JobTitleService) {

    @PostMapping
    fun createJobTitle(@RequestBody titleInsertDto: JobTitleInsertDto): JobTitleResponseDto =
        titleService.createJobTitle(titleInsertDto)

    @PutMapping
    fun updateJobTitle(@RequestBody titleDto: JobTitleUpdateDto): JobTitleResponseDto =
        titleService.updateJobTitle(titleDto)

    @GetMapping
    fun getAllJobTitles(): Collection<JobTitleResponseDto> =
        titleService.getAllJobTitles()
}