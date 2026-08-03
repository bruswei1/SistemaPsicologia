# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

SistemaPsicologia is a Java Swing desktop application (NetBeans/Ant `j2seproject`) for managing a psychology practice (patients, appointments, sessions). It is an early-stage/student project: login, main menu, and patient registration exist; `MenuPrincipal`'s Citas/Sesiones buttons are not yet wired to actions.

- Java target: 1.8 (`javac.source`/`javac.target=1.8` in `nbproject/project.properties`)
- UI: Java Swing, forms built with the NetBeans GUI Builder (`.form` files paired with `.java` files under `src/Vista`)
- Persistence: raw JDBC against MySQL (no ORM, no connection pool)
- Entry point: `Vista.Login` (`main.class=Vista.Login` in `nbproject/project.properties`)

## Build, run, test

This is a NetBeans Ant project. There is no Maven/Gradle wrapper.

- Build: `ant -f build.xml` (or open in NetBeans and use Build/Run — the Ant script `build.xml` just imports the NetBeans-generated `nbproject/build-impl.xml`)
- Run: `ant run` (runs `Vista.Login`, the configured main class), or run/debug directly from NetBeans
- No JDK with `javac` is guaranteed to be on PATH in this environment — only a JRE (`java`) may be present. Check before assuming you can compile from the shell; NetBeans itself has its own bundled JDK reference (`platform.active=default_platform`).
- No test framework/test sources exist yet (`test.src.dir=test` is configured but empty).

### Dependencies

The only external dependency is the MySQL JDBC driver, referenced by an **absolute local path** in `nbproject/project.properties`:
```
file.reference.mysql-connector-j-9.7.0.jar=C:\Users\bru\Downloads\mysql-connector-j-9.7.0\...\mysql-connector-j-9.7.0.jar
```
This means the project will not build on another machine without that jar present at that exact path (or the property updated) — there's no `lib/` folder or dependency manager. If you add another dependency, follow the same pattern (add a `file.reference.*` property and add it to `javac.classpath`), but flag the portability issue to the user if relevant.

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
- Current schema: `db-init/01_schema.sql` creates `usuarios` (`id, usuario, password_hash, salt, nombre, rol, activo, creado_en`); `db-init/02_pacientes.sql` creates `pacientes` (`id, nombre, apellido, fecha_nacimiento, genero, telefono, email, direccion, motivo_consulta, psicologo_id, antecedentes_personales, antecedentes_familiares, anamnesis, creado_en`); `db-init/03_fase1_roles_historia_auditoria.sql` adds `usuarios.rol`, the `pacientes` clinical/ownership columns, and creates `auditoria` (`id, usuario_id, usuario_nombre, accion, entidad, entidad_id, detalle, fecha`).

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

`src/util/Auditoria.java` (`Auditoria.registrar(Connection, accion, entidad, entidadId, detalle)`) inserts one row per action into `auditoria`, reading the actor from `Sesion`. Call it inside the same try-with-resources `Connection` as the operation it's logging (see `Login.jButton1ActionPerformed` for `LOGIN`, `RegistroPaciente.guardarPaciente`/`cargarPaciente` for `CREAR_PACIENTE`/`EDITAR_PACIENTE`/`VER_PACIENTE`). This satisfies Ley 6534/20-style "who accessed which record when" tracking — extend it by calling `Auditoria.registrar` from any new screen that reads or writes patient data, using the same `entidad`/`entidad_id` convention (table name, row id).

## Architecture

Four packages under `src/`:
- `Conexion` — single class `Conexion.conectar()` opens a new `java.sql.Connection` per call (no pooling/singleton). Callers are responsible for closing it (use try-with-resources — `Vista.Login` does this).
- `util` — cross-cutting helpers with no UI dependency: `PasswordUtil` (hashing), `Sesion` (current-user holder), `Auditoria` (access log writer).
- `Vista` — Swing JFrames. `Login` and `MenuPrincipal` are generated/maintained by the NetBeans Form Editor (paired `.form` files); **the region between `//GEN-BEGIN:initComponents` and `//GEN-END:initComponents` (and the `variables` block) is regenerated by the Form Editor and should not be hand-edited** — put custom logic in constructors (after the `initComponents()` call) and in the `*ActionPerformed` method bodies, which are safe to edit. When adding an `Events` handler to a NetBeans-managed component, update both the `.form` XML (`<Events>` block) and the `.java` `initComponents()` registration, or the GUI Builder will silently drop your listener next time the form is saved.
  - `Login` — authenticates against `usuarios` via `PasswordUtil.verificar`, populates `Sesion`, logs `LOGIN` to `auditoria`, then opens `MenuPrincipal` and disposes itself.
  - `MenuPrincipal` — main menu shell; the "Pacientes" button opens `RegistroPaciente`. Citas/Sesiones are not yet implemented.
  - `RegistroPaciente` — patient history screen: a `JTabbedPane` (Datos personales / Anamnesis y antecedentes) over a form, plus a live, role-filtered table of `pacientes`. Selecting a table row loads that patient back into the form for editing (`cargarPaciente`, logs `VER_PACIENTE`); `guardarPaciente` does an `INSERT` or `UPDATE` depending on whether a patient is currently loaded (`pacienteIdActual`). Unlike `Login`/`MenuPrincipal`, this one is **hand-coded with `GridBagLayout`, with no paired `.form` file** — it isn't editable in the NetBeans GUI Form Editor. Follow this same plain-code pattern for further CRUD screens unless you specifically want GUI Builder support (which requires authoring a matching `.form` XML).
- `Principal` — a standalone `main()` (`Principal.main`) that just tests the DB connection; not the app's real entry point (that's `Vista.Login`).

When adding new screens, follow the existing pattern: JDBC access via `new Conexion().conectar()` wrapped in try-with-resources, `PreparedStatement` for all queries (no string-concatenated SQL — the codebase is consistent about this), read the actor from `Sesion` rather than re-querying `usuarios`, and log mutations/views to `auditoria` via `Auditoria.registrar`.
