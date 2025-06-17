package com.alphaone.logisticaRobots.infrastructure.config;

import com.alphaone.logisticaRobots.application.dto.ConfiguracionSimulacionDTO;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class LogisticaRobotsConfigLoaderTest {

    @Test
    void cargarConfiguracionDesdeRecursosPredeterminado() {
        // Arrange
        LogisticaRobotsConfigLoader configLoader = new LogisticaRobotsConfigLoader();
        
        try {
            // Act
            ConfiguracionSimulacionDTO config = configLoader.cargarConfiguracion();
            
            // Assert
            assertNotNull(config, "La configuración no debería ser null");
            assertNotNull(config.dimensionGrilla(), "La dimensión de la grilla no debería ser null");
            assertEquals(10, config.dimensionGrilla().ancho(), "El ancho de la grilla debería ser 10");
            assertEquals(10, config.dimensionGrilla().alto(), "El alto de la grilla debería ser 10");
            
            // Verificar que se cargaron los robopuertos
            assertNotNull(config.robopuertos(), "La lista de robopuertos no debería ser null");
            assertFalse(config.robopuertos().isEmpty(), "Debería haber al menos un robopuerto");
            
            // Verificar que se cargaron los robots
            assertNotNull(config.robots(), "La lista de robots no debería ser null");
            assertFalse(config.robots().isEmpty(), "Debería haber al menos un robot");
            
            // Verificar que se cargaron los cofres
            assertNotNull(config.cofres(), "La lista de cofres no debería ser null");
            assertFalse(config.cofres().isEmpty(), "Debería haber al menos un cofre");
            
        } catch (IOException e) {
            fail("No debería lanzar excepción al cargar la configuración: " + e.getMessage());
        }
    }
    
    @Test
    void cargarConfiguracionDesdeArchivoEspecifico() {
        // Arrange
        LogisticaRobotsConfigLoader configLoader = new LogisticaRobotsConfigLoader();
        File configFile = configLoader.getConfigFile();
        
        // Verificar que el archivo existe
        assertNotNull(configFile, "El archivo de configuración no debería ser null");
        assertTrue(configFile.exists(), "El archivo de configuración debería existir");
        
        try {
            // Act
            ConfiguracionSimulacionDTO config = configLoader.cargarConfiguracion(configFile);
            
            // Assert
            assertNotNull(config, "La configuración no debería ser null");
            assertNotNull(config.dimensionGrilla(), "La dimensión de la grilla no debería ser null");
            assertEquals(10, config.dimensionGrilla().ancho(), "El ancho de la grilla debería ser 10");
            assertEquals(10, config.dimensionGrilla().alto(), "El alto de la grilla debería ser 10");
            
        } catch (IOException e) {
            fail("No debería lanzar excepción al cargar la configuración: " + e.getMessage());
        }
    }
    
    @Test
    void getConfiguracionDeberiaRetornarNullSiNoSeCargo() {
        // Arrange
        LogisticaRobotsConfigLoader configLoader = new LogisticaRobotsConfigLoader();
        
        // Act & Assert
        assertNull(configLoader.getConfiguracion(), "La configuración debería ser null si no se ha cargado");
    }
    
    @Test
    void getConfigFileUrlDeberiaRetornarUrlValida() {
        // Arrange
        LogisticaRobotsConfigLoader configLoader = new LogisticaRobotsConfigLoader();
        
        // Act & Assert
        assertNotNull(configLoader.getConfigFileUrl(), "La URL del archivo de configuración no debería ser null");
    }
}