
package com.gaspoweredcake.client33.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntitySectionStorage.class)
public interface EntitySectionStorageAccessor {
    @Accessor("sectionIds")
    LongSortedSet client33$getSectionIds();

    @Accessor("sections")
    <T extends EntityAccess> Long2ObjectMap<EntitySection<T>> client33$getSections();
}
