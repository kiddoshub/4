
package com.gaspoweredcake.client33.utils.network;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public record JsonBodyHandler<T>(HttpResponse.BodySubscriber<InputStream> delegate, Gson gson, Type type) implements HttpResponse.BodySubscriber<T> {
    private static final Executor DECODERS = Executors.newVirtualThreadPerTaskExecutor();
    private static final int MAX_JSON_BYTES = 16 * 1024 * 1024;

    public static <T> HttpResponse.BodyHandler<T> ofJson(Gson gson, Type type) {
        return info -> info.statusCode() >= 200 && info.statusCode() < 300
            ? new JsonBodyHandler<>(HttpResponse.BodySubscribers.ofInputStream(), gson, type)
            : HttpResponse.BodySubscribers.mapping(HttpResponse.BodySubscribers.discarding(), ignored -> null);
    }

    @Override
    public CompletionStage<T> getBody() {
        // Reading a response stream on HttpClient's delivery thread can prevent further data delivery.
        return delegate.getBody().thenApplyAsync(in -> {
            if (in == null) return null;
            try (in) {
                byte[] bytes = in.readNBytes(MAX_JSON_BYTES + 1);
                if (bytes.length > MAX_JSON_BYTES) throw new IOException("JSON response exceeds 16 MiB");
                return gson.fromJson(new String(bytes, StandardCharsets.UTF_8), type);
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
        }, DECODERS);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.delegate.onSubscribe(subscription);
    }

    @Override
    public void onNext(List<ByteBuffer> item) {
        this.delegate.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        this.delegate.onError(throwable);
    }

    @Override
    public void onComplete() {
        this.delegate.onComplete();
    }
}
