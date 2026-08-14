package br.org.fadex.chamados;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ponto de entrada da Central de Chamados com Triagem Inteligente.
 *
 * <p>Desafio tecnico FADEX — vaga de Analista de Desenvolvimento.
 *
 * <p>{@code @EnableScheduling} sustenta o batimento periodico que mantem vivas as
 * conexoes SSE do painel em tempo real.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class CentralChamadosApplication {

    public static void main(String[] args) {
        SpringApplication.run(CentralChamadosApplication.class, args);
    }
}
