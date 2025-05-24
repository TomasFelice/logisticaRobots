import java.util.HashMap;
import java.util.Map;

public abstract class Cofre {
    protected int id;
    protected Coordenada ubicacion;
    protected Map<Integer, Item> contenido;  // ID -> Item
    protected int capacidadItems;

    public Cofre(int id, Coordenada ubicacion, int capacidadItems) {
        this.id = id;
        this.ubicacion = ubicacion;
        this.capacidadItems = capacidadItems;
        this.contenido = new HashMap<>();
    }

    protected void agregarItem(Integer itemId, Item item) {
        if (contenido.size() == capacidadItems) {
            throw new IllegalStateException("Capacidad excedida. Máximo: " + capacidadItems);
        }
        contenido.put(itemId, item);
    }

    protected void removerItem(Integer itemId) {
        if (!contenido.containsKey(itemId)) {
            throw new IllegalArgumentException("El item no existe en el cofre");
        }
        contenido.remove(itemId);
    }
}