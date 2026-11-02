
package com.gaspoweredcake.client33.utils.tooltip;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.utils.render.RenderUtils;
import com.gaspoweredcake.client33.utils.render.color.Color;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class ContainerTooltipComponent implements ClientTooltipComponent, Client33TooltipData {
    private static final Identifier TEXTURE_CONTAINER_BACKGROUND = Client33.identifier("textures/container.png");

    private final ItemStack[] items;
    private final Color color;

    public ContainerTooltipComponent(ItemStack[] items, Color color) {
        this.items = items;
        this.color = color;
    }

    @Override
    public ClientTooltipComponent getComponent() {
        return this;
    }

    @Override
    public int getHeight(@NonNull Font textRenderer) {
        return 67;
    }

    @Override
    public int getWidth(@NonNull Font textRenderer) {
        return 176;
    }

    @Override
    public void extractImage(@NonNull Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
        // Background
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_CONTAINER_BACKGROUND, x, y, 0, 0, 176, 67, 176, 67, color.getPacked());

        // Contents
        int row = 0;
        int i = 0;

        for (ItemStack itemStack : items) {
            RenderUtils.drawItem(graphics, itemStack, x + 8 + i * 18, y + 7 + row * 18, 1, true, null, false);

            i++;
            if (i >= 9) {
                i = 0;
                row++;
            }
        }
    }
}
