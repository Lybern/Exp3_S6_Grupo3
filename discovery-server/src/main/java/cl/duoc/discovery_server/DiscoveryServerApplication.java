package cl.duoc.discovery_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Servidor de Descubrimiento de Servicios (Netflix Eureka Server) - Banco XYZ.
 * 
 * Funciona como el catalogo o 'guia telefonica' dinamica donde se registran
 * todos los microservicios (core-service, bff-movil, bff-web, bff-cajero).
 * Permite resolver nombres logicos sin quemar IPs ni puertos en el codigo.
 */
@SpringBootApplication
@EnableEurekaServer // Habilita este proyecto como Servidor de Registro Eureka
public class DiscoveryServerApplication {

	public static void main(String[] args) {
		// Inicializa el contexto de Spring Boot en el puerto 8761
		SpringApplication.run(DiscoveryServerApplication.class, args);
	}

}
