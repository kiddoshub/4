
package com.gaspoweredcake.client33.systems.modules;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.addons.AddonManager;
import com.gaspoweredcake.client33.addons.Client33Addon;
import com.gaspoweredcake.client33.gui.GuiTheme;
import com.gaspoweredcake.client33.gui.widgets.WWidget;
import com.gaspoweredcake.client33.settings.Settings;
import com.gaspoweredcake.client33.systems.config.Config;
import com.gaspoweredcake.client33.utils.Utils;
import com.gaspoweredcake.client33.utils.misc.ISerializable;
import com.gaspoweredcake.client33.utils.misc.Keybind;
import com.gaspoweredcake.client33.utils.misc.ModuleLifecycle;
import com.gaspoweredcake.client33.utils.player.ChatUtils;
import com.gaspoweredcake.client33.utils.render.color.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public abstract class Module implements ISerializable<Module>, Comparable<Module> {
    protected final Minecraft mc;

    public final Category category;
    public final String name;
    public final String title;
    public final String description;
    public final String[] aliases;
    public final Color color;

    public final Client33Addon addon;
    public final Settings settings = new Settings();

    private final ModuleLifecycle lifecycle;
    private boolean autoSubscribed;

    public boolean serialize = true;
    public boolean runInMainMenu = false;
    public boolean autoSubscribe = true;

    public final Keybind keybind = Keybind.none();
    public boolean toggleOnBindRelease = false;
    public boolean chatFeedback = true;
    public boolean favorite = false;

    public Module(Category category, String name, String description, String... aliases) {
        if (name.contains(" "))
            Client33.LOG.warn("Module '{}' contains invalid characters in its name making it incompatible with 33 commands.", name);

        this.mc = Minecraft.getInstance();
        this.category = category;
        this.name = name;
        this.title = Utils.nameToTitle(name);
        this.description = description;
        this.aliases = aliases;
        lifecycle = new ModuleLifecycle(
            () -> {
                if (autoSubscribe) {
                    Client33.EVENT_BUS.subscribe(this);
                    autoSubscribed = true;
                }
                onActivate();
            },
            () -> {
                if (autoSubscribed) Client33.EVENT_BUS.unsubscribe(this);
                autoSubscribed = false;
                onDeactivate();
            },
            enabled -> {
                if (enabled) Modules.get().addActive(this);
                else Modules.get().removeActive(this);
            },
            failure -> Client33.LOG.error("Module '{}' lifecycle failed; disabled.", name, failure)
        );
        this.color = Color.fromHsv(Utils.random(0.0, 360.0), 0.35, 1);

        String classname = this.getClass().getName();
        for (Client33Addon addon : AddonManager.ADDONS) {
            if (classname.startsWith(addon.getPackage())) {
                this.addon = addon;
                return;
            }
        }

        this.addon = null;
    }

    public Module(Category category, String name, String desc) {
        this(category, name, desc, new String[0]);
    }

    public WWidget getWidget(GuiTheme theme) {
        return null;
    }

    public void onActivate() {
    }

    public void onDeactivate() {
    }

    public void toggle() {
        if (!mc.isSameThread()) { mc.execute(this::toggle); return; }
        setEnabled(!isActive());
    }

    private void setEnabled(boolean enabling) {
        if (!mc.isSameThread()) { mc.execute(() -> setEnabled(enabling)); return; }
        if (enabling == isActive()) return;
        if (enabling) {
            try {
                settings.onActivated();
            } catch (RuntimeException failure) {
                Client33.LOG.error("Could not activate settings for module '{}'.", name, failure);
                return;
            }
        }
        lifecycle.request(enabling, runInMainMenu || Modules.get().canRunModules());
    }

    void updateAvailability() {
        lifecycle.availability(runInMainMenu || Modules.get().canRunModules());
    }

    public void enable() {
        setEnabled(true);
    }

    public void disable() {
        setEnabled(false);
    }

    public void sendToggledMsg() {
        if (Config.get().chatFeedback.get() && chatFeedback) {
            ChatUtils.forceNextPrefixClass(getClass());
            ChatUtils.sendMsg(this.hashCode(), ChatFormatting.GRAY, "Toggled (highlight)%s(default) %s(default).", title, isActive() ? ChatFormatting.GREEN + "on" : ChatFormatting.RED + "off");
        }
    }

    public void info(Component message) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.sendMsg(title, message);
    }

    public void info(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.infoPrefix(title, message, args);
    }

    public void warning(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.warningPrefix(title, message, args);
    }

    public void error(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.errorPrefix(title, message, args);
    }

    public boolean isActive() {
        return lifecycle.enabled();
    }

    public String getInfoString() {
        return null;
    }

    @Override
    public CompoundTag toTag() {
        if (!serialize) return null;
        CompoundTag tag = new CompoundTag();

        tag.putString("name", name);
        tag.put("keybind", keybind.toTag());
        tag.putBoolean("toggleOnKeyRelease", toggleOnBindRelease);
        tag.putBoolean("chatFeedback", chatFeedback);
        tag.putBoolean("favorite", favorite);
        tag.put("settings", settings.toTag());
        tag.putBoolean("active", isActive());

        return tag;
    }

    @Override
    public Module fromTag(CompoundTag tag) {
        // General
        keybind.fromTag(tag.getCompoundOrEmpty("keybind"));
        toggleOnBindRelease = tag.getBooleanOr("toggleOnKeyRelease", false);
        chatFeedback = !tag.contains("chatFeedback") || tag.getBooleanOr("chatFeedback", false);
        favorite = tag.getBooleanOr("favorite", false);

        // Settings
        Tag settingsTag = tag.get("settings");
        if (settingsTag instanceof CompoundTag compoundTag) settings.fromTag(compoundTag);

        boolean active = tag.getBooleanOr("active", false);
        if (active != isActive()) toggle();

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Module module = (Module) o;
        return Objects.equals(name, module.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public int compareTo(@NonNull Module o) {
        return name.compareTo(o.name);
    }
}
