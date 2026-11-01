package tools.vitruv.cli.configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Versions of the Vitruvius artifacts used by a generated VSUM project.
 *
 * @param parent version of the {@code tools.vitruv:parent} POM
 * @param framework version of the Vitruv framework artifacts ({@code tools.vitruv.framework.*})
 * @param change version of the Vitruv-Change artifacts ({@code tools.vitruv.change.*})
 * @param reactionsRuntime version of the Reactions runtime used by the VSUM
 * @param reactionsLanguage version of the Reactions language used to generate the consistency code
 * @param server version of the Vitruv server ({@code tools.vitruv.server.remote})
 * @param vitruvocl version of VitruvOCL, {@code null} if the project has no constraints
 * @param junit version of the JUnit Jupiter API, which has to match the JUnit engine the parent
 *     runs the tests with
 * @param snapshots whether snapshot versions have to be resolved from the Maven Central snapshots
 */
public record ProjectVersions(
    String parent,
    String framework,
    String change,
    String reactionsRuntime,
    String reactionsLanguage,
    String server,
    String vitruvocl,
    String junit,
    boolean snapshots) {

  /** Versions of projects without constraints. */
  public static final ProjectVersions DEFAULT =
      new ProjectVersions(
          "3.2.0", "3.2.3", "3.2.3", "3.2.3", "3.1.0", "3.2.3", null, "5.12.0", false);

  /**
   * Versions of projects with OCL constraints. The framework must provide the constraint SPI
   * ({@code tools.vitruv.framework.vsum.constraints}), which is available from 4.0.2.
   */
  public static final ProjectVersions WITH_CONSTRAINTS =
      new ProjectVersions(
          "4.0.0",
          "4.0.2-SNAPSHOT",
          "4.0.2-SNAPSHOT",
          "4.0.0",
          "4.0.0",
          "4.0.1-SNAPSHOT",
          "0.1.0-SNAPSHOT",
          "6.1.0",
          true);

  /**
   * Returns the versions for a project with or without constraints.
   *
   * @param hasConstraints whether the project contains OCL constraints
   * @return the versions to use
   */
  public static ProjectVersions forProject(boolean hasConstraints) {
    return hasConstraints ? WITH_CONSTRAINTS : DEFAULT;
  }

  /**
   * Returns the versions as template data; a missing VitruvOCL version is left out.
   *
   * @return the versions by component name
   */
  public Map<String, Object> asTemplateData() {
    Map<String, Object> data = new HashMap<>();
    data.put("parent", parent);
    data.put("framework", framework);
    data.put("change", change);
    data.put("reactionsRuntime", reactionsRuntime);
    data.put("reactionsLanguage", reactionsLanguage);
    data.put("server", server);
    data.put("junit", junit);
    data.put("snapshots", snapshots);
    if (vitruvocl != null) {
      data.put("vitruvocl", vitruvocl);
    }
    return data;
  }
}
