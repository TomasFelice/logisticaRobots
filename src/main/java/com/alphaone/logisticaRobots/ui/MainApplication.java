package com.alphaone.logisticaRobots.ui;

import com.alphaone.logisticaRobots.application.ServicioSimulacion;
import com.alphaone.logisticaRobots.application.ServicioSimulacionImpl;
import com.alphaone.logisticaRobots.ui.controllers.MainSimulacionController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * Clase principal que inicia la aplicación JavaFX
 */
public class MainApplication extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Cargar el FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/alphaone/logisticaRobots/ui/views/MainSimulacionView.fxml"));
            Parent root = loader.load();
            
            // Obtener el controlador
            MainSimulacionController controller = loader.getController();
            
            // Crear e inyectar el servicio
            ServicioSimulacion servicioSimulacion = new ServicioSimulacionImpl();
            controller.setServicioSimulacion(servicioSimulacion);
            
            // Configurar y mostrar la escena
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/com/alphaone/logisticaRobots/ui/css/styles.css").toExternalForm());
            
            primaryStage.setTitle("Sistema Logístico Automatizado");
            
            // Opcional: Configurar icono
            // primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/com/alphaone/logisticaRobots/ui/images/logo.png")));
            
            primaryStage.setScene(scene);
            primaryStage.show();
            
            logger.info("Aplicación iniciada correctamente");
            
        } catch (IOException e) {
            logger.error("Error al iniciar la aplicación", e);
            System.err.println("Error al cargar la interfaz: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void stop() {
        // [TODO] Código para limpieza de recursos al cerrar la aplicación
        // Por ejemplo, detener hilos en ejecución, cerrar conexiones, etc.
        logger.info("Aplicación cerrándose");
    }

    /**
     * Metodo principal para iniciar la aplicación
     */
    public static void main(String[] args) {
        launch(args);
    }
}