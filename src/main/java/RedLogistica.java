import com.alphaone.logisticaRobots.domain.pathfinding.GrillaEspacial;
import com.alphaone.logisticaRobots.domain.strategy.Pedido;

import java.util.Map;

public class RedLogistica { // es el universo donde se componen las cosas

    private final Map<Robopuerto,Integer> robopuertos; //el robopuerto dentro va a contener a los robots y los cofres
    //private final Planificador = new Planificador(); // el que va a implementar dijkstra y las planificaciones
    private final Map<GrillaEspacial,Integer> grillasEspaciales;


    public RedLogistica(Map<Robopuerto, Integer> robopuertos, Map<GrillaEspacial,Integer> grillasEspaciales) {
        this.robopuertos = robopuertos;
        this.grillasEspaciales = grillasEspaciales;
    }

    public void agregarRobopuerto(Robopuerto robopuerto) {

    }


}
