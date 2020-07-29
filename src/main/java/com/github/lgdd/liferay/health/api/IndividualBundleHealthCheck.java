package com.github.lgdd.liferay.health.api;

public interface IndividualBundleHealthCheck {

    public HealthCheckStatus isReady();

    public HealthCheckStatus isLive();

}