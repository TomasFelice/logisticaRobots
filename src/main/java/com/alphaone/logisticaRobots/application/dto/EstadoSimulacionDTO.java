package com.alphaone.logisticaRobots.application.dto;

import java.util.List;

/**
 * DTO que encapsula el estado completo de la simulación en un momento dado,
 * para ser enviado a la UI.
 */
public record EstadoSimulacionDTO(
        List<RobotDTO> robots,
        List<CofreDTO> cofres,
        List<RobopuertoDTO> robopuertos,
        DimensionGrillaDTO dimensionGrilla,
        String estadoGeneral, // Ej: "INICIADA", "PAUSADA", "FINALIZADA_ESTABLE", "ERROR"
        int cicloActual,
        String mensajeEstado, // Para mostrar mensajes relevantes en la UI
        List<PedidoDTO> pedidos
) {
}

