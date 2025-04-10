package me.ezra_home.retail_software_solution.platform.business.subdomain

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.model.ReservedSubdomainEntity
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
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
    fun sanitizeSubdomain(suggestedSubdomain: String?): Result<String> {
        if (suggestedSubdomain.isNullOrBlank()) {
            return Result.failure(RtsGenericException("Subdomain is required"))
        }
        val subdomain = SubdomainGenerator.generateSubdomain(suggestedSubdomain)
        subdomainRepository.findByStatusNot(Status.ABANDONED).find { it.subdomain == subdomain.getOrNull() }?.let {
            return Result.failure(RtsGenericException("Subdomain '$subdomain' is taken"))
        }
        return subdomain
    }

    fun reserveSubdomain(subdomain: String?): Result<UUID?> {
        val sysUserId = SessionContextProvider.getSession().systemUserId
            ?: throw RtsGenericException("User id not found in session")
        subdomainRepository.findByCreatedByIdAndStatus(sysUserId, Status.UNUSED).firstOrNull()?.let {
            throw RtsGenericException("User already has a reserved subdomain")
        }
        val subdomainToReserve = sanitizeSubdomain(subdomain).getOrNull().let {
            if (it != subdomain || it.isNullOrBlank()) {
                throw RtsGenericException("Subdomain '$subdomain' was not sanitized")
            }
            it
        }
        val reservedSubdomain = ReservedSubdomainEntity(subdomainToReserve, Status.UNUSED).apply { createdById = sysUserId }
        subdomainRepository.save(reservedSubdomain)
        return Result.success(reservedSubdomain.id)
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

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getReservedSubdomains(): Collection<ReservedSubdomainDto> {
        return subdomainRepository.findAll().map { reservedSubdomainMapper.toDto(it) }
    }
}
