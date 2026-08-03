# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

SistemaPsicologia is a Java Swing desktop application (NetBeans/Ant `j2seproject`) for managing a psychology practice (patients, appointments, sessions). Implemented so far: login, role-based access, patient clinical history, appointment agenda, SOAP session notes, and patient file attachments.

- Java target: 1.8 (`javac.source`/`javac.target=1.8` in `nbproject/project.properties`)
- UI: Java Swing with [FlatLaf](https://www.formdev.com/flatlaf/) (`FlatLightLaf`) as the look and feel, set once in `main()`. Only `MenuPrincipal` is still built with the NetBeans GUI Builder (paired `.form` file); every other `Vista` screen, including `Login`, is hand-coded (`GridBagLayout`/`BorderLayout`, no `.form` file) — see Architecture below.
- Persistence: raw JDBC against MySQL (no ORM, no connection pool)
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
- Current schema: `db-init/01_schema.sql` creates `usuarios` (`id, usuario, password_hash, salt, nombre, rol, activo, creado_en`); `db-init/02_pacientes.sql` creates `pacientes` (`id, nombre, apellido, fecha_nacimiento, genero, telefono, email, direccion, motivo_consulta, psicologo_id, antecedentes_personales, antecedentes_familiares, anamnesis, creado_en`); `db-init/03_fase1_roles_historia_auditoria.sql` adds `usuarios.rol`, the `pacientes` clinical/ownership columns, and creates `auditoria` (`id, usuario_id, usuario_nombre, accion, entidad, entidad_id, detalle, fecha`); `db-init/04_fase2_agenda_sesiones_documentos.sql` creates `turnos` (appointments), `sesiones` (SOAP notes), and `documentos_paciente` (file attachment metadata).

### Auth model

Passwords are **not** stored in plaintext. `usuarios.password_hash` is `SHA-256(salt + password)` (hex), with a random 16-byte `salt` (hex) per user, implemented in `src/util/PasswordUtil.java` (`generarSalt`, `hash`, `verificar` — the latter is constant-time). There is no bcrypt/scrypt lib in the classpath by design (keeps the single-jar dependency minimal) — if stronger hashing is ever needed, that's a deliberate tradeoff to revisit, not an oversight.

To create a user manually (no registration UI yet), generate a salt+hash pair consistent with `PasswordUtil` (16 random bytes → hex salt; `SHA-256(saltHex + password)` → hex hash) and `INSERT INTO usuarios (usuario, password_hash, salt, nombre, rol) VALUES (...)`. `rol` is one of `admin`, `psicologo`, `secretaria`.

### Roles and session

`src/util/Sesion.java` is a static/global holder for the logged-in user (`usuarioId`, `usuario`, `nombre`, `rol`) plus `esAdmin()`/`esPsicologo()`/`esSecretaria()` helpers. `Vista.Login` populates it via `Sesion.iniciar(...)` right after a successful auth check and before opening `MenuPrincipal` — every other screen reads from `Sesion` statically rather than having the user passed through constructors. It is **process-global, not thread-local**: fine for this single-window desktop app, but don't assume it works if the app ever goes multi-session.

Role behavior implemented so far (in `RegistroPaciente`):
- `psicologo` only sees/queries patients where `pacientes.psicologo_id = Sesion.getUsuarioId()`; the "psicólogo asignado" combo is locked to themselves.
- `secretaria` gets the "Anamnesis y antecedentes" tab omitted entirely from the `JTabbedPane` — but the underlying `JTextArea` fields still exist and still get populated/saved, so editing a patient as secretaria round-trips the clinical fields unchanged instead of blanking them. If you add more secretaria-hidden fields, follow this same "keep the field, hide the tab" pattern rather than conditionally omitting the field, or edits will silently wipe data.
- `admin` has full access, including reassigning `psicologo_id` on any patient.

### Auditoria

`src/util/Auditoria.java` (`Auditoria.registrar(Connection, accion, entidad, entidadId, detalle)`) inserts one row per action into `auditoria`, reading the actor from `Sesion`. Call it inside the same try-with-resources `Connection` as the operation it's logging (see `Login.autenticar` for `LOGIN`; `RegistroPaciente` for `CREAR_PACIENTE`/`EDITAR_PACIENTE`/`VER_PACIENTE`/`ADJUNTAR_DOCUMENTO`/`ELIMINAR_DOCUMENTO`; `Agenda` for `CREAR_TURNO`/`EDITAR_TURNO`; `Sesiones` for `CREAR_SESION`/`EDITAR_SESION`/`VER_SESION`). This satisfies Ley 6534/20-style "who accessed which record when" tracking — extend it by calling `Auditoria.registrar` from any new screen that reads or writes patient data, using the same `entidad`/`entidad_id` convention (table name, row id).

### Patient file attachments

`RegistroPaciente`'s "Documentos" tab copies user-selected files into `adjuntos/<paciente_id>/<timestamp>_<filename>` (relative to the working directory the app is run from) via `java.nio.file.Files.copy`; `documentos_paciente.ruta_archivo` stores that relative path, and "Abrir" opens it with `Desktop.getDesktop().open(...)`. Files are **not** stored as DB blobs. The `adjuntos/` folder is git-ignored (it's local runtime data, not source) — don't assume it exists on a fresh checkout; it's created on first attachment (`File.mkdirs()`).

## Architecture

Four packages under `src/`:
- `Conexion` — single class `Conexion.conectar()` opens a new `java.sql.Connection` per call (no pooling/singleton). Callers are responsible for closing it (use try-with-resources — `Vista.Login` does this).
- `util` — cross-cutting helpers with no UI dependency: `PasswordUtil` (hashing), `Sesion` (current-user holder), `Auditoria` (access log writer).
- `Vista` — Swing JFrames. **Only `MenuPrincipal` is still generated/maintained by the NetBeans Form Editor** (paired `MenuPrincipal.form`); **the region between `//GEN-BEGIN:initComponents` and `//GEN-END:initComponents` (and the `variables` block) is regenerated by the Form Editor and should not be hand-edited** there — put custom logic in the constructor (after `initComponents()`) and in `*ActionPerformed` method bodies. When adding an `Events` handler to a NetBeans-managed component, update both the `.form` XML (`<Events>` block) and the `.java` `initComponents()` registration, or the GUI Builder will silently drop your listener next time the form is saved. Every other screen (`Login`, `RegistroPaciente`, `Agenda`, `Sesiones`) is **hand-coded with `GridBagLayout`/`BorderLayout`, with no paired `.form` file** — not editable in the GUI Form Editor. Follow this plain-code pattern for further screens unless you specifically want GUI Builder support (which requires authoring a matching `.form` XML from scratch).
  - `Login` — authenticates against `usuarios` via `PasswordUtil.verificar`, populates `Sesion`, logs `LOGIN` to `auditoria`, then opens `MenuPrincipal` and disposes itself. Sets `FlatLightLaf` in `main()` before creating any Swing component — this is the only place the look and feel is installed for the real app flow.
  - `MenuPrincipal` — main menu shell; title bar shows the logged-in user/role. "Pacientes" opens `RegistroPaciente`, "Citas" opens `Agenda`, "Sesiones" opens `Sesiones` (disabled for role `secretaria` — SOAP notes are clinical). The second "Citas" button (`jButton4`) is unwired leftover from the original scaffold.
  - `RegistroPaciente` — patient history screen: a `JTabbedPane` (Datos personales / Anamnesis y antecedentes / Documentos) over a form, plus a live, role-filtered table of `pacientes`. Selecting a table row loads that patient back into the form for editing (`cargarPaciente`, logs `VER_PACIENTE`); `guardarPaciente` does an `INSERT` or `UPDATE` depending on whether a patient is currently loaded (`pacienteIdActual`). The clinical and documents tabs are omitted entirely for `secretaria` (see Roles above).
  - `Agenda` — appointment scheduling (`turnos`): create/edit turnos with paciente, psicólogo, date/time (`yyyy-MM-dd HH:mm`), duration, estado (`programado`/`completado`/`cancelado`/`ausente`); filtered to the logged-in psicólogo's own patients/turnos like `RegistroPaciente`.
  - `Sesiones` — SOAP session notes (`sesiones`) per patient: Subjetivo/Objetivo/Análisis/Plan plus a `notas_privadas` field, and a simple start/stop cronómetro (`javax.swing.Timer`) that records `duracion_minutos`. Not reachable by `secretaria` (menu button disabled).

When adding new screens, follow the existing pattern: JDBC access via `new Conexion().conectar()` wrapped in try-with-resources, `PreparedStatement` for all queries (no string-concatenated SQL — the codebase is consistent about this), read the actor from `Sesion` rather than re-querying `usuarios`, and log mutations/views to `auditoria` via `Auditoria.registrar`.
