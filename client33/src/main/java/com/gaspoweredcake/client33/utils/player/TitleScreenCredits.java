
package com.gaspoweredcake.client33.utils.player;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.addons.AddonManager;
import com.gaspoweredcake.client33.addons.GithubRepo;
import com.gaspoweredcake.client33.addons.Client33Addon;
import com.gaspoweredcake.client33.gui.GuiThemes;
import com.gaspoweredcake.client33.gui.screens.CommitsScreen;
import com.gaspoweredcake.client33.mixininterface.IComponent;
import com.gaspoweredcake.client33.utils.network.Http;
import com.gaspoweredcake.client33.utils.network.Client33Executor;
import com.gaspoweredcake.client33.utils.render.Client33Toast;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Items;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.gaspoweredcake.client33.Client33.mc;

public class TitleScreenCredits {
    private static final List<Credit> credits = new ArrayList<>();

    private TitleScreenCredits() {
    }

    private static void init() {
        // Add addons
        for (Client33Addon addon : AddonManager.ADDONS) add(addon);

        // Sort by width (Client33 always first)
        credits.sort(Comparator.comparingInt(value -> value.addon == Client33.ADDON ? Integer.MIN_VALUE : -mc.font.width(value.text)));

        // Check for latest commits
        Client33Executor.execute(() -> {
            for (Credit credit : credits) {
                if (credit.addon == Client33.ADDON || credit.addon.getRepo() == null || credit.addon.getCommit() == null) continue;

                GithubRepo repo = credit.addon.getRepo();
                Http.Request request = Http.get("https://api.github.com/repos/%s/branches/%s".formatted(repo.getOwnerName(), repo.branch()));
                request.exceptionHandler(e -> Client33.LOG.error("Could not fetch repository information for addon '{}'.", credit.addon.name, e));
                repo.authenticate(request);
                HttpResponse<Response> res = request.sendJsonResponse(Response.class);

                switch (res.statusCode()) {
                    case Http.UNAUTHORIZED -> {
                        String message = "Invalid authentication token for repository '%s'".formatted(repo.getOwnerName());
                        Client33Toast toast = new Client33Toast.Builder("GitHub: Unauthorized").icon(Items.BARRIER).text(message).build();
                        mc.gui.toastManager().addToast(toast);
                        Client33.LOG.warn(message);
                        if (System.getenv("client33.github.authorization") == null) {
                            Client33.LOG.info("Consider setting an authorization " +
                                "token with the 'client33.github.authorization' environment variable.");
                            Client33.LOG.info("See: https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens");
                        }
                    }
                    case Http.FORBIDDEN ->
                        Client33.LOG.warn("Could not fetch updates for addon '{}': Rate-limited by GitHub.", credit.addon.name);
                    case Http.NOT_FOUND ->
                        Client33.LOG.warn("Could not fetch updates for addon '{}': GitHub repository '{}' not found.", credit.addon.name, repo.getOwnerName());
                    case Http.SUCCESS -> {
                        if (!credit.addon.getCommit().equals(res.body().commit.sha)) {
                            synchronized (credit.text) {
                                credit.text.append(Component.literal("*").withStyle(ChatFormatting.RED));
                                ((IComponent) ((Component) credit.text)).client33$invalidateCache(); // ???
                            }
                        }
                    }
                }
            }
        });
    }

    private static void add(Client33Addon addon) {
        Credit credit = new Credit(addon);

        if (addon == Client33.ADDON) {
            credit.text.append(Component.literal("33").withStyle(style -> style.withColor(addon.color.getPacked())));
            credits.add(credit);
            return;
        }

        credit.text.append(Component.literal(addon.name).withStyle(style -> style.withColor(addon.color.getPacked())));
        credit.text.append(Component.literal(" by ").withStyle(ChatFormatting.GRAY));

        for (int i = 0; i < addon.authors.length; i++) {
            if (i > 0) {
                credit.text.append(Component.literal(i == addon.authors.length - 1 ? " & " : ", ").withStyle(ChatFormatting.GRAY));
            }

            credit.text.append(Component.literal(addon.authors[i]).withStyle(ChatFormatting.WHITE));
        }

        credits.add(credit);
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (credits.isEmpty()) init();

        int y = 3;
        for (Credit credit : credits) {
            synchronized (credit.text) {
                int x = mc.gui.screen().width - 3 - mc.font.width(credit.text);

                graphics.text(mc.font, credit.text, x, y, -1);
            }

            y += mc.font.lineHeight + 2;
        }
    }

    public static boolean onClicked(double mouseX, double mouseY) {
        int y = 3;
        for (Credit credit : credits) {
            int width;
            synchronized (credit.text) {
                width = mc.font.width(credit.text);
            }

            int x = mc.gui.screen().width - 3 - width;

            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + mc.font.lineHeight + 2) {
                if (credit.addon.getRepo() != null && credit.addon.getCommit() != null) {
                    mc.gui.setScreen(new CommitsScreen(GuiThemes.get(), credit.addon));
                    return true;
                }
            }

            y += mc.font.lineHeight + 2;
        }

        return false;
    }

    private static class Credit {
        public final Client33Addon addon;
        public final MutableComponent text = Component.empty();

        public Credit(Client33Addon addon) {
            this.addon = addon;
        }
    }

    private static class Response {
        public Commit commit;
    }

    private static class Commit {
        public String sha;
    }
}
