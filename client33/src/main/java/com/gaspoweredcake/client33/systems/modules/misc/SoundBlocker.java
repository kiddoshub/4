
package com.gaspoweredcake.client33.systems.modules.misc;

import com.gaspoweredcake.client33.events.world.PlaySoundEvent;
import com.gaspoweredcake.client33.settings.Setting;
import com.gaspoweredcake.client33.settings.SettingGroup;
import com.gaspoweredcake.client33.settings.SoundEventListSetting;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

import java.util.List;

public class SoundBlocker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<SoundEvent>> sounds = sgGeneral.add(new SoundEventListSetting.Builder()
        .name("sounds")
        .description("Sounds to block.")
        .build()
    );

    public SoundBlocker() {
        super(Categories.Misc, "sound-blocker", "Cancels out selected sounds.");
    }

    @EventHandler
    private void onPlaySound(PlaySoundEvent event) {
        for (SoundEvent sound : sounds.get()) {
            if (sound.location().equals(event.sound.getIdentifier())) {
                event.cancel();
                break;
            }
        }
    }

    public boolean shouldBlock(SoundInstance soundInstance) {
        return isActive() && sounds.get().contains(Setting.parseId(BuiltInRegistries.SOUND_EVENT, soundInstance.getIdentifier().getPath()));
    }
}
