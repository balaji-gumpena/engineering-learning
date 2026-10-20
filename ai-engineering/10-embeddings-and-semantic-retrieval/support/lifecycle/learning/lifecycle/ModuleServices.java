package learning.lifecycle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class ModuleServices {
    private ModuleServices() {}

    static List<ServiceSpec> list(Path root, int port, int commercePort) {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        List<ServiceSpec> services = new ArrayList<>();
        Path fixture =
                root.resolve("support/commerce-fixture/learning/fixture/CommerceFixture.java");
        if (Files.exists(fixture)) {
            services.add(
                    new ServiceSpec(
                            "commerce-fixture",
                            List.of(java, fixture.toString(), Integer.toString(commercePort)),
                            fixture.toString(),
                            commercePort,
                            "/api/products",
                            200));
        }
        Path commerce = root.resolve("commerce/build/libs/lesson-commerce.jar");
        if (Files.isDirectory(root.resolve("commerce"))) {
            services.add(
                    new ServiceSpec(
                            "commerce",
                            List.of(
                                    java,
                                    "--enable-native-access=ALL-UNNAMED",
                                    "-jar",
                                    commerce.toString(),
                                    "--server.port=" + commercePort,
                                    "--spring.profiles.active=demo"),
                            commerce.toString(),
                            commercePort,
                            "/api/orders",
                            401));
        }
        Path application = root.resolve("build/libs/lesson.jar");
        services.add(
                new ServiceSpec(
                        "application",
                        List.of(
                                java,
                                "--enable-native-access=ALL-UNNAMED",
                                "-jar",
                                application.toString(),
                                "--server.port=" + port,
                                "--ai.commerce-url=http://127.0.0.1:" + commercePort),
                        application.toString(),
                        port,
                        "/",
                        200));
        return List.copyOf(services);
    }
}
