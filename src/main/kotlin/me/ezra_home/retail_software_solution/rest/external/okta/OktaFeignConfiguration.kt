package me.ezra_home.retail_software_solution.rest.external.okta

import feign.RequestInterceptor
import feign.RequestTemplate
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpHeaders


class OktaFeignConfiguration {

    companion object {
        const val HOST = "https://"
        const val OKTA_DOMAIN = "OKTA_DOMAIN"
        const val OKTA_API_TOKEN = "OKTA_API_TOKEN"
        const val SUFFIX = "/api/v1"
        const val BEARER_PREFIX = "SSWS "
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    fun oktaFeignRequestInterceptor(): RequestInterceptor {
        val baseUrl = HOST + System.getenv(OKTA_DOMAIN) + SUFFIX
        val apiToken = BEARER_PREFIX + System.getenv(OKTA_API_TOKEN)

        return RequestInterceptor { requestTemplate: RequestTemplate ->
            requestTemplate.target(baseUrl)
            requestTemplate.header(HttpHeaders.AUTHORIZATION, apiToken)
        }
    }
}
