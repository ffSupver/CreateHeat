package com.ffsupver.createheat.api.iceAndFire;

import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.block.HeatProvider;
import com.ffsupver.createheat.compat.iceAndFire.IceAndFire;
import com.iafenvoy.iceandfire.data.DragonType;
import com.iafenvoy.iceandfire.registry.IafRegistries;
import com.iafenvoy.iceandfire.registry.IafRegistryKeys;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public record DragonHeater(DragonType dragonType, StageToHeatProviderFunc heatProviderByStage) {

    public static final Codec<IntFunctionFactory> INT_CODEC = Codec.lazyInitialized(() ->
            Codec.STRING.comapFlatMap(
                    DragonHeater::parseExpression,
                    DragonHeater::expressionToString
            )
    );

    public static Codec<DragonHeater> CODEC = RecordCodecBuilder.create(i->i.group(
            Codec.STRING.fieldOf("dragon_type").forGetter(d->IafRegistries.DRAGON_TYPE.getKey(d.dragonType).toString()),
            INT_CODEC.fieldOf("heat_per_tick").forGetter(d->d.heatProviderByStage.heatPerTickProvider),
            INT_CODEC.fieldOf("super_heat_count").forGetter(d->d.heatProviderByStage.superHeatCountProvider)
    ).apply(i,(t,h,s)-> {
        ResourceKey<DragonType> typeResourceKey = ResourceKey.create(IafRegistryKeys.DRAGON_TYPE, ResourceLocation.parse(t));
        DragonType type = IafRegistries.DRAGON_TYPE.get(typeResourceKey);
        DragonHeater result = new DragonHeater(type,new StageToHeatProviderFunc(h, s));
        CreateHeat.LOGGER.info("Register dragon heater {} with type:{}",result,t);
        return result;
    }));



    public static Optional<Holder.Reference<DragonHeater>> getFromDragonType(RegistryAccess registryAccess, DragonType dragonType){
        List<Holder.Reference<DragonHeater>> dragonHeaterList = registryAccess.lookupOrThrow(IceAndFire.DRAGON_HEATER)
                .listElements()
                .filter(hR->{
                    if (hR.value().dragonType == null){
                        CreateHeat.LOGGER.error("[DragonHeater]find null dragon type {} ; Check you datapack createheat/dragon_heater", hR);
                        return false;
                    }
                    return true;
                })
                .filter(hR->hR.value().dragonType.equals(dragonType))
                .toList();
        return dragonHeaterList.isEmpty() ? Optional.empty() : Optional.of(dragonHeaterList.getLast());
    }



    @FunctionalInterface
    private interface IntFunctionFactory{

        int apply(int stage);
    }

    public record StageToHeatProviderFunc(IntFunctionFactory heatPerTickProvider,IntFunctionFactory superHeatCountProvider){
        public HeatProvider apply(int stage){return new DragonHeatProvider(heatPerTickProvider.apply(stage),superHeatCountProvider.apply(stage));}
    }



    // 表达式转字符串
    private static String expressionToString(IntFunctionFactory expression) {
        if (expression instanceof ConstantExpression constant) {
            return String.valueOf(constant.value());
        } else if (expression instanceof StageExpression) {
            return "stage";
        } else if (expression instanceof BinaryExpression binary) {
            return "(" + expressionToString(binary.left()) +
                    binary.operator().getSymbol() +
                    expressionToString(binary.right()) + ")";
        }
        throw new IllegalArgumentException("Unknown expression type: " + expression.getClass()+" exp="+ expression + " 1->"+expression.apply(1) + " 2->"+expression.apply(2));
    }

    // 解析表达式字符串
    private static DataResult<IntFunctionFactory> parseExpression(String expression) {
        try {
            IntFunctionFactory expr = parseExpressionRecursive(expression.trim());
            return DataResult.success(expr);
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression '" + expression + "': " + e.getMessage());
        }
    }

    // 递归解析表达式
    private static IntFunctionFactory parseExpressionRecursive(String expr) {
        expr = expr.trim();

        // 检查是否是阶段变量
        if ("stage".equals(expr)) {
            return new StageExpression();
        }

        // 检查是否是数字
        try {
            int value = Integer.parseInt(expr);
            return new ConstantExpression(value);
        } catch (NumberFormatException e) {
            // 不是数字，继续解析
        }

        // 解析括号表达式
        if (expr.startsWith("(") && expr.endsWith(")")) {
            return parseExpressionRecursive(expr.substring(1, expr.length() - 1));
        }

        // 解析二元运算（按运算符优先级）
        String[] operators = {"+", "-", "*", "/"};
        for (String op : operators) {
            int index = findOperatorIndex(expr, op);
            if (index != -1) {
                IntFunctionFactory left = parseExpressionRecursive(expr.substring(0, index));
                IntFunctionFactory right = parseExpressionRecursive(expr.substring(index + op.length()));
                return new BinaryExpression(left, right, BinaryExpression.Operator.fromSymbol(op));
            }
        }

        throw new IllegalArgumentException("Invalid expression: " + expr);
    }

    // 查找运算符位置（考虑括号）
    private static int findOperatorIndex(String expr, String operator) {
        int parenCount = 0;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') parenCount++;
            else if (c == ')') parenCount--;
            else if (parenCount == 0 && expr.startsWith(operator, i)) {
                return i;
            }
        }
        return -1;
    }

    // 常数表达式
    public record ConstantExpression(int value) implements IntFunctionFactory {
        @Override
        public int apply(int stage) {
            return value;
        }

        @Override
        public @NotNull String toString() {
            return "constant:"+value;
        }
    }

    // 阶段变量表达式
    public record StageExpression() implements IntFunctionFactory {
        @Override
        public int apply(int stage) {
            return stage;
        }

        @Override
        public @NotNull String toString() {
            return "stage";
        }
    }

    // 二元运算表达式
    public record BinaryExpression(IntFunctionFactory left, IntFunctionFactory right, Operator operator) implements IntFunctionFactory {
        @Override
        public int apply(int stage) {
            int leftVal = left.apply(stage);
            int rightVal = right.apply(stage);
            return operator.apply(leftVal, rightVal);
        }

        public enum Operator {
            ADD("+", (a, b) -> a + b),
            SUBTRACT("-", (a, b) -> a - b),
            MULTIPLY("*", (a, b) -> a * b),
            DIVIDE("/", (a, b) -> {
                if (b == 0) throw new ArithmeticException("Division by zero");
                return a / b;
            });

            private final String symbol;
            private final java.util.function.BinaryOperator<Integer> operation;

            Operator(String symbol, java.util.function.BinaryOperator<Integer> operation) {
                this.symbol = symbol;
                this.operation = operation;
            }

            public int apply(int a, int b) {
                return operation.apply(a, b);
            }

            public String getSymbol() {
                return symbol;
            }

            public static Operator fromSymbol(String symbol) {
                for (Operator op : values()) {
                    if (op.symbol.equals(symbol)) {
                        return op;
                    }
                }
                throw new IllegalArgumentException("Unknown operator: " + symbol);
            }
        }

        @Override
        public String toString() {
            return "BinaryExpression{" +
                    left.toString() + " " + operator.symbol + " " + right.toString() +
                    '}';
        }
    }

    public static HeatProvider NO_HEAT_PROVIDER = new DragonHeatProvider(0,0);

    private static class DragonHeatProvider implements HeatProvider{
        private final int heatPerTick;
        private final int superHeatCount;

        private DragonHeatProvider(int heatPerTick, int superHeatCount) {
            this.heatPerTick = heatPerTick;
            this.superHeatCount = superHeatCount;
        }

        @Override
        public int getHeatPerTick() {
            return heatPerTick;
        }

        @Override
        public int getSupperHeatCount() {
            return superHeatCount;
        }

        @Override
        public String toString() {
            return "DragonHeatProvider:"+heatPerTick+"h/s"+superHeatCount;
        }
    }
}
