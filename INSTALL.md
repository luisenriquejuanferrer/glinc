# Guía de instalación de Glinc — desde cero

## Requisitos previos

Instala en este orden:

### 1. Git
Descarga desde https://git-scm.com → instalar con opciones por defecto.

Verifica:
```powershell
git --version
```

### 2. Node.js 20 LTS
Descarga desde https://nodejs.org (versión **20 LTS**).

Verifica:
```powershell
node --version   # v20.x.x
npm --version
```

### 3. Java JDK 21
Descarga desde https://www.oracle.com/java/technologies/downloads/#java21 (Oracle JDK 21) o https://adoptium.net.

Verifica:
```powershell
java --version   # java 21.x.x
```

> **Windows**: asegúrate de que `JAVA_HOME` apunta al JDK 21 y que `%JAVA_HOME%\bin` está en el `PATH`.

### 4. PostgreSQL 16 o superior
Descarga desde https://www.postgresql.org/download/windows.

Durante la instalación:
- Usuario por defecto: `postgres`
- Pon una contraseña al usuario `postgres` (p. ej. `admin`)
- Puerto por defecto: `5432`
- Marca **pgAdmin** si quieres interfaz gráfica

### 5. Ionic CLI (para el frontend)
```powershell
npm install -g @ionic/cli
```

---

## Clonar el repositorio

```powershell
git clone https://github.com/luisenriquejuanferrer/glinc.git
cd glinc
```

---

## Paso 1 — Crear la base de datos

Abre **pgAdmin** o el terminal de PostgreSQL (`psql`) y ejecuta:

```sql
CREATE DATABASE glinc;
```

Si usas `psql`:
```powershell
psql -U postgres -c "CREATE DATABASE glinc;"
```

---

## Paso 2 — Configurar el bridge (`cgm-bridge-service`)

```powershell
cd cgm-bridge-service
npm install
```

Crea el archivo `.env` (copia del ejemplo):
```powershell
Copy-Item .env.example .env
```

El `.env` por defecto ya tiene todo lo necesario para desarrollo local. Solo necesitas ajustar `SERVICE_TOKENS` si cambias el token del backend (por defecto ambos usan `token-interno-1`).

Compila:
```powershell
npm run build
```

---

## Paso 3 — Configurar el backend (`glinc-backend`)

```powershell
cd ..\glinc-backend
```

Crea el archivo `.env` o exporta las variables. El backend necesita saber la contraseña de PostgreSQL que pusiste al instalar:

**Opción A — Variables de entorno en PowerShell** (solo dura la sesión actual):
```powershell
$env:DB_PASSWORD = "admin"    # la contraseña que pusiste al instalar PostgreSQL
$env:DB_USER     = "postgres"
$env:DB_URL      = "jdbc:postgresql://localhost:5432/glinc"
$env:BRIDGE_SERVICE_TOKEN = "token-interno-1"
```

**Opción B — Crear `.env` en el directorio del backend** y editar los valores:
```powershell
Copy-Item .env.example .env
notepad .env
```

> Las Flyway migrations (`V1` a `V4`) se ejecutan solas al arrancar Spring Boot por primera vez — crean todas las tablas automáticamente. No hay que hacer nada manual en la BD.

---

## Paso 4 — Configurar el frontend (`glinc-frontend`)

```powershell
cd ..\glinc-frontend
npm install
```

No necesita configuración adicional. La URL del backend (`http://localhost:8080`) ya está hardcodeada en los servicios para desarrollo local.

---

## Arrancar todo

Vuelve a la raíz del proyecto:

```powershell
cd ..
```

### Opción A — Script automático (recomendado)

```powershell
.\start-glinc.ps1
```

Abre 3 terminales (tabs de Windows Terminal o ventanas separadas):
- `:3001` → bridge
- `:8080` → backend
- `:8100` → frontend

### Opción B — Manual (3 terminales por separado)

**Terminal 1 — Bridge:**
```powershell
cd cgm-bridge-service
npm start
```

**Terminal 2 — Backend** (con las variables de entorno exportadas):
```powershell
cd glinc-backend
$env:DB_PASSWORD = "admin"
.\mvnw spring-boot:run
```

**Terminal 3 — Frontend:**
```powershell
cd glinc-frontend
ionic serve
```

---

## Verificar que funciona

Una vez arrancado todo, abre en el navegador:

| URL | Qué es |
|-----|--------|
| `http://localhost:8100` | App Glinc (frontend) |
| `http://localhost:8080/actuator/health` | Health check backend → `{"status":"UP"}` |
| `http://localhost:3001/v1/health/live` | Health check bridge → `{"status":"ok"}` |
| `http://localhost:8080/docs` | Swagger UI del backend |
| `http://localhost:3001/docs` | Swagger UI del bridge |

---

## Hacer login

En `http://localhost:8100` introduce tus credenciales de **LibreLink Up** (el mismo email y contraseña que usas en la app oficial de Abbott FreeStyle LibreLink).

> Al primer login se generan automáticamente **90 días de lecturas sintéticas** para tener histórico en la demo. Las lecturas reales del sensor empiezan a acumularse cada 90 segundos.

---

## Parar todo

```powershell
.\stop-glinc.ps1
```

Mata los procesos en los puertos 3001, 8080 y 8100.

---

## Solución de problemas comunes

**Backend no arranca — error de BD:**
- Verifica que PostgreSQL está corriendo: `Get-Service postgresql*`
- Verifica que la BD `glinc` existe: `psql -U postgres -l`
- Verifica que `DB_PASSWORD` está exportada correctamente

**Flyway error de checksum:**
```powershell
psql -U postgres -d glinc -c "UPDATE flyway_schema_history SET checksum = NULL WHERE version IN ('1','2','3','4');"
```
Luego vuelve a arrancar el backend.

**Frontend — error de CORS o 401:**
- Asegúrate de que el backend está corriendo antes de abrir la app
- Limpia el `localStorage` del navegador (F12 → Application → Local Storage → Clear)

**Bridge — `No hay pacientes en la cuenta LibreLink`:**
- Verifica que tienes al menos un paciente vinculado en la app oficial FreeStyle LibreLink

---

## Estructura del repo

```
glinc/
  cgm-bridge-service/    Node.js — puente LibreLink Up ↔ backend
  glinc-backend/         Spring Boot — lógica de negocio y API REST
  glinc-frontend/        Angular + Ionic — app web
  start-glinc.ps1        Arranca los 3 servicios
  stop-glinc.ps1         Para los 3 servicios
  README.md              Este archivo
```
