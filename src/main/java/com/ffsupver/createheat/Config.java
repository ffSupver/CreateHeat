package com.ffsupver.createheat;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();
// common
    public static final ModConfigSpec.BooleanValue ALLOW_PASSIVE_HEAT = BUILDER
            .comment("Whether to allow passive heat source to be calculated")
            .define("allow_passive_heat", true);
    public static final ModConfigSpec.BooleanValue ALLOW_GENERATE_SUPER_HEAT = BUILDER
            .comment("Whether to allow non super heat sources to generate super heat source")
            .define("allow_generate_super_heat", false);

    public static final ModConfigSpec.IntValue HEAT_PER_FADING_BLAZE = BUILDER
            .comment("Heat per fading (regular heat) blaze burner provide.")
            .comment("If passive heat source allowed, it will be 1 for passive heat source.")
            .comment("Should be bigger than 1. Default 2")
            .defineInRange("heat_per_fading_blaze", 2, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue HEAT_PER_SEETHING_BLAZE = BUILDER
            .comment("Heat per seething (super heat) blaze burner provide")
            .comment("If passive heat source allowed, it will be 1 for passive heat source.")
            .comment("Should be bigger than 2. Default 4")
            .defineInRange("heat_per_seething_blaze", 4, 2, Integer.MAX_VALUE);
    public static final ModConfigSpec.BooleanValue ALLOW_SUPER_HEAT_REPRODUCE = BUILDER
            .comment("Whether to allow super heat sources to generate more super heat sources")
            .comment("Only works when allow_generate_super_heat is false")
            .define("allow_super_heat_reproduce", false);

    public static final ModConfigSpec.IntValue MAX_CONNECT_RANGE = BUILDER
            .comment("Max range between blocks can be connected")
            .defineInRange("max_connect_range", 32, 1, Integer.MAX_VALUE);


    public static final ModConfigSpec.BooleanValue CONNECT_BLOCK_TEXTURE = BUILDER
            .comment("Should blocks have connected texture")
            .define("connect_block_texture", true);

    static final ModConfigSpec SPEC = BUILDER.build();
    static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();
}
