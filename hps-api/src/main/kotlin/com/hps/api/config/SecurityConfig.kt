package com.hps.api.config

import com.hps.api.auth.JwtAuthFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Error endpoint must be accessible
                    .requestMatchers("/error").permitAll()
                    // Public: auth
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    // Public: read-only geo, categories, providers, services, availability
                    .requestMatchers(HttpMethod.GET, "/api/v1/countries/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/regions/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/cities/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/providers/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/services/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/attributes/**").permitAll()
                    // Everything else requires authentication
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
