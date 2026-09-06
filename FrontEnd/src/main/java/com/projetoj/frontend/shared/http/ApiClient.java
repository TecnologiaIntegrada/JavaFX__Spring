package com.projetoj.frontend.shared.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.projetoj.frontend.shared.config.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApiClient {

    private static final Logger LOGGER = Logger.getLogger(ApiClient.class.getName());

    private final AppConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean sessionExpiredNotified = new AtomicBoolean(false);
    private volatile String authToken;
    private volatile Consumer<ApiException> onAuthSessionInvalid;

    public ApiClient(AppConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getConnectTimeoutSeconds()))
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @SuppressWarnings("unchecked")
    public HealthStatus getHealth() {
        Map<String, Object> response = get("/health", Map.class);
        return new HealthStatus(
                stringValue(response.get("status")),
                stringValue(response.get("api")),
                stringValue(response.get("database"))
        );
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    public <T> T get(String path, Class<T> responseType) {
        return send(HttpRequest.newBuilder()
                .uri(buildUri(path))
                .timeout(Duration.ofSeconds(config.getRequestTimeoutSeconds()))
                .header("Accept", "application/json")
                .GET()
                .build(), responseType, null);
    }

    public <T> List<T> getList(String path, TypeReference<List<T>> typeReference) {
        List<T> result = send(HttpRequest.newBuilder()
                .uri(buildUri(path))
                .timeout(Duration.ofSeconds(config.getRequestTimeoutSeconds()))
                .header("Accept", "application/json")
                .GET()
                .build(), null, typeReference);
        return result == null ? Collections.emptyList() : result;
    }

    public <T> T put(String path, Object body, Class<T> responseType) {
        return sendJson("PUT", path, body, responseType, null);
    }

    public <T> List<T> putList(String path, Object body, TypeReference<List<T>> typeReference) {
        @SuppressWarnings("unchecked")
        List<T> result = (List<T>) sendJson("PUT", path, body, null, typeReference);
        return result == null ? Collections.emptyList() : result;
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        return sendJson("POST", path, body, responseType, null);
    }

    /**
     * Login: usuario/senha apenas em headers (nao no body).
     */
    public <T> T postLogin(String username, String password, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri("/auth/login"))
                .timeout(Duration.ofSeconds(config.getRequestTimeoutSeconds()))
                .header("Accept", "application/json")
                .header("X-Auth-Username", username == null ? "" : username)
                .header("X-Auth-Password", password == null ? "" : password)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return sendWithoutAuth(request, responseType);
    }

    public void delete(String path) {
        send(HttpRequest.newBuilder()
                .uri(buildUri(path))
                .timeout(Duration.ofSeconds(config.getRequestTimeoutSeconds()))
                .header("Accept", "application/json")
                .DELETE()
                .build(), Void.class, null);
    }

    private <T> T sendJson(
            String method,
            String path,
            Object body,
            Class<T> responseType,
            TypeReference<?> typeReference
    ) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(buildUri(path))
                    .timeout(Duration.ofSeconds(config.getRequestTimeoutSeconds()))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json");
            if ("POST".equals(method)) {
                builder.POST(HttpRequest.BodyPublishers.ofString(json));
            } else if ("PUT".equals(method)) {
                builder.PUT(HttpRequest.BodyPublishers.ofString(json));
            } else {
                throw new IllegalArgumentException("Metodo nao suportado: " + method);
            }
            return send(builder.build(), responseType, typeReference);
        } catch (IOException e) {
            throw new ApiException("Falha ao serializar JSON da requisicao", e);
        }
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
        sessionExpiredNotified.set(false);
    }

    public void clearAuthToken() {
        this.authToken = null;
    }

    public void setOnAuthSessionInvalid(Consumer<ApiException> onAuthSessionInvalid) {
        this.onAuthSessionInvalid = onAuthSessionInvalid;
    }

    private URI buildUri(String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(config.getApiBaseUrl() + normalizedPath);
    }

    private <T> T sendWithoutAuth(HttpRequest request, Class<T> responseType) {
        return execute(request, responseType, null, false);
    }

    @SuppressWarnings("unchecked")
    private <T> T send(HttpRequest request, Class<T> responseType, TypeReference<?> typeReference) {
        return execute(applyAuthHeader(request), responseType, typeReference, true);
    }

    @SuppressWarnings("unchecked")
    private <T> T execute(
            HttpRequest request,
            Class<T> responseType,
            TypeReference<?> typeReference,
            boolean notifySessionExpiry
    ) {
        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                if (response.body() == null || response.body().isBlank()) {
                    return null;
                }
                if (typeReference != null) {
                    return (T) objectMapper.readValue(response.body(), typeReference);
                }
                if (responseType == Void.class) {
                    return null;
                }
                return objectMapper.readValue(response.body(), responseType);
            }

            ApiException exception = toApiException(response);
            if (notifySessionExpiry && isAuthSessionInvalid(exception)) {
                notifyAuthSessionInvalid(exception);
            }
            throw exception;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Requisicao interrompida", e);
        } catch (java.net.http.HttpTimeoutException e) {
            LOGGER.log(Level.SEVERE, "Timeout ao chamar a API", e);
            throw new ApiException(
                    "Timeout ao contactar o Backend em " + config.getApiBaseUrl()
                            + ". Confirme se o Backend esta rodando (mvn spring-boot:run na pasta Backend).",
                    e
            );
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Falha de comunicacao com a API", e);
            throw new ApiException("Nao foi possivel conectar ao Backend. Verifique se a API esta em execucao.", e);
        }
    }

    private static boolean isAuthSessionInvalid(ApiException exception) {
        String code = exception.getErrorCode();
        return "AUTH_SESSION_EXPIRED".equals(code) || "AUTH_REQUIRED".equals(code);
    }

    private void notifyAuthSessionInvalid(ApiException exception) {
        if (!sessionExpiredNotified.compareAndSet(false, true)) {
            return;
        }
        clearAuthToken();
        Consumer<ApiException> listener = onAuthSessionInvalid;
        if (listener != null) {
            listener.accept(exception);
        }
    }

    private HttpRequest applyAuthHeader(HttpRequest request) {
        if (authToken == null || authToken.isBlank()) {
            return request;
        }
        return HttpRequest.newBuilder(request, (name, value) -> true)
                .header("Authorization", "Bearer " + authToken)
                .header("X-Api-Key", authToken)
                .build();
    }

    private ApiException toApiException(HttpResponse<String> response) {
        String message = "Erro HTTP " + response.statusCode();
        String errorCode = null;

        try {
            if (response.body() != null && !response.body().isBlank()) {
                Map<?, ?> errorBody = objectMapper.readValue(response.body(), Map.class);
                Object bodyMessage = errorBody.get("message");
                Object bodyCode = errorBody.get("code");
                if (bodyMessage != null) {
                    message = bodyMessage.toString();
                }
                if (bodyCode != null) {
                    errorCode = bodyCode.toString();
                }
            }
        } catch (IOException ignored) {
            message = response.body();
        }

        return new ApiException(response.statusCode(), errorCode, message);
    }
}
