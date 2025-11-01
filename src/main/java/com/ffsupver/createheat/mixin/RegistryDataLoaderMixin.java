package com.ffsupver.createheat.mixin;

import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.compat.Mods;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Decoder;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.stream.Collectors;

@Mixin(RegistryDataLoader.class)
public abstract class RegistryDataLoaderMixin {
    /**
     * Skip loading by "dependency"
     */
    @Inject(
            method = "loadElementFromResource",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/gson/JsonParser;parseReader(Ljava/io/Reader;)Lcom/google/gson/JsonElement;",
                    shift = At.Shift.BY,
                    by = 2
            ),
            cancellable = true
    )
    private static <E> void loadRegistryContents(WritableRegistry<E> registry, Decoder<E> codec, RegistryOps<JsonElement> ops, ResourceKey<E> resourceKey, Resource resource, RegistrationInfo registrationInfo, CallbackInfo ci, @Local JsonElement jsonelement) {
        if (resourceKey.location().getNamespace().equals(CreateHeat.MODID)) {
            JsonObject json = jsonelement.getAsJsonObject();
            if (json.has("dependency")) {
                JsonElement dependencyList = json.get("dependency");
                if (dependencyList.isJsonArray()){
                    JsonArray dependency = dependencyList.getAsJsonArray();
                   Set<String> unLoadMods = dependency.asList().stream().map(JsonElement::getAsString).filter(modid->!Mods.isModLoad(modid)).collect(Collectors.toSet());
                    if (!unLoadMods.isEmpty()) {
                        ci.cancel();
                        CreateHeat.LOGGER.info("[Datapack]: {} in {} was skipped because {} not load.", registry.key().location(),resource.sourcePackId(),unLoadMods);
                    }
                }
            }
        }
    }
}
