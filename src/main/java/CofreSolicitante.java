import java.util.Map;
import java.util.List;

public abstract class CofreSolicitante extends Cofre {
    protected Map<Item, Integer> itemsSolicitados;

    public CofreSolicitante(int id, Coordenada ubicacion, int capacidadItems) {
        super(id, ubicacion, capacidadItems);
    }

    public abstract void solicitar(List<Item> items);
}