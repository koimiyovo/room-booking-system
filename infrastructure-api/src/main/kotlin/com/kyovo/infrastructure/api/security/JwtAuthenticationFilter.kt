package com.kyovo.infrastructure.api.security

import com.kyovo.domain.port.primary.UserUseCase
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    @param:Lazy private val userUseCase: UserUseCase,
    @param:Autowired(required = false)
    private val tokenBlacklistService: TokenBlacklistService?
) : OncePerRequestFilter()
{
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    )
    {
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer "))
        {
            filterChain.doFilter(request, response)
            return
        }

        val token = AuthToken(authHeader.substring(7))

        if (tokenBlacklistService?.isTokenBlacklisted(token) == true)
        {
            filterChain.doFilter(request, response)
            return
        }

        if (jwtService.validateToken(token))
        {
            val userId = jwtService.extractUserId(token)
            val role = jwtService.extractRole(token)
            if (userId != null && role != null)
            {
                if (userUseCase.findById(userId) != null)
                {
                    val authorities = listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
                    val authentication = UsernamePasswordAuthenticationToken(
                        userId.value.toString(), null, authorities
                    )
                    SecurityContextHolder.getContext().authentication = authentication
                }
                else
                {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                    return
                }
            }
        }

        filterChain.doFilter(request, response)
    }
}
