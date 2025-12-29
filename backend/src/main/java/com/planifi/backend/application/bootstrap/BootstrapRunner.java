package com.planifi.backend.application.bootstrap;

import com.planifi.backend.config.BootstrapProperties;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapRunner.class);

    private final BootstrapProperties properties;
    private final BootstrapConfigLoader loader;
    private final BootstrapService bootstrapService;

    public BootstrapRunner(BootstrapProperties properties,
                           BootstrapConfigLoader loader,
                           BootstrapService bootstrapService) {
        this.properties = properties;
        this.loader = loader;
        this.bootstrapService = bootstrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            logger.info("Bootstrap deshabilitado por configuración.");
            return;
        }
        if (properties.getConfigPath() == null || properties.getConfigPath().isBlank()) {
            logger.info("Sin archivo de configuración bootstrap; omitiendo carga inicial.");
            return;
        }
        Path path = Path.of(properties.getConfigPath());
        BootstrapConfig config = loader.load(path);
        bootstrapService.apply(config);
        logger.info("Configuración bootstrap aplicada desde {} (usuarios: {}, cuentas: {}).",
                path,
                config.users().size(),
                config.users().stream().mapToInt(user -> user.accounts().size()).sum());
    }
}
