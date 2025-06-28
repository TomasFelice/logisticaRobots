package com.alphaone.logisticaRobots.application;

import com.alphaone.logisticaRobots.application.dto.EstadoSimulacionDTO;
import com.alphaone.logisticaRobots.application.dto.DetallesEntidadDTO;
import com.alphaone.logisticaRobots.ui.ObservadorEstadoSimulacion;


import java.io.File;

/**
 * Interfaz para el servicio que maneja la lógica de la simulación
 * y actúa como intermediario entre la UI y el dominio.
 */
public interface ServicioSimulacion {
    /**
     * Inicia o reanuda la ejecución de la simulación.
     */
    void iniciarSimulacion();

    /**
     * Pausa la ejecución de la simulación.
     */
    void pausarSimulacion();

    /**
     * Resetea la simulación a su estado inicial definido en la configuración.
     */
    void resetearSimulacion();

    /**
     * Avanza la simulación exactamente un ciclo o paso.
     */
    void avanzarCicloSimulacion();

    /**
     * Carga la configuración inicial del sistema desde un archivo.
     * @param archivoConfig Archivo de configuración (ej. JSON).
     * @throws Exception Si ocurre un error durante la carga.
     */
    void cargarConfiguracion(File archivoConfig) throws Exception;

    /**
     * Obtiene el estado actual de la simulación para ser mostrado en la UI.
     * @return Un DTO con la información actual de la simulación.
     */
    EstadoSimulacionDTO getEstadoActualSimulacion();

    /**
     * Obtiene detalles específicos de una entidad de la simulación (robot, cofre, robopuerto).
     * @param tipoEntidad Tipo de la entidad ("ROBOT", "COFRE", "ROBOPUERTO").
     * @param idEntidad Identificador único de la entidad dentro de su tipo.
     * @return Un DTO con los detalles de la entidad, o null si no se encuentra.
     */
    DetallesEntidadDTO getDetallesEntidad(String tipoEntidad, String idEntidad);

    /**
     * Registra un observador que será notificado de los cambios en el estado de la simulación.
     * @param observador El observador a registrar.
     */
    void registrarObservador(ObservadorEstadoSimulacion observador);

    /**
     * Elimina un observador previamente registrado.
     * @param observador El observador a eliminar.
     */
    void removerObservador(ObservadorEstadoSimulacion observador);

}
