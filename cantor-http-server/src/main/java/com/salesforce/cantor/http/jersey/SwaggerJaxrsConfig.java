package com.salesforce.cantor.http.jersey;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.servers.ServerVariable;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

@OpenAPIDefinition(
        info = @Info(
            title = "Cantor API Documentation",
            version = "1.0.0"
        ),
        servers = {
            @Server(description = "Localhost",
                    url = "http://localhost:{port}/api",
                    variables = @ServerVariable(name = "port", defaultValue = "8083")),
        }
)
class SwaggerJaxrsConfig extends ResourceConfig {
    SwaggerJaxrsConfig() {
        // propagated error handler
        this.register(GenericExceptionMapper.class);

        // cantor server resources
        this.packages("com.salesforce.cantor.http.jersey");

        // swagger initialization resource
        this.register(OpenApiResource.class);

        // jersey extension for file uploads
        this.register(MultiPartFeature.class);
    }
}
