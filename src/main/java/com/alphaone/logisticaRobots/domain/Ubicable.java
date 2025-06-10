package com.alphaone.logisticaRobots.domain;

import com.alphaone.logisticaRobots.domain.pathfinding.Punto;

public interface Ubicable {
    Punto getPosicion();
    void setPosicion(Punto posicion);
}
