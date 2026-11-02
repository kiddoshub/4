<!-- coderabbit-ai-generated:v1 -->

# Security audit: client 33 and source projects

## Scope and conclusion

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:f2c5b47a1f91dd722e6bdfae8257ed47af1d8d4c7c2dc2c74112cfa14f8174a6 -->

Reviewed client 33 at `94dbebdad0f98e4ce73d778c0204fc4b0e8610ef` plus the supplied changes, and selectively inspected OpenZen at `9577d2f78c8081257de8eea3ad24262a144367ba` and Payload at `5fdb104a3efe025275591a5446b4c5ad4cfd2a96`.

<!-- /coderabbit-ai-generated:v1 --> This was a targeted source and build inspection, not an exhaustive audit or proof that the client is free of malware. The requested full feature integration has not been completed.

## Findings

### High: OpenZen WebUI exposes local files and controls

When enabled, OpenZen's [WebUI](https://github.com/vcvvve4/efwwf/blob/9577d2f78c8081257de8eea3ad24262a144367ba/src/main/java/shit/zen/modules/impl/world/WebUI.java#L58) listens on all interfaces and has unauthenticated module and setting endpoints. On the DLL resource path, [StaticFileHandler](https://github.com/vcvvve4/efwwf/blob/9577d2f78c8081257de8eea3ad24262a144367ba/src/main/java/shit/zen/network/webui/StaticFileHandler.java#L21) allows paths to escape the resource directory. A loopback test using the original handler read a synthetic file outside that directory. The WebUI module, handlers and assets have been removed from the OpenZen checkout. No such component was found in the reviewed 33 source or as a named top-level JAR entry.

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:0c338a3f549cd1f01805232746df744c99f807d0fb0a135397fbff915e8f1814 -->

### High: Swarm trusted unauthenticated commands — removed from 33 patch

<!-- /coderabbit-ai-generated:v1 -->

[SwarmWorker](https://github.com/vcvvve4/dwwdaq/blob/94dbebdad0f98e4ce73d778c0204fc4b0e8610ef/src/main/java/com/gaspoweredcake/client33/systems/modules/misc/swarm/SwarmWorker.java#L21) connects over plain TCP and dispatches received Swarm commands without authenticating the host. A malicious or impersonated host or an attacker able to modify the connection can send game chat or server commands and toggle modules through [SwarmCommand](https://github.com/vcvvve4/dwwdaq/blob/94dbebdad0f98e4ce73d778c0204fc4b0e8610ef/src/main/java/com/gaspoweredcake/client33/commands/commands/SwarmCommand.java#L357). This does not establish operating-system command execution. [SwarmHost](https://github.com/vcvvve4/dwwdaq/blob/94dbebdad0f98e4ce73d778c0204fc4b0e8610ef/src/main/java/com/gaspoweredcake/client33/systems/modules/misc/swarm/SwarmHost.java#L19) also accepts unauthenticated peers on all interfaces. <!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:571c4cf350a1fb211c7bfb15ddd71c0a0e794598b4e049f0febbcf2aca268190 -->

The module, command and connection classes have been removed in the supplied 33 patch.

<!-- /coderabbit-ai-generated:v1 -->

### High: Payload contains hardware identifier collection code — not found active in 33

[PlayerVectorMixin](https://github.com/vcvvve4/add/blob/5fdb104a3efe025275591a5446b4c5ad4cfd2a96/payload/src/main/java/net/payload/interfaces/PlayerVectorMixin.java#L10) collects CPU ID, MAC address and disk serial, then sends the combined value to `payload.tech`. The domain is assembled from byte literals. The inspected source has no direct caller of `checkValidity`, and this class is not declared as a mixin. That does not prove a binary cannot invoke it indirectly. This code is not present by name in the reviewed 33 source or top-level JAR entries. Do not import it or opaque Payload binaries without further review.

### High: saved account credentials and HTTP authentication — removed from 33 patch

The original 33 account system serializes Microsoft refresh tokens and session tokens to `client33/accounts.nbt` and sends TheAltening authentication over HTTP. The account manager, TheAltening support, proxy manager, screens, connection hooks and associated bundled libraries have been removed in the supplied patch. Existing saved `accounts.nbt`, backups and proxy configuration files are not automatically deleted; review and remove them locally if no longer needed. The Minecraft launcher still supplies the normal game account.

### Medium: Payload account storage and global chat — not imported

Payload's [AltManager](https://github.com/vcvvve4/add/blob/5fdb104a3efe025275591a5446b4c5ad4cfd2a96/payload/src/main/java/net/payload/altmanager/AltManager.java#L48) derives its key from machine information, uses AES ECB and returns plaintext if encryption fails. Its [global chat](https://github.com/vcvvve4/add/blob/5fdb104a3efe025275591a5446b4c5ad4cfd2a96/payload/src/main/java/net/payload/cmd/GlobalChat.java#L68) starts during initialization, sends the username to a third-party host and uses plain TCP. These source components were not found by name in 33. The separate Payload project was not modified.

### Medium: supply-chain assurance remains incomplete

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:2a89892789d32ee8ec3f9c8b54440187c9cdfe340c208a33ff2023f516d1058c -->

The built 33 JAR includes 24 nested libraries, including native libraries and third-party mods. The 73.4 MiB archive includes 53.1 MiB of Skija libraries for several platforms and 11.5 MiB of SQLite; the bundled Fabric API also includes two modules packaged separately. These libraries support existing features or cross-platform use; removing them requires compatibility testing. <!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:cdf6b9da41ee108307130cfe40d9ac04d2a4a40338a7242826cb6f5c61577aca -->

The build uses remote repositories and lacks a checked-in Gradle dependency verification file. The previously bundled wrapper differed from Gradle 9.8.0’s published wrapper checksum; it has been replaced with the verified official wrapper, and the distribution checksum is pinned.

<!-- /coderabbit-ai-generated:v1 -->

<!-- /coderabbit-ai-generated:v1 --> The inventory is not a code review of those dependencies. Pin and verify build inputs, audit the actual dependency binaries and perform network and runtime tests before distributing a final JAR. The OpenZen repository also includes an original Zen JAR; its README alleges backdoor behavior in that original binary. This audit did not independently analyze or execute it.

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:6943c4b7281bb9d833c803098b4fecf629959cfed447fe2124a3012a10dcf862 -->

### Data integrity: profile paths and stash persistence — addressed

A blank or invalid profile name could resolve to the profiles root, risking unrelated profile data during deletion. Profile directory operations now reject invalid names and symlinks. StashFinder now loads complete JSON or CSV records independently, protects unreadable files, and writes through staged replacement; clearing entries is persisted.

### Availability: player-head image decoding — addressed

Downloaded skin images had no byte or dimension limit and repeatedly copied full raster data while reading pixels. Downloads and image dimensions are now bounded, and malformed or incomplete texture data is rejected.

## Checks and limits

<!-- /coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:907dd704d89b95095c39d49ff4c3147633ac23420c6334110e61507bee223d59 -->

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:b4235d2fd0803e7c7d00624b7110fbd482cf9a463e0eec9a968f676dd251e6c3 -->

- <!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:2ea06cd35d11923c97607348af068481d64c012570bb1658045a8108e24f0e22 -->

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:d9bc2fc022cb77ed35d259e9ef6ce85213312800f4710562c1e6979abc29c8b0 -->

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:370bffe67c62c176be0c4f42bb66d4ea43ab6e45633b612a502820fc1fb10609 -->

Client 33 build, regression suite and bundled runtime verification passed with Java 25, including ESP culling checks. <!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:cae7685fff7eccd9d3fa11025dc49c183923f2d43a58f014128462a2cbe74ab4 -->

A headless development launch loaded a synthetic single-player world with Fabric, Sodium and Baritone using a software Vulkan backend. ESP visuals, pathfinding, multiplayer, teleport, inventory and real-account login remain unverified.

<!-- /coderabbit-ai-generated:v1 --> <!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:a585cd8a64b5bf1bfcb131b1abe10951116dad89a482319497b268488d86e9f6 -->

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:dc376ef1650a670b0046fe56885d08feb066a48fd58ec1f9791291e00af97d18 -->

The current JAR hash is `e454f5499434455deb4958a86b71faca5efa49a3df57399887b061166ee50056`.

<!-- /coderabbit-ai-generated:v1 -->

<!-- /coderabbit-ai-generated:v1 -->

<!-- /coderabbit-ai-generated:v1 -->

<!-- /coderabbit-ai-generated:v1 -->

<!-- /coderabbit-ai-generated:v1 -->

<!-- /coderabbit-ai-generated:v1 -->

<!-- /coderabbit-ai-generated:v1 -->
- Static scan found no references to the removed account/proxy/Discord classes and confirmed declared mixin source files exist. JAR entry inventory found no matching top-level OpenZen, Payload, TheAltening, Discord IPC or removed authentication and proxy library names. This is not proof about nested binaries or renamed code.
<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:5a04bebc9cb90c45d646f7be02b133cd7cad1fda6bfeeddeb80755adfc049794 -->

- OpenZen traversal was reproduced in an isolated loopback test with synthetic data before WebUI removal. The OpenZen project compiled successfully with that removal.
<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:714aeb2be16ded7fdbb2a5d8132f1d4b88832534a670705f98bc36f4ef975984 -->

- The 33 patch, including the removals and feature changes, was checked and applied successfully to a clean worktree at the specified commit; it is a separate deliverable because 33 is a different repository from this task checkout.

<!-- /coderabbit-ai-generated:v1 -->

<!-- /coderabbit-ai-generated:v1 -->
<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:4c89e90a62f7732e33b6529a980ea80552b61612265b3a140217d51a71c96886 -->

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:75e07105e1f87f78b5ff82ae23dd84cfe298c9e8aa4007dda6d7fa4229580de0 -->

- <!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:66f2b176299630bd46c05fa84c3aad3864f994e2a7d1e01cb53c96d4f055eab3 -->

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:8431a5a94ec1ca4ca2faf48b945f6a6e3e9de27bb2307781add6d21dd3679366 -->

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:e2557603b4ba1dd4a33ebb409b975578b203fdfbd32ad647c6cead2d05c83f89 -->

ClamAV 1.5.4 scanned 6,650 distinct files extracted from a previous JAR and 77 nested ZIP/JAR archives: zero detections. <!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:57ff4e44ab380336c24fb14980838900daaa69244317fdc999d54a6decd33e01 -->

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:7779b5076fa5c15373833ec535ee1b7727d9f3ee5c6524bccf3bd66482613259 -->

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:8a055a7803756f6b6d7b1a019e318eab1e6887bc05baee4b809f3a9cd40ac988 -->

Subsequent scans covered seven, 13, one, nine, two, 19 and nine changed files, all with zero detections; the replaced Gradle wrapper also had zero detections. A further scan of 20 newly distinct entries from the current candidate found zero detections; the embedded JARs matched the prior candidate by SHA-256.

<!-- /coderabbit-ai-generated:v1 -->

<!-- /coderabbit-ai-generated:v1 -->

<!-- /coderabbit-ai-generated:v1 --> Other current top-level entries match previously scanned files by SHA-256; no nested archives changed. Other embedded archive formats were not expanded. The initial direct archive scan hit its time limit; that warning was not a malware identification. Signature scanning cannot guarantee the absence of malicious code.

<!-- /coderabbit-ai-generated:v1 -->

<!-- /coderabbit-ai-generated:v1 -->

<!-- /coderabbit-ai-generated:v1 -->

<!-- /coderabbit-ai-generated:v1 -->
<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:271f9bfe207e6892e290b3c606279b51a4ac1348b4bb6ff570484420455e1ac8 -->

- Book clipboard imports previously used unbounded NBT decoding and could exhaust memory; copies and pastes now share a bounded decoder that also accepts older uncompressed exports. Page indexes are clamped.
- When resource-pack spoofing was enabled, a malformed server-provided URL could throw while constructing the chat link. URLs are now validated before use and pack acceptance is tied to the originating connection.
<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:46549fa603c53d32e51f6fd50a020890ee17e361feb4912fb9acab03262e1876 -->

<!-- coderabbit-ai-generated:v1 -->

<!-- coderabbit-ai-generated-section:v1:851776a7b0a1a4afc5570f7e80c4dd5974cf47f2ed50925365759ba95d33f341 -->

- OSV returned no advisory matches for 18 queried direct Maven coordinates and versions, and 48 additional queries covering nested Fabric API modules and Bobby libraries. This does not cover unindexed advisories, Maven coordinates that OSV does not track, all nested dependencies, or dependency provenance.

<!-- /coderabbit-ai-generated:v1 -->
- No claim is made that all RATs, viruses or vulnerabilities have been found. No final combined release has been produced.

<!-- /coderabbit-ai-generated:v1 -->

<!-- /coderabbit-ai-generated:v1 -->

<!-- /coderabbit-ai-generated:v1 -->
