# Reglas y Guía del Proyecto `logisticaRobots`

Este archivo ayuda a IA (como Cursor) y colaboradores a navegar, entender y modificar el proyecto de forma segura y eficiente.

---

## 1. Estructura del Proyecto

- **`src/main/java/com/alphaone/logisticaRobots/`**: Código fuente principal.
  - `application/`: Servicios de simulación y DTOs (objetos de transferencia de datos).
  - `domain/`: Entidades de negocio, lógica de dominio, pathfinding y comportamientos.
  - `infrastructure/`: Configuración, logging y adaptadores.
  - `shared/`: Parámetros globales y utilidades.
  - `ui/`: Controladores y punto de entrada de la interfaz gráfica (JavaFX).
- **`src/main/resources/`**: Recursos estáticos (configuración, vistas FXML, estilos CSS).
- **`src/test/java/`**: Pruebas unitarias y de integración.
- **`pom.xml`**: Configuración de Maven, dependencias y plugins.

---

## 2. Convenciones de Código

- **Nombres de clases**: PascalCase.
- **Nombres de métodos y variables**: camelCase.
- **Paquetes**: minúsculas, separados por funcionalidad.
- **DTOs**: sufijo `DTO`.
- **Interfaces de servicios**: prefijo `Servicio`.
- **Implementaciones**: sufijo `Impl`.
- **No usar lógica de negocio en controladores de UI.**

---

## 3. Dependencias y Herramientas

- **Jackson**: Serialización/deserialización JSON.
- **JavaFX**: Interfaz gráfica.
- **SLF4J + Logback**: Logging.
- **JUnit + Mockito**: Testing.
- **Maven**: Build y gestión de dependencias.

---

## 4. Puntos de Entrada y Clases Clave

- **Main de la app**: `com.alphaone.logisticaRobots.ui.MainApplication`.
- **Controlador principal UI**: `ui.controllers.MainSimulacionController`.
- **Servicio central**: `application.ServicioSimulacion` y `ServicioSimulacionImpl`.
- **DTOs**: En `application.dto`.
- **Dominio**: Clases en `domain` (robots, cofres, pedidos, pathfinding, etc).
- **Configuración**: `infrastructure.config.LogisticaRobotsConfigLoader` y `resources/config/logisticaRobotsConfig.json`.

---

## 5. Buenas Prácticas Específicas

- Mantener separación entre UI, lógica de negocio y persistencia/configuración.
- Los DTOs no deben contener lógica, solo datos.
- Los servicios deben ser inyectables y desacoplados de la UI.
- Usar logging en vez de `System.out.println`.
- Las pruebas deben ir en `src/test/java` y usar JUnit/Mockito.
- Los recursos (FXML, CSS, JSON) deben estar en `src/main/resources`.

---

## 6. Notas para IA y Colaboradores

- Para buscar lógica de negocio, ir a `domain/` y `application/`.
- Para modificar la UI, usar `ui/` y los archivos FXML en `resources/views/`.
- Para cambiar configuración, editar `resources/config/logisticaRobotsConfig.json` y su loader.
- Para agregar entidades, seguir el patrón de dominio y DTOs.
- Para nuevas reglas, añadirlas aquí para mantener la coherencia.

---

**Actualiza este archivo si cambias la estructura, dependencias o convenciones.** 