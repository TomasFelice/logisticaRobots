package com.alphaone.logisticaRobots.application.dto;

import java.util.List;

/**
 * DTO para representar la información de un robopuerto para la UI.
 */
public record RobopuertoDTO(
    String id,
    PuntoDTO posicion,
    double alcanceCobertura,
    List<String> idsCofresCubiertos // Lista de IDs de cofres dentro de su zona
) {
}