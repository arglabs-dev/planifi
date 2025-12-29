package com.planifi.backend.application.bootstrap;

public class BootstrapConfigException extends RuntimeException {

    public BootstrapConfigException(String message) {
        super(message);
    }

    public BootstrapConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
