package com.alphaone.logisticaRobots.ui;

import com.alphaone.logisticaRobots.application.dto.EstadoSimulacionDTO;

/**
 * Interfaz para los componentes de la UI que necesitan ser notificados
 * sobre cambios en el estado de la simulación.
 */
@FunctionalInterface
public interface ObservadorEstadoSimulacion {
    /**
     * Metodo llamado por el servicio de simulación cuando el estado ha cambiado.
     * Este metodo debe ser implementado para actualizar la UI de forma segura
     * @param nuevoEstado El DTO con el estado actualizado de la simulación.
     */
    void actualizarEstado(EstadoSimulacionDTO nuevoEstado);

}
