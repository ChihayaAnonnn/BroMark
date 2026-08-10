package com.jay.domain.strategy.service.armory.algorithm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Alias Method probability table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AliasTable {

    private int[] awardIds;
    private double[] probabilities;
    private int[] aliases;

    public int size() {
        return awardIds == null ? 0 : awardIds.length;
    }
}
