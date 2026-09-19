# Banco XYZ - Arquitectura Backend for Frontend (BFF) Multi-Módulo con Seguridad Integral
### Asignatura: Desarrollo Backend III (PBY2203) - Experiencia 2 / Semana 5
**Autora:** Carolina Delgado Sapunar  
**Repositorio GitHub:** [https://github.com/Lybern/Exp2_S5_Carolina_Delgado_Sapunar](https://github.com/Lybern/Exp2_S5_Carolina_Delgado_Sapunar)

---

## 1. Descripción de la Solución

Este proyecto implementa una arquitectura **Backend for Frontend (BFF) Multi-Módulo y Distribuida** para el **Banco XYZ**, desacoplando los canales de atención en microservicios independientes y altamente especializados, comunicados de forma cifrada con el **Core Bancario**:

```
                                    +-----------------------+
                                    |   CLIENTES BANCARIOS  |
                                    +-----------+-----------+
                                                |
                 +------------------------------+------------------------------+
                 | (HTTPS / TLS Puerto 8443)    | (HTTPS / TLS Puerto 8444)    | (HTTPS / TLS Puerto 8445)
                 v                              v                              v
        +------------------+           +------------------+           +------------------+
        |    BFF MOVIL     |           |     BFF WEB      |           |    BFF CAJERO    |
        |  (Puerto: 8443)  |           |  (Puerto: 8444)  |           |  (Puerto: 8445)  |
        | * Top 3 Movim.   |           | * Dashboard Web  |           | * Giros $5.000   |
        | * Payload Ligero |           | * Tasas y Anual  |           | * Valida PIN 4D  |
        | * aud: "MOVIL"   |           | * aud: "WEB"/CSP |           | * aud: "ATM"     |
        +--------+---------+           +--------+---------+           +--------+---------+
                 |                              |                              |
                 +------------------------------+------------------------------+
                                                |
                                                | Token Exchange: Delegated Service Token
                                                | (Firma: 'jwt.service-secret' | aud: 'core-bancario')
                                                v
                                    +-----------------------+
                                    |     CORE SERVICE      |
                                    |    (Puerto: 8080)     |
                                    | * BancoRepository     |
                                    |   (AtomicLong / Mem)  |
                                    | * Datos Legacy CSV    |
                                    | * ServiceTokenFilter  |
                                    +-----------------------+
```

---

## 2. Microservicios del Sistema

| Microservicio | Puerto | Protocolo | Responsabilidad Principal |
| :--- | :---: | :---: | :--- |
| **`core-service`** | `8080` | HTTP / ServiceToken | Core transaccional y persistencia legacy (`intereses.csv`, `transacciones.csv`, `cuentas_anuales.csv`). Protegido por `ServiceTokenFilter` con clave secreta compartida. |
| **`bff-movil`** | `8443` | HTTPS / PKCS12 | BFF exclusivo para smartphones. Respuestas ultraligeras (Top 3 transacciones, transferencias). Protegido por Spring Security con audiencia `MOVIL`. |
| **`bff-web`** | `8444` | HTTPS / PKCS12 | BFF para navegadores web. Dashboard consolidado de administración, desglose anual con tasas y cabeceras OWASP (CSP, HSTS, `X-Frame-Options: DENY`). Audiencia `WEB`. |
| **`bff-cajero`** | `8445` | HTTPS / PKCS12 | BFF para cajeros automáticos (ATM). Giros en múltiplos de $5.000, validación regex de PIN de 4 dígitos (`^\d{4}$`) y límites de dispensación. Audiencia `ATM`. |

---

## 3. Matriz de Credenciales y Roles de Seguridad

| Microservicio / Canal | Usuario | Contraseña | Rol Spring Security | Audiencia JWT (`aud`) | URL Swagger UI |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 📱 **BFF Móvil** | `usuario_movil` | `movil123` | `ROLE_MOVIL` | `MOVIL` | `https://localhost:8443/swagger-ui/index.html` |
| 🌐 **BFF Web** | `usuario_web` | `web123` | `ROLE_WEB` | `WEB` | `https://localhost:8444/swagger-ui/index.html` |
| 🏧 **BFF Cajero ATM** | `usuario_cajero` | `cajero123` | `ROLE_ATM` | `ATM` | `https://localhost:8445/swagger-ui/index.html` |
| 🏢 **Core Service** | Interno | - | `SERVICE_TOKEN` | `core-bancario` | `http://localhost:8080/swagger-ui/index.html` |

---

## 4. Instrucciones de Compilación y Ejecución

### 1. Compilar y Ejecutar Pruebas Automatizadas:
```bash
./mvnw clean test
```
> Ejecuta la suite de pruebas automatizadas en los 4 microservicios con **`BUILD SUCCESS`** (100% de éxito).

### 2. Ejecutar los Microservicios:
Puedes iniciar cada microservicio en terminales independientes:

* **Terminal 1 - Core Service (Puerto 8080):**
  ```bash
  ./mvnw spring-boot:run -pl core-service
  ```

* **Terminal 2 - BFF Móvil (Puerto HTTPS 8443):**
  ```bash
  ./mvnw spring-boot:run -pl bff-movil
  ```

* **Terminal 3 - BFF Web (Puerto HTTPS 8444):**
  ```bash
  ./mvnw spring-boot:run -pl bff-web
  ```

* **Terminal 4 - BFF Cajero ATM (Puerto HTTPS 8445):**
  ```bash
  ./mvnw spring-boot:run -pl bff-cajero
  ```

---

## 5. Guía y Evidencias de Pruebas con Postman

Todas las pruebas del sistema se ejecutan y validan a través de **Postman** utilizando la colección exportada en la raíz del repositorio: [**`Banco_XYZ_Postman_Collection.json`**](./Banco_XYZ_Postman_Collection.json).

### Configuración Previa en Postman:
1. Ir a **Settings** (ícono de engranaje superior derecho) $\rightarrow$ Pestaña **General**.
2. Desactivar **SSL certificate verification** $\rightarrow$ **`OFF`** (necesario para aceptar los certificados HTTPS TLS autofirmados en `localhost`).
3. Importar la colección `Banco_XYZ_Postman_Collection.json`.

---

### A. Canal Móvil (HTTPS 8443)

#### 1. Request 1: Login Móvil (Obtener Token JWT)
* **Método:** `POST`
* **URL:** `https://localhost:8443/api/auth/login`
* **Headers:** `Content-Type: application/json`
* **Body (raw JSON):**
  ```json

* **Respuesta Esperada (`HTTP 200 OK`):**
  ```json
  {
    "token": "...",
    "tipoToken": "Bearer",
    "username": "usuario_movil",
    "rol": "ROLE_MOVIL",
    "audiencia": "MOVIL",
    "expiracionMilisegundos": 3600000
  }
  ```
* *Captura automática:* El script de pruebas de Postman guarda el token generado directamente en la variable `{{jwt_movil}}`.

#### 2. Request 2: Consultar Saldo (Cuenta 102 - Alice Brown)
* **Método:** `GET`
* **URL:** `https://localhost:8443/api/v1/movil/cuentas/102/saldo`
* **Headers:** `Authorization: Bearer {{jwt_movil}}`
* **Respuesta Esperada (`HTTP 200 OK`):** Retorna saldo disponible de `$312.000`.

#### 3. Request 3: Consultar Resumen Ligero (Top 3 Movimientos)
* **Método:** `GET`
* **URL:** `https://localhost:8443/api/v1/movil/cuentas/102/resumen`
* **Headers:** `Authorization: Bearer {{jwt_movil}}`
* **Respuesta Esperada (`HTTP 200 OK`):** Retorna payload compacto con saldo disponible y el Top 3 de últimos movimientos históricos.

---

### B. Canal Web (HTTPS 8444)

#### 1. Request 4: Login Web (Obtener Token JWT)
* **Método:** `POST`
* **URL:** `https://localhost:8444/api/auth/login`
* **Headers:** `Content-Type: application/json`
* **Body (raw JSON):**
 
* **Respuesta Esperada (`HTTP 200 OK`):** Emite token JWT con audiencia `aud: "WEB"`, guardado en la variable `{{jwt_web}}`.

#### 2. Request 5: Consultar Dashboard Global con Métricas (50 Cuentas)
* **Método:** `GET`
* **URL:** `https://localhost:8444/api/v1/web/dashboard`
* **Headers:** `Authorization: Bearer {{jwt_web}}`
* **Respuesta Esperada (`HTTP 200 OK`):** Consolida las **50 cuentas activas** cargadas desde los CSVs, total de liquidez ($48.250.000) y métricas de riesgo.

#### 3. Request 6: Detalle Contable Completo de Cuenta
* **Método:** `GET`
* **URL:** `https://localhost:8444/api/v1/web/cuentas/102`
* **Headers:** `Authorization: Bearer {{jwt_web}}`
* **Respuesta Esperada (`HTTP 200 OK`):** Detalle financiero exhaustivo, historial anual y tasas de interés aplicadas (3.8% anual).

---

### C. Canal Cajero ATM (HTTPS 8445)

#### 1. Request 7: Login Cajero ATM (Obtener Token JWT)
* **Método:** `POST`
* **URL:** `https://localhost:8445/api/auth/login`
* **Headers:** `Content-Type: application/json`
* **Body (raw JSON):**

* **Respuesta Esperada (`HTTP 200 OK`):** Emite token JWT con audiencia `aud: "ATM"`, guardado en la variable `{{jwt_atm}}`.

#### 2. Request 8: Consulta de Saldo en ATM
* **Método:** `GET`
* **URL:** `https://localhost:8445/api/v1/cajero/cuentas/102/saldo`
* **Headers:** `Authorization: Bearer {{jwt_atm}}`
* **Respuesta Esperada (`HTTP 200 OK`):** Retorna saldo disponible para dispensación en cajero automático.

#### 3. Request 9: Retiro de Efectivo (Múltiplo de $5.000, PIN 4 Dígitos y Terminal ID)
* **Método:** `POST`
* **URL:** `https://localhost:8445/api/v1/cajero/cuentas/102/retiro`
* **Headers:** 
  * `Authorization: Bearer {{jwt_atm}}`
  * `Content-Type: application/json`
* **Body (raw JSON):**
  ```json
  ```
* **Respuesta Esperada (`HTTP 200 OK`):**
---

### D. Casos de Seguridad y Validaciones

#### 1. Request 10: Bloqueo de Acceso Sin Token
* **Método:** `GET`
* **URL:** `https://localhost:8443/api/v1/movil/cuentas/102/saldo`
* **Headers:** *(Sin cabecera Authorization)*
* **Resultado:** `HTTP 403 Forbidden` (Rechazo automático por Spring Security).

#### 2. Request 11: Aislamiento por Audiencia y Mitigación Cross-Channel
* **Método:** `GET`
* **URL:** `https://localhost:8444/api/v1/web/dashboard`
* **Headers:** `Authorization: Bearer {{jwt_movil}}` *(Uso de token emitido para móvil en el endpoint web)*
* **Resultado:** `HTTP 403 Forbidden` (`JwtAuthenticationFilter` detecta discrepancia de audiencia `aud="MOVIL"` vs `aud="WEB"` requerida).

#### 3. Request 12: Rechazo de Conexión HTTP Plano al Puerto Seguro
* **Método:** `GET`
* **URL:** `http://localhost:8443/api/v1/movil/cuentas/102/saldo` *(HTTP plano al puerto TLS 8443)*
* **Resultado:** `HTTP 400 Bad Request` (`This combination of host and port requires TLS/SSL`).

#### 4. Request 13: Rechazo por Bean Validation (Omisión de terminalId o PIN Inválido)
* **Método:** `POST`
* **URL:** `https://localhost:8445/api/v1/cajero/cuentas/102/retiro`
* **Headers:** `Authorization: Bearer {{jwt_atm}}`, `Content-Type: application/json`
* **Body (raw JSON):**
  ```json
  *(Se omite el campo obligatorio `terminalId`)*
* **Resultado:** `HTTP 400 Bad Request`
  ```json
  {
    "timestamp": "2026-09-14T15:32:00",
    "status": 400,
    "error": "Bad Request",
    "message": "El identificador del terminal fisico es obligatorio",
    "path": "/api/v1/cajero/cuentas/102/retiro"
  }
  ```
