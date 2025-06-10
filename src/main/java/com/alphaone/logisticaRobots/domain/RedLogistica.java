package com.alphaone.logisticaRobots.domain;

import com.alphaone.logisticaRobots.domain.pathfinding.GrillaEspacial;
import com.alphaone.logisticaRobots.domain.pathfinding.Planificador;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RedLogistica { // es el universo donde se componen las cosas

    // TODO:
    //  Esto?? Por qué es un mapa y no un SET? Por ahora lo comento y lo transformo en Set
    // private final Map<Robopuerto,Integer> robopuertos; //el robopuerto dentro va a contener a los robots y los cofres
    private Set<Robopuerto> robopuertos;
    private Set<CofreLogistico> cofres;
    private Set<RobotLogistico> robotsLogisticos;
    private List<Pedido> pedidos;

    // TODO:
    //  Cómo podríamos tener más de una grilla espacial? Yo me imagino la grilla espacial
    //  como el mapa completo. No le veo sentido a tener más de una grilla.
    //  Lo comento y hago que sea una unica grilla espacial.
    // private final Map<GrillaEspacial,Integer> grillasEspaciales;
    private final GrillaEspacial grillaEspacial;

    // TODO:
    //  Si el planificador va aca, tenemos que refactorizar el Planificador
    //  Podemos tener este objeto planificador que es general de la RedLogistica
    //  y que luego, le pasemos una ruta y planifique el mejor camino. Para ello,
    //  tenemos que hacer que Planificador no tenga una lista de pedidos como atr.
    private final Planificador planificador; // el que va a implementar dijkstra y las planificaciones

    public RedLogistica(Set<Robopuerto> robopuertos, GrillaEspacial grillaEspacial, Set<CofreLogistico> cofres, Set<RobotLogistico> robotsLogisticos, List<Pedido> pedidos) {
        this.robopuertos = robopuertos;
        this.grillaEspacial = grillaEspacial;
        this.cofres = cofres;
        this.robotsLogisticos = robotsLogisticos;
        this.pedidos = pedidos;
        this.planificador = new Planificador(robopuertos, grillaEspacial, cofres, robotsLogisticos, pedidos);
    }

    public Set<Robopuerto> getRobopuertos() {
        return robopuertos;
    }

    public GrillaEspacial getGrillaEspacial() {
        return grillaEspacial;
    }

    public Set<CofreLogistico> getCofres() {
        return cofres;
    }

    public Set<RobotLogistico> getRobotsLogisticos() {
        return robotsLogisticos;
    }

    public List<Pedido> getPedidos() {
        return pedidos;
    }

    public void agregarRobopuerto(Robopuerto robopuerto) {
        this.robopuertos.add(robopuerto);
    }

    public void eliminarRobopuerto(Robopuerto robopuerto) {
        this.robopuertos.remove(robopuerto);
    }
    
    public void agregarCofre(CofreLogistico cofre) {
        this.cofres.add(cofre);
    }

    public void eliminarCofre(CofreLogistico cofre) {
        this.cofres.remove(cofre);
    }

    public void agregarRobot(RobotLogistico robot) {
        this.robotsLogisticos.add(robot);
    }

    public void eliminarRobot(RobotLogistico robot) {
        this.robotsLogisticos.remove(robot);
    }

    public void agregarPedido(Pedido pedido) {
        this.pedidos.add(pedido);
    }

    public void eliminarPedido(Pedido pedido) {
        this.pedidos.remove(pedido);
    }

    public boolean estaVacia() {
        return
            robopuertos.isEmpty()
            && cofres.isEmpty()
            && robotsLogisticos.isEmpty()
            && pedidos.isEmpty();
    }

    /**
     * Simula un ciclo de movimiento de todos los robots de la red.
     */
    public void simularCiclo() {

    }

    /**
     * Indica que ya cumplió con todos los pedidos y no tiene más
     * Movimientos pendientes
     *
     * @return boolean
     */
    public boolean haAlcanzadoEstadoEstable() {
        return true;
    }

    /**
     * Indica si el cofre es accesible o no
     *
     * @param cofre CofreLogistico
     * @return boolean
     */
    public boolean esCofreAccesible(CofreLogistico cofre) {

    }
}
