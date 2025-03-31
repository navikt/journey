package no.nav.journey.config.m2mConfig

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TokenValidationContextConfig {
    @Bean
    fun tokenValidationContextHolder(): TokenValidationContextHolder {
        return SpringTokenValidationContextHolder()
    }
}