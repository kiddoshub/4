
package com.gaspoweredcake.client33.utils.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.gaspoweredcake.client33.utils.other.JsonDateDeserializer;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Http {
    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .executor(Executors.newVirtualThreadPerTaskExecutor())
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Date.class, new JsonDateDeserializer())
        .create();

    private enum Method {
        GET,
        POST
    }

    public static class Request {
        private final HttpRequest.Builder builder;
        private final Method method;
        private HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.noBody();
        private Consumer<Exception> exceptionHandler = Exception::printStackTrace;

        private Request(Method method, String url) {
            try {
                this.builder = HttpRequest.newBuilder().uri(new URI(url)).timeout(Duration.ofSeconds(30)).header("User-Agent", "33/26.3 (Fabric)");
                this.method = method;
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }

        public Request timeout(Duration timeout) {
            builder.timeout(timeout);
            return this;
        }

        public Request header(String name, String value) {
            builder.header(name, value);

            return this;
        }

        public Request bearer(String token) {
            builder.header("Authorization", "Bearer " + token);

            return this;
        }

        public Request bodyString(String string) {
            builder.setHeader("Content-Type", "text/plain");
            body = HttpRequest.BodyPublishers.ofString(string);

            return this;
        }

        public Request bodyForm(String string) {
            builder.setHeader("Content-Type", "application/x-www-form-urlencoded");
            body = HttpRequest.BodyPublishers.ofString(string);

            return this;
        }

        public Request bodyJson(String string) {
            builder.setHeader("Content-Type", "application/json");
            body = HttpRequest.BodyPublishers.ofString(string);

            return this;
        }

        public Request bodyJson(Object object) {
            builder.setHeader("Content-Type", "application/json");
            body = HttpRequest.BodyPublishers.ofString(GSON.toJson(object));

            return this;
        }

        public Request ignoreExceptions() {
            exceptionHandler = _ -> {
            };
            return this;
        }

        public Request exceptionHandler(Consumer<Exception> exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }

        private <T> HttpResponse<T> _sendResponse(String accept, HttpResponse.BodyHandler<T> responseBodyHandler) {
            builder.setHeader("Accept", accept);
            builder.method(method.name(), body);

            HttpRequest request = builder.build();

            try {
                return CLIENT.send(request, responseBodyHandler);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                exceptionHandler.accept(e);
                return new FailedHttpResponse<>(request, e);
            } catch (IOException e) {
                exceptionHandler.accept(e);
                return new FailedHttpResponse<>(request, e);
            }
        }

        @Nullable
        private <T> T _send(String accept, HttpResponse.BodyHandler<T> responseBodyHandler) {
            HttpResponse<T> res = _sendResponse(accept, responseBodyHandler);
            if (res.statusCode() >= 200 && res.statusCode() < 300) return res.body();
            // Rejected streaming responses still own a connection until closed.
            if (res.body() instanceof AutoCloseable resource) {
                try { resource.close(); } catch (Exception e) { exceptionHandler.accept(e); }
            }
            return null;
        }

        public void send() {
            _send("*/*", HttpResponse.BodyHandlers.discarding());
        }

        public HttpResponse<Void> sendResponse() {
            return _sendResponse("*/*", HttpResponse.BodyHandlers.discarding());
        }

        @Nullable
        public InputStream sendInputStream() {
            return _send("*/*", HttpResponse.BodyHandlers.ofInputStream());
        }

        public HttpResponse<InputStream> sendInputStreamResponse() {
            return _sendResponse("*/*", HttpResponse.BodyHandlers.ofInputStream());
        }

        @Nullable
        public String sendString() {
            return _send("*/*", HttpResponse.BodyHandlers.ofString());
        }

        public HttpResponse<String> sendStringResponse() {
            return _sendResponse("*/*", HttpResponse.BodyHandlers.ofString());
        }

        @Nullable
        public Stream<String> sendLines() {
            return _send("*/*", HttpResponse.BodyHandlers.ofLines());
        }

        public HttpResponse<Stream<String>> sendLinesResponse() {
            return _sendResponse("*/*", HttpResponse.BodyHandlers.ofLines());
        }

        @Nullable
        public <T> T sendJson(Type type) {
            return _send("application/json", JsonBodyHandler.ofJson(GSON, type));
        }

        public <T> HttpResponse<T> sendJsonResponse(Type type) {
            return _sendResponse("*/*", JsonBodyHandler.ofJson(GSON, type));
        }
    }

    public static Request get(String url) {
        return new Request(Method.GET, url);
    }

    public static Request post(String url) {
        return new Request(Method.POST, url);
    }
}
