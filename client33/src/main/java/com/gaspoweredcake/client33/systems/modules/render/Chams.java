
package com.gaspoweredcake.client33.systems.modules.render;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.gui.GuiTheme;
import com.gaspoweredcake.client33.gui.widgets.WWidget;
import com.gaspoweredcake.client33.gui.widgets.WLabel;
import com.gaspoweredcake.client33.utils.render.MaterialFilePicker;
import com.gaspoweredcake.client33.utils.render.UploadedMaterial;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.utils.render.color.SettingColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.Identifier;

import java.util.Set;

public class Chams extends Module {
    private final SettingGroup sgThroughWalls = settings.createGroup("Through Walls");
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgCrystals = settings.createGroup("Crystals");
    private final SettingGroup sgHand = settings.createGroup("Hand");

    // Through walls

    public final Setting<Set<EntityType<?>>> entities = sgThroughWalls.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Select entities to show through walls.")
        .onlyAttackable()
        .build()
    );

    public final Setting<Shader> shader = sgThroughWalls.add(new EnumSetting.Builder<Shader>()
        .name("shader")
        .description("Renders a shader over of the entities.")
        .defaultValue(Shader.Image)
        .build()
    );

    public final Setting<SettingColor> shaderColor = sgThroughWalls.add(new ColorSetting.Builder()
        .name("color")
        .description("The color that the image shader is drawn with.")
        .defaultValue(new SettingColor(157, 111, 255, 150))
        .visible(() -> shader.get() == Shader.Image)
        .build()
    );

    private final SettingGroup sgImage = settings.createGroup("Uploaded Material");
    private boolean storingImage;
    private WLabel imageStatus;
    public final Setting<String> imageFile = sgImage.add(new StringSetting.Builder().name("image-file")
        .description("Local PNG or animated GIF, copied into the client materials folder. Use Upload below to select it.")
        .defaultValue("").onChanged(this::imageFileChanged)
        .onModuleActivated(setting -> { if (!setting.get().isBlank()) imageFileChanged(setting.get()); }).build());
    public final Setting<Boolean> imageAnimate = sgImage.add(new BoolSetting.Builder().name("animate-image")
        .description("Play uploaded GIF frames using their original delays. Turning off freezes the current frame.").defaultValue(true).build());
    public final Setting<Double> imageSpeed = sgImage.add(new DoubleSetting.Builder().name("image-speed")
        .description("GIF playback multiplier; does not change fracture animation speed.")
        .defaultValue(1).range(0.1,4).sliderRange(0.1,2).visible(imageAnimate::get).build());
    public final Setting<Double> imageBlend = sgImage.add(new DoubleSetting.Builder().name("image-blend")
        .description("Uploaded image contribution beneath Fracture lighting and engraving. Transparency reveals the procedural surface.")
        .defaultValue(0.85).range(0,1).sliderRange(0,1).visible(() -> shader.get()==Shader.Fracture).build());
    public final Setting<Double> imageScale = sgImage.add(new DoubleSetting.Builder().name("image-tiling")
        .description("Image repetition across the screen-space entity mask, preserving image proportions.")
        .defaultValue(1).range(0.25,8).sliderRange(0.25,4).build());

    private void imageFileChanged(String file) {
        if (storingImage) return;
        UploadedMaterial.request(file, message -> { if (imageStatus != null) imageStatus.set(message); }, asset -> {
            storingImage = true;
            try { imageFile.set(asset); } finally { storingImage = false; }
        });
    }

    @Override public WWidget getWidget(GuiTheme theme) {
        var list = theme.verticalList();
        imageStatus = list.add(theme.label(UploadedMaterial.status)).expandX().widget();
        var actions = list.add(theme.horizontalList()).expandX().widget();
        var upload = actions.add(theme.button("Upload / Replace PNG or GIF")).widget();
        upload.action = () -> MaterialFilePicker.open(path -> imageFile.set(path.toString()));
        var remove = actions.add(theme.button("Remove")).widget(); remove.action = () -> imageFile.set("");
        var reload = actions.add(theme.button("Reload")).widget(); reload.action = () -> imageFileChanged(imageFile.get());
        return list;
    }

    private final SettingGroup sgFracture = settings.createGroup("Fracture material");

    private final SettingGroup sgComposition = settings.createGroup("Fracture composition");
    private final SettingGroup sgPrint = settings.createGroup("Fracture engraving");

    public enum FracturePalette { Custom, OchrePrint, NeonStorm, IvoryNocturne }
    public final Setting<FracturePalette> fracturePalette = sgFracture.add(new EnumSetting.Builder<FracturePalette>()
        .name("material-palette").description("Custom colors, ochre print, violet neon, or ivory focal lighting.")
        .defaultValue(FracturePalette.Custom).visible(() -> shader.get()==Shader.Fracture).build());
    public final Setting<SettingColor> fractureAccent = sgFracture.add(new ColorSetting.Builder()
        .name("accent-color").description("Metal edge accent for the Custom palette.")
        .defaultValue(new SettingColor(234, 183, 80)).visible(() -> shader.get()==Shader.Fracture && fracturePalette.get()==FracturePalette.Custom).build());

    public final Setting<SettingColor> fractureColor = sgFracture.add(new ColorSetting.Builder()
        .name("cell-color").description("Surface color for Custom palette; opacity applies to every palette.")
        .defaultValue(new SettingColor(188, 139, 44, 230)).visible(() -> shader.get() == Shader.Fracture).build());
    public final Setting<SettingColor> fractureSeam = sgFracture.add(new ColorSetting.Builder()
        .name("seam-color").description("Seam and halo color for Custom palette; opacity applies to every palette.")
        .defaultValue(new SettingColor(255, 238, 153, 255)).visible(() -> shader.get() == Shader.Fracture).build());
    public final Setting<Double> fractureScale = sgFracture.add(new DoubleSetting.Builder()
        .name("cell-size").description("Approximate cell size in screen pixels.")
        .defaultValue(24).range(6, 80).sliderRange(6, 80).visible(() -> shader.get() == Shader.Fracture).build());
    public final Setting<Double> fractureWidth = sgFracture.add(new DoubleSetting.Builder()
        .name("seam-width").description("Width of recessed fracture channels in screen pixels.")
        .defaultValue(1.3).range(0.3, 5).sliderRange(0.3, 5).visible(() -> shader.get() == Shader.Fracture).build());
    public final Setting<Double> fractureGlow = sgFracture.add(new DoubleSetting.Builder()
        .name("halo").description("Intensity of the soft screen-space edge halo; zero disables it.")
        .defaultValue(0.45).range(0, 1).sliderRange(0, 1).visible(() -> shader.get() == Shader.Fracture).build());
    public final Setting<Double> fractureSpeed = sgFracture.add(new DoubleSetting.Builder()
        .name("animation-speed").description("Speed of the moving cells and seam shimmer; zero makes them static.")
        .defaultValue(0.35).range(0, 2).sliderRange(0, 2).visible(() -> shader.get() == Shader.Fracture).build());

    public enum FracturePattern { Cellular, Nested, Veined, Branching }
    public final Setting<FracturePattern> fracturePattern = sgFracture.add(new EnumSetting.Builder<FracturePattern>()
        .name("fracture-pattern").description("Sculpted plates, geometric inlays, flowing veins or organic branching channels.")
        .defaultValue(FracturePattern.Cellular).visible(() -> shader.get()==Shader.Fracture).build());

    public final Setting<Double> fractureDepth = sgFracture.add(new DoubleSetting.Builder()
        .name("surface-depth").description("Recess depth and directional edge relief.")
        .defaultValue(0.55).range(0,1).sliderRange(0,1).visible(() -> shader.get()==Shader.Fracture).build());
    public final Setting<Double> fractureMotion = sgFracture.add(new DoubleSetting.Builder()
        .name("cell-motion").description("Amount of smooth cell deformation; zero retains fixed cells.")
        .defaultValue(0.6).range(0,1).sliderRange(0,1).visible(() -> shader.get()==Shader.Fracture).build());
    public final Setting<Double> fractureFlow = sgFracture.add(new DoubleSetting.Builder()
        .name("seam-flow").description("Travelling light intensity along the seams.")
        .defaultValue(0.65).range(0,1).sliderRange(0,1).visible(() -> shader.get()==Shader.Fracture).build());

    public final Setting<Double> fractureDensity = sgComposition.add(new DoubleSetting.Builder()
        .name("detail-density").description("Secondary fracture coverage; lower values leave larger quiet surfaces.")
        .defaultValue(0.55).range(0,1).sliderRange(0,1).visible(() -> shader.get()==Shader.Fracture).build());
    public final Setting<Double> fractureRoughness = sgComposition.add(new DoubleSetting.Builder()
        .name("surface-roughness").description("Broadens and softens metallic highlights with subtle surface variation.")
        .defaultValue(0.38).range(0,1).sliderRange(0,1).visible(() -> shader.get()==Shader.Fracture).build());
    public final Setting<Double> fractureLightAngle = sgComposition.add(new DoubleSetting.Builder()
        .name("light-direction").description("Direction of the shared screen-space material light, in degrees.")
        .defaultValue(125).range(0,360).sliderRange(0,360).visible(() -> shader.get()==Shader.Fracture).build());
    public final Setting<Double> fractureTonalVariation = sgComposition.add(new DoubleSetting.Builder()
        .name("tonal-variation").description("Natural amber shadows and pale metallic edge variation.")
        .defaultValue(0.5).range(0,1).sliderRange(0,1).visible(() -> shader.get()==Shader.Fracture).build());

    public final Setting<Double> fractureEngraving = sgPrint.add(new DoubleSetting.Builder()
        .name("engraving-strength").description("Curved print-style hatching concentrated in shaded surface regions.")
        .defaultValue(0.4).range(0,1).sliderRange(0,1).visible(() -> shader.get()==Shader.Fracture).build());
    public final Setting<Double> fractureEngravingSpacing = sgPrint.add(new DoubleSetting.Builder()
        .name("engraving-spacing").description("Spacing of engraved strokes in screen pixels; antialiasing preserves thin lines.")
        .defaultValue(4).range(2,12).sliderRange(2,12).visible(() -> shader.get()==Shader.Fracture).build());
    public final Setting<Double> fractureInkContrast = sgPrint.add(new DoubleSetting.Builder()
        .name("ink-contrast").description("Charcoal depth of engraved strokes and recessed channels.")
        .defaultValue(0.55).range(0,1).sliderRange(0,1).visible(() -> shader.get()==Shader.Fracture).build());
    public final Setting<Double> fracturePaperWarmth = sgPrint.add(new DoubleSetting.Builder()
        .name("print-warmth").description("Warm ochre wash inspired by aged illustrated prints.")
        .defaultValue(0.25).range(0,1).sliderRange(0,1).visible(() -> shader.get()==Shader.Fracture).build());

    public final Setting<Boolean> ignoreSelfDepth = sgThroughWalls.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Ignores yourself drawing the player.")
        .defaultValue(true)
        .build()
    );

    // Players

    public final Setting<Boolean> players = sgPlayers.add(new BoolSetting.Builder()
        .name("players")
        .description("Enables model tweaks for players.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> ignoreSelf = sgPlayers.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Ignores yourself when tweaking player models.")
        .defaultValue(false)
        .visible(players::get)
        .build()
    );

    public final Setting<Boolean> playersTexture = sgPlayers.add(new BoolSetting.Builder()
        .name("texture")
        .description("Enables player model textures.")
        .defaultValue(false)
        .visible(players::get)
        .build()
    );

    public final Setting<SettingColor> playersColor = sgPlayers.add(new ColorSetting.Builder()
        .name("color")
        .description("The color of player models.")
        .defaultValue(new SettingColor(157, 111, 255, 150))
        .visible(players::get)
        .build()
    );

    public final Setting<Double> playersScale = sgPlayers.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Players scale.")
        .defaultValue(1.0)
        .min(0.0)
        .visible(players::get)
        .build()
    );

    // Crystals

    public final Setting<Boolean> crystals = sgCrystals.add(new BoolSetting.Builder()
        .name("crystals")
        .description("Enables model tweaks for end crystals.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Double> crystalsScale = sgCrystals.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Crystal scale.")
        .defaultValue(0.6)
        .min(0)
        .visible(crystals::get)
        .build()
    );

    public final Setting<Double> crystalsBounce = sgCrystals.add(new DoubleSetting.Builder()
        .name("bounce")
        .description("How high crystals bounce.")
        .defaultValue(0.6)
        .min(0.0)
        .visible(crystals::get)
        .build()
    );

    public final Setting<Double> crystalsRotationSpeed = sgCrystals.add(new DoubleSetting.Builder()
        .name("rotation-speed")
        .description("Multiplies the rotation speed of the crystal.")
        .defaultValue(0.3)
        .min(0)
        .visible(crystals::get)
        .build()
    );

    public final Setting<Boolean> crystalsTexture = sgCrystals.add(new BoolSetting.Builder()
        .name("texture")
        .description("Whether to render crystal model textures.")
        .defaultValue(true)
        .visible(crystals::get)
        .build()
    );

    public final Setting<SettingColor> crystalsColor = sgCrystals.add(new ColorSetting.Builder()
        .name("crystal-color")
        .description("The color of the of the crystal.")
        .defaultValue(new SettingColor(157, 111, 255, 255))
        .visible(crystals::get)
        .build()
    );

    // Hand

    public final Setting<Boolean> hand = sgHand.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Enables tweaks of hand rendering.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> handTexture = sgHand.add(new BoolSetting.Builder()
        .name("texture")
        .description("Whether to render hand textures.")
        .defaultValue(false)
        .visible(hand::get)
        .build()
    );

    public final Setting<SettingColor> handColor = sgHand.add(new ColorSetting.Builder()
        .name("hand-color")
        .description("The color of your hand.")
        .defaultValue(new SettingColor(157, 111, 255, 150))
        .visible(hand::get)
        .build()
    );

    public static final Identifier BLANK = Client33.identifier("textures/blank.png");

    public Chams() {
        super(Categories.Render, "chams", "Tweaks rendering of entities.");
    }

    public boolean shouldRender(Entity entity) {
        return isActive() && !isShader() && entities.get().contains(entity.getType()) && (entity != mc.player || !ignoreSelfDepth.get());
    }

    public boolean isShader() {
        return isActive() && shader.get() != Shader.None;
    }

    public enum Shader {
        Image,
        None,
        Fracture
    }
}
