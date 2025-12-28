package me.ezra_home.retail_software_solution.util.audit

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.util.Optional
import java.util.UUID

@Configuration
@EnableJpaAuditing(auditorAwareRef = "createdByAudit")
class CreatedByAudit: AuditorAware<UUID> {

    override fun getCurrentAuditor(): Optional<UUID> {
        return Optional.ofNullable(SessionContextProvider.getSession().systemUserId)
    }
}
