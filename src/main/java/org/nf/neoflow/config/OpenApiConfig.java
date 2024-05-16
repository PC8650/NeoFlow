package org.nf.neoflow.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "NeoFlow API", description = "Rough documentation of NeoFlow API"))
public class OpenApiConfig {
}
