package dev.aeoptimizer.dispatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.util.Random;

import org.junit.Test;

public class BatchLimitMathTest {

    @Test
    public void exampleAndZero() {
        long batches = 4096;
        for (long fluid : new long[] { 100000, 50000, 20000 })
            batches = BatchLimitMath.restrict(batches, 1000000, fluid);
        assertEquals(8, BatchLimitMath.restrict(batches, 2000000, 250000));
        assertEquals(4096, BatchLimitMath.restrict(4096, 0, 250000));
        assertEquals(0, BatchLimitMath.restrict(4096, 249999, 250000));
    }

    @Test
    public void boundaryAndRandomExactArithmetic() {
        assertEquals(Long.MAX_VALUE, BatchLimitMath.restrict(Long.MAX_VALUE, Long.MAX_VALUE, 1));
        assertEquals(1, BatchLimitMath.restrict(4096, Long.MAX_VALUE, Long.MAX_VALUE));
        Random random = new Random(0xBA7C);
        for (int i = 0; i < 100000; i++) {
            long cap = random.nextLong() & Long.MAX_VALUE, amount = (random.nextLong() & Long.MAX_VALUE) | 1;
            long requested = random.nextInt(100000);
            long expected = cap == 0 ? requested
                : Math.min(
                    requested,
                    BigInteger.valueOf(cap)
                        .divide(BigInteger.valueOf(amount))
                        .longValueExact());
            assertEquals(expected, BatchLimitMath.restrict(requested, cap, amount));
        }
    }

    @Test
    public void parseExactDecimalUnits() {
        assertEquals(0, BatchLimitMath.parse("0"));
        assertEquals(1000000, BatchLimitMath.parse("1M"));
        assertEquals(2500000, BatchLimitMath.parse("2.5m"));
        assertEquals(1000, BatchLimitMath.parse(" 1k "));
        assertEquals(1000000000000L, BatchLimitMath.parse("1T"));
        assertEquals(Long.MAX_VALUE, BatchLimitMath.parse(Long.toString(Long.MAX_VALUE)));
        for (String invalid : new String[] { "", "-1", "NaN", "Infinity", "0.1", "1e3", "1.1.1M", "1MB",
            "9223372036854775808", "10000000T", "-0", "1,000", "K" }) {
            try {
                BatchLimitMath.parse(invalid);
                fail(invalid);
            } catch (IllegalArgumentException | ArithmeticException expected) { /* rejected */ }
        }
    }
}
