package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.network.Http;
import com.gaspoweredcake.client33.utils.network.FailedHttpResponse;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;

/** Synthetic loopback HTTP only; no provider credentials or external accounts. */
public final class HttpRegression {
    public static void run() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            server.setExecutor(workers);
            server.createContext("/echo", exchange -> {
                try (exchange) {
                    byte[] response = exchange.getRequestBody().readAllBytes();
                    exchange.sendResponseHeaders(201, response.length);
                    exchange.getResponseBody().write(response);
                }
            });
            server.createContext("/json", exchange -> {
                try (exchange) {
                    byte[] response = "{\"name\":\"Café\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                }
            });
            server.createContext("/slow", exchange -> {
                try (exchange) {
                    try { Thread.sleep(300); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                    exchange.sendResponseHeaders(200, -1);
                }
            });
            server.start();
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            var request = Http.post(base + "/echo").bodyString("first").bodyString("second");
            check("second".equals(request.sendString()), "Replacing a request body preserves the method and accepts 201");
            check("second".equals(request.sendString()), "Request reuse retains its body");
            Map<?, ?> json = Http.get(base + "/json").sendJson(Map.class);
            check(json != null && "Café".equals(json.get("name")), "JSON response decodes UTF-8");
            check(Http.get(base + "/slow").timeout(Duration.ofMillis(30)).ignoreExceptions().sendResponse() instanceof FailedHttpResponse<?>, "HTTP deadlines terminate stalled calls");
            Thread.currentThread().interrupt();
            try {
                check(Http.get(base + "/echo").ignoreExceptions().sendResponse() instanceof FailedHttpResponse<?>, "Interrupted call reports failure");
                check(Thread.currentThread().isInterrupted(), "HTTP helper preserves caller cancellation");
            } finally { Thread.interrupted(); }
            System.out.println("Loopback HTTP regressions passed.");
        } finally { server.stop(0); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
