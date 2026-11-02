
package com.gaspoweredcake.client33.addons;

import com.gaspoweredcake.client33.utils.network.Http;

import org.jspecify.annotations.Nullable;

public record GithubRepo(String owner, String name, String branch, @Nullable String accessToken) {
    public GithubRepo(String owner, String name, @Nullable String accessToken) {
        this(owner, name, "master", accessToken);
    }

    public GithubRepo(String owner, String name) {
        this(owner, name, "master", null);
    }

    public String getOwnerName() {
        return owner + "/" + name;
    }

    public void authenticate(Http.Request request) {
        if (this.accessToken != null && !this.accessToken.isBlank()) {
            request.bearer(this.accessToken);
        } else {
            String personalAuthToken = System.getenv("client33.github.authorization");
            if (personalAuthToken != null && !personalAuthToken.isBlank()) {
                request.bearer(personalAuthToken);
            }
        }
    }
}
