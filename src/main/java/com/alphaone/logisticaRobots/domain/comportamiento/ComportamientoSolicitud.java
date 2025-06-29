package com.alphaone.logisticaRobots.domain.comportamiento;

import com.alphaone.logisticaRobots.domain.CofreLogistico;
import com.alphaone.logisticaRobots.domain.Item;

/**
 * Comportamiento que solicita ítems con prioridad configurable
 */
public class ComportamientoSolicitud implements ComportamientoCofre {

    private final int capacidadMaxima;

    public ComportamientoSolicitud(int capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    @Override
    public boolean puedeOfrecer(Item item, int cantidad, CofreLogistico cofre) {
        return false; // No ofrece ítems
    }

    @Override
    public boolean puedeSolicitar(Item item, int cantidad, CofreLogistico cofre) {
        int cantidadActual = cofre.getInventario().getCantidad(item);
        int totalInventario = cofre.getInventario().getTotalItems();

        return cantidadActual < capacidadMaxima &&
                totalInventario + cantidad <= cofre.getCapacidadMaxima();
    }

    @Override
    public String getTipo() {
        return "Solicitud";
    }

    @Override
    public int getPrioridadSolicitud(Item item, CofreLogistico cofre) {
        return 0; // No aplica
    }
}
