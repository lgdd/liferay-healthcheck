package com.github.lgdd.liferay.health.internal;

import com.github.lgdd.liferay.health.api.HealthCheckResponse;
import com.github.lgdd.liferay.health.api.HealthCheckService;
import com.github.lgdd.liferay.health.api.HealthCheckStatus;
import java.util.ArrayList;
import org.osgi.service.component.annotations.Component;

@Component(
    immediate = true,
    service = HealthCheckService.class
)
public class HealthCheckServiceImpl
    implements HealthCheckService {

  @Override
  public HealthCheckResponse isReady() {

    return HealthCheckResponse
        .builder()
        .status(HealthCheckStatus.UP)
        .message("No issue detected.")
        .issues(new ArrayList<>())
        .build();
  }

  @Override
  public HealthCheckResponse isLive() {

    return HealthCheckResponse
        .builder()
        .status(HealthCheckStatus.UP)
        .message("No issue detected.")
        .issues(new ArrayList<>())
        .build();
  }

}
