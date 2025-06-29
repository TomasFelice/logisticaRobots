package com.alphaone.logisticaRobots.application;

import com.alphaone.logisticaRobots.application.dto.*;
import com.alphaone.logisticaRobots.domain.*;
import com.alphaone.logisticaRobots.domain.comportamiento.*;
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
            // Usar el configLoader inyectado
            ConfiguracionSimulacionDTO configuracion = configLoader.cargarConfiguracion();

            // Resto del código igual...
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
        // Crear nueva red y cargar según la configuración
        this.redLogistica = new RedLogistica();

        // Configurar dimensiones de la grilla
        if (configuracion.dimensionGrilla() != null) {
            this.anchoGrilla = configuracion.dimensionGrilla().ancho();
            this.altoGrilla = configuracion.dimensionGrilla().alto();
        }

        // Cargar robopuertos
        if (configuracion.robopuertos() != null) {
            configuracion.robopuertos().forEach(this::cargarRobopuerto);
        }

        // Cargar cofres
        if (configuracion.cofres() != null) {
            configuracion.cofres().forEach(this::cargarCofre);
        }

        // Cargar robots
        if (configuracion.robots() != null) {
            configuracion.robots().forEach(this::cargarRobot);
        }

        // Cargar pedidos - generar automáticamente desde los cofres
        cargarPedidosDesdeConfiguracion(configuracion);

        // Establecer velocidad de simulación
        velocidadSimulacion = configuracion.velocidadSimulacion() > 0 ?
                configuracion.velocidadSimulacion() : 1000;

        // TODO: Revisar si hay que hacer algo con el planificador aca.

        // Actualizar estado
        this.archivoConfigActual = archivoConfig;
        cicloActual = 0;
        estadoGeneral = "NO_INICIADO";
        mensajeEstado = "Configuración cargada exitosamente";

        notificarObservadores();
        logger.info("Configuración cargada desde {}", archivoConfig.getPath());
    }

    // Método auxiliar para cargar pedidos desde la configuración
    // Puede cargar pedidos directamente desde el JSON o generarlos basados en comportamientos
    private void cargarPedidosDesdeConfiguracion(ConfiguracionSimulacionDTO configuracion) {
        if (configuracion.cofres() == null || configuracion.cofres().isEmpty()) {
            return;
        }

        List<Pedido> pedidosGenerados = new ArrayList<>();

         // Intentar cargar pedidos desde el archivo JSON
        try {
            // Usar el mismo archivo que se usó para cargar la configuración
            File archivoConfig = archivoConfigActual;
            if (archivoConfig == null || !archivoConfig.exists()) {
                // Si no hay archivo, usar el predeterminado
                archivoConfig = configLoader.getConfigFile();
            }

            if (archivoConfig != null && archivoConfig.exists()) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(archivoConfig);

                // Verificar si hay pedidos en el JSON
                if (rootNode.has("pedidos") && rootNode.get("pedidos").isArray()) {
                    ArrayNode pedidosArray = (ArrayNode) rootNode.get("pedidos");

                    for (JsonNode pedidoNode : pedidosArray) {
                        String itemNombre = pedidoNode.has("itemNombre") ? 
                                pedidoNode.get("itemNombre").asText() : "";
                        int cantidad = pedidoNode.has("cantidad") ? 
                                pedidoNode.get("cantidad").asInt() : 1; //Por defecto 1 si no viene valor
                        String cofreDestinoId = pedidoNode.has("cofreDestinoId") ? 
                                pedidoNode.get("cofreDestinoId").asText() : "";

                        // Buscar el cofre destino
                        CofreLogistico cofreDestino = redLogistica.buscarCofrePorId(cofreDestinoId);
                        if (cofreDestino == null) {
                            logger.warn("No se encontró el cofre destino con ID: {}", cofreDestinoId);
                            continue;
                        }

                        // Crear el item
                        Item item = new Item(itemNombre, itemNombre);

                        // Buscar cofres origen que pueda ofrecer este item
                        List<CofreLogistico> cofresOrigen = new ArrayList<>();
                        for (CofreLogistico cofre : redLogistica.getCofres()) {
                            if (cofre.puedeOfrecer(item, cantidad) && !cofre.equals(cofreDestino)) {
                                cofresOrigen.add(cofre);
                            }
                        }

                        // Ordeno cofresOrigen por prioridad, descartando los prioridad 0 (no aplican)
                        cofresOrigen.removeIf(cofre -> cofre.getPrioridadSolicitud(item) == 0);
                        cofresOrigen.sort((c1, c2) -> Integer.compare(c2.getPrioridadSolicitud(item), c1.getPrioridadSolicitud(item)));

                        // Si no hay cofres origen, no se crea el pedido
                        if (cofresOrigen.isEmpty()) {
                            logger.warn("No se encontró un cofre origen que pueda ofrecer el item: {}", itemNombre);
                            continue;
                        }

                        // Se crea el pedido con el cofre origen de mayor prioridad
                        CofreLogistico cofreOrigen = cofresOrigen.get(0);
                        Pedido.PrioridadPedido prioridadPorComportamiento = determinarPrioridadPorComportamiento(cofreOrigen, item);

                        // Crear el pedido
                        Pedido pedido = new Pedido(
                                item,
                                cantidad,
                                cofreOrigen,
                                cofreDestino,
                                prioridadPorComportamiento
                        );

                        pedidosGenerados.add(pedido);
                        logger.debug("Pedido cargado desde JSON: {} de {} desde {} hasta {}. Prioridad {}",
                                cantidad,
                                item.getNombre(),
                                cofreOrigen.getId(),
                                cofreDestino.getId(),
                                prioridadPorComportamiento);
                    }

                    // Si se cargaron pedidos desde el JSON, agregarlos a la red logística y terminar
                    if (!pedidosGenerados.isEmpty()) {
                        for (Pedido pedido : pedidosGenerados) {
                            this.redLogistica.agregarPedido(pedido);
                        }
                        logger.info("Se cargaron {} pedidos desde el archivo JSON", pedidosGenerados.size());
                        return;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error al cargar pedidos desde JSON", e);
        }

        // TODO:
        //  @Nacho para mi de aca para abajo lo podemos borrar, deberiamos respetar siempre la estructura de archivo de config
        //  en la que tengamos los pedidos
        // Si no se pudieron cargar pedidos desde el JSON, generarlos basados en comportamientos
        pedidosGenerados.clear();

        // Obtener todos los cofres de la red logística
        List<CofreLogistico> cofres = new ArrayList<>(redLogistica.getCofres());

        // Crear un conjunto de todos los items disponibles en el sistema
        Set<String> itemsDisponibles = new HashSet<>();
        for (CofreLogistico cofre : cofres) {
            for (Item item : cofre.getInventario().getItems()) {
                itemsDisponibles.add(item.getNombre());
            }
        }

        // Para cada item, buscar cofres que lo solicitan y cofres que lo ofrecen
        for (String nombreItem : itemsDisponibles) {
            Item item = new Item(nombreItem, nombreItem);

            // Encontrar cofres que pueden solicitar este item
            List<CofreLogistico> cofresQueSolicitan = new ArrayList<>();
            for (CofreLogistico cofre : cofres) {
                if (cofre.puedeSolicitar(item, 1)) {
                    cofresQueSolicitan.add(cofre);
                }
            }

            // Encontrar cofres que pueden ofrecer este item
            List<CofreLogistico> cofresQueOfrecen = new ArrayList<>();
            for (CofreLogistico cofre : cofres) {
                if (cofre.puedeOfrecer(item, 1)) {
                    cofresQueOfrecen.add(cofre);
                }
            }

            // Crear pedidos entre cofres que solicitan y cofres que ofrecen
            for (CofreLogistico cofreDestino : cofresQueSolicitan) {
                for (CofreLogistico cofreOrigen : cofresQueOfrecen) {
                    // No crear pedidos entre el mismo cofre
                    if (cofreOrigen.equals(cofreDestino)) {
                        continue;
                    }

                    // Determinar la cantidad a solicitar (por simplicidad, usamos 1)
                    // TODO: Validar la cantidad de items que muevo de un cofre a otro
                    int cantidad = 1;

                    // Determinar la prioridad basada en el comportamiento del cofre de origen
                    int prioridadValor = cofreOrigen.getPrioridadSolicitud(item);
                    Pedido.PrioridadPedido prioridad = getPrioridadPedido(prioridadValor);

                    // Crear el pedido
                    Pedido pedido = new Pedido(
                            item,
                            cantidad,
                            cofreOrigen,
                            cofreDestino,
                            prioridad
                    );

                    pedidosGenerados.add(pedido);
                    logger.debug("Pedido generado: {} de {} desde {} hasta {}. Prioridad {}",
                            cantidad,
                            item.getNombre(),
                            cofreOrigen.getPosicion(),
                            cofreDestino.getPosicion(),
                            prioridad);
                }
            }
        }

        // Agregar los pedidos generados a la red logística
        for (Pedido pedido : pedidosGenerados) {
            this.redLogistica.agregarPedido(pedido);
        }

        logger.info("Se generaron {} pedidos automáticamente basados en comportamientos", pedidosGenerados.size());
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

    // Métodos auxiliares para cargar cada tipo de entidad
    private void cargarRobopuerto(RobopuertoDTO rpDTO) {
        Punto posicion = new Punto(rpDTO.posicion().x(), rpDTO.posicion().y());
        Robopuerto robopuerto = new Robopuerto(rpDTO.id(), posicion, rpDTO.alcance(), rpDTO.tasaRecarga());
        redLogistica.agregarRobopuerto(robopuerto);
    }

    private void cargarCofre(CofreDTO cofreDTO) {
        Punto posicion = new Punto(cofreDTO.posicion().x(), cofreDTO.posicion().y());
        CofreLogistico cofre = crearCofreSegunTipo(
                cofreDTO.id(), posicion, cofreDTO.capacidadMaxima(),
                cofreDTO.comportamientoDefecto(), cofreDTO.tiposComportamientoPorItem());

        // Cargar inventario inicial si existe
        if (cofreDTO.inventario() != null) {
            cofreDTO.inventario().entrySet().forEach(entry -> {
                Item item = new Item(entry.getKey(), entry.getKey());
                cofre.agregarItem(item, entry.getValue());
            });
        }

        redLogistica.agregarCofre(cofre);
    }

    private void cargarRobot(RobotDTO robotDTO) {
        Punto posicion = new Punto(robotDTO.posicion().x(), robotDTO.posicion().y());
        Robopuerto robopuertoBase = redLogistica.getRobopuertoMasCercano(posicion);

        if (robopuertoBase == null) {
            throw new IllegalStateException("No se encontró robopuerto cercano para robot en " + posicion);
        }

        RobotLogistico robot = new RobotLogistico(
                Integer.parseInt(robotDTO.id()),
                posicion,
                robopuertoBase,
                (int) robotDTO.bateriaMaxima(),
                robotDTO.capacidadCarga());

        redLogistica.agregarRobot(robot);
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
            itemsEnCarga.put(String.valueOf(robot.getId()), robot.getBateriaActual());


            // Convertir la ruta si existe
            List<PuntoDTO> rutaDTO = null;
            List<Punto> ruta = robot.getRutaActual();
            if (ruta != null && !ruta.isEmpty()) {
                rutaDTO = ruta.stream()
                        .map(p -> new PuntoDTO(p.getX(), p.getY()))
                        .toList();
            }

            RobotDTO robotDTO = new RobotDTO(
                    String.valueOf(robot.getId()),
                    posicionDTO,
                    robot.getBateriaActual(),
                    robot.getBateriaMaxima(),
                    robot.getBateriaActual(), // Supongo que hay un metodo así
                    robot.getBateriaMaxima(),
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
