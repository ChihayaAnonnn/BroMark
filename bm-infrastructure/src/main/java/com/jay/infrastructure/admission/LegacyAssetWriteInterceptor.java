package com.jay.infrastructure.admission;

import com.jay.domain.asset.service.AssetWriteGate;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.plugin.*;

@Intercepts(
    @Signature(
        type = Executor.class,
        method = "update",
        args = {MappedStatement.class, Object.class}))
public final class LegacyAssetWriteInterceptor implements Interceptor {
  private final AssetWriteGate gate;

  public LegacyAssetWriteInterceptor(AssetWriteGate gate) {
    this.gate = gate;
  }

  public Object intercept(Invocation call) throws Throwable {
    var statement = (MappedStatement) call.getArgs()[0];
    String id = statement.getId();
    if (id.startsWith("com.jay.infrastructure.dao.")
        && !id.startsWith("com.jay.infrastructure.dao.v2.")
        && (id.contains("IUserCreditAccountDao.")
            || id.contains("IRaffleActivityAccountDao.")
            || id.contains("IRaffleActivityAccountDayDao.")
            || id.contains("IRaffleActivityAccountMonthDao."))) gate.check("legacy.dao");
    return call.proceed();
  }
}
