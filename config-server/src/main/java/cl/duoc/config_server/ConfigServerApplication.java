package cl.duoc.config_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer; 

/**
 * Servidor de Configuración Centralizada (Spring Cloud Config Server) - Banco XYZ.
 * 
 * Este microservicio actúa como el repositorio central de configuraciones para toda
 * la arquitectura distribuida. Provee puertos, credenciales, parámetros de Eureka
 * y Resilience4j a los demás microservicios (core-service, bff-movil, bff-web, bff-cajero).
 */
@SpringBootApplication
@EnableConfigServer // Habilita este proyecto como Servidor de Configuración de Spring Cloud
public class ConfigServerApplication {

	public static void main(String[] args) {
		// Inicializa el contexto de Spring Boot en el puerto central 8888
		SpringApplication.run(ConfigServerApplication.class, args);
	}

}
