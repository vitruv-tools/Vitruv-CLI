package tools.vitruv.cli.options;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

/** The FileUtils class provides utility methods for file operations. */
public final class FileUtils {
  private static final Logger logger = Logger.getLogger(FileUtils.class.getName());

  private FileUtils() {}

  /**
   * Copy a file to a new location.
   *
   * @param filePath The path of the file that should be copied.
   * @param folderPath The path of the folder to which the file should be copied.
   * @param relativeSubfolder The relative subfolder in which the file should be copied.
   * @return The target file.
   */
  public static File copyFile(String filePath, Path folderPath, String relativeSubfolder) {
    File source = getFile(filePath);
    File target;
    if (folderPath.isAbsolute()) {
      target =
          Path.of(folderPath.toString().trim() + "/" + relativeSubfolder + source.getName().trim())
              .toFile();
    } else {

      target =
          Path.of(
                  new File("").getAbsolutePath().trim()
                      + "/"
                      + folderPath.toString().trim()
                      + "/"
                      + relativeSubfolder
                      + source.getName().trim())
              .toFile();
    }
    // Files.copy throws a misleading Exception if the target File and/or the
    // folders of the target file are not existing.
    logger.info("Copying file " + source.getAbsolutePath() + " to  " + target.getAbsolutePath());
    target.getParentFile().mkdirs();
    try {
      target.createNewFile();
      Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return target;
  }

  /**
   * Creates a new File under <code>filePath</code>.
   * Accounts for the case where <code>filePath</code> is relative, in which case we
   * convert it to an absolute path.
   *
   * @param filePath - String
   * @return new File
   */
  public static File getFile(String filePath) {
    if (new File(filePath).isAbsolute()) {
      return Path.of(filePath).toFile();
    } else {
      return Path.of(new File("").getAbsolutePath().trim() + "/" + filePath.trim()).toFile();
    }
  }

  /**
   * Create a new file in the given path.
   *
   * @param filePath The path of the file that should be created.
   */
  public static void createFile(String filePath) {
    File file = new File(filePath);
    try {
      // Ensure the directory exists
      File parentDir = file.getParentFile();
      if (parentDir != null && !parentDir.exists()) {
        parentDir.mkdirs();
      }
      // Create the file
      if (file.createNewFile()) {
        logger.info("File created: " + file.getAbsolutePath());
      } else {
        logger.info("File already exists: " + file.getAbsolutePath());
      }
    } catch (IOException e) {
      logger.info("An error occurred while creating the file: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Create a new folder in the given path.
   *
   * @param path The path of the folder that should be created.
   * @param folder The name of the folder that should be created.
   * @return The created folder.
   */
  public static Path createNewFolder(Path path, String folder) {
    Path folderPath = path.resolve(folder);
    File file = folderPath.toFile();
    if (file.mkdirs()) {
      logger.info("Directory created: " + file.getAbsolutePath());
    } else {
      logger.info("Directory already exists: " + file.getAbsolutePath());
    }
    return folderPath;
  }

  public static String findOption(File file, String option) {
    try {
      for (String line : Files.readAllLines(file.toPath())) {
        if (line.startsWith(option)) {
          return line.substring(option.length()).trim();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    throw new IllegalArgumentException("Option: " + option + "not found in given file!");
  }

}
