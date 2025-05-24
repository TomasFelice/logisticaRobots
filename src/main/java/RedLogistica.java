import com.alphaone.logisticaRobots.domain.CofreLogistico;

import java.util.Map;

public class RedLogistica {

    private final Map<Integer, Robopuerto> robopuertos;
    private final Map<Integer, CofreLogistico> cofres;
    private final Map<Integer, RobotLogistico> robots;
    private final GrillaEspacial grilla;

    public RedLogistica(Map<Integer, Robopuerto> robopuertos, Map<Integer, CofreLogistico> cofres, Map<Integer, RobotLogistico> robots, GrillaEspacial grilla) {
        this.robopuertos = robopuertos;
        this.cofres = cofres;
        this.robots = robots;
        this.grilla = new GrillaEspacial();
    }
}
