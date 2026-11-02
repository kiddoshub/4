
package com.gaspoweredcake.client33.utils.notebot.instrumentdetect;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.NoteBlock;

public enum InstrumentDetectMode {
    BlockState(((noteBlock, _) -> noteBlock.getValue(NoteBlock.INSTRUMENT))),
    BelowBlock(((_, blockPos) -> Minecraft.getInstance().level.getBlockState(blockPos.below()).instrument()));

    private final InstrumentDetectFunction instrumentDetectFunction;

    InstrumentDetectMode(InstrumentDetectFunction instrumentDetectFunction) {
        this.instrumentDetectFunction = instrumentDetectFunction;
    }

    public InstrumentDetectFunction getInstrumentDetectFunction() {
        return instrumentDetectFunction;
    }
}
