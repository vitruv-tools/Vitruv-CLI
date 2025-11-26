package tools.vitruv.cli;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import tools.vitruv.cli.configuration.MetamodelLocation;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.cli.exceptions.MissingModelException;
import tools.vitruv.cli.options.FileUtils;

/** This class is responsible for generating files from templates. */
public class GenerateFromTemplate {
  /** Constructor. */
  public GenerateFromTemplate() {
  }

  private static final Logger logger = Logger.getLogger(GenerateFromTemplate.class.getName());
  private static final String PACKAGE_NAME = "packageName";

  private Configuration getConfiguration() {
    Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
    cfg.setDefaultEncoding("UTF-8");
    cfg.setClassForTemplateLoading(this.getClass(), "/templates");
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    cfg.setLogTemplateExceptions(false);
    cfg.setWrapUncheckedExceptions(true);
    return cfg;
  }

  private void writeTemplate(Template template, File filePath, Map<String, Object> data)
      throws IOException {
    FileUtils.createFile(filePath.getAbsolutePath());
    // Write output to file
    try (Writer fileWriter = new FileWriter(filePath.getAbsolutePath(), false)) {
      template.process(data, fileWriter);
      fileWriter.flush();
      logger.info("writing to " + filePath.getAbsolutePath());
    } catch (TemplateException e) {
      e.printStackTrace();
    }
  }

  /**
   * Generates the root pom file.
   *
   * @param filePath    The file path to write the root pom file to.
   * @param packageName The package name from the genmodel.
   * @throws IOException           If the file cannot be written.
   * @throws MissingModelException If the package name is missing.
   */
  public void generateRootPom(File filePath, String packageName)
      throws IOException, MissingModelException {

    if (packageName == null || packageName.isEmpty()) {
      throw new MissingModelException("-m ModelOption is missing the PackageName");
    }
    Configuration cfg = getConfiguration();

    Map<String, Object> data = new HashMap<>();
    data.put("packageName", packageName.trim());

    Template template = null;
    try {
      template = cfg.getTemplate("rootPom.ftl");
    } catch (IOException e) {
      e.printStackTrace();
    }
    writeTemplate(template, filePath, data);
  }

  /**
   * Generates the vsum pom file.
   *
   * @param filePath    The file path to write the vsum pom file to.
   * @param packageName The package name from the genmodel.
   * @throws IOException If the file cannot be written.
   */
  public void generateVsumPom(File filePath, String packageName) throws IOException {
    Configuration cfg = getConfiguration();

    Map<String, Object> data = new HashMap<>();
    data.put("packageName", packageName.trim());

    Template template = null;
    try {
      template = cfg.getTemplate("vsumPom.ftl");
    } catch (IOException e) {
      e.printStackTrace();
    }
    writeTemplate(template, filePath, data);
  }

  /**
   * Generates the vsum example file.
   *
   * @param filePath    The file path to write the vsum example file to.
   * @param packageName The package name from the genmodel.
   * @throws IOException If the file cannot be written.
   */
  public void generateVsumExample(File filePath, String packageName) throws IOException {
    Configuration cfg = getConfiguration();

    Map<String, Object> data = new HashMap<>();
    data.put("packageName", packageName.trim());

    Template template = null;
    try {
      template = cfg.getTemplate("vsumExample.ftl");
    } catch (IOException e) {
      e.printStackTrace();
    }
    writeTemplate(template, filePath, data);
  }

  /**
   * Generates the p2wrappers pom file.
   *
   * @param filePath    The file path to write the p2wrappers pom file to.
   * @param packageName The package name from the genmodel.
   * @throws IOException If the file cannot be written.
   */
  public void generateP2WrappersPom(File filePath, String packageName) throws IOException {
    Configuration cfg = getConfiguration();

    Map<String, Object> data = new HashMap<>();
    data.put("packageName", packageName.trim());

    Template template = null;
    try {
      template = cfg.getTemplate("p2wrappersPom.ftl");
    } catch (IOException e) {
      e.printStackTrace();
    }
    writeTemplate(template, filePath, data);
  }

  /**
   * Generates the javautils pom file.
   *
   * @param filePath    The file path to write the javautils pom file to.
   * @param packageName The package name from the genmodel.
   * @throws IOException If the file cannot be written.
   */
  public void generateJavaUtilsPom(File filePath, String packageName) throws IOException {
    Configuration cfg = getConfiguration();

    Map<String, Object> data = new HashMap<>();
    data.put("packageName", packageName.trim());

    Template template = null;
    try {
      template = cfg.getTemplate("javautilsPom.ftl");
    } catch (IOException e) {
      e.printStackTrace();
    }
    writeTemplate(template, filePath, data);
  }

  /**
   * Generates the xannotations pom file.
   *
   * @param filePath    The file path to write the xannotations pom file to.
   * @param packageName The package name from the genmodel.
   * @throws IOException If the file cannot be written.
   */
  public void generateXAnnotationsPom(File filePath, String packageName) throws IOException {
    Configuration cfg = getConfiguration();

    Map<String, Object> data = new HashMap<>();
    data.put("packageName", packageName.trim());

    Template template = null;
    try {
      template = cfg.getTemplate("xannotationsPom.ftl");
    } catch (IOException e) {
      e.printStackTrace();
    }
    writeTemplate(template, filePath, data);
  }

  /**
   * Generates the emfutils pom file.
   *
   * @param filePath    The file path to write the emfutils pom file to.
   * @param packageName The package name from the genmodel.
   * @throws IOException If the file cannot be written.
   */
  public void generateEMFUtilsPom(File filePath, String packageName) throws IOException {
    Configuration cfg = getConfiguration();

    Map<String, Object> data = new HashMap<>();
    data.put("packageName", packageName.trim());

    Template template = null;
    try {
      template = cfg.getTemplate("emfutilsPom.ftl");
    } catch (IOException e) {
      e.printStackTrace();
    }
    writeTemplate(template, filePath, data);
  }

  /**
   * Generates the vsum test file.
   *
   * @param filePath    The file path to write the vsum test file to.
   * @param packageName The package name from the genmodel.
   * @throws IOException If the file cannot be written.
   */
  public void generateVsumTest(File filePath, String packageName) throws IOException {
    Configuration cfg = getConfiguration();

    Map<String, Object> data = new HashMap<>();
    data.put("packageName", packageName.trim());

    Template template = null;
    try {
      template = cfg.getTemplate("vsumTest.ftl");
    } catch (IOException e) {
      e.printStackTrace();
    }
    writeTemplate(template, filePath, data);
  }

  /**
   * Generates the project file.
   *
   * @param filePath    The file path to write the project file to.
   * @param packageName The package name from the genmodel.
   * @throws IOException If the file cannot be written.
   */
  public void generateProjectFile(File filePath, String packageName) throws IOException {
    Configuration cfg = getConfiguration();

    Map<String, Object> data = new HashMap<>();
    data.put("packageName", packageName.trim());

    Template template = null;
    try {
      template = cfg.getTemplate("project.ftl");
    } catch (IOException e) {
      e.printStackTrace();
    }
    writeTemplate(template, filePath, data);
  }

  /**
   * Generates the model pom file.
   *
   * @param filePath    The file path to write the model pom file to.
   * @param packageName The package name from the genmodel.
   * @throws IOException If the file cannot be written.
   */
  public void generateModelPom(File filePath, String packageName) throws IOException {
    Configuration cfg = getConfiguration();

    Map<String, Object> data = new HashMap<>();
    data.put(PACKAGE_NAME, packageName);

    Template template = null;
    try {
      template = cfg.getTemplate("modelPom.ftl");
    } catch (IOException e) {
      e.printStackTrace();
    }
    writeTemplate(template, filePath, data);
  }

  /**
   * Generates the consistency pom file.
   *
   * @param filePath    The file path to write the consistency pom file to.
   * @param packageName The package name from the genmodel.
   * @throws IOException If the file cannot be written.
   */
  public void generateConsistencyPom(File filePath, String packageName) throws IOException {
    Configuration cfg = getConfiguration();

    Map<String, Object> data = new HashMap<>();
    data.put(PACKAGE_NAME, packageName);

    Template template = null;
    try {
      template = cfg.getTemplate("consistencyPom.ftl");
    } catch (IOException e) {
      e.printStackTrace();
    }
    writeTemplate(template, filePath, data);
  }

  private String getNormalizedDirectoryString(String targetDir) {
    return targetDir.replace("\\", "/").replaceAll("//+", "/");
  }

  /**
   * Generates the mwe2 file.
   *
   * @param filePath the file path to write the mwe2 file to.
   * @param models   the list of metamodel locations.
   * @param config   the vitruv cli configuration.
   * @throws IOException If the file cannot be written.
   */
  public void generateMwe2(
      File filePath, List<MetamodelLocation> models, VitruvConfiguration config)
      throws IOException {

    Configuration cfg = getConfiguration();
    List<Map<String, Object>> items = new ArrayList<>();
    for (MetamodelLocation model : models) {
      items.add(
          Map.of(
              "targetDir",
              getNormalizedDirectoryString(config.getLocalPath().toString().trim()),
              "modelName",
              model.genmodel().getName(),
              "modelDirectory", getNormalizedDirectoryString(model.modelDirectory().trim()),
              "packageName",
              config.getPackageName().trim().concat(".model")));
    }
    // Load template
    Template template = null;
    try {
      template = cfg.getTemplate("generator.ftl");
    } catch (IOException e) {
      e.printStackTrace();
    }
    Map<String, Object> data = new HashMap<>();
    data.put("items", items);

    writeTemplate(template, filePath, data);
  }

  /**
   * Generates the plugin file.
   *
   * @param filePath the file path to write the plugin file to.
   * @param config   the vitruv cli configuration.
   * @param models   the list of metamodel locations.
   * @throws IOException If the file cannot be written.
   */
  public void generatePlugin(
      File filePath, VitruvConfiguration config, List<MetamodelLocation> models)
      throws IOException {
    Configuration cfg = getConfiguration();
    List<Map<String, Object>> items = new ArrayList<>();
    for (MetamodelLocation model : models) {
      items.add(
          Map.of(
              PACKAGE_NAME,
              config.getPackageName(),
              "modelUri",
              model.genmodelUri(),
              "modelNameCap",
              model.genmodel().getName().substring(0, 1).toUpperCase()
                  + model
                      .genmodel()
                      .getName()
                      .substring(1, model.genmodel().getName().indexOf('.')),
              "genmodelName",
              model.genmodel().getName()));
    }
    // Load template
    Template template = null;
    try {
      template = cfg.getTemplate("plugin.ftl");
    } catch (IOException e) {
      e.printStackTrace();
    }
    Map<String, Object> data = new HashMap<>();
    data.put("items", items);
    writeTemplate(template, filePath, data);
  }
}
