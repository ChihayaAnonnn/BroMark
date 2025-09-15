package com.jay.types.common;

public class Constants {

    public final static String SPLIT = ",";
    public final static String SPACE = " ";
    public final static String COLON = ":";
    public final static String UNDERLINE = "_";

    public static class RedisKey {
        public static final String STRATEGY_KEY = "strategy:key:";
        public static final String STRATEGY_AWARD_KEY = "strategy:award:key:";
        public static final String STRATEGY_AWARD_LIST_KEY = "strategy:award:list:key:";
        public static final String STRATEGY_RATE_TABLE_KEY = "strategy:rate:table:key:";
        public static final String STRATEGY_RATE_RANGE_KEY = "strategy:rate:range:key:";
        public static final String RULE_TREE_VO_KEY = "rule:tree:vo:key:";
        public static final String STRATEGY_AWARD_COUNT_KEY = "strategy:award:count:key:";
        public static final String STRATEGY_AWARD_COUNT_QUEUE_KEY = "strategy:award:count:query:key:";
        public static final String ACTIVITY_KEY = "activity:key:";
        public static final String ACTIVITY_COUNT_KEY = "activity:count:key:";
        public static final String ACTIVITY_SKU_STOCK_COUNT_KEY = "activity:sku:stock:count:key:";
        public static final String ACTIVITY_SKU_STOCK_QUEUE_KEY = "activity:sku:stock:queue:key:";
        public static final String ACTIVITY_SKU_KEY = "activity:sku:key:";
        public static final String STRATEGY_RULE_WEIGHT_KEY = "strategy:rule:weight:key:";
        public static final String USER_CREDIT_ACCOUNT_LOCK = "user_credit_account_lock";
        public static final String ACTIVITY_ACCOUNT_UPDATE_LOCK = "activity_account_update_lock";
    }
}
