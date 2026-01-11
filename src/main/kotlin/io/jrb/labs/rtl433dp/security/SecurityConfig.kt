/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Jon Brule <brulejr@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.jrb.labs.rtl433dp.security

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder.withIssuerLocation
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@ConfigurationPropertiesScan( basePackages = ["io.jrb.labs.rtl433dp.security"])
class SecurityConfig {

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .authorizeExchange { ex ->
                ex.pathMatchers("/mgmt/health/**", "/mgmt/info").permitAll()
                ex.pathMatchers("/mgmt/**").hasAuthority(Permissions.ACTUATOR_READ)
                ex.anyExchange().authenticated()
            }
            .oauth2ResourceServer { rs ->
                rs.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthConverter())
                }
            }
            .build()
    }

    @Bean
    fun jwtAuthConverter(): Converter<Jwt, Mono<AbstractAuthenticationToken>> {
        val authoritiesConverter: Converter<Jwt, Collection<GrantedAuthority>> =
            Converter { jwt ->
                // Works across Spring Security versions (avoids relying on getClaimAsStringList)
                val perms = (jwt.claims["permissions"] as? Collection<*>)
                    ?.mapNotNull { it?.toString() }
                    .orEmpty()

                perms.map { SimpleGrantedAuthority(it) }
            }

        val delegate = JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter(authoritiesConverter)
        }

        return ReactiveJwtAuthenticationConverterAdapter(delegate)
    }

    @Bean
    fun jwtDecoder(datafill: SecurityDatafill): ReactiveJwtDecoder {
        val issuer = datafill.jwt.issuerUri
        val decoder = withIssuerLocation(issuer).build()

        val issuerValidator = JwtValidators.createDefaultWithIssuer(issuer)
        val azpValidator = AzpValidator(datafill.clientId) // audience field now means “expected clientId”

        decoder.setJwtValidator(DelegatingOAuth2TokenValidator(issuerValidator, azpValidator))
        return decoder
    }

}