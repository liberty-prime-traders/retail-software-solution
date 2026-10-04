package me.ezra_home.retail_software_solution.util.business.mappers

import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.SysUserService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NameResolution(
  private val contactService: ContactService,
  private val sysUserService: SysUserService
) {

  fun organizationContacts(ids: Collection<UUID>): Map<UUID, String> =
    resolveNamesById(ids, contactService.getAllContactDtos(), { it.id }, { it.identity.displayName })

  fun systemUsers(ids: Collection<UUID>): Map<UUID, String> =
    resolveNamesById(ids, sysUserService.getAllUsers(), { it.id }, { it.fullName })

  private fun <T> resolveNamesById(
    ids: Collection<UUID>,
    allValues: Collection<T>,
    idOf: (T) -> UUID,
    nameOf: (T) -> String
  ): Map<UUID, String> {
    if (ids.isEmpty()) return emptyMap()
    val idSet = ids.toSet()
    return allValues.filter { idOf(it) in idSet }.associate { idOf(it) to nameOf(it) }
  }
}
