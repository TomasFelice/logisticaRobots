package com.alphaone.logisticaRobots.domain.entities;

public class Punto {
    private int x;
    private int y;

    public Punto(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public double distanciaEuclidea(Punto p) {
        return Math.sqrt(Math.pow(this.x - p.x, 2) + Math.pow(this.y - p.y, 2));
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
