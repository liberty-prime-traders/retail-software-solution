package me.ezra_home.retail_software_solution.platform.business.reserved_subdomain

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.model.ReservedSubdomainEntity
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.enums.Status
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnPlatformSchema
class ReservedSubdomainService(
    private val subdomainRepository: SubdomainRepository,
    private val reservedSubdomainMapper: ReservedSubdomainMaper
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getReservedSubdomains(): Collection<ReservedSubdomainDto> {
        return subdomainRepository.findAll().map { reservedSubdomainMapper.toDto(it) }
    }

    fun sanitizeThenReserveSubdomain(suggestedSubdomain: String?): ReservedSubdomainDto {
        if (!StringUtils.hasValue(suggestedSubdomain)) {
            throw RtsGenericException("An empty subdomain cannot be verified")
        }
        val subdomain = SubdomainGenerator.generateSubdomain(suggestedSubdomain!!)
        subdomainRepository.findByStatusNot(Status.ABANDONED).find { it.subdomain == subdomain }?.let {
            throw RtsGenericException("Subdomain '$subdomain' is taken")
        }
        return reservedSubdomainMapper.toDto(reserveSubdomain(subdomain))
    }

    private fun reserveSubdomain(sanitizedSubdomain: String): ReservedSubdomainEntity {
        val sysUserId = SessionContextProvider.getUserId()
        subdomainRepository.abandonSubdomainsForUser(sysUserId)
        val reservedSubdomain = ReservedSubdomainEntity(sanitizedSubdomain, Status.UNUSED).apply { createdById = sysUserId }
        return subdomainRepository.save(reservedSubdomain)
    }

    fun releaseSubdomain(id: UUID?) {
        id?.let{
            subdomainRepository.findByIdOrNull(id)?.let {
                it.status = Status.ABANDONED
                subdomainRepository.save(it)
            }
        }
    }

    fun markSubdomainAsUsed(id: UUID?) {
        id?.let{
            subdomainRepository.findByIdOrNull(id)?.let {
                it.status = Status.USED
                subdomainRepository.save(it)
            }
        }
    }

}
