package com.alphaone.logisticaRobots.domain.comportamiento;

import com.alphaone.logisticaRobots.domain.strategy.CofreLogistico;
import com.alphaone.logisticaRobots.domain.strategy.Item;

/**
 * Comportamiento que solicita ítems con prioridad configurable
 */
public class ComportamientoSolicitud implements ComportamientoCofre {
    public enum Prioridad {
        ACTIVO(3), BUFFER(2), PASIVO(1);

        private final int valor;

        Prioridad(int valor) {
            this.valor = valor;
        }

        public int getValor() { return valor; }
    }

    private final Prioridad prioridad;
    private final int capacidadMaxima;

    public ComportamientoSolicitud(Prioridad prioridad, int capacidadMaxima) {
        this.prioridad = prioridad;
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
    public int getPrioridadSolicitud(Item item, CofreLogistico cofre) {
        return prioridad.getValor();
    }

    @Override
    public String getTipo() {
        return "Solicitud " + prioridad.name().toLowerCase();
    }
}
