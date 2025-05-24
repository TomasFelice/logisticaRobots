package com.alphaone.logisticaRobots.domain.comportamiento;

import com.alphaone.logisticaRobots.domain.CofreLogistico;
import com.alphaone.logisticaRobots.domain.Item;

public class ComportamientoIntermedioBuffer implements ComportamientoCofre {
    private final int umbralMinimo;
    private final int umbralMaximo;

    public ComportamientoIntermedioBuffer(int umbralMinimo, int umbralMaximo) {
        this.umbralMinimo = umbralMinimo;
        this.umbralMaximo = umbralMaximo;
    }

    @Override
    public boolean puedeOfrecer(Item item, int cantidad, CofreLogistico cofre) {
        int cantidadActual = cofre.getInventario().getCantidad(item);
        return cantidadActual > umbralMinimo && cantidadActual >= cantidad;
    }

    @Override
    public boolean puedeSolicitar(Item item, int cantidad, CofreLogistico cofre) {
        int cantidadActual = cofre.getInventario().getCantidad(item);
        int totalInventario = cofre.getInventario().getTotalItems();

        return cantidadActual < umbralMaximo &&
                totalInventario + cantidad <= cofre.getCapacidadMaxima();
    }

    @Override
    public int getPrioridadSolicitud(Item item, CofreLogistico cofre) {
        return 2; // Prioridad media
    }

    @Override
    public String getTipo() {
        return "Buffer Intermedio";
    }
}
