package com.alphaone.logisticaRobots.domain;

import java.util.Objects;

/**
 * Reporesenta una coordenada (x,y) en el espacio 2D (grilla)
 */
public class Punto {
    private final int x;
    private final int y;

    public Punto(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }

    /**
     * Calcula la distancia euclídea entre este punto y otro
     * @param p Punto
     * @return double
     */
    public double distanciaHacia(Punto p) {
        return Math.sqrt(Math.pow(this.x - p.x, 2) + Math.pow(this.y - p.y, 2));
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Punto punto = (Punto) o;
        return Double.compare(punto.x, x) == 0 && Double.compare(punto.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
