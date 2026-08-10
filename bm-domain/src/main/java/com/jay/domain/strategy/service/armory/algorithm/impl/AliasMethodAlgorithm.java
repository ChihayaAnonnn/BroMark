package com.jay.domain.strategy.service.armory.algorithm.impl;

import com.jay.domain.strategy.model.entity.StrategyAwardEntity;
import com.jay.domain.strategy.service.armory.algorithm.AliasTable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Builds Alias Method tables in O(n) time and selects an award in O(1) time.
 */
@Component
public class AliasMethodAlgorithm {

    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

    public AliasTable build(List<StrategyAwardEntity> strategyAwards) {
        if (strategyAwards == null || strategyAwards.isEmpty()) {
            throw new IllegalArgumentException("strategy awards must not be empty");
        }

        BigDecimal totalRate = BigDecimal.ZERO;
        for (StrategyAwardEntity strategyAward : strategyAwards) {
            if (strategyAward.getAwardId() == null || strategyAward.getAwardRate() == null
                    || strategyAward.getAwardRate().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("strategy award id and non-negative rate are required");
            }
            totalRate = totalRate.add(strategyAward.getAwardRate());
        }
        if (totalRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("strategy award total rate must be positive");
        }

        int size = strategyAwards.size();
        int[] awardIds = new int[size];
        double[] scaledRates = new double[size];
        double[] probabilities = new double[size];
        int[] aliases = new int[size];
        Deque<Integer> small = new ArrayDeque<>();
        Deque<Integer> large = new ArrayDeque<>();

        for (int i = 0; i < size; i++) {
            StrategyAwardEntity strategyAward = strategyAwards.get(i);
            awardIds[i] = strategyAward.getAwardId();
            scaledRates[i] = strategyAward.getAwardRate()
                    .multiply(BigDecimal.valueOf(size))
                    .divide(totalRate, MATH_CONTEXT)
                    .doubleValue();
            if (scaledRates[i] < 1D) {
                small.addLast(i);
            } else {
                large.addLast(i);
            }
        }

        while (!small.isEmpty() && !large.isEmpty()) {
            int smallIndex = small.removeFirst();
            int largeIndex = large.removeFirst();
            probabilities[smallIndex] = scaledRates[smallIndex];
            aliases[smallIndex] = largeIndex;
            scaledRates[largeIndex] = scaledRates[largeIndex] - (1D - scaledRates[smallIndex]);
            if (scaledRates[largeIndex] < 1D - 1E-12D) {
                small.addLast(largeIndex);
            } else {
                large.addLast(largeIndex);
            }
        }

        while (!large.isEmpty()) {
            int index = large.removeFirst();
            probabilities[index] = 1D;
            aliases[index] = index;
        }
        while (!small.isEmpty()) {
            int index = small.removeFirst();
            probabilities[index] = 1D;
            aliases[index] = index;
        }

        return new AliasTable(awardIds, probabilities, aliases);
    }

    public Integer dispatch(AliasTable aliasTable, Random random) {
        if (aliasTable == null || aliasTable.size() == 0 || random == null) {
            throw new IllegalArgumentException("alias table and random must be available");
        }
        int column = random.nextInt(aliasTable.size());
        int selected = random.nextDouble() < aliasTable.getProbabilities()[column]
                ? column : aliasTable.getAliases()[column];
        return aliasTable.getAwardIds()[selected];
    }
}
