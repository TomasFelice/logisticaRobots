package com.alphaone.logisticaRobots.domain.pathfinding;

public class GrillaEspacial {

    private final Punto origen;
    private final int ancho;
    private final int alto;

    public GrillaEspacial(Punto origen, int ancho, int alto) {
        this.origen = origen;
        this.ancho = ancho;
        this.alto = alto;
    }

    // Método que devuelve si un punto está dentro de la grilla rectangular
    public boolean dentroDeGrilla(Punto punto) {
        int x = punto.getX();
        int y = punto.getY();
        int x0 = origen.getX();
        int y0 = origen.getY();
        return x >= x0 && x < x0 + ancho && y >= y0 && y < y0 + alto;
    }

    public Punto getOrigen() {
        return origen;
    }

    public int getAncho() {
        return ancho;
    }

    public int getAlto() {
        return alto;
    }
}
