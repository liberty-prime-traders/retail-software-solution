package me.ezra_home.retail_software_solution

import com.okta.sdk.client.Client
import com.okta.sdk.resource.user.User
import com.okta.sdk.resource.user.UserList
import com.okta.sdk.resource.user.UserProfile
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.JwtDecoder

@TestConfiguration
class TestMockBeansConfiguration {

  @Bean
  @Primary
  fun oktaClient(): Client {
    val client = mock<Client>()
    val users = mock<UserList>()
    `when`(users.iterator()).thenAnswer { mockedUsers().iterator() }
    `when`(client.listUsers()).thenReturn(users)
    return client
  }

  @Bean
  @Primary
  fun jwtDecoder(): JwtDecoder = mock()

  private fun mockedUsers(): MutableList<User> = mutableListOf(
    mockUser(TestConstants.Okta.ORGANIZATION_USER, "Fake Organization User"),
    mockUser(TestConstants.Okta.PLATFORM_USER, "Fake Platform Admin User"),
  )

  private fun mockUser(id: String, firstName: String): User {
    val user = mock<User>()
    val profile = mock<UserProfile>()
    `when`(user.id).thenReturn(id)
    `when`(user.profile).thenReturn(profile)
    `when`(profile.firstName).thenReturn(firstName)
    `when`(profile.lastName).thenReturn("lastName")
    return user
  }

}
