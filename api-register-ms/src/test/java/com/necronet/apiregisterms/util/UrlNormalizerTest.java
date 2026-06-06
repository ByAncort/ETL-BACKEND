package com.necronet.apiregisterms.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tema 3 — Validacion de Datos.
 * Pruebas unitarias de UrlNormalizer: validacion de "/" y doble "//".
 */
class UrlNormalizerTest {

    @Test
    @DisplayName("Base con barra final + path con barra inicial -> una sola barra")
    void join_collapsesDoubleSlashAtJunction() {
        assertThat(UrlNormalizer.join("http://api.test/v1/", "/users"))
                .isEqualTo("http://api.test/v1/users");
    }

    @Test
    @DisplayName("Base sin barra + path sin barra -> inserta la barra faltante")
    void join_insertsMissingSlash() {
        assertThat(UrlNormalizer.join("http://api.test/v1", "users"))
                .isEqualTo("http://api.test/v1/users");
    }

    @Test
    @DisplayName("Preserva el esquema http:// y https://")
    void collapse_preservesScheme() {
        assertThat(UrlNormalizer.collapseSlashes("http://api.test//v1//users"))
                .isEqualTo("http://api.test/v1/users");
        assertThat(UrlNormalizer.collapseSlashes("https://api.test///a"))
                .isEqualTo("https://api.test/a");
    }

    @Test
    @DisplayName("Colapsa multiples barras consecutivas a una")
    void collapse_multipleSlashes() {
        assertThat(UrlNormalizer.collapseSlashes("http://x/a////b"))
                .isEqualTo("http://x/a/b");
    }

    @Test
    @DisplayName("Path vacio o null devuelve la base colapsada")
    void join_emptyPath_returnsBase() {
        assertThat(UrlNormalizer.join("http://api.test/v1/", "")).isEqualTo("http://api.test/v1/");
        assertThat(UrlNormalizer.join("http://api.test//v1", null)).isEqualTo("http://api.test/v1");
    }

    @Test
    @DisplayName("No inserta barra antes de un query string")
    void join_doesNotInsertSlashBeforeQuery() {
        assertThat(UrlNormalizer.join("http://api.test/users", "?active=true"))
                .isEqualTo("http://api.test/users?active=true");
    }

    @Test
    @DisplayName("Ambos lados con barra y multiples no dejan rastro de doble barra")
    void join_bothSlashesAndExtra() {
        assertThat(UrlNormalizer.join("http://api.test/v1//", "//users/123"))
                .isEqualTo("http://api.test/v1/users/123");
    }

    @Test
    @DisplayName("Base null se trata como vacia")
    void join_nullBase() {
        assertThat(UrlNormalizer.join(null, "/users")).isEqualTo("/users");
    }
}
