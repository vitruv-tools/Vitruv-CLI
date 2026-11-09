package tools.vitruv.cli.options;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import tools.vitruv.cli.configuration.ConstraintLocation;

/** Checks OCL constraint files given on the command line and copies them into the project. */
final class ConstraintFiles {
  private static final String EXTENSION = ".ocl";

  private ConstraintFiles() {}

  /**
   * Checks the given constraint files and assigns each a distinct file name in the generated VSUM.
   * Files with the same name from different directories get a numeric suffix.
   *
   * @param files the constraint files
   * @return the constraint files with their names in the generated VSUM, in the given order
   * @throws IllegalArgumentException if a file does not exist, is no {@code .ocl} file or is empty
   */
  static List<ConstraintLocation> locate(List<Path> files) {
    Set<String> usedNames = new HashSet<>();
    List<ConstraintLocation> locations = new ArrayList<>();
    for (Path file : files) {
      Path source = file.toAbsolutePath().normalize();
      check(source);
      String fileName = source.getFileName().toString();
      String baseName = fileName.substring(0, fileName.length() - EXTENSION.length());
      String name = fileName;
      for (int suffix = 1; !usedNames.add(name.toLowerCase(Locale.ROOT)); suffix++) {
        name = baseName + "-" + suffix + EXTENSION;
      }
      locations.add(new ConstraintLocation(source, name));
    }
    return locations;
  }

  /**
   * Lists the constraint files of a directory, sorted by name. Subdirectories are not searched.
   *
   * @param directory the directory
   * @return the {@code .ocl} files of the directory
   * @throws IllegalArgumentException if the directory does not exist or contains no {@code .ocl}
   *     file
   */
  static List<Path> list(Path directory) {
    Path absolute = directory.toAbsolutePath().normalize();
    if (!Files.exists(absolute)) {
      throw new IllegalArgumentException("Constraints directory does not exist: " + absolute);
    }
    if (!Files.isDirectory(absolute)) {
      throw new IllegalArgumentException("Constraints path is not a directory: " + absolute);
    }
    try (var stream = Files.list(absolute)) {
      List<Path> files =
          stream
              .filter(Files::isRegularFile)
              .filter(ConstraintFiles::isOclFile)
              .sorted(Comparator.comparing(path -> path.getFileName().toString()))
              .toList();
      if (files.isEmpty()) {
        throw new IllegalArgumentException("No .ocl files found in directory: " + absolute);
      }
      return files;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read constraints directory: " + absolute, e);
    }
  }

  /**
   * Copies the constraint files into the resources of the VSUM module of the project, so that the
   * generated VSUM loads them from its classpath.
   *
   * @param projectRoot the root folder of the generated project
   * @param locations the constraint files with their names in the generated VSUM
   * @throws IllegalStateException if copying fails
   */
  static void copy(Path projectRoot, List<ConstraintLocation> locations) {
    Path targetDirectory = projectRoot.resolve(ConstraintLocation.RESOURCE_DIRECTORY);
    try {
      Files.createDirectories(targetDirectory);
      for (ConstraintLocation location : locations) {
        Files.copy(
            location.source(),
            targetDirectory.resolve(location.fileName()),
            StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new IllegalStateException(
          "Failed to copy constraint files into project: " + e.getMessage(), e);
    }
  }

  private static void check(Path file) {
    if (!Files.isRegularFile(file)) {
      throw new IllegalArgumentException("Constraint file does not exist: " + file);
    }
    if (!isOclFile(file)) {
      throw new IllegalArgumentException("Constraint file must end with .ocl: " + file);
    }
    try {
      if (Files.size(file) == 0) {
        throw new IllegalArgumentException("Constraint file is empty: " + file);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read constraint file: " + file, e);
    }
  }

  private static boolean isOclFile(Path file) {
    return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(EXTENSION);
  }
}
