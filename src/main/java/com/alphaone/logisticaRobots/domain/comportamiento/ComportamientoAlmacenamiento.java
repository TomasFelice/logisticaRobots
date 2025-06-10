package com.alphaone.logisticaRobots.domain.comportamiento;

import com.alphaone.logisticaRobots.domain.CofreLogistico;
import com.alphaone.logisticaRobots.domain.Item;

/**
 * Comportamiento que almacena excedentes
 */
public class ComportamientoAlmacenamiento implements ComportamientoCofre {
    @Override
    public boolean puedeOfrecer(Item item, int cantidad, CofreLogistico cofre) {
        return cofre.getInventario().tiene(item, cantidad);
    }

    @Override
    public boolean puedeSolicitar(Item item, int cantidad, CofreLogistico cofre) {
        int totalInventario = cofre.getInventario().getTotalItems();
        return totalInventario + cantidad <= cofre.getCapacidadMaxima();
    }

    @Override
    public int getPrioridadSolicitud(Item item, CofreLogistico cofre) {
        return 1; // Prioridad baja
    }

    @Override
    public String getTipo() {
        return "Almacenamiento";
    }
}
