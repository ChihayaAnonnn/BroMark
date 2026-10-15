package com.jay.infrastructure.dao.v2;

import com.jay.infrastructure.dao.v2.po.AssetAccountRow;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AssetAccountDao {
  int ensure(String userId);

  AssetAccountRow lock(String userId);

  int adjust(@Param("userId") String userId, @Param("delta") java.math.BigDecimal delta);
}
