package org.oplauncher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class OPLauncherDispatcherPool {
    static private final Lock LOCK = new ReentrantLock();
    static private final Logger LOGGER = LogManager.getLogger(OPLauncherDispatcherPool.class);
    static private final Map<String, OPLauncherDispatcher> INSTANCE_POOL = new LinkedHashMap<>();
    static private final OPLauncherDispatcherPool instance = new OPLauncherDispatcherPool();
    static private final AtomicReference<OPLauncherController> OP_CONTROLLER = new AtomicReference<>();

    private OPLauncherDispatcherPool() {
        _poolService = Executors.newSingleThreadScheduledExecutor();

        // triggers the pooling stats mechanism
        _poolService.scheduleAtFixedRate(this::poolStatistics, 1, 15, TimeUnit.MINUTES);
    }

    public void poolStatistics() {
        try {
            DecimalFormat df = new DecimalFormat("#,###,###.00");
            String freeMem = df.format(Long.valueOf(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()).floatValue() / (1024f * 1024f));
            String maxMem = df.format(Long.valueOf(Runtime.getRuntime().maxMemory()).floatValue() / (1024f * 1024f));
            LOGGER.info("=== APPLET RUNNING STATS ===");
            LOGGER.info("[STATS] JVM Heap allocation (Used/Max): {}/{} MB", freeMem, maxMem);
            LOGGER.info("[STATS] Number of load/ToBeLoaded Applets: {} applet(s)", INSTANCE_POOL.size());
            LOGGER.info("[STATS] Loaded Applet details (name/class): {}", INSTANCE_POOL.values().stream().map(op->getLoadAppletNames(op)).collect(Collectors.joining(";")));
            //LOGGER.info("[STATS] Loaded Applet parameters (name/key-pairs): {}", INSTANCE_POOL.values().stream().map(op->getLoadedAppletParameters(op)).collect(Collectors.joining(";")));
            LOGGER.info("============================");
        }
        catch (Throwable t) {
            LOGGER.warn("It was not possible to run the stats for the loaded Applets", t);
        }
    }

    public static final OPLauncherDispatcherPool getActivePoolInstance(OPLauncherController controller) {
        OP_CONTROLLER.set(controller);
        return instance;
    }

    public static final OPLauncherController getActiveControllerInstance() {
        return OP_CONTROLLER.get();
    }

    public final OPLauncherDispatcherPool pushInstance(String name, OPLauncherDispatcher dispatcher) {
        LOCK.lock();
        try {
            INSTANCE_POOL.put(name, dispatcher);
            return this;
        }
        finally {
            LOCK.unlock();
        }
    }

    public final OPLauncherDispatcher popInstance(String name) {
        LOCK.lock();
        try {
            return INSTANCE_POOL.remove(name);
        }
        finally {
            LOCK.unlock();
        }
    }

    public final OPLauncherDispatcher getInstance(String name) {
        LOCK.lock();
        try {
            if (!INSTANCE_POOL.containsKey(name)) {
                instance.pushInstance(name, new OPLauncherDispatcher(instance));
            }

            return INSTANCE_POOL.get(name);
        }
        finally {
            LOCK.unlock();
        }
    }

    private String getLoadAppletNames(OPLauncherDispatcher dispatcher) {
        return new StringBuilder("[")
                .append(dispatcher.getAllAppletLoaders()
                    .stream().map((loaders)->(loaders.getAppletName()
                                .concat(" - Class(")).concat(loaders.getAppletClassName()).concat(")"))
                    .collect(Collectors.joining(",")))
                .append("]").toString();
    }
    /*private String getLoadedAppletParameters(OPLauncherDispatcher dispatcher) {
        return dispatcher.getAllAppletLoaders()
                .stream()
                .map(loader->loader.getAppletParameters())
                .map(param->param.getAllParameterKeyValuePairs())
                .map(i->"[".concat(i.toString()).concat("]"))
                .collect(Collectors.joining(","));
    }*/

    // class properties
    private ScheduledExecutorService _poolService;
}
