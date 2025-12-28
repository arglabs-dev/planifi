package com.planifi.backend.application;

public class IdempotencyKeyReuseException extends RuntimeException {

    public IdempotencyKeyReuseException(String idempotencyKey) {
        super("Idempotency-Key already used with different payload: " + idempotencyKey);
    }
}
