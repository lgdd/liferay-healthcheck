package com.github.lgdd.liferay.health.api;

import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface HealthCheckService {

  HealthCheckResponse isReady();

  HealthCheckResponse isLive();

}
