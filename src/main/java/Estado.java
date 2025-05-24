public abstract class Estado { // me parece que el módelo state no va. Podríamos usar un enum por cada estado para que sea mucho más sencillo

    protected String nombre;

    public Estado(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    protected abstract void cambiarEstado();
}
