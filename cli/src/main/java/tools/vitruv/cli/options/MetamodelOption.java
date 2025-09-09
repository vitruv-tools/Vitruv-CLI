package tools.vitruv.cli.options;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import org.apache.commons.cli.CommandLine;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import tools.vitruv.cli.configuration.MetamodelLocation;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

public class MetamodelOption extends VitruvCLIOption {
  // resource/tools.vitruv.methodologisttemplate.model/src/main/ecore/model.genmodel
  public static final String SUBFOLDER = "/model/src/main/ecore/";
  public static final String WORKFLOW_CONFIGURATION_STRING = """
        component = EcoreGenerator {
          genModel = "platform:/resource/%s"
          srcPath = "platform:/resource/%s/target/generated-sources/ecore"
          generateCustomClasses = false
      }
      """;
    private static final Logger LOGGER = Logger.getLogger(MetamodelOption.class.getName());

  public MetamodelOption() {
    super(
        "m",
        "metamodel",
        true,
        "A semicolon separated list of pairs of paths to the metamodels and their genmodels that"
            + " are used in the reactions, e.g.,"
            + " MyMetamodel.ecore,MyGenmodel.genmodel;MyMetamodel1.ecore,MyGenmodel1.genmodel");
    this.setValueSeparator(';');
    this.setRequired(true);
  }

  @Override
  public VirtualModelBuilder applyInternal(
      CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
    template(cmd.getOptionValue(getOpt()).split(";").length, configuration);
    for (String modelPaths : cmd.getOptionValue(getOpt()).split(";")) {
      String metamodelPath = modelPaths.split(",")[0];
      String genmodelPath = modelPaths.split(",")[1];
      File metamodel = FileUtils.copyFile(metamodelPath, getPath(cmd, builder), SUBFOLDER);
      File genmodel = FileUtils.copyFile(genmodelPath, getPath(cmd, builder), SUBFOLDER);
      // getting the URI from the genmodels
      Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
      reg.getExtensionToFactoryMap().put("ecore", new XMIResourceFactoryImpl());
      ResourceSet resourceSet = new ResourceSetImpl();
      URI uri = URI.createFileURI(metamodel.getAbsolutePath().trim());
      Resource resource = resourceSet.getResource(uri, true);
      if (!resource.getContents().isEmpty() && resource.getContents().get(0) instanceof EPackage ePackage) {
        configuration.addMetamodelLocations(
            new MetamodelLocation(metamodel, genmodel, ePackage.getNsURI()));
      }
    }
    return builder;
  }

  private void template(int count, VitruvConfiguration configuration) {
    try {
      List<String> alines = Files.readAllLines(configuration.getWorkflow().toPath());
      List<String> lines = new ArrayList<>(alines);
      LOGGER.info(configuration.getWorkflow().toPath().toString());
      for (int i = 0; i < lines.size(); i++) {
        if (lines.get(i).contains("#")) {
          LOGGER.info(lines.toString());
          lines.set(i, lines.get(i).replace("#", createSpecialString(count, "#")));
          LOGGER.info(lines.toString());
          Files.write(
              configuration.getWorkflow().toPath(), lines, StandardOpenOption.TRUNCATE_EXISTING);
          return;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String createSpecialString(int x, String specialChar) {
    StringJoiner joiner = new StringJoiner(" ");
    IntStream.range(0, x).forEach((int i) -> joiner.add(specialChar));
    return joiner.toString();
  }

  @Override
  public void prepare(CommandLine cmd, VitruvConfiguration configuration) {
    configuration.setMetaModelLocations(cmd.getOptionValue(getOpt()));
  }
}
