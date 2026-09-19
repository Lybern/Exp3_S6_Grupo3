package cl.duoc.bancoxyz.core.repository;

import cl.duoc.bancoxyz.core.model.Cuenta;
import cl.duoc.bancoxyz.core.model.MovimientoAnual;
import cl.duoc.bancoxyz.core.model.Transaccion;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Component
public class CargadorDatosLegacy {

    private static final Logger log = LoggerFactory.getLogger(CargadorDatosLegacy.class);
    private final BancoRepository bancoRepository;
    private final ResourceLoader resourceLoader;

    public CargadorDatosLegacy(BancoRepository bancoRepository, ResourceLoader resourceLoader) {
        this.bancoRepository = bancoRepository;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void cargarDatos() {
        log.info("[CORE] Iniciando carga de datos legacy del Banco XYZ...");
        cargarCuentas();
        cargarTransacciones();
        cargarCuentasAnuales();
        log.info("[CORE] Datos cargados exitosamente. Total cuentas registradas: {}", bancoRepository.obtenerTodasLasCuentas().size());
    }


    private void cargarCuentas() {
        try {
            Resource resource = resourceLoader.getResource("classpath:data/intereses.csv");
            if (!resource.exists()) return;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String linea = br.readLine();
                while ((linea = br.readLine()) != null) {
                    if (linea.trim().isEmpty()) continue;
                    String[] datos = linea.split(",", -1);
                    if (datos.length < 5) continue;
                    try {
                        String idStr = datos[0].trim();
                        if (idStr.isEmpty()) continue;
                        Long cuentaId = Long.parseLong(idStr);

                        String nombre = datos[1].trim();
                        if (nombre.isEmpty() || "Unknown".equalsIgnoreCase(nombre)) {
                            nombre = "Cliente " + cuentaId;
                        }

                        long saldo = 0L;
                        if (!datos[2].trim().isEmpty()) {
                            saldo = (long) Math.max(0.0, Double.parseDouble(datos[2].trim()));
                        }

                        int edad = 30;
                        if (!datos[3].trim().isEmpty()) {
                            try {
                                int edadLeida = Integer.parseInt(datos[3].trim());
                                if (edadLeida >= 18 && edadLeida <= 100) edad = edadLeida;
                            } catch (NumberFormatException ignored) {}
                        }

                        String tipo = datos[4].trim().toLowerCase();
                        if (tipo.equals("-1") || tipo.isEmpty()) tipo = "cuenta_corriente";

                        long sobregiro = tipo.contains("corriente") ? 300000L : 0L;
                        double tasaInteres = tipo.contains("ahorro") ? 3.8 : 0.5;

                        Cuenta cuenta = new Cuenta(
                                cuentaId, nombre, saldo, edad, tipo, sobregiro, tasaInteres, "ACTIVA"
                        );

                        bancoRepository.guardarCuenta(cuenta);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            log.error("[CORE] Error al cargar intereses.csv: {}", e.getMessage());
        }
    }

    private void cargarTransacciones() {
        try {
            Resource resource = resourceLoader.getResource("classpath:data/transacciones.csv");
            if (!resource.exists()) return;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String linea = br.readLine();
                while ((linea = br.readLine()) != null) {
                    if (linea.trim().isEmpty()) continue;
                    String[] datos = linea.split(",", -1);
                    if (datos.length < 4) continue;
                    try {
                        Long id = Long.parseLong(datos[0].trim());
                        LocalDate fecha = normalizarFecha(datos[1].trim());

                        long monto = 0L;
                        if (!datos[2].trim().isEmpty()) {
                            monto = (long) Math.abs(Double.parseDouble(datos[2].trim()));
                        }
                        if (monto == 0L) {
                            monto = (id % 5 + 1) * 5000L;
                        }

                        String tipo = datos[3].trim().toLowerCase();
                        if (tipo.equals("invalid") || tipo.equals("desconocido") || tipo.isEmpty()) {
                            tipo = (id % 2 == 0) ? "credito" : "debito";
                        }

                        String descripcion = ("credito".equalsIgnoreCase(tipo)) ? "Abono bancario" : "Cargo en cuenta";
                        Long cuentaId = (id % 150) + 100;

                        Transaccion tx = new Transaccion(
                                id, cuentaId, fecha, monto, tipo, descripcion, "LEGACY_BATCH"
                        );

                        bancoRepository.guardarTransaccion(tx);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            log.error("[CORE] Error al cargar transacciones.csv: {}", e.getMessage());
        }
    }

    private LocalDate normalizarFecha(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) return LocalDate.of(2024, 6, 15);
        fecha = fecha.trim().replace("/", "-");
        if (fecha.matches("\\d{2}-\\d{2}-\\d{4}")) {
            String[] partes = fecha.split("-");
            fecha = partes[2] + "-" + partes[1] + "-" + partes[0];
        }
        if (fecha.matches("\\d{4}-13-\\d{2}")) {
            fecha = fecha.replace("-13-", "-12-");
        }
        try {
            return LocalDate.parse(fecha);
        } catch (Exception e) {
            return LocalDate.of(2024, 6, 15);
        }
    }

    private void cargarCuentasAnuales() {
        try {
            Resource resource = resourceLoader.getResource("classpath:data/cuentas_anuales.csv");
            if (!resource.exists()) return;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String linea = br.readLine();
                while ((linea = br.readLine()) != null) {
                    if (linea.trim().isEmpty()) continue;
                    String[] datos = linea.split(",", -1);
                    if (datos.length < 5) continue;
                    try {
                        Long cuentaId = Long.parseLong(datos[0].trim());
                        LocalDate fecha = normalizarFecha(datos[1].trim());
                        String transaccion = datos[2].trim();
                        long monto = (long) Math.abs(Double.parseDouble(datos[3].trim()));
                        String descripcion = datos[4].trim();
                        if (descripcion.isEmpty()) descripcion = "Movimiento anual";

                        MovimientoAnual mov = new MovimientoAnual(
                                cuentaId, fecha, transaccion, monto, descripcion
                        );

                        bancoRepository.guardarMovimientoAnual(mov);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            log.error("[CORE] Error al cargar cuentas_anuales.csv: {}", e.getMessage());
        }
    }
}
