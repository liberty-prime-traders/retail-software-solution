package me.ezra_home.retail_software_solution.platform.business.subdomain

import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.platform.model.ReservedSubdomainEntity
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.enums.Status
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class SubdomainService(
    private val organizationCache: OrganizationCache,
    private val subdomainRepository: SubdomainRepository,
    private val reservedSubdomainMapper: ReservedSubdomainMaper
) {

    @Transactional
    fun sanitizeSubdomain(suggestedSubdomain: String?): Result<String> {
        if (suggestedSubdomain.isNullOrBlank()) {
            return Result.failure(RtsGenericException("Subdomain is required"))
        }
        val subdomain = SubdomainGenerator.generateSubdomain(suggestedSubdomain)
        organizationCache.getAllOrganizations().find { it.subdomain == subdomain.getOrNull() }?.let {
            return Result.failure(RtsGenericException("Subdomain '$subdomain' is taken"))
        }
        return subdomain
    }

    @Transactional
    fun reserveSubdomain(subdomain: String): Result<String> {
        val sysUserId = SessionContextProvider.getSession().systemUserId
            ?: throw RtsGenericException("User id not found in session")
        subdomainRepository.findByCreatedByIdAndStatus(sysUserId, Status.UNUSED).firstOrNull()?.let {
            throw RtsGenericException("User already has a reserved subdomain")
        }
        organizationCache.getAllOrganizations().find { it.subdomain == subdomain }?.let {
            return Result.failure(RtsGenericException("Subdomain '$subdomain' is taken"))
        }
        val reservedSubdomain = ReservedSubdomainEntity(subdomain, Status.UNUSED).apply { createdById = sysUserId }
        subdomainRepository.save(reservedSubdomain)
        return Result.success(subdomain)
    }

    @Transactional
    fun releaseSubdomain(id: UUID?) {
        id?.let{
            subdomainRepository.findByIdOrNull(id)?.let {
                it.status = Status.ABANDONED
                subdomainRepository.save(it)
            }
        }
    }

    @Transactional
    fun markSubdomainAsUsed(id: UUID?) {
        id?.let{
            subdomainRepository.findByIdOrNull(id)?.let {
                it.status = Status.USED
                subdomainRepository.save(it)
            }
        }
    }

    @Transactional
    fun getReservedSubdomains(): Collection<ReservedSubdomainDto> {
        return subdomainRepository.findAll().map { reservedSubdomainMapper.toDto(it) }
    }
}
