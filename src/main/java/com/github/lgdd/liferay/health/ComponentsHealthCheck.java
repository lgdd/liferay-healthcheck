package com.github.lgdd.liferay.health;

import java.util.ArrayList;
import java.util.List;
import org.apache.felix.dm.ComponentDeclaration;
import org.apache.felix.dm.diagnostics.DependencyGraph;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    immediate = true,
    service = ComponentsHealthCheck.class
)
public class ComponentsHealthCheck {

  /**
   * Verify if unregistered components are present.
   *
   * @return HTTP response corresponding to the result
   * @see HealthCheckResponse
   * @see HealthCheckConfiguration
   */
  public HealthCheckResponse verify() {

    List<String> issues = new ArrayList<>();
    HealthCheckStatus status = HealthCheckStatus.UP;
    String message = OK_MESSAGE;

    DependencyGraph graph = DependencyGraph.getGraph(
        DependencyGraph.ComponentState.UNREGISTERED,
        DependencyGraph.DependencyState.REQUIRED_UNAVAILABLE);

    List<ComponentDeclaration> unregisteredComponents =
        graph.getAllComponents();

    if (!unregisteredComponents.isEmpty()) {
      status = HealthCheckStatus.DOWN;
      message =
          unregisteredComponents.size() +
              " unregistered components found";

      _log.warn(message);

      for (ComponentDeclaration componentDeclaration :
          unregisteredComponents) {

        BundleContext bundleContext =
            componentDeclaration.getBundleContext();

        if (bundleContext != null) {
          Bundle bundle = bundleContext.getBundle();

          if (bundle != null) {
            _log.warn(
                "Found unregistered component " +
                    componentDeclaration.getName() +
                    " in bundle: " + bundle.getSymbolicName());
            issues.add("Unregistered component " +
                           componentDeclaration.getName() +
                           " in bundle: " + bundle.getSymbolicName());
          }
        }
      }
    }
    return HealthCheckResponse.builder()
                              .status(status)
                              .message(message)
                              .issues(issues)
                              .build();
  }

  public static final String OK_MESSAGE = "No unregistered components found";

  private final static Logger _log = LoggerFactory.getLogger(ComponentsHealthCheck.class);

}
