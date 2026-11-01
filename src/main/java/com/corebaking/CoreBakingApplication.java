package com.corebaking;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(title = "Core Banking API", version = "v1",
        description = "API para contas, movimentações e consulta de extratos."))
@SpringBootApplication
public class CoreBakingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreBakingApplication.class, args);
    }

}
