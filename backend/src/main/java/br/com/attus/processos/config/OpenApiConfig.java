package br.com.attus.processos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Processos API")
                .version("v1")
                .description("""
                        Gestão de processos judiciais de uma procuradoria.

                        * Erros seguem RFC 9457 (Problem Details) e trazem `correlationId`.
                        * Envie o header `X-Correlation-Id` para rastrear a operação nos logs (opcional).
                        * Alterações exigem a `versao` lida (controle de concorrência otimista).
                        * O histórico é atualizado de forma assíncrona (Kafka)."""))
                .addTagsItem(new Tag().name("Processos").description("Cadastro e acompanhamento de processos judiciais"));
    }
}
