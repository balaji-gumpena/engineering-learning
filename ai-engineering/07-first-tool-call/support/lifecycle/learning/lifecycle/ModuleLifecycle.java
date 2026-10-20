package learning.lifecycle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Make-managed process lifecycle; lesson operations remain in Spring controllers. */
public final class ModuleLifecycle {
    private ModuleLifecycle() {}

    public static void main(String[] arguments) throws Exception {
        Path root = Path.of("").toRealPath();
        String action = arguments[0];
        int port = arguments.length > 1 ? Integer.parseInt(arguments[1]) : 8000;
        int commercePort = arguments.length > 2 ? Integer.parseInt(arguments[2]) : 8080;
        switch (action) {
            case "start" -> start(root, port, commercePort, true);
            case "stop" -> stop(root, port, commercePort);
            case "status" -> status(root, port, commercePort);
            default ->
                    throw new IllegalArgumentException("Use make start, restart, stop or status");
        }
    }

    static void start(Path root, int port, int commercePort, boolean build) throws Exception {
        List<ServiceSpec> specs = ModuleServices.list(root, port, commercePort);
        ManagedService application = new ManagedService(root, specs.getLast());
        if (application.running()) {
            System.out.println("Module is already running; use make restart for changes.");
            return;
        }
        if (build) {
            ModuleBuild.build(root);
        }
        List<ManagedService> started = new ArrayList<>();
        try {
            for (ServiceSpec spec : specs) {
                ManagedService service = new ManagedService(root, spec);
                if (service.start()) {
                    started.add(service);
                }
            }
        } catch (Exception error) {
            for (ManagedService service : started.reversed()) {
                service.stop();
            }
            throw error;
        }
        System.out.println("Open http://127.0.0.1:" + port);
    }

    static void stop(Path root, int port, int commercePort) throws Exception {
        for (ServiceSpec spec : ModuleServices.list(root, port, commercePort).reversed()) {
            new ManagedService(root, spec).stop();
        }
    }

    private static void status(Path root, int port, int commercePort) throws Exception {
        for (ServiceSpec spec : ModuleServices.list(root, port, commercePort)) {
            System.out.println(
                    spec.name()
                            + ": "
                            + (new ManagedService(root, spec).running() ? "running" : "stopped"));
        }
    }
}
