package me.ezra_home.retail_software_solution.rest.endpoints.title

import me.ezra_home.retail_software_solution.business.title.TitleService
import me.ezra_home.retail_software_solution.business.title.dto.TitleInsertDto
import me.ezra_home.retail_software_solution.business.title.dto.TitleResponseDto
import me.ezra_home.retail_software_solution.business.title.dto.TitleUpdateDto
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@CrossOrigin
@RestController
@RequestMapping("secured/title")
class TitleEndpoint(private val titleService: TitleService) {

    @PostMapping
    fun createTitle(@RequestBody titleInsertDto: TitleInsertDto): TitleResponseDto =
        titleService.createTitle(titleInsertDto)

    @PutMapping
    fun updateTitle(@RequestBody titleDto: TitleUpdateDto): TitleResponseDto =
        titleService.updateTitle(titleDto)

    @GetMapping
    fun getAllTitles(): Collection<TitleResponseDto> =
        titleService.getAllTitles()
}