package me.ezra_home.retail_software_solution.platform.business.reserved_subdomain

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.platform.model.ReservedSubdomainEntity
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.enums.ReservedDomainStatus
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
        val userId = SessionContextProvider.getUserId()
        subdomainRepository.findByStatusNotAndSubdomain(ReservedDomainStatus.ABANDONED, subdomain)
            .find { it.status == ReservedDomainStatus.USED || it.createdById != userId }
            ?.let { throw RtsGenericException("Subdomain '$subdomain' is taken") }
        return reservedSubdomainMapper.toDto(reserveSubdomain(subdomain))
    }

    private fun reserveSubdomain(sanitizedSubdomain: String): ReservedSubdomainEntity {
        val sysUserId = SessionContextProvider.getUserId()
        subdomainRepository.abandonSubdomainsForUser(sysUserId)
        val reservedSubdomain = ReservedSubdomainEntity(sanitizedSubdomain, ReservedDomainStatus.UNUSED)
        return subdomainRepository.save(reservedSubdomain)
    }

    fun releaseSubdomain(id: UUID?) {
        id?.let{
            subdomainRepository.findByIdOrNull(id)?.let {
                it.status = ReservedDomainStatus.ABANDONED
                subdomainRepository.save(it)
            }
        }
    }

    fun markSubdomainAsUsed(id: UUID?) {
        id?.let{
            subdomainRepository.findByIdOrNull(id)?.let {
                it.status = ReservedDomainStatus.USED
                subdomainRepository.save(it)
            }
        }
    }

}
