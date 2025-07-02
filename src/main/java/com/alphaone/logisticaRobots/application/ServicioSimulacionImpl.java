package com.alphaone.logisticaRobots.application;

import com.alphaone.logisticaRobots.application.dto.*;
import com.alphaone.logisticaRobots.domain.*;
import com.alphaone.logisticaRobots.domain.comportamiento.*;
import com.alphaone.logisticaRobots.domain.pathfinding.GrillaEspacial;
import com.alphaone.logisticaRobots.domain.pathfinding.Punto;
import com.alphaone.logisticaRobots.infrastructure.config.LogisticaRobotsConfigLoader;
import com.alphaone.logisticaRobots.ui.ObservadorEstadoSimulacion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Implementación del servicio de simulación que gestiona la lógica y el estado del sistema.
 */
public class ServicioSimulacionImpl implements ServicioSimulacion {
    private static final Logger logger = LoggerFactory.getLogger(ServicioSimulacionImpl.class);

    // Sistema del dominio
    private RedLogistica redLogistica;

    // Configuración
    private File archivoConfigActual;
    private int velocidadSimulacion = 1000; // milisegundos entre ciclos

    // com.alphaone.logisticaRobots.domain.Estado de la simulación
    private boolean enEjecucion = false;
    private int cicloActual = 0;
    private String estadoGeneral = "NO_INICIADO";
    private String mensajeEstado = "Sistema no iniciado";

    // Dimensiones de la grilla (configurables desde archivo)
    private int anchoGrilla = 50;
    private int altoGrilla = 50;

    // Scheduler para ejecución automática
    private ScheduledExecutorService scheduler;

    // Observadores
    private final List<ObservadorEstadoSimulacion> observadores = new CopyOnWriteArrayList<>();

    // Loader del config
    private final LogisticaRobotsConfigLoader configLoader;

    // Constructor
    public ServicioSimulacionImpl() {
        // Inicializar objetos necesarios
        this.redLogistica = new RedLogistica();
        this.configLoader = new LogisticaRobotsConfigLoader();
    }

    @Override
    public void iniciarSimulacion() {
        if (redLogistica == null || redLogistica.estaVacia()) {
            mensajeEstado = "No se puede iniciar: no hay configuración cargada";
            notificarObservadores();
            return;
        }

        if (!enEjecucion) {
            enEjecucion = true;
            estadoGeneral = "INICIADA";
            mensajeEstado = "Simulación iniciada";

            // Iniciar ejecución automática en un hilo separado
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(
                    this::cicloSimulacionSeguro,
                    0,
                    velocidadSimulacion,
                    TimeUnit.MILLISECONDS
            );

            notificarObservadores();
        }
    }

    @Override
    public void pausarSimulacion() {
        if (enEjecucion && scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
            enEjecucion = false;
            estadoGeneral = "PAUSADA";
            mensajeEstado = "Simulación pausada";
            notificarObservadores();
        }
    }

    @Override
    public void resetearSimulacion() {
        // Detener la simulación si está en marcha
        if (enEjecucion) {
            pausarSimulacion();
        }

        // Resetear variables
        cicloActual = 0;
        estadoGeneral = "NO_INICIADO";

        // Si hay un archivo de configuración cargado, lo recargamos
        if (archivoConfigActual != null) {
            try {
                cargarConfiguracion(archivoConfigActual);
                mensajeEstado = "Simulación reseteada y configuración recargada";
            } catch (Exception e) {
                mensajeEstado = "Error al resetear: " + e.getMessage();
                logger.error("Error al resetear la simulación", e);
            }
        } else {
            redLogistica = new RedLogistica(); // O como se reinicie
            mensajeEstado = "Simulación reseteada";
        }

        notificarObservadores();
    }

    @Override
    public void avanzarCicloSimulacion() {
        if (redLogistica == null || redLogistica.estaVacia()) {
            mensajeEstado = "No se puede avanzar: no hay configuración cargada";
            notificarObservadores();
            return;
        }

        if (!enEjecucion) { // Solo avanzamos manualmente si no está en ejecución automática
            cicloSimulacionSeguro();
        }
    }

    // Metodo que ejecuta un ciclo de simulación con manejo de excepciones
    private void cicloSimulacionSeguro() {
        try {
            ejecutarCicloSimulacion();
            notificarObservadores();
        } catch (Exception e) {
            pausarSimulacion(); // Detener si hay error
            estadoGeneral = "ERROR";
            mensajeEstado = "Error en ciclo: " + e.getMessage();
            logger.error("Error durante el ciclo de simulación", e);
            notificarObservadores();
        }
    }

    // Implementación real del ciclo
    private void ejecutarCicloSimulacion() {
        // Aquí iría la llamada al metodo simularCiclo() de com.alphaone.logisticaRobots.domain.RedLogistica
        redLogistica.simularCiclo();

        cicloActual++;

        // Verificar si se alcanzó un estado estable
        if (redLogistica.haAlcanzadoEstadoEstable()) {
            pausarSimulacion();
            estadoGeneral = "FINALIZADA_ESTABLE";
            mensajeEstado = "Simulación finalizada: estado estable alcanzado";
        } else {
            estadoGeneral = "CORRIENDO";
            mensajeEstado = "Ciclo " + cicloActual + " completado";
        }
    }

    @Override
    public void cargarConfiguracion(File archivoConfig) throws Exception {
        Objects.requireNonNull(archivoConfig, "El archivo no puede ser null");

        if (!archivoConfig.exists() || !archivoConfig.canRead()) {
            throw new IllegalArgumentException("Archivo no existe o no se puede leer: " + archivoConfig.getPath());
        }

        try {
            // Usar el configLoader inyectado con el archivo específico
            ConfiguracionSimulacionDTO configuracion = configLoader.cargarConfiguracion(archivoConfig);

            // Procesar la configuración
            procesarConfiguracion(configuracion, archivoConfig);

        } catch (IOException e) {
            logger.error("Error de E/S al cargar la configuración desde {}", archivoConfig.getPath(), e);
            throw new Exception("Error de E/S al cargar configuración: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error al cargar la configuración desde {}", archivoConfig.getPath(), e);
            throw new Exception("Error al cargar configuración: " + e.getMessage(), e);
        }
    }

    // Método auxiliar para procesamiento de configuración (extrae lógica común)
    private void procesarConfiguracion(ConfiguracionSimulacionDTO configuracion, File archivoConfig) {
        // Configurar dimensiones de la grilla
        if (configuracion.dimensionGrilla() != null) {
            this.anchoGrilla = configuracion.dimensionGrilla().ancho();
            this.altoGrilla = configuracion.dimensionGrilla().alto();
        }
        GrillaEspacial grilla = new GrillaEspacial(new Punto(0, 0), anchoGrilla, altoGrilla);

        // Crear colecciones temporales para cargar todas las entidades
        Set<Robopuerto> robopuertos = new HashSet<>();
        Set<CofreLogistico> cofres = new HashSet<>();
        Set<RobotLogistico> robots = new HashSet<>();
        List<Pedido> pedidos = new ArrayList<>();

        // Cargar robopuertos
        if (configuracion.robopuertos() != null) {
            for (RobopuertoDTO rpDTO : configuracion.robopuertos()) {
                Robopuerto robopuerto = new Robopuerto(
                    rpDTO.id(),
                    new Punto(rpDTO.posicion().x(), rpDTO.posicion().y()),
                    rpDTO.alcance(),
                    rpDTO.tasaRecarga()
                );
                robopuertos.add(robopuerto);
            }
        }

        // Cargar cofres
        if (configuracion.cofres() != null) {
            for (CofreDTO cofreDTO : configuracion.cofres()) {
                CofreLogistico cofre = crearCofreSegunTipo(
                    cofreDTO.id(),
                    new Punto(cofreDTO.posicion().x(), cofreDTO.posicion().y()),
                    cofreDTO.capacidadMaxima(),
                    cofreDTO.comportamientoDefecto(),
                    cofreDTO.tiposComportamientoPorItem()
                );
                
                // Cargar inventario inicial si existe
                if (cofreDTO.inventario() != null) {
                    cofreDTO.inventario().entrySet().forEach(entry -> {
                        Item item = new Item(entry.getKey(), entry.getKey());
                        cofre.agregarItem(item, entry.getValue());
                    });
                }
                
                cofres.add(cofre);
            }
        }

        // Cargar robots
        if (configuracion.robots() != null) {
            for (RobotDTO robotDTO : configuracion.robots()) {
                // Buscar el robopuerto base del robot
                Robopuerto robopuertoBase = null;
                for (Robopuerto rp : robopuertos) {
                    if (rp.getPosicion().equals(new Punto(robotDTO.posicion().x(), robotDTO.posicion().y()))) {
                        robopuertoBase = rp;
                        break;
                    }
                }
                if (robopuertoBase == null && !robopuertos.isEmpty()) {
                    robopuertoBase = robopuertos.iterator().next(); // Usar el primero como fallback
                }

                RobotLogistico robot = new RobotLogistico(
                    Integer.parseInt(robotDTO.id()),
                    new Punto(robotDTO.posicion().x(), robotDTO.posicion().y()),
                    robopuertoBase,
                    (int) robotDTO.bateriaMaxima(),
                    robotDTO.capacidadCarga()
                );
                robots.add(robot);
            }
        }

        // Cargar pedidos desde configuración JSON
        cargarPedidosDesdeConfiguracion(configuracion, cofres, pedidos);

        // Crear la red logística con todas las entidades cargadas
        this.redLogistica = new RedLogistica(robopuertos, grilla, cofres, robots, pedidos);

        // Establecer velocidad de simulación
        velocidadSimulacion = configuracion.velocidadSimulacion() > 0 ?
                configuracion.velocidadSimulacion() : 1000;

        // Actualizar estado
        this.archivoConfigActual = archivoConfig;
        cicloActual = 0;
        estadoGeneral = "NO_INICIADO";
        mensajeEstado = "Configuración cargada exitosamente";

        notificarObservadores();
        logger.info("Configuración cargada desde {}", archivoConfig.getPath());
    }

    // Método auxiliar para cargar pedidos desde la configuración JSON
    private void cargarPedidosDesdeConfiguracion(ConfiguracionSimulacionDTO configuracion, Set<CofreLogistico> cofres, List<Pedido> pedidosGenerados) {
        if (configuracion.cofres() == null || configuracion.cofres().isEmpty()) {
            return;
        }

        pedidosGenerados.clear();

        // Crear un mapa de cofres por ID para facilitar la búsqueda
        Map<String, CofreLogistico> cofresPorId = cofres.stream()
                .collect(Collectors.toMap(CofreLogistico::getId, c -> c));

        // Cargar pedidos del JSON
        if (configuracion.pedidos() != null && !configuracion.pedidos().isEmpty()) {
            for (PedidoDTO pedidoDTO : configuracion.pedidos()) {
                // Buscar el cofre de destino
                CofreLogistico cofreDestino = cofresPorId.get(pedidoDTO.cofreDestinoId());
                if (cofreDestino == null) {
                    logger.warn("No se encontró cofre destino con ID: {}", pedidoDTO.cofreDestinoId());
                    continue;
                }

                // Crear el item para validaciones
                Item item = new Item(pedidoDTO.itemNombre(), pedidoDTO.itemNombre());

                // Validar que el cofre destino pueda solicitar el item
                if (!cofreDestino.puedeSolicitar(item, pedidoDTO.cantidad())) {
                    logger.warn("El cofre destino {} no puede solicitar {} unidades de {}. Comportamiento: {}",
                            cofreDestino.getId(), pedidoDTO.cantidad(), item.getNombre(), 
                            cofreDestino.getTipoComportamiento(item));
                    continue;
                }

                // Buscar dinámicamente el cofre origen que pueda ofrecer el item
                CofreLogistico cofreOrigen = encontrarCofreOrigenParaItem(item, pedidoDTO.cantidad(), cofres);
                if (cofreOrigen == null) {
                    logger.warn("No se encontró cofre origen para item: {}", pedidoDTO.itemNombre());
                    continue;
                }

                // Validar que origen y destino no sean el mismo cofre
                if (cofreOrigen.equals(cofreDestino)) {
                    logger.warn("No se puede crear pedido del cofre {} a sí mismo", cofreOrigen.getId());
                    continue;
                }

                // Determinar prioridad
                Pedido.PrioridadPedido prioridad = parsearPrioridad(pedidoDTO.prioridad());

                // Crear el pedido
                Pedido pedido = new Pedido(
                        item,
                        pedidoDTO.cantidad(),
                        cofreOrigen,
                        cofreDestino,
                        prioridad
                );

                pedidosGenerados.add(pedido);
                logger.debug("Pedido cargado desde JSON: {} de {} desde {} hasta {}. Prioridad {}",
                        pedidoDTO.cantidad(),
                        item.getNombre(),
                        cofreOrigen.getId(),
                        cofreDestino.getId(),
                        prioridad);
            }

            logger.info("Se cargaron {} pedidos desde la configuración JSON", pedidosGenerados.size());
        } else {
            logger.info("No se encontraron pedidos en la configuración JSON");
        }
    }

    /**
     * Encuentra un cofre origen que pueda ofrecer el item especificado
     */
    private CofreLogistico encontrarCofreOrigenParaItem(Item item, int cantidad, Set<CofreLogistico> cofres) {
        return cofres.stream()
                .filter(cofre -> cofre.puedeOfrecer(item, cantidad))
                .findFirst()
                .orElse(null);
    }

    /**
     * Parsea la prioridad desde string a enum
     */
    private Pedido.PrioridadPedido parsearPrioridad(String prioridadStr) {
        if (prioridadStr == null) return Pedido.PrioridadPedido.MEDIA;
        
        return switch (prioridadStr.toUpperCase()) {
            case "ALTA" -> Pedido.PrioridadPedido.ALTA;
            case "MEDIA" -> Pedido.PrioridadPedido.MEDIA;
            case "BAJA" -> Pedido.PrioridadPedido.BAJA;
            case "NO_APLICA" -> Pedido.PrioridadPedido.NO_APLICA;
            default -> Pedido.PrioridadPedido.MEDIA;
        };
    }



    private static Pedido.PrioridadPedido getPrioridadPedido(int prioridadValor) {
        Pedido.PrioridadPedido prioridad;

        // Mapear el valor de prioridad al enum PrioridadPedido
        if (prioridadValor == 3) {
            prioridad = Pedido.PrioridadPedido.ALTA;
        } else if (prioridadValor == 2) {
            prioridad = Pedido.PrioridadPedido.MEDIA;
        } else if (prioridadValor == 1) {
            prioridad = Pedido.PrioridadPedido.BAJA;
        } else {
            // Valor por defecto si no se puede determinar la prioridad
            prioridad = Pedido.PrioridadPedido.MEDIA;
        }
        return prioridad;
    }

    /**
     * Determina la prioridad del pedido basada en el tipo de comportamiento del cofre
     * Orden de prioridad: proveedores activos, búfer, pasivos
     */
    private Pedido.PrioridadPedido determinarPrioridadPorComportamiento(CofreLogistico cofre, Item item) {
        String tipo = cofre.getTipoComportamiento(item);

        // Convertir a minúsculas para hacer la comparación insensible a mayúsculas/minúsculas
        String tipoLower = tipo.toLowerCase();

        // Determinar prioridad según el tipo de comportamiento
        if (tipoLower.contains("activa") || tipoLower.contains("provision activa")) {
            return Pedido.PrioridadPedido.ALTA; // Proveedores activos tienen la mayor prioridad
        } else if (tipoLower.contains("buffer") || tipoLower.contains("intermedio")) {
            return Pedido.PrioridadPedido.MEDIA; // Búfer tienen prioridad media
        } else if (tipoLower.contains("pasiva") || tipoLower.contains("provision pasiva")) {
            return Pedido.PrioridadPedido.BAJA; // Proveedores pasivos tienen la menor prioridad
        } else if (tipoLower.contains("solicitud")) {
            return Pedido.PrioridadPedido.NO_APLICA; // Si es de tipo solicitud no tiene prioridad porque no realiza pedidos.
        } else {
            return Pedido.PrioridadPedido.MEDIA; // Por defecto, prioridad media
        }
    }



    private CofreLogistico crearCofreSegunTipo(String id, Punto posicion, int capacidad,
                                               String comportamientoPorDefecto,
                                               Map<String, String> comportamientosPorItem) {

        CofreLogistico cofre = CofreFactory.crearCofre(id, posicion, capacidad, comportamientoPorDefecto);

        if (comportamientosPorItem != null) {
            for (Map.Entry<String, String> entry : comportamientosPorItem.entrySet()) {
                Item item = new Item(entry.getKey(), entry.getKey());
                // Parsear la configuración del comportamiento para obtener parámetros adicionales
                ComportamientoCofre comportamiento = ComportamientoFactory.crearConParametros(
                        entry.getValue(),
                        capacidad // Pasar la capacidad del cofre como parámetro por defecto
                );
                cofre.setComportamiento(item, comportamiento);
            }
        }

        return cofre;
    }

    @Override
    public EstadoSimulacionDTO getEstadoActualSimulacion() {
        if (redLogistica == null || redLogistica.estaVacia()) {
            return new EstadoSimulacionDTO(
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new DimensionGrillaDTO(anchoGrilla, altoGrilla),
                    estadoGeneral,
                    cicloActual,
                    mensajeEstado
            );
        }

        // Convertir las entidades del dominio a DTOs
        List<RobotDTO> robotDTOs = convertirRobotsADTOs();
        List<CofreDTO> cofreDTOs = convertirCofresADTOs();
        List<RobopuertoDTO> robopuertoDTOs = convertirRobopuertosADTOs();

        return new EstadoSimulacionDTO(
                robotDTOs,
                cofreDTOs,
                robopuertoDTOs,
                new DimensionGrillaDTO(anchoGrilla, altoGrilla),
                estadoGeneral,
                cicloActual,
                mensajeEstado
        );
    }

    private List<RobotDTO> convertirRobotsADTOs() {
        List<RobotDTO> resultado = new ArrayList<>();

        for (RobotLogistico robot : redLogistica.getRobotsLogisticos()) {
            Punto posicion = robot.getPosicion();
            PuntoDTO posicionDTO = new PuntoDTO(posicion.getX(), posicion.getY());

            // Convertir carga a Map<String, Integer>
            Map<String, Integer> itemsEnCarga = new HashMap<>();
            for (Map.Entry<Item, Integer> entry : robot.getCargaActual().entrySet()) {
                itemsEnCarga.put(entry.getKey().getNombre(), entry.getValue());
            }


            // Convertir la ruta si existe
            List<PuntoDTO> rutaDTO = null;
            List<Punto> ruta = robot.getRutaActual();
            if (ruta != null && !ruta.isEmpty()) {
                rutaDTO = ruta.stream()
                        .map(p -> new PuntoDTO(p.getX(), p.getY()))
                        .toList();
            }

            // Calcular la carga actual total
            int cargaActualTotal = itemsEnCarga.values().stream().mapToInt(Integer::intValue).sum();

            RobotDTO robotDTO = new RobotDTO(
                    String.valueOf(robot.getId()),
                    posicionDTO,
                    robot.getBateriaActual(),
                    robot.getBateriaMaxima(),
                    cargaActualTotal,
                    robot.getCapacidadCarga(),
                    itemsEnCarga,
                    robot.getEstado().toString(),
                    rutaDTO
            );

            resultado.add(robotDTO);
        }

        return resultado;
    }

    private List<CofreDTO> convertirCofresADTOs() {
        List<CofreDTO> resultado = new ArrayList<>();

        for (CofreLogistico cofre : redLogistica.getCofres()) {
            Punto posicion = cofre.getPosicion();
            PuntoDTO posicionDTO = new PuntoDTO(posicion.getX(), posicion.getY());

            // Convertir inventario a Map<String, Integer>
            Map<String, Integer> inventarioDTO = new HashMap<>();
            for (Map.Entry<Item, Integer> entry : cofre.getInventario().getTodos().entrySet()) {
                inventarioDTO.put(entry.getKey().getNombre(), entry.getValue());
            }

            // Convertir comportamientos a Map<String, String>
            Map<String, String> comportamientosDTO = new HashMap<>();
            for (Item item : cofre.getInventario().getItems()) {
                comportamientosDTO.put(item.getNombre(), cofre.getTipoComportamiento(item));
            }

            CofreDTO cofreDTO = new CofreDTO(
                    cofre.getId(),
                    posicionDTO,
                    inventarioDTO,
                    cofre.getInventario().getTotalItems(),
                    cofre.getCapacidadMaxima(),
                    comportamientosDTO,
                    cofre.getTipoComportamientoDefecto(),
                    redLogistica.esCofreAccesible(cofre) // Metodo que indica si está en zona de cobertura
            );

            resultado.add(cofreDTO);
        }

        return resultado;
    }

    private List<RobopuertoDTO> convertirRobopuertosADTOs() {
        List<RobopuertoDTO> resultado = new ArrayList<>();

        for (Robopuerto rp : redLogistica.getRobopuertos()) {
            Punto posicion = rp.getPosicion();
            PuntoDTO posicionDTO = new PuntoDTO(posicion.getX(), posicion.getY());

            // Obtener los IDs de cofres cubiertos
            List<String> idsCofresCubiertos = redLogistica.getCofresEnCobertura(rp).stream()
                    .map(CofreLogistico::getId)
                    .toList();

            RobopuertoDTO rpDTO = new RobopuertoDTO(
                    rp.getId(),
                    posicionDTO,
                    rp.getAlcance(),
                    rp.getTasaRecarga(),
                    idsCofresCubiertos
            );

            resultado.add(rpDTO);
        }

        return resultado;
    }

    @Override
    public DetallesEntidadDTO getDetallesEntidad(String tipoEntidad, String idEntidad) {
        if (tipoEntidad == null || idEntidad == null) return null;
        switch (tipoEntidad) {
            case "ROBOT" -> {
                RobotLogistico robot = redLogistica.buscarRobotPorId(idEntidad);
                if (robot != null) {
                    RobotDTO robotDTO = obtenerRobotDTO(robot);
                    return new DetallesEntidadDTO(robotDTO);
                }
            }
            case "COFRE" -> {
                CofreLogistico cofre = redLogistica.buscarCofrePorId(idEntidad);
                if (cofre != null) {
                    CofreDTO cofreDTO = obtenerCofreDTO(cofre);
                    return new DetallesEntidadDTO(cofreDTO);
                }
            }
            case "ROBOPUERTO" -> {
                Robopuerto robopuerto = redLogistica.buscarRobopuertoPorId(idEntidad);
                if (robopuerto != null) {
                    RobopuertoDTO rpDTO = obtenerRobopuertoDTO(robopuerto);
                    return new DetallesEntidadDTO(rpDTO);
                }
            }
        }
        return null; // No se encontró la entidad
    }

    // Métodos auxiliares para convertir entidades individuales a DTOs

    private RobotDTO obtenerRobotDTO(RobotLogistico robot) {
        return convertirRobotsADTOs().stream()
                .filter(r -> r.id().equals(String.valueOf(robot.getId())))
                .findFirst()
                .orElse(null);
    }

    private CofreDTO obtenerCofreDTO(CofreLogistico cofre) {
        // ... (implementación similar)
        return convertirCofresADTOs().stream()
                .filter(c -> c.id().equals(cofre.getId()))
                .findFirst()
                .orElse(null);
    }

    private RobopuertoDTO obtenerRobopuertoDTO(Robopuerto robopuerto) {
        // ... (implementación similar)
        return convertirRobopuertosADTOs().stream()
                .filter(rp -> rp.id().equals(robopuerto.getId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void registrarObservador(ObservadorEstadoSimulacion observador) {
        if (observador != null && !observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    @Override
    public void removerObservador(ObservadorEstadoSimulacion observador) {
        observadores.remove(observador);
    }

    private void notificarObservadores() {
        EstadoSimulacionDTO estadoActual = getEstadoActualSimulacion();
        for (ObservadorEstadoSimulacion observador : observadores) {
            observador.actualizarEstado(estadoActual);
        }
    }

    // Clases auxiliares para crear entidades de dominio según tipo
    private static class CofreFactory {

        /**
         * Crea un cofre logístico con el comportamiento por defecto especificado
         *
         * @param id El identificador único del cofre
         * @param posicion La posición del cofre en la grilla
         * @param capacidad La capacidad máxima del cofre
         * @param tipoComportamiento El tipo de comportamiento por defecto
         * @return El cofre logístico configurado
         */
        public static CofreLogistico crearCofre(String id, Punto posicion, int capacidad, String tipoComportamiento) {
            requireNonNull(id, "ID del cofre no puede ser null");
            requireNonNull(posicion, "Posición del cofre no puede ser null");
            requireNonNull(tipoComportamiento, "Tipo de comportamiento no puede ser null");

            if (capacidad <= 0) {
                throw new IllegalArgumentException("La capacidad debe ser mayor a 0");
            }

            // Crear el cofre
            CofreLogistico cofre = new CofreLogistico(id, posicion, capacidad);

            // Configurar comportamiento por defecto con parámetros
            ComportamientoCofre comportamientoDefecto = ComportamientoFactory.crearConParametros(
                    tipoComportamiento,
                    capacidad
            );
            cofre.setComportamientoPorDefecto(comportamientoDefecto);

            return cofre;
        }

        /**
         * Crea un cofre logístico con comportamientos específicos por item
         *
         * @param id El identificador único del cofre
         * @param posicion La posición del cofre en la grilla
         * @param capacidad La capacidad máxima del cofre
         * @param tipoComportamientoDefecto El tipo de comportamiento por defecto
         * @param comportamientosPorItem Mapa de comportamientos específicos por item
         * @return El cofre logístico configurado
         */
        public static CofreLogistico crearCofreConComportamientos(
                String id,
                Punto posicion,
                int capacidad,
                String tipoComportamientoDefecto,
                Map<String, String> comportamientosPorItem) {

            CofreLogistico cofre = crearCofre(id, posicion, capacidad, tipoComportamientoDefecto);

            // Configurar comportamientos específicos por item
            if (comportamientosPorItem != null && !comportamientosPorItem.isEmpty()) {
                for (Map.Entry<String, String> entry : comportamientosPorItem.entrySet()) {
                    String nombreItem = entry.getKey();
                    String tipoComportamiento = entry.getValue();

                    Item item = new Item(nombreItem, nombreItem); // Asumiendo constructor simple
                    ComportamientoCofre comportamiento = ComportamientoFactory.crearConParametros(
                            tipoComportamiento,
                            capacidad
                    );
                    cofre.setComportamiento(item, comportamiento);
                }
            }

            return cofre;
        }
    }

    private static class ComportamientoFactory {

        /**
         * Crea una instancia de comportamiento según el tipo especificado con parámetros por defecto
         *
         * @param tipo El tipo de comportamiento a crear
         * @return La instancia del comportamiento correspondiente
         * @throws IllegalArgumentException Si el tipo no es reconocido
         */
        public static ComportamientoCofre crear(String tipo) {
            return crearConParametros(tipo, 100); // Capacidad por defecto
        }

        /**
         * Crea una instancia de comportamiento según el tipo especificado con parámetros personalizados
         *
         * @param configuracion La configuración del comportamiento (puede incluir parámetros)
         * @param capacidadMaximaDefecto La capacidad máxima por defecto del cofre
         * @return La instancia del comportamiento correspondiente
         * @throws IllegalArgumentException Si el tipo no es reconocido
         */
        public static ComportamientoCofre crearConParametros(String configuracion, int capacidadMaximaDefecto) {
            requireNonNull(configuracion, "Configuración de comportamiento no puede ser null");

            return switch (configuracion) {
                case "comportamiento_almacenamiento" -> new ComportamientoAlmacenamiento();

                case "comportamiento_intermedio_buffer" -> {
                    int umbralMinimo = (int) (capacidadMaximaDefecto * 0.2); // 20% por defecto
                    int umbralMaximo = (int) (capacidadMaximaDefecto * 0.8); // 80% por defecto
                    yield new ComportamientoIntermedioBuffer(umbralMinimo, umbralMaximo);
                }

                case "comportamiento_provision_activa" -> new ComportamientoProvisionActiva();

                case "comportamiento_provision_pasiva" -> new ComportamientoProvisionPasiva();

                case "comportamiento_solicitud" -> new ComportamientoSolicitud(capacidadMaximaDefecto);

                default -> throw new IllegalArgumentException("Tipo de comportamiento no reconocido: " + configuracion);
            };
        }

    }
}
