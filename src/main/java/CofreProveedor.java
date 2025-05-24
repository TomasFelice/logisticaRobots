import java.util.Map;
import java.util.List;

public abstract class CofreProveedor extends Cofre {
    protected Map<Item, Integer> itemsOfrecidos;

    public CofreProveedor(int id, Coordenada ubicacion, int capacidadItems) {
        super(id, ubicacion, capacidadItems);
    }

    public abstract void ofrecer(List<Item> items);
}