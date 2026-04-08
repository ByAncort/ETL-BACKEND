package com.necronet.registerapi.entity.enums;

public enum ScheduleFrequency {
    MINUTELY("Cada minuto"),
    HOURLY("Cada hora"),
    DAILY("Diario"),
    WEEKLY("Semanal"),
    MONTHLY("Mensual"),
    CUSTOM("Personalizado");

    private final String description;

    ScheduleFrequency(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}