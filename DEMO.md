# DEMO.md — guion para defender Glinc al tribunal

> **Antes de la defensa, ensaya el flujo completo en la maquina destino. Si todo arranca a la primera, la mitad del estres desaparece.**

## 0. La noche anterior

- [ ] Ordenador conectado a corriente y con bateria al 100%.
- [ ] WiFi del aula verificado (es OBLIGATORIO: hay que hablar con la API de Abbott).
- [ ] Si la sala tiene proxy/firewall, prueba `https://api-eu.libreview.io` desde el navegador.
- [ ] **Haz el primer login en frio**: arranca todo y entra al menos una vez para que el `DemoSeeder` siembre los 90 dias sinteticos. Asi el dia D los datos ya estan en BD y el primer click en "3m" pinta inmediatamente.
- [ ] Apaga notificaciones del sistema (Slack, Teams, email, antivirus...).
- [ ] Cierra todo excepto el navegador y los 3 terminales del proyecto.

## 1. Justo antes de empezar

```powershell
cd C:\ruta\al\proyecto\Glinc
.\start-glinc.ps1
```

Espera a ver en cada tab:
- **Bridge** — `cgm-bridge listening on :3001`
- **Backend** — `Started GlincBackendApplication in X seconds`
- **Frontend** — `Local: http://localhost:8100`

Abre `http://localhost:8100` en el navegador y deja la pantalla de login lista.

## 2. Guion sugerido (15 min)

### Login (1 min)
- Muestra la pantalla de login. Explica: "el usuario introduce sus credenciales de LibreLink Up; las credenciales solo viajan al bridge durante el login, nunca se persisten en BD".
- Inicia sesion con tu cuenta LibreLink.

### Dashboard (3 min)
- Sidebar: tarjetas de pacientes con su ultima lectura.
- Badge de alertas: explica los umbrales bajo/alto.
- Filtro Todos/Alto/Bajo y buscador del topbar.
- "Esta lista se actualiza sola cada 90 segundos sin recargar la pagina — esa es la misma cadencia con la que el backend escala lecturas reales del bridge".

### Detalle de paciente (5 min)
- Hero con lectura actual + barra Tiempo en Rango.
- Grafica ApexCharts: cambia entre 12h, 1d, 7d, 14d, 30d y **3m**.
- Explica HbA1c estimada (formula ADAG). El 3m da el calculo mas fiel porque cubre el rango clinico real de 2-3 meses.
- Eje Y: si una lectura baja del suelo o supera el techo, el eje se adapta automaticamente y los ticks siempre caen en numeros redondos (40, 80, 120, 160...).
- Inventario inline-editable (4 tipos fijos) → cambia una cantidad/estado y muestra que persiste al instante en BD.
- Citas medicas → crea una cita rapida (modal CRUD).

### Configuracion (2 min)
- Tab Cuenta: edita el perfil (campo nombre/apellido) — explica que persiste en la tabla `users` del backend.
- Tab Mediciones: cambia unidades mg/dL ↔ mmol/L. Muestra que la grafica y todas las lecturas se actualizan al instante (las preferencias viven en localStorage, los datos siempre en mg/dL).
- Tab Aplicacion: modo oscuro toggle (cambia toda la app al instante).

### Seccion Ayuda (1-2 min)
- Explica brevemente los 7 tabs: glucosa, rango, tir, a1c, tendencias, actualizacion, glosario.
- "Esta seccion es 100% estatica — no consulta al backend, solo lo usa el tribunal o un usuario nuevo para entender lo que esta viendo".

### Arquitectura (2-3 min)
- Abre `CLAUDE.md` (raiz) o pinta un diagrama:
  ```
  Frontend → Backend → Bridge → LibreLink Up (Abbott)
  ```
- Razones por las que NO se llama al bridge desde el frontend.
- Razones por las que las credenciales LibreLink solo viajan al backend en el login.
- Tabla `users` con PK = email (sin tabla de credenciales).

## 3. Que decir si algo falla

| Sintoma en el momento | Reaccion |
|---|---|
| El login devuelve "Login failed" | Verifica WiFi. Como ultimo recurso, di que la demo continua con los datos sinteticos sembrados ayer (la BD ya los tiene). |
| Grafica vacia tras login | El `DemoSeeder` no llegó a ejecutarse o el paciente no esta vinculado. Cambia de paciente. |
| Frontend pantalla blanca | F12 → Consola: probablemente 401. Cierra sesion (Cerrar sesion del menu usuario) y vuelve a entrar. |
| Backend cae | Mira el terminal del backend, copia la excepcion. Reinicia solo el backend: en su tab, Ctrl+C y `.\mvnw.cmd spring-boot:run`. |
| Postgres pide password | Has reiniciado la maquina y la sesion de PS perdio las env vars. Vuelve a exportarlas (DB_USER, DB_PASSWORD). |
| Polling no actualiza | El backend GlucosePoller fallo. Refresca la pagina (F5) — el frontend hara una peticion nueva y mostrara la ultima lectura en BD. |

## 4. Preguntas frecuentes del tribunal

> "¿Por que no Spring Security?"
- Es un TFG de 2DAM. Filtro manual con `OncePerRequestFilter` + UUID en memoria es defendible al nivel del curso. Spring Security/JWT habria anadido complejidad innecesaria.

> "¿Por que tres servicios?"
- Aislamiento de responsabilidades: el bridge habla con la API de Abbott (cambiante, externa), el backend mantiene el estado y la logica, el frontend solo pinta. Si Abbott cambia su API, solo se toca el bridge.

> "¿Que pasaria si LibreLink Up cae?"
- El bridge entra en estado `backoff` con retry exponencial. El backend sigue sirviendo del histórico de BD. El frontend muestra la ultima lectura conocida (con su timestamp para que el cuidador vea que esta caduca).

> "¿Hay tests?"
- No. Decision consciente para concentrar tiempo en funcionalidad y UI. Los `*.spec.ts` del frontend estan obsoletos (no actualizados al nuevo template). Punto pendiente reconocido en el TFG.

> "¿Y produccion?"
- Roadmap documentado en CLAUDE.md raiz: bridge en red privada, mTLS o OAuth2 Client Credentials entre backend y bridge, credenciales LibreLink cifradas en BD con KMS, rotacion de SERVICE_TOKENS, sesiones en Redis en vez de en memoria.

## 5. Despues de la defensa

- `.\stop-glinc.ps1`
- Borra la BD si la maquina no es tuya: `psql -U postgres -c "DROP DATABASE glinc;"`
- Borra `.env` del bridge (lleva el SERVICE_TOKEN).
