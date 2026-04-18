package me.ezra_home.retail_software_solution.organizations.business.account

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountInsertDto
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ACCOUNT])
class AccountCache(
    private val accountRepository: AccountRepository,
    private val accountMapper: AccountMapper
) {

    @Cacheable
    fun getAll(): List<AccountDto> {
        return accountRepository.findAll().map { accountMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun update(dto: AccountDto): AccountDto {
        val toSave = accountRepository.getReferenceById(dto.id)
        val updated = dto.applyTo(toSave)
        return accountMapper.toDomainDto(accountRepository.save(updated))
    }

    @CacheEvict(allEntries = true)
    fun saveAll(dtos: List<AccountInsertDto>): List<AccountDto> {
        val saved = accountRepository.saveAllAndFlush(dtos.map { accountMapper.toEntity(it) })
        return saved.map { accountMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun create(dto: AccountInsertDto): AccountDto {
        return accountMapper.toDomainDto(accountRepository.saveAndFlush(accountMapper.toEntity(dto)))
    }
}
