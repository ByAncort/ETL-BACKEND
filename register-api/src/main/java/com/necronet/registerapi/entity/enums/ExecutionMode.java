package com.necronet.registerapi.entity.enums;

public enum ExecutionMode {
    ORCHESTRATED("Orquestada"),
    SCHEDULED("Programada");

    private final String description;

    ExecutionMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}