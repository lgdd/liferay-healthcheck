package com.github.lgdd.liferay.health;

import com.github.lgdd.liferay.health.api.HealthCheckResponse;
import com.github.lgdd.liferay.health.api.HealthCheckService;
import com.github.lgdd.liferay.health.api.HealthCheckStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.felix.dm.diagnostics.CircularDependency;
import org.apache.felix.dm.diagnostics.DependencyGraph;
import org.apache.felix.dm.diagnostics.DependencyGraph.ComponentState;
import org.apache.felix.dm.diagnostics.DependencyGraph.DependencyState;
import org.apache.felix.dm.diagnostics.MissingDependency;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    immediate = true,
    service = BundlesHealthCheck.class
)
public class BundlesHealthCheck {

  private ServiceTracker<HealthCheckService, HealthCheckService> individualBundleHealthCheckServiceTracker;

  /**
   * Verify every bundle state, if checked in the configuration.
   *
   * @return a response entity to be sent in the HTTP response body as JSON
   * @see HealthCheckResponse
   * @see HealthCheckStatus
   */
  public HealthCheckResponse verify() {

    String message = "No issues with bundles";
    List<String> issues = _getIssues();

    if (issues.isEmpty()) {
      return HealthCheckResponse.builder()
                                .status(HealthCheckStatus.UP)
                                .message(message)
                                .build();
    }

    message = issues.size() > 1 ? "Found " + issues.size() + " issues with bundles"
                                : "Found 1 issue with bundles";

    return HealthCheckResponse.builder()
                              .status(HealthCheckStatus.DOWN)
                              .message(message)
                              .issues(issues)
                              .build();
  }

  /**
   * Verify if the required bundles from the configuration are present and in a proper state, i.e.
   * ACTIVE or RESOLVED if the bundle is a fragment.
   *
   * @param probeType                   type of probe we're looking for (e.g. readiness or
   *                                    liveness)
   * @param requiredBundleSymbolicNames list of required bundle symbolic names
   * @return a response entity to be sent in the HTTP response body as JSON
   * @see HealthCheckResponse
   * @see HealthCheckStatus
   */
  public HealthCheckResponse verifyBundles(
      HealthCheckProbeType probeType,
      Set<String> requiredBundleSymbolicNames) {

    String message = "No issues with bundles";
    List<String> issues = new ArrayList<>();

    Set<Bundle> bundlesFound = Arrays.stream(_context.getBundles())
                                     .filter(bundle -> requiredBundleSymbolicNames
                                         .contains(bundle.getSymbolicName()))
                                     .collect(Collectors.toSet());

    _checkIndividualBundleHealth(probeType, bundlesFound, issues);

    if (bundlesFound.size() != requiredBundleSymbolicNames.size()) {

      message = "Found " + bundlesFound.size() + " out of " + requiredBundleSymbolicNames.size()
          + " bundles required by the configuration";

      Set<String> bundleFoundSymbolicNames = bundlesFound.stream().map(Bundle::getSymbolicName)
                                                         .collect(Collectors.toSet());
      issues = requiredBundleSymbolicNames
          .stream()
          .filter(symbolicName -> !bundleFoundSymbolicNames.contains(symbolicName))
          .map(symbolicName -> "Bundle [" + symbolicName + "] was not found.")
          .collect(Collectors.toList());

      return HealthCheckResponse.builder()
                                .status(HealthCheckStatus.DOWN)
                                .message(message)
                                .issues(issues)
                                .build();
    }

    issues.addAll(_listResolvedBundles(_context.getBundles()));
    issues.addAll(_listInstalledBundles(_context.getBundles()));

    if (!issues.isEmpty()) {
      message = "Found some required bundles in an undesired state.";
      return HealthCheckResponse.builder()
                                .status(HealthCheckStatus.DOWN)
                                .message(message)
                                .issues(issues)
                                .build();
    }

    return HealthCheckResponse.builder()
                              .status(HealthCheckStatus.UP)
                              .message(message)
                              .issues(issues)
                              .build();
  }

  /**
   * Check individual bundles health, i.e. the given bundles which are providing a service exposing
   * readiness and liveness methods with their own implementation, and thus their own definition of
   * the readiness and liveness for the bundle which can be independent from the bundle state
   * itself.
   *
   * @param probeType type of probe we're looking for (e.g. readiness or liveness)
   * @param bundles   list of bundles on which we want to gather the custom health check
   * @param issues    list of issues to populate if any issue is detected in this method
   * @see HealthCheckService
   * @see HealthCheckProbeType
   */
  private void _checkIndividualBundleHealth(
      HealthCheckProbeType probeType, Set<Bundle> bundles, List<String> issues) {

    ServiceReference<HealthCheckService>[] serviceReferences =
        this.individualBundleHealthCheckServiceTracker.getServiceReferences();

    Arrays.stream(serviceReferences).forEach(serviceReference -> {
      Bundle observedBundle = serviceReference.getBundle();
      if (bundles.contains(observedBundle)) {
        HealthCheckService healthCheckService =
            this.individualBundleHealthCheckServiceTracker.getService(serviceReference);
        String symbolicName = serviceReference.getBundle().getSymbolicName();
        HealthCheckResponse healthCheckResponse =
            _getIndividualBundleHealthCheckResponse(probeType, healthCheckService);
        if (HealthCheckStatus.DOWN.equals(healthCheckResponse.getStatus())) {
          _log.warn("Bundle [" + symbolicName + "] declares being DOWN with following issues:");
          healthCheckResponse.getIssues().forEach(_log::warn);
          issues.addAll(healthCheckResponse.getIssues());
        } else {
          _log.info("Bundle [" + symbolicName + "] declares being UP");
        }
      }
    });

  }

  private HealthCheckResponse _getIndividualBundleHealthCheckResponse(
      HealthCheckProbeType probeType, HealthCheckService healthCheckService) {

    if (HealthCheckProbeType.READINESS.equals(probeType)) {
      return healthCheckService.isReady();
    }
    return healthCheckService.isLive();
  }

  @Activate
  public void activate(BundleContext bundleContext) {

    _context = bundleContext;
    this.individualBundleHealthCheckServiceTracker = new ServiceTracker<>(
        _context, HealthCheckService.class, null);
    this.individualBundleHealthCheckServiceTracker.open();

  }

  @Deactivate
  public void deactivate() {

    this.individualBundleHealthCheckServiceTracker.close();
  }

  private List<String> _getIssues() {

    List<String> issues = new ArrayList<>();

    DependencyGraph graph = DependencyGraph
        .getGraph(ComponentState.UNREGISTERED, DependencyState.REQUIRED_UNAVAILABLE);

    issues.addAll(_listResolvedBundles(_context.getBundles()));
    issues.addAll(_listInstalledBundles(_context.getBundles()));

    List<CircularDependency> circularDependencies = graph.getCircularDependencies();
    if (!circularDependencies.isEmpty()) {
      _log.warn("Circular dependencies:");
      issues.addAll(
          _getCircularDependenciesIssues(circularDependencies)
      );
    }

    List<MissingDependency> missingConfigDependencies = graph
        .getMissingDependencies("configuration");
    if (!missingConfigDependencies.isEmpty()) {
      _log.warn("The following configuration(s) are missing: ");
      issues.addAll(
          _getMissingDependenciesIssues(missingConfigDependencies)
      );
    }

    List<MissingDependency> missingServiceDependencies = graph.getMissingDependencies("service");
    if (!missingServiceDependencies.isEmpty()) {
      _log.warn("The following service(s) are missing: ");
      issues.addAll(
          _getMissingDependenciesIssues(missingServiceDependencies)
      );
    }

    List<MissingDependency> missingResourceDependencies = graph.getMissingDependencies("resource");
    if (!missingResourceDependencies.isEmpty()) {
      _log.warn("The following resource(s) are missing: ");
      issues.addAll(
          _getMissingDependenciesIssues(missingResourceDependencies)
      );
    }

    List<MissingDependency> missingBundleDependencies = graph.getMissingDependencies("bundle");
    if (!missingBundleDependencies.isEmpty()) {
      _log.warn("The following bundle(s) are missing: ");
      issues.addAll(
          _getMissingDependenciesIssues(missingBundleDependencies)
      );
    }

    List<MissingDependency> missingCustomDependencies = graph.getMissingCustomDependencies();
    if (!missingCustomDependencies.isEmpty()) {
      _log.warn("The following custom dependency(ies) are missing: ");
      issues.addAll(
          _getMissingCustomDependenciesIssues(missingCustomDependencies)
      );
    }
    return issues;
  }

  private List<String> _getCircularDependenciesIssues(
      List<CircularDependency> circularDependencies) {

    List<String> issues = new ArrayList<>();
    circularDependencies
        .forEach(c -> {
          _log.warn(" *");
          c.getComponents()
           .forEach(cd -> {
             _log.warn(" -> " + cd.getName());
             issues.add(cd.getName());
           });
        });
    return issues;
  }

  private List<String> _getMissingDependenciesIssues(List<MissingDependency> missingDependencies) {

    List<String> issues = new ArrayList<>();
    missingDependencies
        .forEach(m -> {
          _log.warn(" * " + m.getName() + " for bundle " + m.getBundleName());
          issues.add("Missing dependency " + m.getName() + " for bundle " + m.getBundleName());
        });
    return issues;
  }

  private List<String> _getMissingCustomDependenciesIssues(
      List<MissingDependency> missingDependencies) {

    List<String> issues = new ArrayList<>();
    missingDependencies
        .forEach(m -> {
          _log.warn(
              " * " + m.getName() + "(" + m.getType() + ")" + " for bundle " + m.getBundleName());
          issues.add(
              "Missing custom dependency " + m.getName() + "(" + m.getType() + ")" + " for bundle "
                  + m.getBundleName());
        });
    return issues;
  }

  private List<String> _listResolvedBundles(Bundle[] bundles) {

    List<String> resolveBundleNames = new ArrayList<>();
    boolean areResolved = false;
    for (Bundle b : bundles) {
      if (b.getState() == Bundle.RESOLVED && !_isFragment(b)) {
        areResolved = true;
        break;
      }
    }
    if (areResolved) {
      _log.warn("Please note that the following bundles are in the RESOLVED state:");
      for (Bundle b : bundles) {
        if (b.getState() == Bundle.RESOLVED && !_isFragment(b)) {
          resolveBundleNames
              .add("[" + b.getBundleId() + "] " + b.getSymbolicName() + " is RESOLVED");
          _log.warn(" * [{}] {}", b.getBundleId(), b.getSymbolicName());
        }
      }
    }
    return resolveBundleNames;
  }

  private List<String> _listInstalledBundles(Bundle[] bundles) {

    List<String> installedBundles = new ArrayList<>();
    boolean areInstalled = false;
    for (Bundle b : bundles) {
      if (b.getState() == Bundle.INSTALLED) {
        areInstalled = true;
        break;
      }
    }
    if (areInstalled) {
      _log.warn("Please note that the following bundles are in the INSTALLED state:");
      for (Bundle b : bundles) {
        if (b.getState() == Bundle.INSTALLED) {
          installedBundles
              .add("[" + b.getBundleId() + "] " + b.getSymbolicName() + " is INSTALLED");
          _log.warn(" * [{}] {}", b.getBundleId(), b.getSymbolicName());
        }
      }
    }
    return installedBundles;
  }

  private boolean _isFragment(Bundle b) {

    Dictionary<String, String> headers = b.getHeaders();
    return headers.get("Fragment-Host") != null;
  }

  private BundleContext _context;
  private static final Logger _log = LoggerFactory.getLogger(BundlesHealthCheck.class);
}
