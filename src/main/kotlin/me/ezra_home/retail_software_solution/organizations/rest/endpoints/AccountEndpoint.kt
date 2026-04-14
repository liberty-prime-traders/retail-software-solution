package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountChildCreateRequest
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountResponseDto
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountRootCreateRequest
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountTreeBuilder
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountsTreesForSelection
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
class AccountEndpoint(
    private val accountService: AccountService,
    private val accountTreeBuilder: AccountTreeBuilder
) {

    @GetMapping
    fun getAll(): List<AccountResponseDto> = accountService.getAll()

    @GetMapping("selection-trees")
    fun getTreesForSelection(): AccountsTreesForSelection = accountTreeBuilder.build()

    @PostMapping("child")
    fun createChild(@RequestBody dto: AccountChildCreateRequest): AccountResponseDto = accountService.createChild(dto)

    @PostMapping("root")
    fun createRoot(@RequestBody dto: AccountRootCreateRequest): AccountResponseDto = accountService.createRoot(dto)

    @PutMapping
    fun rename(@RequestBody dto: AccountUpdateDto): AccountResponseDto = accountService.rename(dto)

    @PutMapping("{id}/deactivate")
    fun deactivate(@PathVariable id: UUID): AccountResponseDto = accountService.toggleActive(id, false)

    @PutMapping("{id}/activate")
    fun activate(@PathVariable id: UUID): AccountResponseDto = accountService.toggleActive(id, true)
}
