package com.gaspoweredcake.client33.events.world;

import com.gaspoweredcake.client33.utils.intel.SectionIntel;

/** Detached snapshot delivered on the client thread after worker processing. */
public record SectionIntelEvent(SectionIntel observation) {}
