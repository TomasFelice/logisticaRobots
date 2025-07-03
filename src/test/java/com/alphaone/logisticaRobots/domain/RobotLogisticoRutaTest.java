package com.alphaone.logisticaRobots.domain;

import com.alphaone.logisticaRobots.domain.pathfinding.Punto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Test para verificar que el cálculo de la ruta del robot funciona correctamente.
 * Verifica que:
 * 1. La ruta no contiene movimientos diagonales
 * 2. La ruta llega al destino correcto
 * 3. La ruta es la más corta posible usando movimientos ortogonales
 */
public class RobotLogisticoRutaTest {

    private RobotLogistico robot;
    private Robopuerto robopuertoBase;

    @BeforeEach
    void setUp() {
        // Crear un robopuerto base
        robopuertoBase = new Robopuerto("RP1", new Punto(0, 0), 10.0, 1);
        
        // Crear un robot en posición (0, 0)
        robot = new RobotLogistico(1, new Punto(0, 0), robopuertoBase, 100, 10);
    }

    @Test
    void testRutaSinPedidoActual() {
        // Cuando no hay pedido actual, la ruta debe estar vacía
        List<Punto> ruta = robot.getRutaActual();
        assertTrue(ruta.isEmpty(), "La ruta debe estar vacía cuando no hay pedido actual");
    }

    @Test
    void testRutaMovimientoOrtogonal() {
        // Crear un pedido simple para probar el movimiento
        Item item = new Item("TestItem", "TestItem");
        CofreLogistico origen = new CofreLogistico("C1", new Punto(2, 2), 10);
        CofreLogistico destino = new CofreLogistico("C2", new Punto(5, 5), 10);
        
        // Agregar items al cofre origen
        origen.getInventario().agregar(item, 5);
        
        Pedido pedido = new Pedido(item, 2, origen, destino, Pedido.PrioridadPedido.MEDIA);
        robot.agregarPedido(pedido);
        
        // Procesar el pedido para que se convierta en pedido actual
        ///robot.procesarSiguientePedido();
        
        // Obtener la ruta
        List<Punto> ruta = robot.getRutaActual();
        
        // Verificar que la ruta no esté vacía
        assertFalse(ruta.isEmpty(), "La ruta no debe estar vacía cuando hay un pedido actual");
        
        // Verificar que no hay movimientos diagonales
        for (int i = 1; i < ruta.size(); i++) {
            Punto anterior = ruta.get(i - 1);
            Punto actual = ruta.get(i);
            
            int dx = Math.abs(actual.getX() - anterior.getX());
            int dy = Math.abs(actual.getY() - anterior.getY());
            
            // Verificar que el movimiento es ortogonal (solo una dirección a la vez)
            assertTrue((dx == 1 && dy == 0) || (dx == 0 && dy == 1), 
                "Movimiento diagonal detectado entre " + anterior + " y " + actual);
        }
        
        // Verificar que la ruta llega al destino correcto (origen en este caso)
        Punto ultimoPunto = ruta.get(ruta.size() - 1);
        assertEquals(origen.getPosicion(), ultimoPunto, 
            "La ruta debe llegar al cofre de origen");
    }

    @Test
    void testRutaConCargaHaciaDestino() {
        // Crear un pedido y simular que ya se cargó el item
        Item item = new Item("TestItem", "TestItem");
        CofreLogistico origen = new CofreLogistico("C1", new Punto(2, 2), 10);
        CofreLogistico destino = new CofreLogistico("C2", new Punto(5, 5), 10);
        
        Pedido pedido = new Pedido(item, 2, origen, destino, Pedido.PrioridadPedido.MEDIA);
        robot.agregarPedido(pedido);
        
        // Procesar el pedido
       // robot.procesarSiguientePedido();
        
        // Simular que el robot ya cargó el item (moverlo al origen y agregar carga)
        robot.setPosicion(origen.getPosicion());
        robot.getCargaActual().put(item, 2);
        
        // Obtener la ruta
        List<Punto> ruta = robot.getRutaActual();
        
        // Verificar que la ruta llega al destino correcto
        assertFalse(ruta.isEmpty(), "La ruta no debe estar vacía");
        Punto ultimoPunto = ruta.get(ruta.size() - 1);
        assertEquals(destino.getPosicion(), ultimoPunto, 
            "La ruta debe llegar al cofre de destino cuando ya se cargó el item");
    }

    @Test
    void testRutaOptima() {
        // Crear un pedido con destino en diagonal
        Item item = new Item("TestItem", "TestItem");
        CofreLogistico origen = new CofreLogistico("C1", new Punto(3, 3), 10);
        CofreLogistico destino = new CofreLogistico("C2", new Punto(6, 6), 10);
        
        Pedido pedido = new Pedido(item, 2, origen, destino, Pedido.PrioridadPedido.MEDIA);
        robot.agregarPedido(pedido);
        
        // Procesar el pedido
       // robot.procesarSiguientePedido();
        
        // Obtener la ruta
        List<Punto> ruta = robot.getRutaActual();
        
        // Verificar que la ruta es la más corta posible
        // De (0,0) a (3,3) debería ser 6 pasos (3 horizontal + 3 vertical)
        int pasosEsperados = 6; // 3 pasos en X + 3 pasos en Y
        assertEquals(pasosEsperados, ruta.size(), 
            "La ruta debe tener el número mínimo de pasos ortogonales");
    }

    @Test
    void testRutaMismoPunto() {
        // Crear un pedido donde el robot ya está en el destino
        Item item = new Item("TestItem", "TestItem");
        CofreLogistico origen = new CofreLogistico("C1", new Punto(0, 0), 10);
        CofreLogistico destino = new CofreLogistico("C2", new Punto(5, 5), 10);
        
        Pedido pedido = new Pedido(item, 2, origen, destino, Pedido.PrioridadPedido.MEDIA);
        robot.agregarPedido(pedido);
        
        // Procesar el pedido
       // robot.procesarSiguientePedido();
        
        // Obtener la ruta
        List<Punto> ruta = robot.getRutaActual();
        
        // Verificar que la ruta no esté vacía (debe ir al origen)
        assertFalse(ruta.isEmpty(), "La ruta debe existir para ir al origen");
        
        // Verificar que el primer punto es la posición actual
        assertEquals(robot.getPosicion(), ruta.get(0), 
            "El primer punto de la ruta debe ser la posición actual del robot");
    }
} 