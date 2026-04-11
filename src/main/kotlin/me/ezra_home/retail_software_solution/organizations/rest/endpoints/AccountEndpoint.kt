package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountCreateRequest
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountResponseDto
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountUpdateDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/accounts")
class AccountEndpoint(private val accountService: AccountService) {

    @GetMapping
    fun getAll(): List<AccountResponseDto> = accountService.getAll()

    @PostMapping
    fun create(@RequestBody dto: AccountCreateRequest): AccountResponseDto = accountService.create(dto)

    @PutMapping
    fun rename(@RequestBody dto: AccountUpdateDto): AccountResponseDto = accountService.rename(dto)

    @PostMapping("{id}/deactivate")
    fun deactivate(@PathVariable id: UUID): AccountResponseDto = accountService.toggleActive(id, false)

    @PostMapping("{id}/activate")
    fun activate(@PathVariable id: UUID): AccountResponseDto = accountService.toggleActive(id, true)
}
