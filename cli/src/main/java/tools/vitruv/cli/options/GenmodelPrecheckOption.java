package tools.vitruv.cli.options;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import tools.vitruv.cli.configuration.MetamodelLocation;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/**
 * CLI option to validate and standardize .genmodel files for MWE2 compatibility.
 */
public class GenmodelPrecheckOption extends VitruvCLIOption {

  private static final String OPT = "pg";

  /**
   * Constructs the genmodel precheck option.
   */
  public GenmodelPrecheckOption() {
    super(
        OPT,
        "precheck-genmodel",
        false,
        "Precheck and standardize .genmodel files for MWE2 template compatibility.");
    this.setArgs(0);
    this.setOptionalArg(true);
  }

  /**
   * Validates and processes all configured genmodel files.
   *
   * @param cmd the command line
   * @param configuration the Vitruv configuration
   */
  @Override
  public void prepare(CommandLine cmd, VitruvConfiguration configuration) {
    if (!cmd.hasOption(OPT)) return;

    List<MetamodelLocation> locations = configuration.getMetaModelLocations();
    if (locations == null || locations.isEmpty()) {
      throw new IllegalArgumentException(
          "No metamodels configured. Provide -m/--metamodel before running --precheck-genmodel.");
    }

    GenmodelPrecheck precheck = new GenmodelPrecheck();

    List<String> failures = new ArrayList<>();
    List<GenmodelPrecheck.Issue> issues = new ArrayList<>();

    for (MetamodelLocation loc : locations) {
      File genmodelFile = loc.genmodel();

      if (genmodelFile == null) {
        failures.add("Metamodel has no genmodel file reference: " + loc);
        continue;
      }

      if (!genmodelFile.exists()) {
        failures.add("Genmodel file does not exist: " + genmodelFile.getAbsolutePath());
        continue;
      }

      issues.addAll(precheck.process(genmodelFile));
    }

    if (!failures.isEmpty()) {
      throw new IllegalArgumentException(
          "Genmodel precheck failed:\n- " + String.join("\n- ", failures));
    }

    System.out.println(issues);
  }

  @Override
  public VirtualModelBuilder preBuild(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    return builder;
  }

  @Override
  public VirtualModelBuilder applyInternal(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    return builder;
  }

  @Override
  public VirtualModelBuilder postBuild(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    return builder;
  }
}
