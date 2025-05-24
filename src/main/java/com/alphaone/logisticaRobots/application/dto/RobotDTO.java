package com.alphaone.logisticaRobots.application.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO para representar la información de un robot para la UI.
 */
public record RobotDTO(
    String id,
    PuntoDTO posicion,
    double nivelBateria,
    double bateriaMaxima,
    int cargaActual, // Podría ser la suma de cantidades de items
    int capacidadCarga,
    Map<String, Integer> itemsEnCarga, // ItemID a cantidad
    String estadoActual, // Ej: "Moviendose", "Cargando", "Esperando"
    List<PuntoDTO> rutaActual // Opcional, para visualización
) {
}