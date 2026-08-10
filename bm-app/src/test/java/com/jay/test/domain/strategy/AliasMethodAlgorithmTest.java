package com.jay.test.domain.strategy;

import com.jay.domain.strategy.model.entity.StrategyAwardEntity;
import com.jay.domain.strategy.service.armory.algorithm.AliasTable;
import com.jay.domain.strategy.service.armory.algorithm.impl.AliasMethodAlgorithm;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AliasMethodAlgorithmTest {

    @Test
    public void shouldAlwaysSelectTheOnlyAward() {
        AliasMethodAlgorithm algorithm = new AliasMethodAlgorithm();
        AliasTable table = algorithm.build(awards(101, "1"));

        assertEquals(Integer.valueOf(101), algorithm.dispatch(table, new FixedRandom(0, 0.99D)));
    }

    @Test
    public void shouldSelectPrimaryAndAliasAwardsFromTheSameColumn() {
        AliasMethodAlgorithm algorithm = new AliasMethodAlgorithm();
        AliasTable table = algorithm.build(awards(101, "0.25", 102, "0.75"));

        assertEquals(Integer.valueOf(101), algorithm.dispatch(table, new FixedRandom(0, 0.10D)));
        assertEquals(Integer.valueOf(102), algorithm.dispatch(table, new FixedRandom(0, 0.90D)));
    }

    @Test
    public void shouldRejectZeroTotalRate() {
        AliasMethodAlgorithm algorithm = new AliasMethodAlgorithm();
        try {
            algorithm.build(awards(101, "0", 102, "0"));
            fail("zero total rate should be rejected");
        } catch (IllegalArgumentException e) {
            assertEquals("strategy award total rate must be positive", e.getMessage());
        }
    }

    private List<StrategyAwardEntity> awards(Object... values) {
        if (values.length == 2) {
            return Collections.singletonList(award((Integer) values[0], (String) values[1]));
        }
        return Arrays.asList(
                award((Integer) values[0], (String) values[1]),
                award((Integer) values[2], (String) values[3])
        );
    }

    private StrategyAwardEntity award(Integer awardId, String rate) {
        return StrategyAwardEntity.builder()
                .awardId(awardId)
                .awardRate(new BigDecimal(rate))
                .build();
    }

    private static class FixedRandom extends Random {
        private final int index;
        private final double coin;

        private FixedRandom(int index, double coin) {
            this.index = index;
            this.coin = coin;
        }

        @Override
        public int nextInt(int bound) {
            return index;
        }

        @Override
        public double nextDouble() {
            return coin;
        }
    }
}
