package com.alphaone.logisticaRobots.domain.comportamiento;

import com.alphaone.logisticaRobots.domain.CofreLogistico;
import com.alphaone.logisticaRobots.domain.Item;

/**
 * Comportamiento que almacena ítems para ser tomados
 */
public class ComportamientoProvisionPasiva implements ComportamientoCofre {
    @Override
    public boolean puedeOfrecer(Item item, int cantidad, CofreLogistico cofre) {
        return cofre.getInventario().tiene(item, cantidad);
    }

    @Override
    public boolean puedeSolicitar(Item item, int cantidad, CofreLogistico cofre) {
        return false; // No solicita ítems
    }

    @Override
    public int getPrioridadSolicitud(Item item, CofreLogistico cofre) {
        return 1; // Baja
    }

    @Override
    public String getTipo() {
        return "Provision Pasiva";
    }
}
