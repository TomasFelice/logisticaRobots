package com.alphaone.logisticaRobots.domain.comportamiento;

import com.alphaone.logisticaRobots.domain.CofreLogistico;
import com.alphaone.logisticaRobots.domain.Item;

/**
 * Interfaz para los diferentes comportamientos de los cofres (Strategy)
 */
public interface ComportamientoCofre {

    /**
     * Indica si el cofre puede ofrecer el item
     */
    boolean puedeOfrecer(Item item, int cantidad, CofreLogistico cofre);

    /**
     * Indica si el cofre puede solicitar el item
     */
    boolean puedeSolicitar(Item item, int cantidad, CofreLogistico cofre);

    /**
     * Obtiene la prioridad para solicitar el item (mayor número = mayor prioridad)
     */
    int getPrioridadSolicitud(Item item, CofreLogistico cofre);

    /**
     * Describe el tipo de comportamiento
     */
    String getTipo();
}
