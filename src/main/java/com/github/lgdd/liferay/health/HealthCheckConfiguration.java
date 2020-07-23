package com.github.lgdd.liferay.health;

import aQute.bnd.annotation.metatype.Meta;

/**
 * Provide an OSGi configuration to customize what needs to be verified for the readiness probe and
 * the liveness probe respectively. In Liferay, this configuration can be found in Control Panel >
 * System Settings > Third Party > Health Check. Like any OSGi configuration, it can be exported to
 * a config file and be installed in a Liferay bundle under $LIFERAY_HOME/osgi/configs.
 */
@Meta.OCD(
    id = HealthCheckConfiguration.PID,
    localization = "content/Language",
    name = "com.github.lgdd.liferay.health.config-name"
)
public interface HealthCheckConfiguration {

  @Meta.AD(
      deflt = "false",
      required = false,
      name = "com.github.lgdd.liferay.health.liveness-config-verify-bundle-states",
      description = "com.github.lgdd.liferay.health.liveness-config-verify-bundle-states-desc"
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
      name = "com.github.lgdd.liferay.health.readiness-config-verify-bundle-states",
      description = "com.github.lgdd.liferay.health.readiness-config-verify-bundle-states-desc"
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
