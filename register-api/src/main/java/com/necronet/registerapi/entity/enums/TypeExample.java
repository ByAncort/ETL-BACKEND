package com.necronet.registerapi.entity.enums;

public enum TypeExample {
    REQUEST("Request", "Ejemplo de petición de entrada"),
    RESPONSE("Response", "Ejemplo de respuesta esperada");

    private final String name;
    private final String description;

    TypeExample(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

}
