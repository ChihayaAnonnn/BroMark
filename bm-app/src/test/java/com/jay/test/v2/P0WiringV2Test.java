package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jay.config.V2RaffleConfig;
import com.jay.domain.asset.service.AssetWriteGate;
import com.jay.domain.asset.service.RequestAdmission;
import com.jay.domain.raffle.service.DrawDeliveryService;
import com.jay.domain.raffle.service.DrawService;
import com.jay.domain.task.service.EventHandler;
import com.jay.trigger.http.v2.DrawController;
import com.jay.trigger.listener.v2.StockCommandListener;
import com.jay.trigger.security.CurrentUser;
import java.lang.reflect.Method;
import java.util.*;
import org.junit.Test;
import org.springframework.context.annotation.Bean;

public class P0WiringV2Test {
  @Test
  public void cancelEndpointIsGone() throws Exception {
    var user = mock(CurrentUser.class);
    var service = mock(DrawService.class);
    var admission = mock(RequestAdmission.class);
    var controller =
        new DrawController(user, service, admission, new AssetWriteGate(true, "V2"));

    var mvc =
        org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new com.jay.trigger.http.v2.V2ExceptionHandler())
            .build();

    mvc.perform(post("/api/v2/draws/o1/cancel")).andExpect(status().isMethodNotAllowed());

    verifyNoInteractions(service, admission);
  }

  @Test
  public void contextHasOnlyP0Handlers() {
    Map<String, Method> handlers = new LinkedHashMap<>();
    Set<Class<?>> beanTypes = new HashSet<>();
    for (Method method : V2RaffleConfig.class.getDeclaredMethods()) {
      Bean bean = method.getAnnotation(Bean.class);
      if (bean == null) continue;
      beanTypes.add(method.getReturnType());
      if (!EventHandler.class.isAssignableFrom(method.getReturnType())) continue;
      handlers.put(bean.name().length == 0 ? method.getName() : bean.name()[0], method);
    }

    assertThat(handlers).containsOnlyKeys("ReserveAward", "ReservationConfirmed");
    assertThat(handlers.get("ReserveAward").getParameterTypes())
        .containsExactly(StockCommandListener.class);
    assertThat(handlers.get("ReservationConfirmed").getParameterTypes())
        .containsExactly(DrawDeliveryService.class);
    assertThat(beanTypes).contains(StockCommandListener.class);
  }
}
