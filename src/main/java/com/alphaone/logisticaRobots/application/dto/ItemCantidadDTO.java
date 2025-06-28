package com.alphaone.logisticaRobots.application.dto;

/**
 * DTO para representar un item con su cantidad.
 */
public record ItemCantidadDTO(
    String item,
    int cantidad
) {
}