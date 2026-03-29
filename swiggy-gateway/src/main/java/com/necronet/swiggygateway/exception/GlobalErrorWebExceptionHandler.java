package com.necronet.swiggygateway.exception;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        HttpStatusCode statusCode = determineStatusCode(ex);
        response.setStatusCode(statusCode);

        HttpStatus httpStatus = HttpStatus.valueOf(statusCode.value());

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                statusCode.value(),
                httpStatus.getReasonPhrase(),
                ex.getMessage(),
                exchange.getRequest().getURI().getPath()
        );
        String jsonResponse = convertToJson(errorResponse);
        DataBuffer buffer = response.bufferFactory().wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    private HttpStatusCode determineStatusCode(Throwable ex) {
        if (ex instanceof org.springframework.web.server.ResponseStatusException) {
            return ((org.springframework.web.server.ResponseStatusException) ex).getStatusCode();
        }
        if (ex.getMessage() != null && ex.getMessage().contains("un authorized")) {
            return HttpStatusCode.valueOf(401);
        }
        if (ex.getMessage() != null && ex.getMessage().contains("missing authorization")) {
            return HttpStatusCode.valueOf(401);
        }
        return HttpStatusCode.valueOf(500);
    }

    private String convertToJson(ErrorResponse error) {
        return String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                error.getTimestamp(),
                error.getStatus(),
                error.getError(),
                error.getMessage(),
                error.getPath()
        );
    }
}