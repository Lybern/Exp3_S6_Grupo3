package cl.duoc.bancoxyz.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.duoc.bancoxyz.core.model.Cuenta;
import cl.duoc.bancoxyz.core.repository.BancoRepository;
import cl.duoc.bancoxyz.core.security.ServiceTokenUtil;
import cl.duoc.bancoxyz.core.service.BancoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class CoreServiceTests {

    @Autowired
    private BancoRepository bancoRepository;

    @Autowired
    private BancoService bancoService;

    @Autowired
    private ServiceTokenUtil serviceTokenUtil;

    @Test
    @DisplayName("Carga de datos legacy inicializa al menos 50 cuentas bancarias")
    void cargaDatosLegacyExitosa() {
        List<Cuenta> cuentas = bancoRepository.obtenerTodasLasCuentas();
        assertThat(cuentas).hasSizeGreaterThanOrEqualTo(50);
    }

    @Test
    @DisplayName("Ejecución de retiro exitosa sobre cuenta bancaria")
    void ejecutarRetiroCoreExitoso() {
        Cuenta primeraCuenta = bancoRepository.obtenerTodasLasCuentas().get(0);
        Long cuentaId = primeraCuenta.getCuentaId();
        
        // Asignar saldo seguro para la prueba
        primeraCuenta.setSaldoContable(100000L);
        bancoRepository.guardarCuenta(primeraCuenta);

        Cuenta actualizada = bancoService.ejecutarRetiro(cuentaId, 10000L, "ATM");
        assertThat(actualizada.getSaldoContable()).isEqualTo(90000L);
    }

    @Test
    @DisplayName("Retiro mayor al saldo disponible lanza IllegalArgumentException")
    void retiroExcedenteLanzaExcepcion() {
        Cuenta primeraCuenta = bancoRepository.obtenerTodasLasCuentas().get(0);
        Long cuentaId = primeraCuenta.getCuentaId();
        
        assertThatThrownBy(() -> bancoService.ejecutarRetiro(cuentaId, 9999999999L, "ATM"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
