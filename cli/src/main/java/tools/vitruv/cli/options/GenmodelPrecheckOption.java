package tools.vitruv.cli.options;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import org.apache.commons.cli.CommandLine;
import tools.vitruv.cli.configuration.MetamodelLocation;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/**
 * CLI option to inspect and optionally standardize .genmodel files for MWE2 compatibility.
 */
public class GenmodelPrecheckOption extends VitruvCLIOption {

  private static final String OPT = "pg";
  private static final String APPLY = "apply";

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
  }

  /**
   * Runs the precheck flow.
   *
   * <p>Usage behaviour:
   *
   * <ul>
   *   <li>{@code -pg} → show problems and ask the user whether fixes should be applied
   *   <li>{@code -pg --apply} → apply fixes immediately
   *   <li>{@code -pg -f <folder>} → ask and then continue generation if accepted
   *   <li>{@code -pg --apply -f <folder>} → fix immediately and continue generation
   * </ul>
   *
   * @param cmd the command line
   * @param configuration the Vitruv configuration
   */
  @Override
  public void prepare(CommandLine cmd, VitruvConfiguration configuration) {
    if (!cmd.hasOption(OPT)) {
      return;
    }

    boolean applyImmediately = cmd.hasOption(APPLY);

    List<MetamodelLocation> locations = configuration.getMetaModelLocations();
    if (locations == null || locations.isEmpty()) {
      throw new IllegalArgumentException(
              "No metamodels configured. Provide -m/--metamodel before running --precheck-genmodel.");
    }

    GenmodelPrecheck precheck = new GenmodelPrecheck();

    List<String> failures = new ArrayList<>();
    List<GenmodelPrecheck.Issue> previewIssues = new ArrayList<>();

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

      previewIssues.addAll(precheck.inspect(genmodelFile));
    }

    if (!failures.isEmpty()) {
      throw new IllegalArgumentException(
              "Genmodel precheck failed:\n- " + String.join("\n- ", failures));
    }

    if (previewIssues.isEmpty()) {
      System.out.println("No problems found in the provided genmodel files.");
      return;
    }

    System.out.println("We found some problems in your genmodel files:");
    for (GenmodelPrecheck.Issue issue : previewIssues) {
      System.out.println("- " + issue);
    }

    if (!applyImmediately) {
      if (!askForFixConfirmation()) {
        System.out.println("Process finished without modifying genmodel files.");
        return;
      }
    }

    List<GenmodelPrecheck.Issue> appliedIssues = new ArrayList<>();

    for (MetamodelLocation loc : locations) {
      File genmodelFile = loc.genmodel();
      if (genmodelFile != null && genmodelFile.exists()) {
        appliedIssues.addAll(precheck.process(genmodelFile));
      }
    }

    if (appliedIssues.isEmpty()) {
      System.out.println("No changes were necessary.");
    } else {
      System.out.println("Applied genmodel changes:");
      for (GenmodelPrecheck.Issue issue : appliedIssues) {
        System.out.println("- " + issue);
      }
    }
  }

  /**
   * Prompts the user to confirm whether detected fixes should be applied.
   *
   * @return {@code true} if the user confirmed the fixes
   */
  public boolean askForFixConfirmation() {
    System.out.print("Do you want to fix them? [y/N]: ");
    Scanner scanner = new Scanner(System.in);
    String input = scanner.nextLine();
    String normalized = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    return "y".equals(normalized) || "yes".equals(normalized);
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