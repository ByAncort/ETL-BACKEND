package com.necronet.apiregisterms.util;

/**
 * Utilidad de normalizacion de URLs.
 * Evita los dos defectos al concatenar base + path:
 *   - barra doble: "host/api/" + "/users" -> "host/api//users"
 *   - barra faltante: "host/api" + "users" -> "host/apiusers"
 * Preserva el esquema ("http://", "https://") y no inserta barra antes de
 * un query ("?") o fragmento ("#").
 */
public final class UrlNormalizer {

    private UrlNormalizer() {
    }

    /**
     * Une base y path con exactamente una barra de separacion y colapsa
     * cualquier barra duplicada, preservando el "://" del esquema.
     */
    public static String join(String base, String path) {
        String b = base == null ? "" : base;
        String p = path == null ? "" : path;

        if (p.isEmpty()) {
            return collapseSlashes(b);
        }

        boolean baseEndsSlash = b.endsWith("/");
        boolean pathStartsSlash = p.startsWith("/");

        String joined;
        if (baseEndsSlash && pathStartsSlash) {
            // Evita la barra doble en la union.
            joined = b + p.substring(1);
        } else if (!baseEndsSlash && !pathStartsSlash
                && !b.isEmpty()
                && !p.startsWith("?") && !p.startsWith("#")) {
            // Inserta la barra faltante (salvo si el path es un query/fragment).
            joined = b + "/" + p;
        } else {
            joined = b + p;
        }

        return collapseSlashes(joined);
    }

    /**
     * Colapsa secuencias de 2 o mas barras en una sola, sin tocar el "://"
     * del esquema (la barra precedida por ':' no se considera duplicada).
     */
    public static String collapseSlashes(String url) {
        if (url == null) {
            return null;
        }
        return url.replaceAll("(?<!:)//+", "/");
    }
}
