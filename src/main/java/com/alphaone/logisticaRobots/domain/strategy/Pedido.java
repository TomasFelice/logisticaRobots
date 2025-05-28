package com.alphaone.logisticaRobots.domain.strategy;

import java.util.HashMap;
import java.util.Map;

public class Pedido {
    private int id;
    private CofreLogistico origen;
    private CofreLogistico destino;
    private Map<Item,Integer> itemsPedido; //representaría la cantidad y el item que se tiene como
    //private Ruta rutaOptima;

    public Pedido(int id, CofreLogistico origen, CofreLogistico destino) {
        this.id = id;
        this.origen = origen;
        this.destino = destino;
        this.itemsPedido = new HashMap<>();
    }

    //Getters:
    public int getId() {return id;}
    public CofreLogistico getOrigen() {return origen;}
    public CofreLogistico getDestino() {return destino;}
    public Map<Item, Integer> getItemsPedido() {return itemsPedido;}

    //Setters:
    public void setItemPedido(Item item, int cantidad) {this.itemsPedido.put(item, cantidad);}

}
