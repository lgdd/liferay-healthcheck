package com.github.lgdd.liferay.health;

import aQute.bnd.annotation.metatype.Meta;

@Meta.OCD(
    id = HealthCheckConfiguration.PID,
    localization = "content/Language",
    name = "com.github.lgdd.liferay.health.config-name"
)
public interface HealthCheckConfiguration {

  @Meta.AD(
      deflt = "false",
      required = false,
      name = "com.github.lgdd.liferay.health.liveness-config-verify-bundles-states",
      description = "com.github.lgdd.liferay.health.liveness-config-verify-bundles-states-desc"
  )
  boolean verifyBundlesStatesForLiveness();

  @Meta.AD(
      deflt = "",
      required = false,
      name = "com.github.lgdd.liferay.health.liveness-config-bundle-symbolic-names",
      description = "com.github.lgdd.liferay.health.liveness-config-bundle-symbolic-names-desc"
  )
  String[] bundleSymbolicNamesForLiveness();

  @Meta.AD(
      deflt = "true",
      required = false,
      name = "com.github.lgdd.liferay.health.readiness-config-verify-bundles-states",
      description = "com.github.lgdd.liferay.health.readiness-config-verify-bundles-states-desc"
  )
  boolean verifyBundlesStatesForReadiness();

  @Meta.AD(
      deflt = "",
      required = false,
      name = "com.github.lgdd.liferay.health.readiness-config-bundle-symbolic-names",
      description = "com.github.lgdd.liferay.health.readiness-config-bundle-symbolic-names-desc"
  )
  String[] bundleSymbolicNamesForReadiness();

  String PID = "com.github.lgdd.liferay.health.HealthCheckConfiguration";

}
