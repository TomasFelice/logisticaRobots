package com.alphaone.logisticaRobots.ui.controllers;

import com.alphaone.logisticaRobots.application.ServicioSimulacion;
import com.alphaone.logisticaRobots.application.dto.*;
import com.alphaone.logisticaRobots.ui.ObservadorEstadoSimulacion;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.scene.control.Slider;

import java.io.File;
import java.util.List;

import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.transform.Affine;

public class MainSimulacionController implements ObservadorEstadoSimulacion {

    // --- Componentes FXML ---
    @FXML
    private Canvas canvasGrilla;
    @FXML
    private Button botonIniciar;
    @FXML
    private Button botonPausar;
    @FXML
    private Button botonResetear;
    @FXML
    private Button botonAvanzarCiclo;
    @FXML
    private Button botonCargarConfig;
    @FXML
    private Label labelCicloActual;
    @FXML
    private Label labelEstadoSimulacion;
    @FXML
    private TextArea textAreaDetallesEntidad;
    @FXML
    private Slider sliderVelocidad;
    @FXML
    private Label labelVelocidadValor;

    // --- Dependencias ---
    private ServicioSimulacion servicioSimulacion; // Se inyectará o instanciará

    // --- com.alphaone.logisticaRobots.domain.Estado del controlador ---
    private DimensionGrillaDTO dimensionGrilla;
    private List<RobotDTO> ultimosRobots;
    private List<CofreDTO> ultimosCofres;
    private List<RobopuertoDTO> ultimosRobopuertos;

    // Escala para dibujar en el canvas (píxeles por unidad de grilla)
    private static final double ESCALA_DIBUJO = 40.0;
    private static final double RADIO_ROBOT = 5.0;
    private static final double TAMANO_COFRE = 10.0;
    private static final double TAMANO_ROBOPUERTO = 20.0;

    // Variables para panning y zooming
    private double offsetX = 0;
    private double offsetY = 0;
    private double escalaZoom = 1.0;
    private double lastMouseX;
    private double lastMouseY;
    private boolean isPanning = false;


    /**
     * Método de inicialización llamado por JavaFX después de que los campos FXML han sido inyectados.
     */
    @FXML
    public void initialize() {

        // En JavaFX, initialize se llama antes de que MainApplication pueda inyectar el servicio
        // Por lo tanto, deshabilitamos los controles inicialmente y esperamos a que setServicioSimulacion sea llamado
        deshabilitarControles(true);

        // Si el servicio ya está disponible (poco probable en este punto), lo configuramos
        if (this.servicioSimulacion != null) {
            this.servicioSimulacion.registrarObservador(this);
            deshabilitarControles(false);
            botonPausar.setDisable(true);
        }

        // Configurar el canvas para clics (selección de entidades)
        canvasGrilla.setOnMouseClicked(this::handleCanvasClick);

        // Configurar eventos para panning (arrastrar el mapa)
        canvasGrilla.setOnMousePressed(event -> {
            lastMouseX = event.getX();
            lastMouseY = event.getY();
            isPanning = true;
        });

        canvasGrilla.setOnMouseDragged(event -> {
            if (isPanning) {
                double deltaX = event.getX() - lastMouseX;
                double deltaY = event.getY() - lastMouseY;
                offsetX += deltaX;
                offsetY += deltaY;
                lastMouseX = event.getX();
                lastMouseY = event.getY();

                // Redibujar el canvas con la nueva posición
                limpiarCanvas();
                if (dimensionGrilla != null) {
                    dibujarEstado(servicioSimulacion.getEstadoActualSimulacion());
                }
            }
        });

        canvasGrilla.setOnMouseReleased(event -> {
            isPanning = false;
        });

        // Configurar eventos para zooming (rueda del ratón)
        canvasGrilla.setOnScroll(this::handleScroll);

        // Inicialmente, no hay detalles para mostrar
        textAreaDetallesEntidad.setText("Seleccione una entidad en la grilla para ver sus detalles.");
        labelCicloActual.setText("Ciclo: N/A");
        labelEstadoSimulacion.setText("Estado: No iniciado");

        // Configurar slider de velocidad
        if (sliderVelocidad != null && labelVelocidadValor != null) {
            sliderVelocidad.valueProperty().addListener((obs, oldVal, newVal) -> {
                int ms = newVal.intValue();
                labelVelocidadValor.setText(ms + " ms");
                if (servicioSimulacion != null) {
                    servicioSimulacion.setVelocidadSimulacion(ms);
                }
            });
            // Valor inicial
            labelVelocidadValor.setText(((int)sliderVelocidad.getValue()) + " ms");
        }
    }

    /**
     * Permite inyectar el servicio de simulación desde fuera (ej. desde la clase Main de JavaFX).
     * @param servicioSimulacion El servicio de simulación.
     */
    public void setServicioSimulacion(ServicioSimulacion servicioSimulacion) {
        this.servicioSimulacion = servicioSimulacion;
        // Si initialize ya se llamó, registrar observador aquí
        if (canvasGrilla != null) { // Un indicador de que FXML ha cargado
            this.servicioSimulacion.registrarObservador(this);
            deshabilitarControles(false);
            botonPausar.setDisable(true);
            actualizarEstado(this.servicioSimulacion.getEstadoActualSimulacion());
            // Inicializar el slider con la velocidad actual si es posible
            if (sliderVelocidad != null) {
                sliderVelocidad.setValue(1000); // Valor por defecto, o podrías obtenerlo del servicio si se expone
            }
        }
    }

    private void deshabilitarControles(boolean deshabilitar) {
        botonIniciar.setDisable(deshabilitar);
        // botonPausar se maneja según el estado de la simulación
        botonResetear.setDisable(deshabilitar);
        botonAvanzarCiclo.setDisable(deshabilitar);
        botonCargarConfig.setDisable(false); // Asumimos que cargar config siempre es una opción
    }


    // --- Manejadores de Eventos FXML ---

    @FXML
    private void handleIniciarSimulacion() {
        if (servicioSimulacion != null) {
            servicioSimulacion.iniciarSimulacion();
            botonIniciar.setDisable(true);
            botonPausar.setDisable(false);
            botonAvanzarCiclo.setDisable(true); // No avanzar manualmente si está corriendo
        }
    }

    @FXML
    private void handlePausarSimulacion() {
        if (servicioSimulacion != null) {
            servicioSimulacion.pausarSimulacion();
            botonIniciar.setDisable(false);
            botonPausar.setDisable(true);
            botonAvanzarCiclo.setDisable(false); // Se puede avanzar manualmente si está pausada
        }
    }

    @FXML
    private void handleResetearSimulacion() {
        if (servicioSimulacion != null) {
            servicioSimulacion.resetearSimulacion();
            // El estado se actualizará a través del observador
            botonIniciar.setDisable(false);
            botonPausar.setDisable(true);
            botonAvanzarCiclo.setDisable(false);
            textAreaDetallesEntidad.setText("Simulación reseteada. Seleccione una entidad.");
        }
    }

    @FXML
    private void handleAvanzarCiclo() {
        if (servicioSimulacion != null) {
            servicioSimulacion.avanzarCicloSimulacion();
            // El estado se actualizará a través del observador
        }
    }

    @FXML
    private void handleCargarConfiguracion() {
        if (servicioSimulacion == null) {
            textAreaDetallesEntidad.setText("Error: Servicio de simulación no disponible.");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Cargar Archivo de Configuración");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );
        File archivoSeleccionado = fileChooser.showOpenDialog(canvasGrilla.getScene().getWindow());

        if (archivoSeleccionado != null) {
            try {
                servicioSimulacion.cargarConfiguracion(archivoSeleccionado);
                // La actualización del estado y la UI vendrá por el observador
                textAreaDetallesEntidad.setText("Configuración cargada desde: " + archivoSeleccionado.getName());
                botonIniciar.setDisable(false);
                botonPausar.setDisable(true);
                botonResetear.setDisable(false);
                botonAvanzarCiclo.setDisable(false);
            } catch (Exception e) {
                // Aquí deberías mostrar un Alert de JavaFX
                System.err.println("Error al cargar la configuración: " + e.getMessage());
                e.printStackTrace();
                textAreaDetallesEntidad.setText("Error al cargar configuración: " + e.getMessage());
            }
        }
    }

    // --- Implementación de ObservadorEstadoSimulacion ---

    @Override
    public void actualizarEstado(EstadoSimulacionDTO nuevoEstado) {
        Platform.runLater(() -> {
            if (nuevoEstado == null) {
                // Podría ser un estado inicial antes de cargar config
                limpiarCanvas();
                labelCicloActual.setText("Ciclo: N/A");
                labelEstadoSimulacion.setText("Estado: Esperando configuración");
                this.dimensionGrilla = null;
                this.ultimosRobots = null;
                this.ultimosCofres = null;
                this.ultimosRobopuertos = null;
                return;
            }

            this.dimensionGrilla = nuevoEstado.dimensionGrilla();
            this.ultimosRobots = nuevoEstado.robots();
            this.ultimosCofres = nuevoEstado.cofres();
            this.ultimosRobopuertos = nuevoEstado.robopuertos();

                if (dimensionGrilla != null) {
                canvasGrilla.setWidth(dimensionGrilla.ancho() * ESCALA_DIBUJO);
                canvasGrilla.setHeight(dimensionGrilla.alto() * ESCALA_DIBUJO);
                // Forzar layout para que el ScrollPane actualice su viewport
                if (canvasGrilla.getParent() != null) {
                    canvasGrilla.getParent().layout();
                }
                // Centrar el mapa cuando se carga la configuración
                if (offsetX == 0 && offsetY == 0) {
                    // Solo centrar si no se ha movido manualmente
                    double canvasParentWidth = canvasGrilla.getParent().getBoundsInLocal().getWidth();
                    double canvasParentHeight = canvasGrilla.getParent().getBoundsInLocal().getHeight();

                    // Calcular el offset para centrar
                    offsetX = (canvasParentWidth - canvasGrilla.getWidth()) / 2;
                    offsetY = (canvasParentHeight - canvasGrilla.getHeight()) / 2;

                    // Reiniciar zoom
                    escalaZoom = 1.0;
                }
            }
            limpiarCanvas();
            dibujarEstado(nuevoEstado);

            labelCicloActual.setText("Ciclo: " + nuevoEstado.cicloActual());
            labelEstadoSimulacion.setText("Estado: " + nuevoEstado.estadoGeneral());

            // Actualizar estado de botones según el estado de la simulación
            // Por ejemplo, si la simulación está "CORRIENDO", "PAUSADA", "FINALIZADA"
            String estadoSim = nuevoEstado.estadoGeneral() != null ? nuevoEstado.estadoGeneral().toUpperCase() : "";
            switch (estadoSim) {
                case "INICIADA":
                case "CORRIENDO": // Asumiendo que "INICIADA" puede transicionar a "CORRIENDO"
                    botonIniciar.setDisable(true);
                    botonPausar.setDisable(false);
                    botonAvanzarCiclo.setDisable(true);
                    break;
                case "PAUSADA":
                    botonIniciar.setDisable(false);
                    botonPausar.setDisable(true);
                    botonAvanzarCiclo.setDisable(false);
                    break;
                case "FINALIZADA_ESTABLE":
                case "ERROR":
                case "NO_INICIADO": // Después de reset o carga
                    botonIniciar.setDisable(false);
                    botonPausar.setDisable(true);
                    botonAvanzarCiclo.setDisable(false); // Permitir avanzar si está finalizada o error podría ser opcional
                    break;
                default:
                    // com.alphaone.logisticaRobots.domain.Estado desconocido o inicial antes de cargar config
                    if (servicioSimulacion != null && dimensionGrilla != null) { // Si hay una config cargada
                        botonIniciar.setDisable(false);
                        botonPausar.setDisable(true);
                    } else { // Sin config
                        botonIniciar.setDisable(true);
                        botonPausar.setDisable(true);
                    }
                    botonAvanzarCiclo.setDisable(dimensionGrilla == null); // Solo avanzar si hay grilla
                    break;
            }
            botonResetear.setDisable(dimensionGrilla == null); // Solo resetear si hay algo cargado
        });
    }

    // --- Lógica de Dibujo ---

    private void limpiarCanvas() {
        GraphicsContext gc = canvasGrilla.getGraphicsContext2D();

        // Limpiar todo el canvas
        gc.clearRect(0, 0, canvasGrilla.getWidth(), canvasGrilla.getHeight());

        // Aplicar transformaciones (panning y zooming)
        gc.save();
        gc.transform(new Affine(escalaZoom, 0, offsetX, 0, escalaZoom, offsetY));

        // Opcional: Dibujar una grilla de fondo
        if (dimensionGrilla != null) {
            gc.setStroke(Color.LIGHTGRAY);
            gc.setLineWidth(0.5 / escalaZoom); // Ajustar el ancho de línea según el zoom

            for (int x = 0; x <= dimensionGrilla.ancho(); x++) {
                gc.strokeLine(x * ESCALA_DIBUJO, 0, x * ESCALA_DIBUJO, dimensionGrilla.alto() * ESCALA_DIBUJO);
            }
            for (int y = 0; y <= dimensionGrilla.alto(); y++) {
                gc.strokeLine(0, y * ESCALA_DIBUJO, dimensionGrilla.ancho() * ESCALA_DIBUJO, y * ESCALA_DIBUJO);
            }
        }

        // Restaurar el contexto gráfico para que las transformaciones no afecten a otros dibujos
        gc.restore();
    }

    private void dibujarEstado(EstadoSimulacionDTO estado) {
        GraphicsContext gc = canvasGrilla.getGraphicsContext2D();

        // Aplicar transformaciones (panning y zooming)
        gc.save();
        gc.transform(new Affine(escalaZoom, 0, offsetX, 0, escalaZoom, offsetY));

        if (estado.robopuertos() != null) {
            estado.robopuertos().forEach(this::dibujarRobopuerto);
        }
        if (estado.cofres() != null) {
            estado.cofres().forEach(this::dibujarCofre);
        }
        if (estado.robots() != null) {
            estado.robots().forEach(this::dibujarRobot);
        }

        // Restaurar el contexto gráfico
        gc.restore();
    }

// Reemplazar los métodos de dibujo en MainSimulacionController con estos:

private void dibujarRobopuerto(RobopuertoDTO robopuerto) {
    GraphicsContext gc = canvasGrilla.getGraphicsContext2D();
    double x = robopuerto.posicion().x() * ESCALA_DIBUJO;
    double y = robopuerto.posicion().y() * ESCALA_DIBUJO;
    double alcance = robopuerto.alcance() * ESCALA_DIBUJO;

    // Dibujar zona de cobertura con un gradiente radial
    gc.save();
    RadialGradient gradient = new RadialGradient(
            0, 0, 
            x, y, alcance,
            false, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(100, 149, 237, 0.2)), // Azul claro en el centro
            new Stop(1, Color.rgb(100, 149, 237, 0.05)) // Más transparente en los bordes
    );

    gc.setFill(gradient);
    gc.fillOval(x - alcance, y - alcance, alcance * 2, alcance * 2);

    // Borde de la zona de cobertura
    gc.setStroke(Color.CORNFLOWERBLUE);
    gc.setLineWidth(0.8);
    gc.setLineDashes(5, 5);
    gc.strokeOval(x - alcance, y - alcance, alcance * 2, alcance * 2);
    gc.setLineDashes(null);

    // Dibujar robopuerto como estructura octagonal
    double size = TAMANO_ROBOPUERTO;
    double s = size * 0.4; // Factor para los lados del octágono

    gc.setFill(Color.DARKBLUE);
    double[] xPoints = {
        x, x+s, x+size/2, x+s, x, x-s, x-size/2, x-s
    };
    double[] yPoints = {
        y-size/2, y-s, y, y+s, y+size/2, y+s, y, y-s
    };
    gc.fillPolygon(xPoints, yPoints, 8);

    // Añadir un efecto de brillo
    gc.setFill(Color.rgb(255, 255, 255, 0.3));
    gc.fillOval(x-size/4, y-size/4, size/2, size/2);

    // Añadir identificador si hay espacio
    if (size > 12) {
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Arial", 10));
        gc.fillText("RP", x - 7, y + 4);
    }

    gc.restore();
}

private void dibujarCofre(CofreDTO cofre) {
    GraphicsContext gc = canvasGrilla.getGraphicsContext2D();
    double x = cofre.posicion().x() * ESCALA_DIBUJO;
    double y = cofre.posicion().y() * ESCALA_DIBUJO;
    double size = TAMANO_COFRE;

    // Determinar color base según accesibilidad y porcentaje de llenado
    Color colorBase;
    double porcentajeLlenado = (double) cofre.capacidadActual() / cofre.capacidadMaxima();

    if (!cofre.esAccesible()) {
        colorBase = Color.GRAY; // Cofre inaccesible
    } else {
        // Gradiente de color según el porcentaje de llenado (verde a rojo)
        double r = Math.min(1.0, porcentajeLlenado * 2); // Más rojo mientras más lleno
        double g = Math.min(1.0, (1 - porcentajeLlenado) * 2); // Menos verde mientras más lleno
        colorBase = Color.color(r * 0.8, g * 0.6, 0.2); // Tonos tierra/cofre
    }

    // Sombra
    gc.setFill(Color.rgb(0, 0, 0, 0.2));
    gc.fillRect(x - size/2 + 2, y - size/2 + 2, size, size);

    // Cuerpo principal del cofre
    gc.setFill(colorBase);
    gc.fillRect(x - size/2, y - size/2, size, size);

    // Borde del cofre
    gc.setStroke(Color.BLACK);
    gc.setLineWidth(1);
    gc.strokeRect(x - size/2, y - size/2, size, size);

    // Dibuja la "tapa" del cofre
    gc.setFill(colorBase.brighter());
    gc.fillRect(x - size/2, y - size/2, size, size/4);
    gc.setStroke(Color.BLACK);
    gc.strokeRect(x - size/2, y - size/2, size, size/4);

    // Cerradura/candado
    gc.setFill(Color.DARKGOLDENROD);
    gc.fillOval(x - 2, y - size/2 + size/8 - 2, 4, 4);

    // Indicador de tipo de comportamiento (primera letra del comportamiento principal)
    if (!cofre.inventario().isEmpty() || cofre.comportamientoDefecto() != null) {
        String comportamiento = cofre.comportamientoDefecto();
        if (comportamiento != null && !comportamiento.isEmpty()) {
            gc.setFill(Color.WHITE);
            gc.setFont(new Font("Arial Bold", 9));
            gc.fillText(comportamiento.substring(0, 1).toUpperCase(), x - 3, y + 3);
        }
    }

    // Indicador de llenado (barra de progreso)
    if (porcentajeLlenado > 0) {
        double barraAncho = size * 0.8;
        double barraAlto = 3;
        double barraPosX = x - barraAncho/2;
        double barraPosY = y + size/2 + 4;

        // Fondo de la barra
        gc.setFill(Color.rgb(200, 200, 200, 0.8));
        gc.fillRect(barraPosX, barraPosY, barraAncho, barraAlto);

        // Barra de progreso
        Color colorBarra = porcentajeLlenado > 0.8 ? Color.RED :
                           porcentajeLlenado > 0.6 ? Color.ORANGE :
                           Color.GREEN;
        gc.setFill(colorBarra);
        gc.fillRect(barraPosX, barraPosY, barraAncho * porcentajeLlenado, barraAlto);
    }
}

private void dibujarRobot(RobotDTO robot) {
    GraphicsContext gc = canvasGrilla.getGraphicsContext2D();
    double x = robot.posicion().x() * ESCALA_DIBUJO;
    double y = robot.posicion().y() * ESCALA_DIBUJO;

    // Dibujar ruta actual si existe
    if (robot.rutaActual() != null && !robot.rutaActual().isEmpty()) {
        gc.setStroke(Color.LIGHTGREEN);
        gc.setLineWidth(2.0);

        // Si la ruta comienza en un punto diferente a la posición actual, partir desde el primer punto de la ruta
        PuntoDTO pAnterior = robot.rutaActual().get(0);
        int inicio = 1;
        if (!robot.rutaActual().get(0).equals(robot.posicion())) {
            // Dibujar línea desde el primer punto de la ruta hasta el segundo
            // (no desde la posición actual del robot)
            // Si se quiere, se puede dibujar una línea punteada desde la posición actual al primer punto de la ruta
            // pero por ahora solo mostramos la ruta planificada
            inicio = 1;
        } else {
            // Si la ruta comienza en la posición actual, partir desde ahí
            pAnterior = robot.posicion();
            inicio = 1;
        }
        for (int i = inicio; i < robot.rutaActual().size(); i++) {
            PuntoDTO pSiguiente = robot.rutaActual().get(i);
            gc.strokeLine(
                pAnterior.x() * ESCALA_DIBUJO, pAnterior.y() * ESCALA_DIBUJO,
                pSiguiente.x() * ESCALA_DIBUJO, pSiguiente.y() * ESCALA_DIBUJO
            );
            // Dibujar punto de paso (círculo más pequeño para puntos intermedios)
            gc.setFill(Color.LIGHTGREEN);
            double radioPunto = (i == robot.rutaActual().size() - 1) ? 3 : 2; // Punto final más grande
            gc.fillOval(pSiguiente.x() * ESCALA_DIBUJO - radioPunto, pSiguiente.y() * ESCALA_DIBUJO - radioPunto, 
                       radioPunto * 2, radioPunto * 2);
            pAnterior = pSiguiente;
        }
        // Dibujar flecha en el punto final para indicar dirección (doble de grande)
        if (robot.rutaActual().size() > 1) {
            PuntoDTO penultimo = robot.rutaActual().get(robot.rutaActual().size() - 2);
            PuntoDTO ultimo = robot.rutaActual().get(robot.rutaActual().size() - 1);
            double x1 = penultimo.x() * ESCALA_DIBUJO;
            double y1 = penultimo.y() * ESCALA_DIBUJO;
            double x2 = ultimo.x() * ESCALA_DIBUJO;
            double y2 = ultimo.y() * ESCALA_DIBUJO;
            double dx = x2 - x1;
            double dy = y2 - y1;
            if (dx != 0 || dy != 0) {
                double length = Math.sqrt(dx * dx + dy * dy);
                // Doble de grande: longitud de la flecha 16 (antes era 8)
                double arrowLength = 16;
                double arrowHeadSize = 10; // tamaño de la cabeza de la flecha (doble de 5)
                double unitDx = dx / length;
                double unitDy = dy / length;

                // Punto de inicio de la flecha (un poco antes del final para que la cabeza no tape el punto)
                double startX = x2 - unitDx * 2;
                double startY = y2 - unitDy * 2;
                double endX = x2;
                double endY = y2;

                // Dibujar la línea principal de la flecha
                gc.setStroke(Color.DARKGREEN);
                gc.setLineWidth(3.0);
                gc.strokeLine(startX - unitDx * (arrowLength - 2), startY - unitDy * (arrowLength - 2), endX, endY);

                // Dibujar la cabeza de la flecha
                double angle = Math.atan2(unitDy, unitDx);
                double arrowAngle = Math.toRadians(25); // ángulo entre la línea y los lados de la cabeza

                double xArrow1 = endX - arrowHeadSize * Math.cos(angle - arrowAngle);
                double yArrow1 = endY - arrowHeadSize * Math.sin(angle - arrowAngle);

                double xArrow2 = endX - arrowHeadSize * Math.cos(angle + arrowAngle);
                double yArrow2 = endY - arrowHeadSize * Math.sin(angle + arrowAngle);

                gc.setFill(Color.DARKGREEN);
                gc.fillPolygon(
                    new double[]{endX, xArrow1, xArrow2},
                    new double[]{endY, yArrow1, yArrow2},
                    3
                );
            }
        }
    }

    // Determinar color basado en estado
    Color colorRobot;
    switch (robot.estadoActual()) {
        case "CARGANDO":
            colorRobot = Color.ORANGE;
            break;
        case "EN_MISION":
            colorRobot = Color.GREEN;
            break;
        case "INACTIVO":
            colorRobot = Color.RED;
            break;
        case "PASIVO":
            colorRobot = Color.YELLOW;
            break;
        default: // ACTIVO u otros
            colorRobot = Color.LIMEGREEN;
    }

    // Calcular el porcentaje de batería para el degradado
    double porcentajeBateria = robot.nivelBateria() / robot.bateriaMaxima();

    // Base del robot (círculo)
    double radio = RADIO_ROBOT;

    // Sombra
    gc.setFill(Color.rgb(0, 0, 0, 0.2));
    gc.fillOval(x - radio + 1, y - radio + 1, radio * 2, radio * 2);

    // Cuerpo del robot
    gc.setFill(colorRobot);
    gc.fillOval(x - radio, y - radio, radio * 2, radio * 2);
    gc.setStroke(colorRobot.darker());
    gc.setLineWidth(1);
    gc.strokeOval(x - radio, y - radio, radio * 2, radio * 2);

    // Indicador de carga de batería
    double indicadorRadio = radio * 0.7;
    if (porcentajeBateria < 0.3) { // Batería baja
        gc.setFill(Color.RED);
    } else if (porcentajeBateria < 0.7) { // Batería media
        gc.setFill(Color.ORANGE);
    } else { // Batería alta
        gc.setFill(Color.LIGHTGREEN);
    }

    double angulo = 360 * porcentajeBateria;
    gc.fillArc(x - indicadorRadio, y - indicadorRadio, 
               indicadorRadio * 2, indicadorRadio * 2, 
               90, -angulo, ArcType.ROUND);

    // Indicador de carga (items)
    if (!robot.itemsEnCarga().isEmpty()) {
        // Un pequeño cuadrado encima del robot
        double tamano = 4;
        gc.setFill(Color.YELLOW);
        gc.fillRect(x - tamano/2, y - radio - tamano - 2, tamano, tamano);
    }

    // ID del robot o alguna otra identificación
    if (radio > 4) {
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Arial", 8));
        gc.fillText(robot.id().substring(0, Math.min(2, robot.id().length())), x - 4, y + 3);
    }
}


    // --- Manejo de Selección de Entidades ---
    private void handleCanvasClick(MouseEvent event) {
        if (dimensionGrilla == null || servicioSimulacion == null) return;

        double clickX = event.getX();
        double clickY = event.getY();

        // Convertir coordenadas del canvas a coordenadas transformadas (considerando panning y zoom)
        // Invertir la transformación para obtener las coordenadas en el espacio de la grilla
        double transformedX = (clickX - offsetX) / escalaZoom;
        double transformedY = (clickY - offsetY) / escalaZoom;

        // Buscar la entidad más cercana al clic
        EntidadSeleccionada entidadSeleccionada = encontrarEntidadEnPosicion(transformedX, transformedY);

        if (entidadSeleccionada != null) {
            DetallesEntidadDTO detalles = servicioSimulacion.getDetallesEntidad(entidadSeleccionada.tipo(), entidadSeleccionada.id());
            mostrarDetallesEntidad(detalles);
        } else {
            // Mostrar las coordenadas transformadas (en el espacio de la grilla)
            double grillaX = transformedX / ESCALA_DIBUJO;
            double grillaY = transformedY / ESCALA_DIBUJO;
            textAreaDetallesEntidad.setText("Ninguna entidad seleccionada en (" + 
                String.format("%.1f", grillaX) + ", " + 
                String.format("%.1f", grillaY) + ") - Zoom: " + 
                String.format("%.1f", escalaZoom) + "x");
        }
    }

    /**
     * Clase auxiliar para representar una entidad seleccionada
     */
    private static class EntidadSeleccionada {
        private final String id;
        private final String tipo;
        private final double distancia;

        public EntidadSeleccionada(String id, String tipo, double distancia) {
            this.id = id;
            this.tipo = tipo;
            this.distancia = distancia;
        }

        public String id() { return id; }
        public String tipo() { return tipo; }
        public double distancia() { return distancia; }
    }

    /**
     * Encuentra la entidad más cercana a una posición dada
     */
    private EntidadSeleccionada encontrarEntidadEnPosicion(double x, double y) {
        EntidadSeleccionada entidadMasCercana = null;
        double minDistancia = Double.MAX_VALUE;

        // Buscar robots
        if (ultimosRobots != null) {
            for (RobotDTO robot : ultimosRobots) {
                double rX = robot.posicion().x() * ESCALA_DIBUJO;
                double rY = robot.posicion().y() * ESCALA_DIBUJO;
                double distancia = Math.sqrt(Math.pow(x - rX, 2) + Math.pow(y - rY, 2));
                
                // Área de clic para robots (radio del robot + margen)
                double areaClickRobot = RADIO_ROBOT * 1.5;
                
                if (distancia < areaClickRobot && distancia < minDistancia) {
                    minDistancia = distancia;
                    entidadMasCercana = new EntidadSeleccionada(robot.id(), "ROBOT", distancia);
                }
            }
        }

        // Buscar cofres
        if (ultimosCofres != null) {
            for (CofreDTO cofre : ultimosCofres) {
                double cX = cofre.posicion().x() * ESCALA_DIBUJO;
                double cY = cofre.posicion().y() * ESCALA_DIBUJO;
                double distancia = Math.sqrt(Math.pow(x - cX, 2) + Math.pow(y - cY, 2));
                
                // Área de clic para cofres (mitad del tamaño del cofre)
                double areaClickCofre = TAMANO_COFRE / 2;
                
                if (distancia < areaClickCofre && distancia < minDistancia) {
                    minDistancia = distancia;
                    entidadMasCercana = new EntidadSeleccionada(cofre.id(), "COFRE", distancia);
                }
            }
        }

        // Buscar robopuertos
        if (ultimosRobopuertos != null) {
            for (RobopuertoDTO rp : ultimosRobopuertos) {
                double rpX = rp.posicion().x() * ESCALA_DIBUJO;
                double rpY = rp.posicion().y() * ESCALA_DIBUJO;
                double distancia = Math.sqrt(Math.pow(x - rpX, 2) + Math.pow(y - rpY, 2));
                
                // Área de clic para robopuertos (mitad del tamaño del robopuerto)
                double areaClickRobopuerto = TAMANO_ROBOPUERTO / 2;
                
                if (distancia < areaClickRobopuerto && distancia < minDistancia) {
                    minDistancia = distancia;
                    entidadMasCercana = new EntidadSeleccionada(rp.id(), "ROBOPUERTO", distancia);
                }
            }
        }

        return entidadMasCercana;
    }

    /**
     * Maneja el evento de scroll para implementar zoom
     */
    private void handleScroll(ScrollEvent event) {
        double zoomFactor = 1.05;
        double deltaY = event.getDeltaY();

        if (deltaY < 0) {
            // Zoom out (reduce scale)
            escalaZoom /= zoomFactor;
        } else {
            // Zoom in (increase scale)
            escalaZoom *= zoomFactor;
        }

        // Limitar el zoom para evitar valores extremos
        escalaZoom = Math.max(0.1, Math.min(escalaZoom, 5.0));

        // Redibujar el canvas con la nueva escala
        limpiarCanvas();
        if (dimensionGrilla != null) {
            dibujarEstado(servicioSimulacion.getEstadoActualSimulacion());
        }

        event.consume();
    }

    private void mostrarDetallesEntidad(DetallesEntidadDTO detalles) {
        if (detalles == null) {
            textAreaDetallesEntidad.setText("No se encontraron detalles para la entidad seleccionada.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        switch (detalles.tipoEntidad()) {
            case "ROBOT":
                RobotDTO robot = detalles.robot();
                sb.append("--- ROBOT ---\n");
                sb.append("ID: ").append(robot.id()).append("\n");
                sb.append("Posición: ").append(robot.posicion()).append("\n");
                sb.append(String.format("Batería: %.1f / %.1f\n", robot.nivelBateria(), robot.bateriaMaxima()));
                sb.append("Carga: ").append(robot.cargaActual()).append(" / ").append(robot.capacidadCarga()).append("\n");
                sb.append("Items en carga: \n");
                robot.itemsEnCarga().forEach((item, cant) -> sb.append("  - ").append(item).append(": ").append(cant).append("\n"));
                sb.append("Estado: ").append(robot.estadoActual()).append("\n");
                if (robot.rutaActual() != null && !robot.rutaActual().isEmpty()) {
                    sb.append("Ruta: ").append(robot.rutaActual().size()).append(" pasos\n");
                    
                    // Mostrar información adicional de la ruta
                    PuntoDTO destino = robot.rutaActual().get(robot.rutaActual().size() - 1);
                    sb.append("Destino: (").append(destino.x()).append(", ").append(destino.y()).append(")\n");
                    
                    // Calcular distancia total de la ruta
                    double distanciaTotal = 0;
                    PuntoDTO anterior = robot.posicion();
                    for (PuntoDTO punto : robot.rutaActual()) {
                        distanciaTotal += Math.sqrt(Math.pow(punto.x() - anterior.x(), 2) + Math.pow(punto.y() - anterior.y(), 2));
                        anterior = punto;
                    }
                    sb.append(String.format("Distancia total: %.1f unidades\n", distanciaTotal));
                }
                break;
            case "COFRE":
                CofreDTO cofre = detalles.cofre();
                sb.append("--- COFRE ---\n");
                sb.append("ID: ").append(cofre.id()).append("\n");
                sb.append("Posición: ").append(cofre.posicion()).append("\n");
                sb.append("Capacidad: ").append(cofre.capacidadActual()).append(" / ").append(cofre.capacidadMaxima()).append("\n");
                sb.append("Accesible: ").append(cofre.esAccesible() ? "Sí" : "No").append("\n");
                sb.append("Inventario: \n");
                cofre.inventario().forEach((item, cant) -> sb.append("  - ").append(item).append(": ").append(cant).append("\n"));
                sb.append("Comportamiento por defecto: ").append(cofre.comportamientoDefecto()).append("\n");
                sb.append("Comportamientos específicos: \n");
                cofre.tiposComportamientoPorItem().forEach((item, comp) -> sb.append("  - ").append(item).append(": ").append(comp).append("\n"));
                break;
            case "ROBOPUERTO":
                RobopuertoDTO rp = detalles.robopuerto();
                sb.append("--- ROBOPUERTO ---\n");
                sb.append("ID: ").append(rp.id()).append("\n");
                sb.append("Posición: ").append(rp.posicion()).append("\n");
                sb.append("Alcance: ").append(rp.alcance()).append("\n");
                sb.append("Cofres cubiertos: ").append(rp.idsCofresCubiertos() != null ? rp.idsCofresCubiertos().size() : 0).append("\n");
                // rp.idsCofresCubiertos().forEach(id -> sb.append("  - ").append(id).append("\n")); // Podría ser muy largo
                break;
            default:
                sb.append("Tipo de entidad desconocido.");
        }
        textAreaDetallesEntidad.setText(sb.toString());
    }
}
