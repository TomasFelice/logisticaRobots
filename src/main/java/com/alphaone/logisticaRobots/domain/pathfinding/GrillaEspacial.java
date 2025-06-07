package com.alphaone.logisticaRobots.domain.pathfinding;

public class GrillaEspacial {

    private Punto origen;
    private double radio;

    public GrillaEspacial(Punto origen, double radio){
        this.origen = origen;
        this.radio = radio;
    }

    // Metodo que devuelve si un robot Logistico se encuentra dentro de la grilla espacial
    public boolean dentroDeGrilla(Punto posRobotLogistico){
        double dx = posRobotLogistico.getX() - this.origen.getX();
        double dy = posRobotLogistico.getY() - this.origen.getY();
        double distanciaCuadrada = dx*dx + dy*dy;

        return distanciaCuadrada <= this.radio * this.radio;
    }

    public Punto getOrigen() {
        return origen;
    }

    public double getRadio() {
        return radio;
    }
}
