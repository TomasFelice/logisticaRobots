package com.alphaone.logisticaRobots.application.dto;

import java.util.Map;

/**
 * DTO para representar la información de un cofre para la UI.
 */
public record CofreDTO(
    String id,
    PuntoDTO posicion,
    Map<String, Integer> inventario, // ItemID (o nombre) a cantidad
    int capacidadActual,
    int capacidadMaxima,
    Map<String, String> tiposComportamientoPorItem, // ItemID a descripción del comportamiento
    String comportamientoDefecto,
    boolean esAccesible // Indica si está dentro de alguna zona de cobertura
) {
}