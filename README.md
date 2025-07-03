# Logística de Robots

## Descripción
Sistema de simulación logística con robots autónomos, cofres y robopuertos, visualizado mediante una interfaz gráfica JavaFX. Permite cargar configuraciones de red, simular pedidos y observar el comportamiento de los robots en tiempo real.

## Requisitos previos
- **Java 17** o superior (JDK)
- **Maven 3.6+**
- Sistema operativo: Windows, Linux o MacOS

## Pasos para compilar y ejecutar

1. **Clonar o descargar el repositorio**

2. **Compilar el proyecto con Maven:**
   ```bash
   mvn clean install
   ```

3. **Ejecutar la aplicación JavaFX:**
   ```bash
   mvn javafx:run
   ```
   > Si tienes problemas con el plugin de JavaFX, asegúrate de tener configuradas las variables de entorno JAVA_HOME y PATH correctamente, y que tu JDK incluya JavaFX o esté referenciado en el pom.xml.

4. **Abrir la interfaz gráfica**
   - Al ejecutar el comando anterior, se abrirá la ventana principal de la simulación.

## Cargar una configuración de ejemplo
- En la interfaz, utiliza el botón **"Cargar Configuración"** y selecciona el archivo de ejemplo:
  - `src/main/resources/config/redConectadaCompletaCumplible.json`
- Luego, inicia la simulación con el botón **"Iniciar Simulación"**.

## Estructura de la interfaz
- **FXML principal:** `src/main/resources/views/MainSimulacionView.fxml`
- **Estilos CSS:** `src/main/resources/css/styles.css`

## Notas adicionales
- Puedes pausar, avanzar ciclo a ciclo o resetear la simulación desde la interfaz.
- El sistema muestra visualmente la red logística, los robots, cofres y robopuertos, así como las rutas planificadas y recorridas.
- Para modificar o crear nuevas configuraciones de red, edita o duplica archivos `.json` en `src/main/resources/config/`.

## Ejecución directa (opcional)
Si prefieres ejecutar el JAR generado:

```bash
java -jar target/logisticaRobots-1.0-SNAPSHOT.jar
```

> Asegúrate de que el JAR incluya las dependencias y recursos necesarios (puedes necesitar un plugin como `javafx-maven-plugin` para empaquetar correctamente).

## Instalación y configuración de JavaFX SDK

### 1. Descargar JavaFX SDK

1. Ve a [https://openjfx.io/](https://openjfx.io/)
2. Haz clic en **"Download"**.
3. Selecciona la versión que coincida con tu JDK (por ejemplo, *Version*: 21 si usas JDK 21).
4. Elige tu sistema operativo (Windows, Linux, MacOS) y arquitectura (x64, ARM, etc.).
5. Descarga el archivo tipo **SDK** (por ejemplo: `openjfx-21_windows-x64_bin-sdk.zip`).

### 2. Extraer JavaFX SDK

1. Crea una carpeta para JavaFX, por ejemplo:
   - En Windows: `C:\javafx-sdk-21`
   - En Linux/MacOS: `/opt/javafx-sdk-21` o en tu carpeta de usuario
2. Extrae el contenido del ZIP descargado en esa carpeta.
3. Deberías tener una estructura similar a:

   ```
   javafx-sdk-21/
   ├── bin/
   ├── legal/
   └── lib/
       ├── javafx.base.jar
       ├── javafx.controls.jar
       ├── javafx.fxml.jar
       ├── javafx.graphics.jar
       └── ...
   ```

### 3. Configurar tu IDE para JavaFX

#### **Configuración de VM Options (genérico para cualquier IDE):**

Debes agregar las siguientes opciones de VM al ejecutar tu aplicación JavaFX:

```
--module-path "RUTA_A_JAVAFX/lib" --add-modules javafx.controls,javafx.fxml
```

- Reemplaza `RUTA_A_JAVAFX/lib` por la ruta real donde extrajiste JavaFX SDK.
  - Ejemplo en Windows: `C:\javafx-sdk-21\lib`
  - Ejemplo en Linux/MacOS: `/opt/javafx-sdk-21/lib`

#### **¿Dónde configurar esto?**
- **IntelliJ IDEA:**
  - Ve a *Run → Edit Configurations*.
  - Selecciona tu configuración de ejecución (por ejemplo, MainApplication).
  - En el campo *VM Options*, pega la línea anterior.
  - Haz clic en *Apply* y luego *OK*.
- **Eclipse:**
  - Ve a *Run Configurations*.
  - En *Arguments → VM arguments*, pega la línea anterior.
- **NetBeans:**
  - Ve a *Project Properties → Run*.
  - En *VM Options*, pega la línea anterior.
- **Desde terminal:**
  - Al ejecutar el JAR, agrega las opciones:
    ```bash
    java --module-path "RUTA_A_JAVAFX/lib" --add-modules javafx.controls,javafx.fxml -jar target/logisticaRobots-1.0-SNAPSHOT.jar
    ```

### 4. Verificar y ejecutar

- Asegúrate de que la ruta a la carpeta `lib` de JavaFX es correcta y contiene los archivos JAR.
- Ejecuta tu aplicación desde el IDE o terminal.

### 5. Si tienes problemas:
- **Verifica la ruta:** Asegúrate de que la ruta a `lib` es correcta y contiene los JAR de JavaFX.
- **Prueba ruta alternativa:** Si pusiste JavaFX en otra ubicación, ajusta la ruta en consecuencia.
- **Verifica permisos:** Asegúrate de tener permisos de lectura en la carpeta.
- **Versiones:** La versión de JavaFX debe coincidir con la de tu JDK.

---

**¡Listo! Ya puedes simular y visualizar la logística de robots en tu red personalizada!** 