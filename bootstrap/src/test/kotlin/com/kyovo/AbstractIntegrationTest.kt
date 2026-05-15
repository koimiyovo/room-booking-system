package com.kyovo

import com.kyovo.config.TestTimeProviderConfig
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestTimeProviderConfig::class)
abstract class AbstractIntegrationTest
{
    @MockitoBean
    lateinit var mailSender: JavaMailSender

    companion object
    {
        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry)
        {
            registry.add("spring.datasource.url", PostgresTestContainer.instance::getJdbcUrl)
            registry.add("spring.datasource.username", PostgresTestContainer.instance::getUsername)
            registry.add("spring.datasource.password", PostgresTestContainer.instance::getPassword)
        }
    }
}
