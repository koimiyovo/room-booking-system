package com.kyovo.infrastructure.api.controller

import com.kyovo.infrastructure.api.API_V1
import com.kyovo.infrastructure.api.dto.AppVersionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.info.BuildProperties
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Version", description = "Application version information")
@RestController
@RequestMapping("$API_V1/version")
class AppVersionController(private val buildProperties: BuildProperties)
{
    @Operation(summary = "Get the current application version")
    @ApiResponse(responseCode = "200", description = "Version returned successfully")
    @GetMapping
    fun getVersion(): ResponseEntity<AppVersionResponse>
    {
        return ResponseEntity.ok(AppVersionResponse(buildProperties.version ?: "unknown"))
    }
}
