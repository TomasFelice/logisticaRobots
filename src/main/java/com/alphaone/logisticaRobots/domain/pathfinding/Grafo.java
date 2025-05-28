package com.alphaone.logisticaRobots.domain.pathfinding;
import java.util.ArrayList;
import java.util.List;

public class Grafo {
    private List<Nodo> nodos;

    public Grafo() {
        this.nodos = new ArrayList<>();
    }

    public void agregarNodo(Nodo nodo) {
        nodos.add(nodo);
    }

    public List<Nodo> getNodos() {
        return nodos;
    }
}