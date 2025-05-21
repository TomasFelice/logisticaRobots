public class Coordenada {
    private final int x;
    private final int y;

    public Coordenada(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double distanciaEuclidea(Coordenada otra) {
        int dx = this.x - otra.x;
        int dy = this.y - otra.y;
        return Math.sqrt(dx * dx + dy * dy); // d = √(x2-x1)^2 + (y2-y1)^2
    }
}
