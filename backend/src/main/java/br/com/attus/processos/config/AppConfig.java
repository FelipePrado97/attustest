package br.com.attus.processos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
public class AppConfig {

    @Bean
    Clock clock(@Value("${app.fuso-horario:America/Sao_Paulo}") String fusoHorario) {
        return Clock.system(ZoneId.of(fusoHorario));
    }
}
