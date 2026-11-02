
package meteordevelopment.meteorclient.utils.network;

import javax.annotation.Nullable;
import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

public record FailedHttpResponse<T>(HttpRequest request, Exception exception) implements HttpResponse<T> {
    @Override
    public int statusCode() {
        return Http.BAD_REQUEST;
    }

    @Override
    public Optional<HttpResponse<T>> previousResponse() {
        return Optional.empty();
    }

    @Override
    public HttpHeaders headers() {
        return HttpHeaders.of(Map.of(), (s1, s2) -> true);
    }

    @Override
    public T body() {
        return null;
    }

    @Override
    public Optional<SSLSession> sslSession() {
        return Optional.empty();
    }

    @Override
    public URI uri() {
        return this.request.uri();
    }

    @Nullable
    @Override
    public HttpClient.Version version() {
        return this.request.version().orElse(null);
    }
}
