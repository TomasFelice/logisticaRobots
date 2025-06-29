package com.alphaone.logisticaRobots.domain;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class Pedido {

    public enum EstadoPedido {
        NUEVO, EN_PROCESO, COMPLETADO, FALLIDO
    }

    public enum PrioridadPedido {
        ALTA, MEDIA, BAJA, NO_APLICA
    }
    private final Item item;
    private final int cantidad;
    private final CofreLogistico cofreOrigen;
    private final CofreLogistico cofreDestino;
    private EstadoPedido estado;
    private PrioridadPedido prioridad;


    public Pedido(Item item, int cantidad, CofreLogistico origen, CofreLogistico destino,PrioridadPedido prioridad) {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser positiva");
        this.item = requireNonNull(item);
        this.cantidad = cantidad;
        this.cofreOrigen = requireNonNull(origen);
        this.cofreDestino = requireNonNull(destino);
        this.estado = EstadoPedido.NUEVO;
        this.prioridad = prioridad;
    }

    // Getters
    public Item getItem() { return item; }
    public int getCantidad() { return cantidad; }
    public CofreLogistico getCofreOrigen() { return cofreOrigen; }
    public CofreLogistico getCofreDestino() { return cofreDestino; }
    public EstadoPedido getEstado() { return estado; }
    public PrioridadPedido getPrioridad() { return prioridad; }

    // Estados
    public void marcarEnProceso() {
        estado = EstadoPedido.EN_PROCESO;
    }

    public void marcarCompletado() {
        estado = EstadoPedido.COMPLETADO;
    }

    public void marcarFallido() {
        estado = EstadoPedido.FALLIDO;
    }

    public boolean estaCompletado() {
        return estado == EstadoPedido.COMPLETADO;
    }

    public boolean estaFallido() {
        return estado == EstadoPedido.FALLIDO;
    }

    @Override
    public String toString() {
        return String.format("Pedido{%s, %d unidades, de %s a %s, estado=%s}",
                item.getNombre(), cantidad, cofreOrigen.getId(), cofreDestino.getId(), estado);
    }
}
