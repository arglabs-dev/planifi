package com.planifi.backend.application.bootstrap;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BootstrapConfigLoader {

    private final ObjectMapper jsonMapper;
    private final Validator validator;

    public BootstrapConfigLoader(ObjectMapper objectMapper, Validator validator) {
        this.jsonMapper = objectMapper.copy()
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true);
        this.validator = validator;
    }

    public BootstrapConfig load(Path path) {
        if (path == null || path.toString().isBlank()) {
            throw new BootstrapConfigException("El path de configuración está vacío.");
        }
        if (!Files.exists(path)) {
            throw new BootstrapConfigException("No se encontró el archivo de configuración: " + path);
        }
        if (!Files.isRegularFile(path)) {
            throw new BootstrapConfigException("La ruta de configuración no es un archivo regular: " + path);
        }
        if (!Files.isReadable(path)) {
            throw new BootstrapConfigException("No se puede leer el archivo de configuración: " + path);
        }

        ObjectMapper mapper = selectMapper(path);
        BootstrapConfig config;
        try {
            config = mapper.readValue(path.toFile(), BootstrapConfig.class);
        } catch (IOException ex) {
            throw new BootstrapConfigException("No se pudo leer la configuración: " + path, ex);
        }

        List<String> errors = new ArrayList<>();
        Set<ConstraintViolation<BootstrapConfig>> violations = validator.validate(config);
        if (!violations.isEmpty()) {
            errors.addAll(violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.toList()));
        }

        validateSemanticRules(config, errors);

        if (!errors.isEmpty()) {
            throw new BootstrapConfigException("Configuración inválida: " + String.join("; ", errors));
        }

        return config;
    }

    private ObjectMapper selectMapper(Path path) {
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (filename.endsWith(".yml") || filename.endsWith(".yaml")) {
            return new ObjectMapper(new YAMLFactory())
                    .findAndRegisterModules()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                    .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true);
        }
        if (filename.endsWith(".json")) {
            return jsonMapper;
        }
        throw new BootstrapConfigException("Extensión no soportada para configuración: " + filename);
    }

    private void validateSemanticRules(BootstrapConfig config, List<String> errors) {
        String version = config.version() == null ? "" : config.version().trim();
        if (!version.equalsIgnoreCase("v1") && !version.equals("1")) {
            errors.add("version: solo se admite v1");
        }

        BootstrapStorageConfig storage = config.storage();
        String provider = storage.provider() == null ? "" : storage.provider().trim().toLowerCase(Locale.ROOT);
        if (!"local".equals(provider)) {
            errors.add("storage.provider: solo se admite local");
        }
        if ("local".equals(provider) && storage.local() == null) {
            errors.add("storage.local: requerido cuando provider es local");
        }

        List<BootstrapUserConfig> users = config.users();
        if (users == null || users.isEmpty()) {
            return;
        }
        List<String> emails = users.stream()
                .map(user -> user.email() == null ? "" : user.email().trim().toLowerCase(Locale.ROOT))
                .toList();
        long unique = emails.stream().distinct().count();
        if (unique != emails.size()) {
            errors.add("users: hay correos duplicados");
        }

        for (int index = 0; index < users.size(); index++) {
            BootstrapUserConfig user = users.get(index);
            String password = user.password();
            String passwordHash = user.passwordHash();
            if ((password == null || password.isBlank())
                    && (passwordHash == null || passwordHash.isBlank())) {
                errors.add("users[" + index + "].password: se requiere password o passwordHash");
            }
        }
    }
}
