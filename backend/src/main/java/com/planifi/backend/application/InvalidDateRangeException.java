package com.planifi.backend.application;

import java.time.LocalDate;

public class InvalidDateRangeException extends RuntimeException {

    public InvalidDateRangeException(LocalDate from, LocalDate to) {
        super("Fecha inválida: from=" + from + " to=" + to);
    }
}
