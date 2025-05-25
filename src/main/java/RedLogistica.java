import com.alphaone.logisticaRobots.domain.CofreLogistico;

import java.util.Map;

public class RedLogistica { // es el universo donde se componen las cosas

    private final Map<Integer, Robopuerto> robopuertos; //el robopuerto dentro va a contener a los robots y los cofres
    private final Planificador planificador; // el que va a implementar dijkstra y las planificaciones
    private final Map<Integer,Pedido> pedidos;
    private final Map<Integer, GrillaEspacial> grillasEspaciales;


    public RedLogistica(Map<Integer, Robopuerto> robopuertos, Planificador planificador, Map<Integer, Pedido> pedidos, Map<Integer, GrillaEspacial> grillasEspaciales) {
        this.robopuertos = robopuertos;
        this.planificador = planificador;
        this.pedidos = pedidos;
        this.grillasEspaciales = grillasEspaciales;
    }

    public void agregarRobopuerto(Robopuerto robopuerto) {

    }

    public void agregarPedido(Pedido pedido) {

    }
}
