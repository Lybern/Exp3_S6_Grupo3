# Banco XYZ - Arquitectura de Microservicios Distribuida con Spring Cloud y BFFs Resilientes
### Asignatura: Desarrollo Backend III (PBY2203) - Experiencia 3 / Semana 6
**Grupo:** Grupo 3  
**Autora:** Carolina Delgado Sapunar  
**Repositorio GitHub:** [https://github.com/Lybern/Exp3_S6_Grupo3](https://github.com/Lybern/Exp3_S6_Grupo3)

---

## 1. Descripción de la Solución

Este proyecto implementa una arquitectura distribuida de microservicios autónomos y resilientes basada en **Spring Cloud 2025.1.2** y **Java 21** para el **Banco XYZ**. 

El sistema desacopla la lógica de negocio central en un **Core Service** y los canales de interacción mediante el patrón **Backend for Frontend (BFF)** (`bff-movil`, `bff-web`, `bff-cajero`), orquestados por un **Servidor de Configuración Centralizada (Spring Cloud Config Server)** y un **Servidor de Descubrimiento (Netflix Eureka Server)** con soporte de **Balanceo de Carga del Lado del Cliente** y **Tolerancia a Fallos / Circuit Breakers (Resilience4j)**.

```
                                  +-----------------------+
                                  |     CONFIG SERVER     |
                                  |    (Puerto: 8888)     |
                                  | * Repositorio Nativo  |
                                  | * config-repo/*.props |
                                  +-----------+-----------+
                                              |
                   +--------------------------+--------------------------+
                   |                          |                          |
                   v                          v                          v
        +--------------------+     +--------------------+     +--------------------+
        |  DISCOVERY SERVER  |     |    CORE SERVICE    |     |   BFFs (MOVIL/WEB/ |
        |  (Eureka: 8761)    |<----+   (Puerto: 8080)   |     |    CAJERO ATM)     |
        | * Registro Activo  |     | * Cuentas y Saldos |     | * Canales HTTPS    |
        | * Heartbeat / Info |     | * Persistencia CSV |     | * JWT + Delegated  |
        +---------^----------+     +----------^---------+     +---------+----------+
                  |                           |                         |
                  +---------------------------+-------------------------+
                                              |
                                              | @LoadBalanced RestTemplate + Eureka
                                              | Circuit Breaker & Fallback (Resilience4j)
                                              +-------------------------+
```

---

## 2. Mapa de Microservicios y Componentes

| Microservicio / Servidor | Puerto | Protocolo / Seguridad | Responsabilidad Principal |
| :--- | :---: | :---: | :--- |
| **`config-server`** | `8888` | HTTP (Basic Auth: `admin`/`gato`) | Servidor Spring Cloud Config con perfil nativo. Expone las propiedades externalizadas de todos los microservicios desde `config-repo/`. |
| **`discovery-server`** | `8761` | HTTP (Basic Auth: `eureka`/`eureka2026`) | Servidor Netflix Eureka para registro y descubrimiento dinámico de instancias en tiempo de ejecución. |
| **`core-service`** | `8080` | HTTP (Service Token JWT) | Microservicio transaccional del Core Bancario. Maneja persistencia de cuentas, movimientos y tasas (`intereses.csv`, `transacciones.csv`, `cuentas_anuales.csv`). |
| **`bff-movil`** | `8443` | HTTPS / PKCS12 + JWT (`aud: MOVIL`) | BFF optimizado para dispositivos móviles con respuestas ultraligeras (Top 3 movimientos, transferencias). |
| **`bff-web`** | `8444` | HTTPS / PKCS12 + JWT (`aud: WEB`) | BFF para navegadores web. Dashboard consolidado, desglose anual con tasas de interés y cabeceras de protección OWASP (CSP, HSTS, `X-Frame-Options: DENY`). |
| **`bff-cajero`** | `8445` | HTTPS / PKCS12 + JWT (`aud: ATM`) | BFF para cajeros automáticos (ATM). Giros en múltiplos de $5.000, validación de PIN de 4 dígitos y límites de dispensación configurados vía Config Server. |

---

## 3. Matriz de Credenciales y Autenticación

| Componente | Usuario | Contraseña | Rol / Audiencia | Tipo de Autenticación |
| :--- | :--- | :--- | :--- | :--- |
| ⚙️ **Config Server** | `admin` | `gato` | `ROLE_ADMIN` | HTTP Basic Auth |
| 🔍 **Discovery Server** | `eureka` | `eureka2026` | `ROLE_ADMIN` | HTTP Basic Auth |
| 📱 **BFF Móvil** | `usuario_movil` | `movil123` | `ROLE_MOVIL` / `aud: MOVIL` | JWT Bearer Token |
| 🌐 **BFF Web** | `usuario_web` | `web123` | `ROLE_WEB` / `aud: WEB` | JWT Bearer Token |
| 🏧 **BFF Cajero ATM** | `operador_atm` | `atm123` | `ROLE_ATM` / `aud: ATM` | JWT Bearer Token |
| 🏧 **BFF Cajero ATM (Alt)**| `usuario_cajero` | `cajero123` | `ROLE_ATM` / `aud: ATM` | JWT Bearer Token |
| 🏢 **Core Bancario (Interno)**| `bff-*-client` | *Delegated Token* | `SERVICE_TOKEN` / `aud: core-bancario` | Service JWT firmado compartido |

---

## 4. Resiliencia y Tolerancia a Fallos (Resilience4j)

Cada BFF (`bff-movil`, `bff-web`, `bff-cajero`) cuenta con una instancia de **Circuit Breaker** (`coreServiceCB`) configurada en el repositorio central de configuración:

* **Ventana Deslizante (`slidingWindowSize`):** 10 llamadas.
* **Umbral de Fallos (`failureRateThreshold`):** 50%.
* **Tiempo en Estado Abierto (`waitDurationInOpenState`):** 10.000 ms (10 segundos).
* **Llamadas de Prueba en Half-Open (`permittedNumberOfCallsInHalfOpenState`):** 3 llamadas.
* **Comportamiento Degradado (Fallback):**
  * Si el `core-service` no está disponible o el circuito está **OPEN**, los BFFs devuelven una respuesta estructurada con estado `"DEGRADADO_FALLBACK"` o mensaje de contingencia sin propagar errores `500 Internal Server Error`.
  * La dispensación física en cajeros rechaza transacciones no confirmadas con `503 Service Unavailable`, manteniendo la integridad monetaria.

---

## 5. Instrucciones de Compilación y Puesta en Marcha

### Prerrequisitos:
* **Java:** OpenJDK 21 (LTS) o superior.
* **Maven:** Incluido a través de `./mvnw` / `mvnw.cmd`.

### Paso 1: Compilar todo el ecosistema
```bash
./mvnw clean test-compile
```

### Paso 2: Orden de Inicio de los Microservicios

Es fundamental iniciar los componentes en el siguiente orden secuencial:

1. **Terminal 1 - Iniciar Config Server (Puerto 8888):**
   ```bash
   ./mvnw -pl config-server spring-boot:run
   ```
   *Verificación:* `curl -u admin:gato http://localhost:8888/core-service/default`

2. **Terminal 2 - Iniciar Discovery Server Eureka (Puerto 8761):**
   ```bash
   ./mvnw -pl discovery-server spring-boot:run
   ```
   *Dashboard Eureka:* [http://localhost:8761](http://localhost:8761) (Credenciales: `eureka` / `eureka2026`)

3. **Terminal 3 - Iniciar Core Service (Puerto 8080):**
   ```bash
   ./mvnw -pl core-service spring-boot:run
   ```

4. **Terminal 4 - Iniciar BFF Móvil (Puerto HTTPS 8443):**
   ```bash
   ./mvnw -pl bff-movil spring-boot:run
   ```

5. **Terminal 5 - Iniciar BFF Web (Puerto HTTPS 8444):**
   ```bash
   ./mvnw -pl bff-web spring-boot:run
   ```

6. **Terminal 6 - Iniciar BFF Cajero ATM (Puerto HTTPS 8445):**
   ```bash
   ./mvnw -pl bff-cajero spring-boot:run
   ```

---

## 6. Pruebas y Verificación de Endpoints

### A. Verificación de Config Server y Eureka
```bash
# Consultar propiedades de bff-movil desde Config Server
curl -u admin:gato http://localhost:8888/bff-movil/default

# Consultar aplicaciones registradas en Eureka
curl -u eureka:eureka2026 -H "Accept: application/json" http://localhost:8761/eureka/apps
```

### B. Pruebas en BFF Móvil (HTTPS 8443)
```powershell
# 1. Login y obtención de JWT
[System.Net.ServicePointManager]::ServerCertificateValidationCallback = {$true}
$login = Invoke-RestMethod -Uri 'https://localhost:8443/api/auth/login' -Method Post -Body '{"username":"usuario_movil","password":"movil123"}' -ContentType 'application/json'
$headers = @{ Authorization = 'Bearer ' + $login.token }

# 2. Consultar Resumen Ligero de Cuenta 108
Invoke-RestMethod -Uri 'https://localhost:8443/api/v1/movil/cuentas/108' -Method Get -Headers $headers

# 3. Realizar Transferencia Móvil
$tx = '{"cuentaDestinoId":113,"monto":1000,"comentario":"Pago almuerzo"}'
Invoke-RestMethod -Uri 'https://localhost:8443/api/v1/movil/cuentas/108/transferencia' -Method Post -Body $tx -ContentType 'application/json' -Headers $headers
```

### C. Pruebas en BFF Web (HTTPS 8444)
```powershell
# 1. Login y obtención de JWT Web
$loginWeb = Invoke-RestMethod -Uri 'https://localhost:8444/api/auth/login' -Method Post -Body '{"username":"usuario_web","password":"web123"}' -ContentType 'application/json'
$headersWeb = @{ Authorization = 'Bearer ' + $loginWeb.token }

# 2. Consultar Detalle Web Completo de Cuenta 106
Invoke-RestMethod -Uri 'https://localhost:8444/api/v1/web/cuentas/106' -Method Get -Headers $headersWeb
```

### D. Pruebas en BFF Cajero ATM (HTTPS 8445)
```powershell
# 1. Login Cajero ATM
$loginAtm = Invoke-RestMethod -Uri 'https://localhost:8445/api/auth/login' -Method Post -Body '{"username":"operador_atm","password":"atm123"}' -ContentType 'application/json'
$headersAtm = @{ Authorization = 'Bearer ' + $loginAtm.token }

# 2. Consultar Saldo en Cajero
Invoke-RestMethod -Uri 'https://localhost:8445/api/v1/cajero/cuentas/106/saldo' -Method Get -Headers $headersAtm

# 3. Realizar Giro en Efectivo (PIN 4 dígitos)
$giro = '{"monto":5000,"pin":"1234","terminalId":"ATM-001"}'
Invoke-RestMethod -Uri 'https://localhost:8445/api/v1/cajero/cuentas/106/retiro' -Method Post -Body $giro -ContentType 'application/json' -Headers $headersAtm
```

### E. Prueba de Resiliencia / Circuit Breaker (Core Service Offline)
Si se detiene `core-service`, los BFFs activan automáticamente sus fallbacks:
```powershell
# Consulta de cuenta bajo caída de Core -> Retorna Modo Degradado
Invoke-RestMethod -Uri 'https://localhost:8443/api/v1/movil/cuentas/106' -Method Get -Headers $headers
```
**Respuesta:**
```json
{
  "cuentaId": 106,
  "nombreTitular": "Usuario Móvil (Modo Degradado)",
  "tipoCuenta": "ahorro",
  "saldoDisponible": 0,
  "ultimosMovimientos": [],
  "canal": "MOVIL"
}
```

---

## 7. Documentación Swagger / OpenAPI

Cada microservicio expone su interfaz Swagger UI para exploración interactiva:
* 📱 **BFF Móvil:** `https://localhost:8443/swagger-ui/index.html`
* 🌐 **BFF Web:** `https://localhost:8444/swagger-ui/index.html`
* 🏧 **BFF Cajero:** `https://localhost:8445/swagger-ui/index.html`
* 🏢 **Core Service:** `http://localhost:8080/swagger-ui/index.html`
* 🩺 **Actuator Health & Metrics:** `https://localhost:8443/actuator/health`, `https://localhost:8444/actuator/health`, `https://localhost:8445/actuator/health`
