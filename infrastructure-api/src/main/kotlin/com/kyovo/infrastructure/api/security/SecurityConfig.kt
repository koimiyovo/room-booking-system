package com.kyovo.infrastructure.api.security

import com.kyovo.domain.model.user.UserRole
import com.kyovo.infrastructure.api.API_V1
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
class SecurityConfig(private val jwtAuthenticationFilter: JwtAuthenticationFilter)
{
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain
    {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.POST, "$API_V1/auth/register", "$API_V1/auth/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "$API_V1/auth/logout").authenticated()
                    .requestMatchers(HttpMethod.GET, "$API_V1/version").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/h2-console/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "$API_V1/users", "$API_V1/users/**").hasRole(UserRole.ADMIN.label)
                    .requestMatchers(HttpMethod.PUT, "$API_V1/users/**").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "$API_V1/users/**").authenticated()
                    .requestMatchers(HttpMethod.POST, "$API_V1/rooms").hasRole(UserRole.ADMIN.label)
                    .requestMatchers(HttpMethod.GET, "$API_V1/rooms", "$API_V1/rooms/**").authenticated()
                    .requestMatchers(HttpMethod.GET, "$API_V1/bookings").hasRole(UserRole.ADMIN.label)
                    .requestMatchers(HttpMethod.GET, "$API_V1/bookings/**").authenticated()
                    .requestMatchers(HttpMethod.POST, "$API_V1/bookings").authenticated()
                    .requestMatchers(HttpMethod.POST, "$API_V1/bookings/*/validate").hasRole(UserRole.ADMIN.label)
                    .requestMatchers(HttpMethod.POST, "$API_V1/bookings/**").authenticated()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder
    {
        return BCryptPasswordEncoder()
    }
}
