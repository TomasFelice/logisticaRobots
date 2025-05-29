package com.alphaone.logisticaRobots.domain.pathfinding;

public class Ruta {

    private int id;
    private Punto puntoInicio;
    private Punto puntoFin;

    public Ruta(int id, Punto puntoInicio, Punto puntoFin) {
        this.id = id;
        this.puntoInicio = puntoInicio;
        this.puntoFin = puntoFin;
    }

    public Punto getPuntoInicio() {
        return puntoInicio;
    }

    public Punto getPuntoFin() {
        return puntoFin;
    }
}
