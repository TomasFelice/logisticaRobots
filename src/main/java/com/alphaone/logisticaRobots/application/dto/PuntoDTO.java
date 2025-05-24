package com.alphaone.logisticaRobots.application.dto;

/**
 * DTO para representar coordenadas (x, y)
 * Similar a la clase Punto del dominio, pero desacoplado para la capa de aplicación/UI
 * @param x
 * @param y
 */
public record PuntoDTO(int x, int y) {
}
