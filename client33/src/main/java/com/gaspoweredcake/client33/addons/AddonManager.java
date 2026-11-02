
package com.gaspoweredcake.client33.addons;

import com.gaspoweredcake.client33.Client33;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;

import java.util.ArrayList;
import java.util.List;

public class AddonManager {
    public static final List<Client33Addon> ADDONS = new ArrayList<>();

    public static void init() {
        // Client33 pseudo addon
        {
            Client33.ADDON = new Client33Addon() {
                @Override
                public void onInitialize() {}

                @Override
                public String getPackage() {
                    return "com.gaspoweredcake.client33";
                }

                @Override
                public String getWebsite() {
                    return "https://github.com/GasPoweredCake/33";
                }

                @Override
                public GithubRepo getRepo() {
                    return new GithubRepo("GasPoweredCake", "33");
                }

                @Override
                public String getCommit() {
                    String commit = Client33.MOD_META.getCustomValue(Client33.MOD_ID + ":commit").getAsString();
                    return commit.isEmpty() ? null : commit;
                }
            };

            ModMetadata metadata = FabricLoader.getInstance().getModContainer(Client33.MOD_ID).get().getMetadata();

            Client33.ADDON.name = metadata.getName();
            Client33.ADDON.authors = new String[metadata.getAuthors().size()];
            if (metadata.containsCustomValue(Client33.MOD_ID + ":color")) {
                Client33.ADDON.color.parse(metadata.getCustomValue(Client33.MOD_ID + ":color").getAsString());
            }

            int i = 0;
            for (Person author : metadata.getAuthors()) {
                Client33.ADDON.authors[i++] = author.getName();
            }

            ADDONS.add(Client33.ADDON);
        }

        // Addons
        for (EntrypointContainer<Client33Addon> entrypoint : FabricLoader.getInstance().getEntrypointContainers("client33", Client33Addon.class)) {
            ModMetadata metadata = entrypoint.getProvider().getMetadata();
            Client33Addon addon;
            try {
                addon = entrypoint.getEntrypoint();
            } catch (Throwable throwable) {
                throw new RuntimeException("Exception during addon init \"%s\".".formatted(metadata.getName()), throwable);
            }

            addon.name = metadata.getName();

            if (metadata.getAuthors().isEmpty()) throw new RuntimeException("Addon \"%s\" requires at least 1 author to be defined in it's fabric.mod.json. See https://fabricmc.net/wiki/documentation:fabric_mod_json_spec".formatted(addon.name));
            addon.authors = new String[metadata.getAuthors().size()];

            if (metadata.containsCustomValue(Client33.MOD_ID + ":color")) {
                addon.color.parse(metadata.getCustomValue(Client33.MOD_ID + ":color").getAsString());
            }

            int i = 0;
            for (Person author : metadata.getAuthors()) {
                addon.authors[i++] = author.getName();
            }

            ADDONS.add(addon);
        }
    }
}
