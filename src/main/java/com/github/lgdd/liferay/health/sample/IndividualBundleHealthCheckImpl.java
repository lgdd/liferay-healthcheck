package com.github.lgdd.liferay.health.sample;

import com.github.lgdd.liferay.health.api.HealthCheckStatus;
import com.github.lgdd.liferay.health.api.IndividualBundleHealthCheck;

import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = IndividualBundleHealthCheck.class)
public class IndividualBundleHealthCheckImpl implements IndividualBundleHealthCheck {

    @Override
    public HealthCheckStatus isReady() {
        return HealthCheckStatus.UP;
    }

    @Override
    public HealthCheckStatus isLive() {
        return HealthCheckStatus.UP;
    }
    
}