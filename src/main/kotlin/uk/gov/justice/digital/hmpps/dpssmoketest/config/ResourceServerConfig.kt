package uk.gov.justice.digital.hmpps.dpssmoketest.config

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.config.web.servlet.invoke
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class ResourceServerConfig : WebSecurityConfigurerAdapter() {
  override fun configure(http: HttpSecurity) {
    http {
      sessionManagement { SessionCreationPolicy.STATELESS }
      csrf { disable() }
      authorizeRequests {
        listOf("/webjars/**", "/favicon.ico", "/health/**", "/info", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
            .forEach { authorize(AntPathRequestMatcher(it), permitAll) }
        authorize(anyRequest)
      }
      oauth2ResourceServer { jwt { jwtAuthenticationConverter = AuthAwareTokenConverter() } }
    }
  }

}
