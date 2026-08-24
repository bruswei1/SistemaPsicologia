# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

SistemaPsicologia is a Java Swing desktop application (NetBeans/Ant `j2seproject`), built as the
**Sistema de Gestión del Departamento de Psicología** for the **Colegio Nacional E.M.D. San Roque
González de Santacruz** (students, appointments, psychological attention notes). Implemented so
far: login, role-based access, student records, appointment agenda, SOAP-based attention notes,
file attachments, a dashboard, and an official academic-structure (curso/sección/turno) selector.

**Terminology note (important, read before touching UI strings):** the rebrand from a generic
"paciente" (patient) system to this school's terminology was done **at the UI text layer only**.
Every screen a user sees says "Estudiante" (not "Paciente"), "Atención"/"Atenciones" (not
"Sesión"/"Sesiones"), "Cita" (not "Turno" — "Turno" is reserved for the Mañana/Tarde shift of the
academic structure), and role labels via `Tema.etiquetaRol()` ("Profesional de Psicología",
"Usuario autorizado", "Administrador/a"). **Java class/package names, DB table/column names, and
role values stored in the DB were deliberately left unchanged** (`Paciente`, `PacienteDAO`, table
`pacientes`, `sesiones`, `turnos`, roles `admin`/`psicologo`/`secretaria`, etc.) to avoid touching
JDBC/FK-sensitive code. Don't get confused seeing "Paciente"/"Sesión"/"Turno" in the source — that's
expected; only add new UI-facing strings using the school terminology, never the old clinic terms.

- Java target: 1.8 (`javac.source`/`javac.target=1.8` in `nbproject/project.properties`)
- UI: Java Swing with [FlatLaf](https://www.formdev.com/flatlaf/) (`FlatLightLaf`/`FlatDarkLaf`, toggle in `Configuracion`) as the look and feel, installed via `Vista.Tema.instalarLookAndFeelGuardado()` in every screen's `main()`. **There are no NetBeans `.form` files in this project** — every `Vista` screen, including `MenuPrincipal`, is hand-coded (`GridBagLayout`/`BorderLayout`), not editable in the GUI Form Editor.
- Persistence: raw JDBC against MySQL (no ORM, no connection pool). The single `Conexion.conectar()` class lives at `src/Conexion/Conexion.java` but declares `package conexion;` (lowercase) — the source folder name and the Java package name don't match case, which works because Windows filesystems are case-insensitive; always write `import conexion.Conexion;` / `new conexion.Conexion()`, not `Conexion.Conexion`.
- Persistence layer: `DAO`/`Modelos` (lowercase packages `dao`/`modelos`) hold the more recently added screens' data access (`PacienteDAO`, `SesionDAO`, `TurnoDAO`, `UsuarioDAO`, `HistoriaPsicologicaDAO`, `AuditoriaDAO`); several `Vista` screens (`Agenda`, `Sesiones`) instead run their own inline JDBC/SQL rather than going through a DAO — check the screen itself before assuming a DAO method exists for what you need.
- Entry point: `Vista.Login` (`main.class=Vista.Login` in `nbproject/project.properties`)

## Build, run, test

This is a NetBeans Ant project. There is no Maven/Gradle wrapper.

- Build: `ant -f build.xml` (or open in NetBeans and use Build/Run — the Ant script `build.xml` just imports the NetBeans-generated `nbproject/build-impl.xml`)
- Run: `ant run` (runs `Vista.Login`, the configured main class), or run/debug directly from NetBeans
- No JDK with `javac` is guaranteed to be on PATH in this environment — only a JRE (`java`) may be present. Check before assuming you can compile from the shell; NetBeans itself has its own bundled JDK reference (`platform.active=default_platform`).
- No test framework/test sources exist yet (`test.src.dir=test` is configured but empty).

### Dependencies

Two external jars, both wired via `file.reference.*` properties + `javac.classpath` in `nbproject/project.properties` — there's no Maven/Gradle dependency manager:
- MySQL JDBC driver, referenced by an **absolute local path**: `file.reference.mysql-connector-j-9.7.0.jar=C:\Users\bru\Downloads\mysql-connector-j-9.7.0\...\mysql-connector-j-9.7.0.jar`. This means the project will not build on another machine without that exact jar at that exact path (or the property updated).
- FlatLaf, checked into the repo at `lib/flatlaf-3.5.4.jar` and referenced with a **project-relative path** (`file.reference.flatlaf-3.5.4.jar=lib/flatlaf-3.5.4.jar`) — this one *does* build on another machine out of the box. Prefer this `lib/` + relative-path pattern for any future dependency instead of copying the MySQL driver's absolute-path approach; flag the portability issue only if you have to touch the MySQL reference itself.

## Database

MySQL database name: `psicologia`. Connection is hardcoded in `src/Conexion/Conexion.java`:
```
jdbc:mysql://localhost:3306/psicologia, user "root", empty password
```
There is no config file for DB credentials — changing host/user/password means editing `Conexion.java` directly.

### Local MySQL via Docker

`docker-compose.yml` at the project root runs MySQL 8.0 for local development, matching `Conexion.java` exactly (db `psicologia`, root, no password), so no code changes are needed to use it:
```
docker compose up -d
```
- Port 3306 is exposed on the host — **this conflicts with any locally-running MySQL** (this project's dev machine has XAMPP, whose `mysqld` binds 3306; stop it first: XAMPP control panel, or find/kill the process bound to port 3306).
- `db-init/*.sql` files are auto-executed by the MySQL entrypoint **only on first container/volume creation**. If the container/volume already exists, apply new SQL manually, e.g.:
  ```
  docker exec -i psicologia_mysql mysql -uroot psicologia < path/to/script.sql
  ```
- Current schema: `db-init/01_schema.sql` creates `usuarios` (`id, usuario, password_hash, salt, nombre, rol, activo, creado_en`); `db-init/02_pacientes.sql` creates `pacientes` (`id, nombre, apellido, fecha_nacimiento, genero, telefono, email, direccion, motivo_consulta, psicologo_id, antecedentes_personales, antecedentes_familiares, anamnesis, creado_en`); `db-init/03_fase1_roles_historia_auditoria.sql` adds `usuarios.rol`, the `pacientes` clinical/ownership columns, and creates `auditoria` (`id, usuario_id, usuario_nombre, accion, entidad, entidad_id, detalle, fecha`); `db-init/04_fase2_agenda_sesiones_documentos.sql` creates `turnos` (appointments), `sesiones` (SOAP notes), and `documentos_paciente` (file attachment metadata); `db-init/05_historia_psicologica.sql` creates `historia_psicologica` (extended clinical history, one row per `pacientes.id`); `db-init/06_estructura_academica.sql` adds `pacientes.curso` (nullable `VARCHAR(100)`) — the single canonical label produced by `util.EstructuraAcademica`/`Vista.SelectorCursoSeccion`, see Architecture below.

### Auth model

Passwords are **not** stored in plaintext. `usuarios.password_hash` is `SHA-256(salt + password)` (hex), with a random 16-byte `salt` (hex) per user, implemented in `src/util/PasswordUtil.java` (`generarSalt`, `hash`, `verificar` — the latter is constant-time). There is no bcrypt/scrypt lib in the classpath by design (keeps the single-jar dependency minimal) — if stronger hashing is ever needed, that's a deliberate tradeoff to revisit, not an oversight.

To create a user manually (no registration UI yet), generate a salt+hash pair consistent with `PasswordUtil` (16 random bytes → hex salt; `SHA-256(saltHex + password)` → hex hash) and `INSERT INTO usuarios (usuario, password_hash, salt, nombre, rol) VALUES (...)`. `rol` is one of `admin`, `psicologo`, `secretaria`.

### Roles and session

`src/util/Sesion.java` is a static/global holder for the logged-in user (`usuarioId`, `usuario`, `nombre`, `rol`) plus `esAdmin()`/`esPsicologo()`/`esSecretaria()` helpers. `Vista.Login` populates it via `Sesion.iniciar(...)` right after a successful auth check and before opening `MenuPrincipal` — every other screen reads from `Sesion` statically rather than having the user passed through constructors. It is **process-global, not thread-local**: fine for this single-window desktop app, but don't assume it works if the app ever goes multi-session.

Role behavior implemented so far (in `GestionPacientes` and other screens querying `pacientes`):
- `psicologo` (shown to the user as "Profesional de Psicología", see `Tema.etiquetaRol`) only sees/queries students where `pacientes.psicologo_id = Sesion.getUsuarioId()`; the assigned-professional combo is locked to themselves.
- `secretaria` (shown as "Usuario autorizado") is blocked from `Sesiones` entirely (menu button disabled in `MenuPrincipal`, SOAP-based attention notes are clinical/confidential).
- `admin` (shown as "Administrador/a") has full access, including reassigning `psicologo_id` on any student and managing user accounts in `Configuracion`.

### Auditoria

`src/util/Auditoria.java` (`Auditoria.registrar(Connection, accion, entidad, entidadId, detalle)`) inserts one row per action into `auditoria`, reading the actor from `Sesion`. Call it inside the same try-with-resources `Connection` as the operation it's logging (see `Login.autenticar` for `LOGIN`; `GestionPacientes` for `CREAR_PACIENTE`/`EDITAR_PACIENTE`/`VER_PACIENTE`/`ELIMINAR_PACIENTE`; `Agenda` for `CREAR_TURNO`/`EDITAR_TURNO`/`ELIMINAR_TURNO`/`COMPLETAR_TURNO`; `Sesiones` for `CREAR_SESION`/`EDITAR_SESION`/`VER_SESION`; `HistoriaPsicologicaView` for `VER_HISTORIA`/`EDITAR_HISTORIA`; `Configuracion` for account-management actions). The `accion` codes stay in Spanish/snake_case matching the internal entity names (`PACIENTE`, `SESION`, `TURNO`) even though the UI now says "Estudiante"/"Atención"/"Cita" — they're technical log identifiers, not user-facing copy, and `Configuracion`'s "Actividad reciente" viewer shows them raw. This satisfies Ley 6534/20-style "who accessed which record when" tracking — extend it by calling `Auditoria.registrar` from any new screen that reads or writes student data, using the same `entidad`/`entidad_id` convention (table name, row id).

### Student file attachments

`documentos_paciente` (schema only, see `db-init/04_fase2_agenda_sesiones_documentos.sql`) is designed to store attachment metadata in an `adjuntos/<paciente_id>/<timestamp>_<filename>` layout via `java.nio.file.Files.copy`, opened with `Desktop.getDesktop().open(...)`; files are **not** stored as DB blobs. As of the current `Vista` screens (`GestionPacientes`, `DetallePaciente`), there is no attachments UI wired up yet — `Configuracion` only exposes an "Abrir carpeta de adjuntos" shortcut. The `adjuntos/` folder is git-ignored (it's local runtime data, not source) — don't assume it exists on a fresh checkout.

## Architecture

Five packages under `src/`:
- `Conexion` (folder) / `conexion` (package) — single class `Conexion.conectar()` opens a new `java.sql.Connection` per call (no pooling/singleton). Callers are responsible for closing it (use try-with-resources).
- `Modelos` (folder) / `modelos` (package) — plain data classes: `Paciente`, `Usuario`, `Sesion` (model, not to be confused with `util.Sesion`), `Turno`, `HistoriaPsicologica`.
- `DAO` (folder) / `dao` (package) — JDBC access for the newer screens: `PacienteDAO`, `SesionDAO`, `TurnoDAO`, `UsuarioDAO`, `HistoriaPsicologicaDAO`, `AuditoriaDAO`, and the shared `DAO` base class (connection/logging helpers). Not every screen goes through a DAO — `Agenda` and `Sesiones` run their own inline JDBC instead (see below).
- `util` — cross-cutting helpers with no UI dependency: `PasswordUtil` (hashing), `Sesion` (current-user holder), `Auditoria` (access log writer), `ControlPermiso` and `Validador` (exist but are **not currently called** from any `Vista` screen — don't assume their rules are enforced; e.g. `Validador`'s password-length rule differs from what `Configuracion` actually enforces inline), `GeneradorReportes` (plain-text `.txt`/`.csv` report/export generation — despite the method name `guardarReportePDF`, it does **not** produce PDF), `EstructuraAcademica` (the closed, official catalog of curso/sección/turno for the school — grados 7°/8°/9° EEB and Bachillerato Técnico/Científico with their specific turno rules; this is the single source of truth for academic structure, extend it only if the school's actual course offering changes, never by guessing/adding plausible-looking options).
- `Vista` — Swing panels/frames. **There are no NetBeans `.form` files in this project** — every screen, including `MenuPrincipal`, is hand-coded with `GridBagLayout`/`BorderLayout`, not editable in the GUI Form Editor.
  - `Tema` — centralized palette/typography/spacing/icon-drawing helpers; every screen should use `Tema.*` instead of declaring its own `new Color(...)`/`new Font(...)`. Also holds the dark-mode toggle (persisted to `tema.properties`) and the role/estado display-label translation (`Tema.etiquetaRol`, `Tema.rendererRol`, `Tema.rendererRolCombo`, `Tema.etiquetaEstadoCita`, `Tema.rendererEstado`) — these translate the raw DB values (`admin`/`psicologo`/`secretaria`, `programado`/`completado`/`cancelado`/`ausente`) to the school-facing labels ("Profesional de Psicología", "Programada", etc.) **for display only**; the underlying value saved to/read from the DB is never translated. `PRIMARIO`/`SECUNDARIO` are the violet/blue pair taken from the Department's real banner (photo supplied by the user) — every screen's top banner is built with `Tema.panelEncabezado(titulo, subtitulo)` (a `SECUNDARIO`→`PRIMARIO` gradient bar) and every section border with `Tema.tituloSeccion(texto)` (a brand-colored `TitledBorder`) instead of calling `BorderFactory.createTitledBorder(...)` directly — keep using those two instead of hand-rolling a header or a plain titled border on any new screen.
  - `Login` — authenticates against `usuarios` via `PasswordUtil.verificar`, populates `Sesion`, logs `LOGIN` to `auditoria`, then opens `MenuPrincipal` and disposes itself. Installs the saved look-and-feel via `Tema.instalarLookAndFeelGuardado()` — the only place that happens for the real app flow.
  - `MenuPrincipal` — main menu shell (a single `JFrame` with a `CardLayout` swapping panels, not one `JFrame` per screen); title bar and header show the logged-in user/role. "Estudiantes" opens `GestionPacientes`, "Atenciones" opens `Sesiones` (disabled for role `secretaria`), "Historia" opens `HistoriaPsicologicaView`, "Panel General" opens `Dashboard`, "Agenda" opens `Agenda`, "Configuración" opens `Configuracion`.
  - `GestionPacientes` — student list/search/CRUD screen: a role-filtered table of `pacientes` plus a registration form (basic data + `SelectorCursoSeccion` for curso/sección/turno) and a curso filter combo. Selecting a table row or double-clicking opens `DetallePaciente`; `guardarPaciente` does an `INSERT` or `UPDATE` depending on whether a student is currently loaded (`pacienteIdActual`).
  - `DetallePaciente` — read-only student detail window (`JTabbedPane`: Información Personal / Seguimiento del Estudiante / Atenciones), opened from `GestionPacientes`.
  - `HistoriaPsicologicaView` — extended clinical history (`historia_psicologica`, one row per student) with antecedentes/motivo/observaciones/diagnóstico/plan de intervención, an audit-backed "Historial de cambios" (who/when, not text diffs), and a plain-text report export via `GeneradorReportes`.
  - `SelectorCursoSeccion` — reusable `JPanel` with dependent combo boxes (Nivel → Grado+Sección or Año+Modalidad+Especialidad → Turno) backed entirely by `util.EstructuraAcademica`; it is structurally impossible to select a curso/turno combination that isn't in that catalog. Persists as a single canonical label string in `pacientes.curso` (e.g. `"9° EEB - Sección B - Turno Mañana"`); `EstructuraAcademica.parsearEtiqueta`/`construirEtiqueta*` convert between that string and the selector's UI state.
  - `Agenda` — appointment scheduling (`turnos`, shown to the user as "Citas"): create/edit with estudiante, profesional, date/time (`dd/MM/yyyy HH:mm`), duration, estado (`programado`/`completado`/`cancelado`/`ausente`, raw DB values — combo items are not translated, only read-only display is via `Tema.etiquetaEstadoCita`/`rendererEstado`), overlap warning, and a monthly calendar view. Filtered to the logged-in psicólogo's own students/citas. Runs its own inline JDBC (`PacienteItem`/`PsicologoItem` inner classes), not `PacienteDAO`/`TurnoDAO`.
  - `Sesiones` — SOAP-based attention notes (`sesiones`, shown to the user as "Atenciones") per student: the four SOAP fields are re-labeled for the school context ("Relato del estudiante (Subjetivo)", "Observación profesional (Objetivo)", "Análisis de la situación", "Acuerdos y recomendaciones (Plan)") plus `notas_privadas` ("Seguimiento y notas privadas"), and a simple start/stop cronómetro (`javax.swing.Timer`) that records `duracion_minutos`. Saving a note linked to a `programado` cita auto-marks it `completado`. Not reachable by `secretaria`. Also runs its own inline JDBC, independent of `SesionDAO`.
  - `Dashboard` — aggregate counts only (never raw clinical text): estudiantes/atenciones/citas/profesionales tiles, a comparison bar chart, a 6-month new-students trend, and an "Estudiantes por curso" table (`PacienteDAO.contarPorCurso`).
  - `Configuracion` — own-account settings (name, password) for everyone; for `admin` only, adds user management (create/reset-password/activate-deactivate/delete/change-role — role combos use `Tema.rendererRolCombo` for display but submit the raw role value) and an "Actividad reciente" `auditoria` viewer (text/date-range filter, pagination via "Cargar más antiguos"). Also has dark-mode toggle and CSV/report export shortcuts.

When adding new screens, follow the existing pattern: JDBC access via `new conexion.Conexion().conectar()` wrapped in try-with-resources, `PreparedStatement` for all queries (no string-concatenated SQL — the codebase is consistent about this), read the actor from `Sesion` rather than re-querying `usuarios`, log mutations/views to `auditoria` via `Auditoria.registrar`, use `Tema.*` for styling instead of ad-hoc colors/fonts, and use the school terminology ("Estudiante", "Atención", "Cita", role labels via `Tema.etiquetaRol`) in every new user-facing string — never "Paciente"/"Sesión"/"Turno"/"Doctor"/"Clínica" in anything the user reads.
