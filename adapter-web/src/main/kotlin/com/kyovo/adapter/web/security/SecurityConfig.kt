package com.kyovo.adapter.web.security

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
                    .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/h2-console/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/users", "/api/users/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/users/**").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/users/**").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/rooms").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/rooms", "/api/rooms/**").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/bookings").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/bookings/**").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/bookings").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/bookings/**").authenticated()
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
