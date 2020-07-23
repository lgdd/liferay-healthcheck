package com.github.lgdd.liferay.health;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * Represent a response body for the readiness and liveness probes and provide information about the
 * status, a message summarizing the situation and a list of issues detected, if any.
 */
@Data
@Builder
public class HealthCheckResponse {

  @NonNull
  private HealthCheckStatus status;

  @NonNull
  private String message;

  @NonNull
  @Builder.Default
  private List<String> issues = new ArrayList<>();

  public String toJson() {

    return JSONFactoryUtil.createJSONSerializer().serializeDeep(this);
  }
}
