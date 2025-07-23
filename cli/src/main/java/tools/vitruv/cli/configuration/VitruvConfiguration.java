package tools.vitruv.cli.configuration;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

/**
 * The VitruvConfiguration class is used to store the configuration of the
 * Vitruv CLI.
 */
public class VitruvConfiguration {
  private Path localPath;
  private String packageName;
  
  private static final Logger logger = Logger.getLogger(VitruvConfiguration.class.getName());

  /**
   * Returns the local path of the configuration.
   *
   * @return The local path of the configuration.
   */
  public Path getLocalPath() {
    return localPath;
  }

  /**
   * Sets the local path of the configuration.
   *
   * @param localPath The local path of the configuration.
   */
  public void setLocalPath(Path localPath) {
    this.localPath = localPath;
  }

  private List<MetamodelLocation> metamodelLocations = new ArrayList<>();

  /**
   * Adds a metamodel location to the configuration.
   *
   * @param metamodelLocations The metamodel location to add.
   * @return True if the metamodel location was added successfully, false
   *         otherwise.
   */
  public boolean addMetamodelLocations(MetamodelLocation metamodelLocations) {
    return this.metamodelLocations.add(metamodelLocations);
  }

  private File workflow;

  /**
   * Returns the workflow of the configuration.
   *
   * @return The workflow of the configuration.
   */
  public File getWorkflow() {
    return workflow;
  }

  /**
   * Sets the workflow of the configuration.
   *
   * @param workflow The workflow of the configuration.
   */
  public void setWorkflow(File workflow) {
    this.workflow = workflow;
  }

  /**
   * Sets the metamodel locations.
   *
   * @param paths The paths of the metamodels.
   */
  public void setMetaModelLocations(String paths) {
    // Register the GenModel resource factory
    Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
    reg.getExtensionToFactoryMap().put("ecore", new XMIResourceFactoryImpl());
    reg.getExtensionToFactoryMap().put("genmodel", new XMIResourceFactoryImpl());

    // Register the GenModel package
    GenModelPackage.eINSTANCE.eClass();

    for (String modelPaths : paths.split(";")) {
      String metamodelPath = modelPaths.split(",")[0];
      String genmodelPath = modelPaths.split(",")[1];
      File metamodel = new File(metamodelPath);
      File genmodel = new File(genmodelPath);

      // getting the URI from the genmodels
      ResourceSet resourceSet = new ResourceSetImpl();
      URI uri = URI.createFileURI(metamodel.getAbsolutePath().replaceAll("\\s", ""));
      Resource resource = resourceSet.getResource(uri, true);
      if (!resource.getContents().isEmpty() && resource.getContents().get(0) instanceof EPackage ePackage) {
        this.addMetamodelLocations(new MetamodelLocation(metamodel, genmodel, ePackage.getNsURI()));
        // Load the GenModel to get the modelPluginID
        URI genmodelURI = URI.createFileURI(genmodel.getAbsolutePath());
        Resource genmodelResource = resourceSet.getResource(genmodelURI, true);
        if (!genmodelResource.getContents().isEmpty()
            && genmodelResource.getContents().get(0) instanceof GenModel genModel) {
          String packageString = removeLastSegment(genModel.getModelPluginID());
          logger.info("--------------------->>>>  " + packageString);
          this.setPackageName(packageString);
        }
      }
    }
  }

  /**
   * Removes the last segment of a string.
   *
   * @param input The input string.
   * @return The input string without the last segment.
   */
  public static String removeLastSegment(String input) {
    int lastDotIndex = input.lastIndexOf('.');
    if (lastDotIndex == -1) {
      // No dot found, return the original string
      return input;
    }
    return input.substring(0, lastDotIndex);
  }

  /**
   * Returns the metamodel locations.
   *
   * @return The metamodel locations.
   */
  public List<MetamodelLocation> getMetaModelLocations() {
    return this.metamodelLocations;
  }

  /**
   * Returns the package name.
   *
   * @return The package name.
   */
  public String getPackageName() {
    return this.packageName;
  }

  /**
   * Sets the package name.
   *
   * @param packageName The package name.
   */
  public void setPackageName(String packageName) {
    this.packageName = packageName.replace("\\s", "");
  }
}
