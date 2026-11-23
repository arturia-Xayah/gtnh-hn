package dev.aeoptimizer.dispatch;

import java.math.BigDecimal;
import java.util.Locale;

public final class BatchLimitMath {

    private BatchLimitMath() {}

    /** Division before multiplication: even Long.MAX_VALUE limits cannot overflow. */
    public static long restrict(long batches, long limit, long perRecipe) {
        if (batches < 0 || limit < 0 || perRecipe <= 0) return 0;
        return limit == 0 ? batches : Math.min(batches, limit / perRecipe);
    }

    /** Decimal suffixes; fluid values always use mB, never buckets or fluid-packet counts. */
    public static long parse(String text) {
        if (text == null || text.length() > 40) throw new IllegalArgumentException("Invalid limit");
        String value = text.trim()
            .toUpperCase(Locale.ROOT);
        if (!value.matches("[0-9]+(?:\\.[0-9]+)?[KMGT]?")) throw new IllegalArgumentException("Invalid limit");
        int power = 0;
        char last = value.charAt(value.length() - 1);
        int suffix = "KMGT".indexOf(last);
        if (suffix >= 0) {
            power = (suffix + 1) * 3;
            value = value.substring(0, value.length() - 1);
        }
        long result = new BigDecimal(value).scaleByPowerOfTen(power)
            .longValueExact();
        if (result < 0) throw new IllegalArgumentException("Negative limit");
        return result;
    }
}
