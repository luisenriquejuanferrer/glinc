# Glinc

App de monitoreo de glucosa en tiempo real (CGM) para multiples pacientes usando FreeStyle Libre (Abbott LibreLink Up). Permite a un cuidador ver la glucosa de todos sus pacientes asociados, con historico, estadisticas, inventario y citas medicas.

TFG de 2DAM. Tres servicios en monorepo:

```
Glinc/
  cgm-bridge-service/    Node.js + TypeScript   — puente LibreLink Up
  glinc-backend/         Spring Boot + Java 21  — API REST, logica, BD
  glinc-frontend/        Angular 20 + Ionic 8   — web (solo navegador)
```

Para el contexto tecnico completo ver `CLAUDE.md` (raiz + uno por modulo).

---

## 1. Requisitos previos

Esta guia asume **Windows 10/11** con PowerShell. La app es solo web (Android descartado).

| Software | Version minima probada | Notas |
|---|---|---|
| **Node.js LTS** | 20.x | tambien para el frontend Ionic |
| **JDK** | **21 exacto** | el backend valida el schema con `ddl-auto=validate`, no funciona con otra major |
| **PostgreSQL** | 16 (probado en 18) | usuario `postgres`, password `admin` por defecto |
| **Git** | cualquier reciente | para clonar el repo |
| **Ionic CLI** | 7+ | `npm install -g @ionic/cli` |
| Windows Terminal (opcional) | — | `start-glinc.ps1` lo detecta y abre 3 tabs |

### Verificacion rapida

```powershell
node --version       # v20.x.x o superior
java --version       # openjdk 21.x.x  (o oracle jdk 21)
psql --version       # psql (PostgreSQL) 16.x  o superior
ionic --version      # 7.x o superior
```

Si `java --version` muestra otra major:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"  # ajusta a tu ruta real
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

---

## 2. Clonar y crear la base de datos

```powershell
git clone <URL-del-repo> Glinc
cd Glinc
```

Crea la BD vacia (Flyway aplicara las migraciones la primera vez que arranque el backend):

```powershell
psql -U postgres -c "CREATE DATABASE glinc;"
```

Si tu usuario/password de Postgres no son `postgres / admin`, exporta antes de arrancar el backend:

```powershell
$env:DB_USER = "tu_usuario"
$env:DB_PASSWORD = "tu_password"
# DB_URL solo si la BD no esta en localhost:5432/glinc
$env:DB_URL = "jdbc:postgresql://localhost:5432/glinc"
```

(Defaults internos del backend: `postgres / admin / localhost:5432/glinc`.)

---

## 3. Configurar el bridge (LibreLink)

```powershell
cd cgm-bridge-service
copy .env.example .env
npm install
cd ..
```

El `.env` ya viene con `SERVICE_TOKENS=token-interno-1,token-interno-2`, que coincide con el token que usa el backend por defecto. No necesitas tocarlo.

> El bridge no guarda ninguna credencial LibreLink. El usuario las introduce en la pantalla de login del frontend en cada sesion.

---

## 4. Instalar dependencias del backend y frontend

```powershell
cd glinc-backend
.\mvnw.cmd package -DskipTests
cd ..

cd glinc-frontend
npm install
cd ..
```

El primer `mvnw package` tarda varios minutos (descarga todas las dependencias Maven).

---

## 5. Arrancar los 3 servicios

Desde la raiz del repo:

```powershell
.\start-glinc.ps1
```

- Si tienes Windows Terminal (`wt.exe`) en el PATH → abre 3 tabs.
- Si no → 3 ventanas PowerShell independientes.

| Servicio | Puerto | URL util |
|---|---|---|
| Bridge | 3001 | http://localhost:3001/docs (Swagger) |
| Backend | 8080 | http://localhost:8080/docs (Swagger) y `/actuator/health` |
| Frontend | 8100 | **http://localhost:8100** ← abrir en navegador |

Cuando los 3 estan arrancados (cada uno tarda 20-60 s):
1. Abre `http://localhost:8100`.
2. Inicia sesion con tu cuenta de **LibreLink Up** (correo + password).
3. La primera vez, el backend siembra ~90 dias de glucosa sintetica (`DemoSeeder`) para que la grafica tenga historial al instante. A partir de ahi, el poller cada 90 s va aniadiendo lecturas reales de Abbott.

Para parar todo:

```powershell
.\stop-glinc.ps1
```

(Mata el proceso que escuche en cada puerto.)

---

## 6. Troubleshooting rapido

| Sintoma | Causa | Solucion |
|---|---|---|
| Backend no arranca, log: `password authentication failed for user "postgres"` | Tu Postgres no usa `admin` como password | `$env:DB_PASSWORD = "tu_pwd"` y vuelve a lanzar |
| Backend log: `database "glinc" does not exist` | Olvidaste crear la BD | `psql -U postgres -c "CREATE DATABASE glinc;"` |
| Backend log: `Flyway migration validate failed` | Otra version de JDK | Verifica `java --version` es 21 |
| Frontend muestra "Login failed" con credenciales correctas | El bridge no responde o no tiene tu token | Comprueba que bridge esta en :3001 y `cgm-bridge-service\.env` existe |
| Login bucle sin error | El backend responde 401 | El `bridge.service-token` del backend no coincide con `SERVICE_TOKENS` del bridge — manten los defaults |
| `ionic: command not found` | Falta CLI global | `npm install -g @ionic/cli` |
| Puerto en uso | Algo ya esta escuchando | `.\stop-glinc.ps1` y reintenta |
| La grafica de 3m esta vacia | Aun no has hecho login (no se ha sembrado SEED) | Inicia sesion, luego selecciona 3m |

### Logs detallados de cada servicio

Cada tab/ventana de PowerShell muestra el log de su servicio. El bridge y el backend emiten JSON estructurado: copia/pega una linea en https://jsonlint.com/ si necesitas leerla.

---

## 7. Demo al tribunal

Ver `DEMO.md` para el guion paso a paso del dia D.

---

## 8. Estructura del proyecto

Cada modulo tiene su propio `CLAUDE.md` con el detalle interno:

- `CLAUDE.md` — vision global del ecosistema, decisiones cross-modulo
- `cgm-bridge-service/CLAUDE.md` — bridge LibreLink Up
- `glinc-backend/CLAUDE.md` — backend Spring Boot (auth, persistencia, API)
- `glinc-frontend/CLAUDE.md` — frontend Angular/Ionic (UI, servicios, gráfica)

## 9. Licencia

TFG academico — no licencia comercial. Glinc es un cliente independiente de la API publica de LibreLinkUp; no esta afiliado a Abbott.
