<!-- coderabbit-ai-generated:v1 -->

# Model surface effects and Fracture materials

## ESP

Select FlowingContours, RadarSweep, GhostTrails, OrbitingParticles or TwoLayer in ESP Mode. Signature Accent, Animate and Signature Through Walls apply to these modes. Model Surface Effects contains their controls.

- FlowingContours intersects actual model faces with moving height planes, drawing feathered ribbons.
- RadarSweep clips faces into a moving band with a bright central contour.
- GhostTrails retains detached historical model poses, samples movement at most every 80 ms, expires poses and clears on teleports greater than 16 blocks, mode/world changes and deactivation. Disabling Animate clears trails.
- OrbitingParticles traverses welded silhouette edge chains with sparse star points. Disconnected body parts can have separate chains.
- TwoLayer draws low-opacity dashed ribbons without depth testing, then crisp ribbons with depth testing. Through Walls disables the first pass when turned off.

Captured meshes are capped at 512 faces. Surface Budget defaults to 16 entities, maximum 32; trails default to four poses, maximum six. Targets and nearby entities are prioritized. Fine contour/particle detail reduces beyond 48 and 128 blocks. Large custom models can be truncated by the face limit.

Ribbon width uses camera distance, projection scale and framebuffer height to preserve approximate pixel width. Bright cores have feathered side strips; this does not require a full-screen bloom pass. Extended Surface Range applies only to these new modes, bypassing the existing terrain render-radius filter for entities already received by the client. Max Distance still applies (up to the existing 512-block limit). No server tracking expansion or unloaded entity discovery is implemented.

## Fracture Chams

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:dc2c0f6f8679d1bd753a1f0646f194aa0314b5f0e746e010b90c0d6d5917a90c -->

Fracture Pattern selects **Cellular** sculpted plates, **Nested** geometric inlays, **Veined** directional currents or **Branching** connected growth systems. Existing pattern indices are retained, with Branching appended. All four preserve the same screen-space silhouette, coordinates, cell scale and camera perspective.

Fracture Material retains colors, opacity, cell size, channel width, halo, depth, deformation and animation speed. Fracture Composition adds detail density, roughness, directional light angle and amber/gold tonal variation. Fracture Engraving adds curved stroke strength/spacing, charcoal ink contrast and ochre warmth. Animation speed zero freezes procedural phase. Engraving is concentrated in broad shaded regions; fine subdivisions leave quiet surfaces around them. Channels are recessed, with variable bevel widths, restrained metallic highlights and shared directional lighting.

Material Palette offers **Custom**, **OchrePrint**, **NeonStorm** and **IvoryNocturne**. Custom retains the user's surface/seam colors and editable metal accent. Other palettes preserve those saved colors and use preset RGB values; surface and seam opacity still apply. OchrePrint combines warm illustrated-print colors and charcoal channels. NeonStorm uses indigo negative space, violet channel light and warm gold/coral glints. IvoryNocturne uses broad ivory focal illumination, warm amber edges, deep shadows and reduced engraving. These are procedural interpretations of the supplied visual references; the pictures are not bundled as material assets.

Cellular and Nested evaluate one bounded 9-candidate/25-neighbor Voronoi search. Nested uses an analytic eight-point inlay instead of a second search. Veined and Branching skip the Voronoi search; Branching uses at most six quadratic forks/offshoots, each approximated with six segments. The outside halo uses sixteen bounded mask samples and can be disabled. No extra framebuffer or full-screen rendering pass is added. Hardware performance remains unverified. Relief normals are screen-space approximations: model normals, physically based reflection, motion-vector reprojection and temporal upscaling are not implemented.

### Upload PNG or animated GIF

Open Chams settings and use **Upload / Replace PNG or GIF**. The SDL file picker selects a local file; a single worker snapshots, validates and decodes it, then stores an immutable content-addressed copy under the client `materials/` folder. Uploaded Material provides playback enable/speed, image tiling and Fracture image blend. Remove restores the procedural/resource material, and Reload rereads the saved file. Failed or superseded imports keep the last valid image.

Image mode displays the image through the entity mask. Fracture blends it beneath the chosen procedural relief and engraving; image alpha reveals the procedural surface. Aspect-correct cover sampling preserves image proportions. This is screen-space mapping, not model UV wrapping or automatic style transfer from a reference. Silhouette and camera matrices remain unchanged.

GIF decoding composes frame offsets, transparency, restore-to-background and restore-to-previous disposal, retaining each frame delay with a 20 ms minimum. Playback loops and freezes the current frame when disabled. A reusable GPU texture uploads only when the displayed frame changes. Source limits are 16 MiB, 2048 pixels per side and 512 frames; oversized inputs are rejected clearly. Textures resize proportionally to at most 1024 pixels per side and a 64 MiB decoded animation budget; frames are not silently dropped. The Fracture uniform block has eight vec4 slots (128 bytes); Image mode has two (32 bytes).


<!-- /coderabbit-ai-generated:v1 -->
## GUI readability

The module library opens directly to tools and paginates its responsive card grid. Module descriptions remain available as tooltips. Module editors initially show their first section, provide a section selector and search across all sections. Text size is preserved. The Strata workspace has category navigation, Favorites/Enabled filters, high-contrast text and paged cards. Existing saved theme colors remain user-controlled.

## Research and provenance

The previous shader used second-nearest minus nearest distance as a boundary proxy and frame duration as phase. The timing repair is retained. The new geometry and material code was written for this repository; external code was inspected as reference rather than copied.

- [glsl-aastep](https://github.com/glslify/glsl-aastep), MIT: derivative-based smoothing informed the seam anti-aliasing design.
- [Filament standard surface shading](https://github.com/google/filament/blob/main/shaders/src/surface_shading_model_standard.fs), Apache-2.0: inspected for the distinction between directional surface response and actual normal-dependent material lobes. The current bevel effect is an artistic approximation.
- [AMD FidelityFX SDK](https://github.com/GPUOpen-LibrariesAndSDKs/FidelityFX-SDK): inspected for future temporal rendering requirements. No SDK code, temporal upscaler or frame generation is integrated.

## Verification limits

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:7f0fafe0dae2bbad25319000c1361111a6a0dd0b2c21c958e023d4da49501905 -->

Independent regressions exercise contour intersections, band clipping, connected silhouettes, particle position, distance-scaled strokes, history bounds/expiry/teleports, animation timing, pagination and PNG/GIF composition/timing/resource bounds. The GLSL 330 shader fixture renders all four structures in four palettes, verifies animated/static phases, mask/opacity and uniform layouts, and exercises uploaded-image blending and texture updates. Generated study images use synthetic masks; they do not demonstrate native Minecraft rendering.

<!-- /coderabbit-ai-generated:v1 -->

Full client compilation and native/Preview gameplay checks remain blocked by the unfinished Minecraft 26.3 port. These checks do not establish in-game appearance, hardware performance or multiplayer behavior.
