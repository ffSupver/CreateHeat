package com.ffsupver.createheat.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.*;

public class BlockStateTester {
    private final Block block;
    private final TagKey<Block> blockTag;
    private final List<PropertyTester> properties;
    private final BlockStateTesterBuilder builder;
    public final static Codec<BlockStateTester> CODEC = RecordCodecBuilder.create(i->i.group(
            BlockStateTesterBuilder.CODEC.fieldOf("tester").forGetter(b->b.builder)
    ).apply(i,BlockStateTester::new));

    private BlockStateTester(Block block, TagKey<Block> blockTag, List<PropertyTester> properties, BlockStateTesterBuilder builder) {
        this.block = block;
        this.blockTag = blockTag;
        this.properties = properties;
        this.builder = builder;
    }

    private BlockStateTester(BlockStateTesterBuilder builder){
        this(
                builder.block.orElse(null),
                builder.blockTagS.map(s -> TagKey.create(BuiltInRegistries.BLOCK.key(), ResourceLocation.read(s).getOrThrow())).orElse(null),
                builder.pTL,builder
        );
    }

    public boolean test(BlockState blockState){
        boolean blockType = false;
        if (this.block != null){
           blockType = blockState.getBlock().equals(this.block);
        }else if (this.blockTag != null){
            blockType = blockState.is(blockTag);
        }

        boolean propertiesTest = properties.isEmpty();
        if (blockType){
            Collection<Property<?>> propertyList = blockState.getProperties();
            Map<String,Property<?>> matchedProperty = new HashMap<>();
            boolean skipPropertyTest = false;  //判断方块是否包含 properties 的所有检测器
            for (PropertyTester pT : properties){
                boolean find = false;
                for (Property<?> p : propertyList){
                    if (p.getName().equals(pT.name)){
                        find = true;
                        matchedProperty.put(pT.name,p);
                        break;
                    }
                }
                if (!find){
                    skipPropertyTest = true;
                    break;
                }
            }

            //判断Property
            if (!skipPropertyTest){
                for (PropertyTester pT : properties) {
                    propertiesTest = true;
                    Property<?> property = matchedProperty.get(pT.name);
                    boolean pPass = pT.testPropertyValue(blockState,property);
                    if (!pPass) {
                        propertiesTest = false;
                        break;
                    }
                }
            }
        }

        return blockType && propertiesTest;
    }

    public List<Block> toBlockList(){
        List<Block> result = new ArrayList<>();
        if (this.block != null){
            result.add(block);
        }else {
            if(BuiltInRegistries.BLOCK.getTag(blockTag).isPresent()){
                BuiltInRegistries.BLOCK.getTag(blockTag).get().forEach(blockHolder -> result.add(blockHolder.value()));
            }
        }
        return result;
    }

    public NonNullList<Ingredient> toIngredient(){
        ItemStack[] itemStacks = toBlockList().stream()
                .map(Block::asItem)
                .map(Item::getDefaultInstance)
                .toArray(ItemStack[]::new);
        Ingredient ingredient = Ingredient.of(itemStacks);
        NonNullList<Ingredient> result = NonNullList.create();
        result.add(ingredient);
        return result;
    }

    @Override
    public String toString() {
        return "BlockStateTester:"+block+" B/T "+blockTag+" P:"+properties;
    }

    public record PropertyTester(String name, List<String> values) {
            public static final Codec<PropertyTester> CODEC = RecordCodecBuilder.create(propertyTesterInstance ->
                    propertyTesterInstance.group(
                            Codec.STRING.fieldOf("name").forGetter(p -> p.name),
                            Codec.list(Codec.STRING).fieldOf("value").forGetter(p -> p.values)
                    ).apply(propertyTesterInstance, PropertyTester::new));


        public  <T extends Comparable<T>> boolean testPropertyValue(BlockState blockState, Property<T> property) {
            T value = blockState.getValue(property);
            String valueName = property.getName(value);
            return values.contains(valueName);
        }
    }

    private record BlockStateTesterBuilder(Optional<Block> block, Optional<String> blockTagS, List<PropertyTester> pTL){
        public final static Codec<BlockStateTesterBuilder> CODEC = RecordCodecBuilder.create(i->i.group(
                BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("block").forGetter(b-> b.block),
                Codec.STRING.optionalFieldOf("tag").forGetter(b-> b.blockTagS),
                Codec.list(PropertyTester.CODEC).optionalFieldOf("properties",List.of()).forGetter(b-> b.pTL)

        ).apply(i,BlockStateTesterBuilder::new));
    }
}
