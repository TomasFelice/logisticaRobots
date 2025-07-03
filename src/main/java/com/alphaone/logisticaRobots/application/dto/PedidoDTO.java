package com.alphaone.logisticaRobots.application.dto;

/**
 * DTO para representar un pedido en la configuración.
 */
public record PedidoDTO(
    String id,
    String itemNombre,
    int cantidad,
    String cofreDestinoId,
    String prioridad, // "ALTA", "MEDIA", "BAJA"
    String estado // "NUEVO", "EN_PROCESO", "COMPLETADO", "FALLIDO"
) {
} 