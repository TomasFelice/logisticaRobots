public abstract class Estado {

    protected String nombre;

    public Estado(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    protected abstract void cambiarEstado();
}
