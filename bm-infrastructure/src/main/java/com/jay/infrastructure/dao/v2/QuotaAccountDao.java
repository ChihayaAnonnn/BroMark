package com.jay.infrastructure.dao.v2;
import com.jay.infrastructure.dao.v2.po.QuotaAccountRow;
import org.apache.ibatis.annotations.*;
@Mapper public interface QuotaAccountDao {
  int ensure(@Param("userId") String userId,@Param("activityId") long activityId);
  QuotaAccountRow lock(@Param("userId") String userId,@Param("activityId") long activityId);
}
