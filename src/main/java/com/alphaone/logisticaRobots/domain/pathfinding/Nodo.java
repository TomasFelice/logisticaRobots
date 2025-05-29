package com.alphaone.logisticaRobots.domain.pathfinding;

import java.util.ArrayList;
import java.util.List;

public class Nodo {
    private Punto nodo; // Puede ser Cofre o Robopuerto
    private List<Arista> aristas;

    public Nodo(Punto elemento) {
        this.nodo = elemento;
        this.aristas = new ArrayList<>();
    }

    public void agregarArista(Arista arista) {
        this.aristas.add(arista);
    }

    public List<Arista> getAristas() {
        return aristas;
    }

    public Punto getNodo() {
        return nodo;
    }
}
