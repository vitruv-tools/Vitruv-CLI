package tools.vitruv.cli.configuration;

import java.nio.file.Path;

/**
 * An OCL constraint file of the project and the name it gets in the generated VSUM.
 *
 * @param source the constraint file given on the command line
 * @param fileName the distinct name of the file in the resources of the generated VSUM
 */
public record ConstraintLocation(Path source, String fileName) {
  /** Directory, relative to the generated project root, containing the constraint files. */
  public static final String RESOURCE_DIRECTORY = "vsum/src/main/resources/constraints";

  /** Classpath prefix of the constraint files in the generated VSUM. */
  public static final String RESOURCE_PREFIX = "constraints/";

  /**
   * Returns the classpath resource name of the file in the generated VSUM.
   *
   * @return the resource name, e.g. {@code constraints/prices.ocl}
   */
  public String resourceName() {
    return RESOURCE_PREFIX + fileName;
  }
}
