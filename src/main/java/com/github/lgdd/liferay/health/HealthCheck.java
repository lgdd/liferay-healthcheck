package com.github.lgdd.liferay.health;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

@Component(
    immediate = true,
    property = {
        JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE + "=/health",
        JaxrsWhiteboardConstants.JAX_RS_NAME + "=Health",
        "oauth2.scopechecker.type=none",
        "liferay.access.control.disable=true"
    },
    configurationPid = HealthCheckConfiguration.PID,
    service = Application.class
)
public class HealthCheck
    extends Application {

  @GET
  @Path("/readiness")
  @Produces(MediaType.APPLICATION_JSON)
  public Response readiness() {

    return _verifyRequiredBundles(
        _config.bundleSymbolicNamesForReadiness(),
        _config.verifyBundlesStatesForReadiness()
    );
  }

  @GET
  @Path("/liveness")
  @Produces(MediaType.APPLICATION_JSON)
  public Response liveness() {

    return _verifyRequiredBundles(
        _config.bundleSymbolicNamesForLiveness(),
        _config.verifyBundlesStatesForLiveness()
    );
  }

  /**
   * Verify if the required bundles from the configuration are present and in a proper state, i.e.
   * ACTIVE or RESOLVED if the bundle is a fragment.
   *
   * @param requiredBundleSymbolicNames list of required bundle symbolic names
   * @param isVerificationRequired      true if all bundle states need to be verified (passed to the
   *                                    next method)
   * @return HTTP response corresponding to the result
   * @see HealthCheckResponse
   * @see HealthCheckConfiguration
   * @see BundlesHealthCheck#verifyBundles
   * @see HealthCheck#_verifyBundlesStates
   */
  private Response _verifyRequiredBundles(String[] requiredBundleSymbolicNames,
      boolean isVerificationRequired) {

    Set<String> bundleSymbolicNames =
        Arrays.stream(requiredBundleSymbolicNames)
              .filter(symbolicName -> !symbolicName.trim().isEmpty())
              .collect(Collectors.toSet());

    if (!bundleSymbolicNames.isEmpty()) {
      HealthCheckResponse requiredBundlesResponse = _bundlesHealthCheck
          .verifyBundles(bundleSymbolicNames);
      if (HealthCheckStatus.DOWN.equals(requiredBundlesResponse.getStatus())) {
        return Response.serverError()
                       .entity(requiredBundlesResponse.toJson())
                       .build();
      }
    }
    return _verifyBundlesStates(isVerificationRequired);
  }

  /**
   * Verify every bundle state, if checked in the configuration.
   *
   * @param hasToVerifyBundlesStates true if bundle states has to be verified
   * @return HTTP response corresponding to the result
   * @see HealthCheckResponse
   * @see HealthCheckConfiguration
   */
  private Response _verifyBundlesStates(boolean hasToVerifyBundlesStates) {

    if (hasToVerifyBundlesStates) {
      final HealthCheckResponse bundlesResponse = _bundlesHealthCheck.verify();

      if (HealthCheckStatus.DOWN.equals(bundlesResponse.getStatus())) {
        return Response.serverError()
                       .entity(bundlesResponse.toJson())
                       .build();
      }
    }

    return _verifyComponents();
  }

  /**
   * Verify if unregistered components are present.
   *
   * @return HTTP response corresponding to the result
   * @see HealthCheckResponse
   * @see HealthCheckConfiguration
   * @see ComponentsHealthCheck#verify
   */
  private Response _verifyComponents() {

    final HealthCheckResponse componentsResponse = _componentsHealthCheck.verify();

    if (HealthCheckStatus.DOWN.equals(componentsResponse.getStatus())) {
      return Response.serverError()
                     .entity(componentsResponse.toJson())
                     .build();
    }
    return Response.ok(componentsResponse.toJson()).build();
  }


  public Set<Object> getSingletons() {

    return Collections.singleton(this);
  }

  @Activate
  @Modified
  public void activate(Map<String, String> properties) {

    _config =
        ConfigurableUtil.createConfigurable(HealthCheckConfiguration.class, properties);

  }

  private volatile HealthCheckConfiguration _config;

  @Reference
  private ComponentsHealthCheck _componentsHealthCheck;

  @Reference
  private BundlesHealthCheck _bundlesHealthCheck;

}
