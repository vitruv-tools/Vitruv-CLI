package tools.vitruv.cli.options;

import static org.junit.jupiter.api.Assertions.*;

import com.google.inject.Injector;
import java.io.InputStream;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.mwe2.language.Mwe2StandaloneSetup;
import org.eclipse.emf.mwe2.launch.runtime.Mwe2Runner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.cli.GenerateFromTemplate;
import tools.vitruv.cli.configuration.MetamodelLocation;
import tools.vitruv.cli.configuration.VitruvConfiguration;

class PrecheckAndMweIntegrationTest {

  @TempDir Path tempDir;

  @Test
  void precheck_then_run_mwe2_successfully() throws Exception {
    String modelProjectName = "test.pkg.model";

    Path modelProjectDir = tempDir.resolve(modelProjectName);
    Path ecoreDir = modelProjectDir.resolve("src/main/ecore");
    Path workflowDir = modelProjectDir.resolve("workflow");

    Files.createDirectories(ecoreDir);
    Files.createDirectories(workflowDir);

    Path ecore = ecoreDir.resolve("model.ecore");
    Path genmodel = ecoreDir.resolve("model.genmodel");

    copyResource("/model/simulink.ecore", ecore);
    copyResource("/model/simulink.genmodel", genmodel);

    URI platformRoot = URI.createURI("platform:/resource/" + modelProjectName + "/");
    URI fileRoot = URI.createFileURI(modelProjectDir.toAbsolutePath() + "/");
    URIConverter.URI_MAP.put(platformRoot, fileRoot);

    VitruvConfiguration config = new VitruvConfiguration();
    config.setLocalPath(tempDir);
    config.setPackageName("test.pkg");

    String modelDirectory = "/" + modelProjectName + "/src/main/ecore";

    MetamodelLocation location =
        new MetamodelLocation(
            ecore.toFile(),
            genmodel.toFile(),
            "platform:/resource/" + modelProjectName + "/src/main/ecore/model.genmodel",
            modelDirectory);

    config.addMetamodelLocations(location);

    GenmodelPrecheck precheck = new GenmodelPrecheck();
    List<GenmodelPrecheck.Issue> issues = precheck.process(genmodel.toFile());
    assertNotNull(issues);

    GenerateFromTemplate generator = new GenerateFromTemplate();
    Path workflow = workflowDir.resolve("generate.mwe2");
    generator.generateMwe2(workflow.toFile(), config.getMetaModelLocations(), config);
    assertTrue(Files.exists(workflow));

    Injector injector = new Mwe2StandaloneSetup().createInjectorAndDoEMFRegistration();
    Mwe2Runner runner = injector.getInstance(Mwe2Runner.class);

    Map<String, String> params = new HashMap<>();
    runner.run(URI.createFileURI(workflow.toAbsolutePath().toString()), params);
  }

  private void copyResource(String resource, Path target) throws Exception {
    try (InputStream in = getClass().getResourceAsStream(resource)) {
      if (in == null) throw new IllegalStateException("Resource not found: " + resource);
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
