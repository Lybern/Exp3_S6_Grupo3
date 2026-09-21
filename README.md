# Banco XYZ - Arquitectura de Microservicios Distribuida con Spring Cloud. Implementación de Config Server, Eureka y BFFs resilientes
### Asignatura: Desarrollo Backend III (PBY2203) - Experiencia 3 / Semana 6
**Grupo:** Grupo 3  
**Integrantes:** Leonardo Bustamante - Carolina Delgado Sapunar  
**Repositorio GitHub:** [https://github.com/Lybern/Exp3_S6_Grupo3](https://github.com/Lybern/Exp3_S6_Grupo3)

---

## 1. Descripción de la Solución

Este proyecto implementa una arquitectura distribuida de microservicios autónomos y resilientes basada en **Spring Cloud 2025.1.2** y **Java 21** para el **Banco XYZ**. 

El sistema desacopla la lógica de negocio central en un **Core Service** y los canales de interacción mediante el patrón **Backend for Frontend (BFF)** (`bff-movil`, `bff-web`, `bff-cajero`), orquestados por un **Servidor de Configuración Centralizada (Spring Cloud Config Server)** y un **Servidor de Descubrimiento (Netflix Eureka Server)** con soporte de **Balanceo de Carga del Lado del Cliente** y **Tolerancia a Fallos / Circuit Breakers y Retry (Resilience4j)**.

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
                                              | Circuit Breaker, Retry & Fallback (Resilience4j)
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

### Aislamiento de Autorización por Canal (Seguridad Diferenciada)

Cada microservicio BFF aplica control de acceso perimetral e independiente mediante Spring Security y JJWT:
* **Validación de Audiencia (`aud`) y Rol:** Cada BFF valida criptográficamente que el token recibido corresponda estrictamente a su propio canal (`MOVIL`, `WEB`, `ATM`) y que el usuario posea la autoridad correspondiente (`ROLE_MOVIL`, `ROLE_WEB`, `ROLE_ATM`).
* **Protección Cruzada (Cross-Channel Isolation):** Un token emitido para un canal específico (ej: `bff-movil`) es rechazado de inmediato con **`HTTP 403 Forbidden`** si se intenta utilizar para acceder a los endpoints de otro canal (`bff-web` o `bff-cajero`).
* **Bloqueo No Autenticado:** Cualquier consulta a rutas protegidas sin cabecera `Authorization: Bearer <token>` es bloqueada con **`HTTP 403 Forbidden`**.
* **Tokens de Servicio Delegados:** La comunicación interna entre los BFFs y el `core-service` utiliza tokens firmados independientes con audiencia `core-bancario` y expiración corta, desacoplando los tokens de los clientes finales del backend central.

---

## 4. Resiliencia y Tolerancia a Fallos (Resilience4j)

Cada BFF (`bff-movil`, `bff-web`, `bff-cajero`) cuenta con una instancia combinada de **Circuit Breaker** y **Retry** (`coreServiceCB`) configurada en el repositorio central de configuración:

* **Circuit Breaker:**
  * **Ventana Deslizante (`slidingWindowSize`):** 10 llamadas.
  * **Umbral de Fallos (`failureRateThreshold`):** 50%.
  * **Tiempo en Estado Abierto (`waitDurationInOpenState`):** 10.000 ms (10 segundos).
  * **Llamadas de Prueba en Half-Open (`permittedNumberOfCallsInHalfOpenState`):** 3 llamadas.
* **Retry (Reintentos automáticos ante fallas transitorias):**
  * **Intentos Máximos (`maxAttempts`):** 3 llamadas.
  * **Tiempo de Espera (`waitDuration`):** 1.000 ms (1 segundo) entre intentos.
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

## 6. Pruebas y Verificación de Endpoints en Postman

Las pruebas de integración y verificación funcional se realizan a través de **Postman** (y navegador web para dashboards), consumiendo las APIs REST expuestas por cada microservicio.

### A. Matriz de Endpoints para Pruebas

| Canal / Servicio | Método | Endpoint | Descripción | Autenticación en Postman |
| :--- | :---: | :--- | :--- | :--- |
| **Config Server** | `GET` | `/core-service/default` | Obtener propiedades centralizadas del microservicio | Basic Auth (`admin`/`gato`) |
| **Discovery Server** | `GET` | `/eureka/apps` | Consultar microservicios registrados en Eureka | Basic Auth (`eureka`/`eureka2026`) |
| **BFF Móvil** | `POST` | `/api/auth/login` | Autenticación de usuario móvil y emisión de JWT | Body JSON (Pública) |
| **BFF Móvil** | `GET` | `/api/v1/movil/cuentas/{id}` | Resumen ligero de cuenta y movimientos | Bearer Token (`aud: MOVIL`) |
| **BFF Móvil** | `POST` | `/api/v1/movil/cuentas/{id}/transferencia` | Transferencia electrónica de fondos | Bearer Token (`aud: MOVIL`) |
| **BFF Web** | `POST` | `/api/auth/login` | Autenticación de usuario web y emisión de JWT | Body JSON (Pública) |
| **BFF Web** | `GET` | `/api/v1/web/cuentas/{id}` | Detalle financiero anual con cálculo de intereses | Bearer Token (`aud: WEB`) |
| **BFF Cajero ATM** | `POST` | `/api/auth/login` | Autenticación de terminal cajero y emisión de JWT | Body JSON (Pública) |
| **BFF Cajero ATM** | `GET` | `/api/v1/cajero/cuentas/{id}/saldo` | Consulta de saldo disponible para dispensación | Bearer Token (`aud: ATM`) |
| **BFF Cajero ATM** | `POST` | `/api/v1/cajero/cuentas/{id}/retiro` | Retiro de efectivo (múltiplos de $5.000) | Bearer Token (`aud: ATM`) |

---

### B. Especificación de Pruebas Paso a Paso en Postman

#### 1. Verificación de Infraestructura (Config Server y Eureka)
* **Config Server (Puerto 8888):**
  * **Método:** `GET` | **URL:** `http://localhost:8888/core-service/default` (o `/bff-movil/default`)
  * **Pestaña Authorization:** Type *Basic Auth* (Username: `admin` / Password: `gato`).
  * **Respuesta Esperada (`200 OK`):** Retorna el JSON con las fuentes de configuración cargadas dinámicamente desde `config-repo/`.
* **Discovery Server (Puerto 8761):**
  * **Navegador Web:** Ingresar a [http://localhost:8761](http://localhost:8761) con credenciales `eureka` / `eureka2026`.
  * **Verificación Visual:** El panel de Eureka lista las 4 aplicaciones registradas en estado `UP` (`BFF-MOVIL`, `BFF-WEB`, `BFF-CAJERO`, `CORE-SERVICE`).

#### 2. Autenticación de Clientes en Postman (Login)
* **Método:** `POST`
* **URL:** `https://localhost:8443/api/auth/login` *(o 8444 para Web / 8445 para Cajero)*
* **Pestaña Body:** Seleccionar `raw` -> `JSON`:
  ```json
  {
    "username": "usuario_movil",
    "password": "movil123"
  }
  ```
* **Respuesta Esperada (`200 OK`):** Retorna el token JWT emitido, detallando el canal (`MOVIL`), rol (`ROLE_MOVIL`) y tiempo de expiración. Copiar el valor del campo `token`.

#### 3. Consumo de Negocio en Operación Normal
* **Método:** `GET`
* **URL:** `https://localhost:8443/api/v1/movil/cuentas/106`
* **Pestaña Headers:**
  * Key: `Authorization`
  * Value: `Bearer <TOKEN_JWT>`
* **Respuesta Esperada (`200 OK`):** El BFF Móvil descubre dinámicamente a `http://core-service` mediante Eureka y retorna los datos contables del titular (`John Doe`), saldo disponible y los 3 últimos movimientos históricos.

#### 4. Prueba de Resiliencia y Tolerancia a Fallos (Fallback)
* **Escenario:** Detener el microservicio central `core-service` en la terminal.
* **Método:** `GET`
* **URL:** `https://localhost:8443/api/v1/movil/cuentas/106`
* **Pestaña Headers:** `Authorization: Bearer <TOKEN_JWT>`
* **Respuesta Esperada (`200 OK` - Modo Degradado):** El Circuit Breaker (`coreServiceCB`) de Resilience4j intercepta la caída del servicio central y deriva la ejecución al método fallback sin lanzar error 500:
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
