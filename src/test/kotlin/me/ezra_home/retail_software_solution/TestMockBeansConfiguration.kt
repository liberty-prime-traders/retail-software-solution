package me.ezra_home.retail_software_solution

import com.okta.sdk.client.Client
import com.okta.sdk.resource.user.User
import com.okta.sdk.resource.user.UserList
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
    `when`(users.iterator()).thenReturn(mutableListOf<User>().iterator())
    `when`(client.listUsers()).thenReturn(users)
    return client
  }

  @Bean
  @Primary
  fun jwtDecoder(): JwtDecoder = mock()
}
