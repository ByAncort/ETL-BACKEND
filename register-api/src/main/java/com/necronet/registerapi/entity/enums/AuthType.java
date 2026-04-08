package com.necronet.registerapi.entity.enums;

public enum AuthType {
    NONE("Sin autenticación"),
    BASIC("Basic Auth"),
    BEARER_TOKEN("Bearer Token"),
    API_KEY("API Key"),
    OAUTH2("OAuth 2.0"),
    JWT("JWT"),
    CUSTOM("Personalizado");

    private final String description;

    AuthType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}