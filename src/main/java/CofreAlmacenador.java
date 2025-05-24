import java.util.List;
import java.util.Map;

public abstract  class CofreAlmacenador extends Cofre{
    protected Map<Item, Integer> itemsAlmacenados;

    public CofreAlmacenador(int id, Coordenada ubicacion, int capacidadItems) {
        super(id, ubicacion, capacidadItems);
    }

    public abstract void almacenar(List<Item> items);
}
