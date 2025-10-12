package com.ffsupver.createheat.api.iceAndFire;

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

import java.util.Optional;
import java.util.function.Function;

public record DragonHeater(DragonType dragonType, Function<Integer, HeatProvider> heatProviderByStage) {

    public static final Codec<IntFunctionFactory> INT_CODEC = Codec.lazyInitialized(() ->
            Codec.STRING.comapFlatMap(
                    DragonHeater::parseExpression,
                    DragonHeater::expressionToString
            )
    );

    public static Codec<DragonHeater> CODEC = RecordCodecBuilder.create(i->i.group(
            Codec.STRING.fieldOf("dragon_type").forGetter(d->d.dragonType.name()),
            INT_CODEC.fieldOf("heat_per_tick").forGetter(d->s->d.heatProviderByStage.apply(s).getHeatPerTick()),
            INT_CODEC.fieldOf("super_heat_count").forGetter(d->s->d.heatProviderByStage.apply(s).getSupperHeatCount())
    ).apply(i,(t,h,s)->new DragonHeater(
            IafRegistries.DRAGON_TYPE.get(
                ResourceKey.create(IafRegistryKeys.DRAGON_TYPE, ResourceLocation.parse(t))
            ),(stage)->new DragonHeatProvider(h.apply(stage),s.apply(stage))
    )));



    public static Optional<Holder.Reference<DragonHeater>> getFromDragonType(RegistryAccess registryAccess, DragonType dragonType){
        return registryAccess.lookupOrThrow(IceAndFire.DRAGON_HEATER)
                .listElements()
                .filter(hR->hR.value().dragonType.equals(dragonType))
                .findFirst();
    }



    @FunctionalInterface
    private interface IntFunctionFactory{

        int apply(int stage);
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
        throw new IllegalArgumentException("Unknown expression type: " + expression.getClass());
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
    }

    // 阶段变量表达式
    public record StageExpression() implements IntFunctionFactory {
        @Override
        public int apply(int stage) {
            return stage;
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
