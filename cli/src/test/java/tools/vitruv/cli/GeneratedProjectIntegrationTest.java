package tools.vitruv.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.utils.ProjectMarker;
import tools.vitruv.cli.options.FileUtils;
import tools.vitruv.framework.views.ViewTypeFactory;

class GeneratedProjectIntegrationTest {
  private static final Path OUTPUT = Path.of("target/deep-verification");
  private static final Path RESOURCES = Path.of("src/test/resources");

  @Test
  void projectWithoutReactionsBuildsInDirectoryContainingSpaces() throws Exception {
    Path output = OUTPUT.resolve("no reactions with spaces").toAbsolutePath();
    var model = new CLI().run(new String[] {"-m", metamodels(RESOURCES.resolve("model")),
        "-f", output.toString(), "-u", "default"});
    assertThat(model).isPresent();
    assertGeneratedTestPassed(output);
    GeneratedServerAssertions.assertStartsAndStops(output, "tools.vitruv.methodologisttemplate", false);
    assertThat(output.resolve("vsum/src/main/java/VSUMExample.java"))
        .content().contains(".withChangePropagationSpecifications()")
        .doesNotContain("mir.reactions");
  }

  @Test
  void customPackageAndPathsWithSpacesBuildAndPropagateChanges() throws Exception {
    Path inputs = OUTPUT.resolve("custom metamodels").toAbsolutePath();
    Files.createDirectories(inputs);
    String packageName = "custom.deepverification";
    for (String name : List.of("model.ecore", "model.genmodel", "model2.ecore", "model2.genmodel")) {
      Path target = inputs.resolve(name);
      Files.copy(RESOURCES.resolve("model").resolve(name), target, StandardCopyOption.REPLACE_EXISTING);
      Files.writeString(target, Files.readString(target, StandardCharsets.UTF_8)
          .replace("tools.vitruv.methodologisttemplate", packageName)
          .replace("http://vitruv.tools/methodologisttemplate", "http://vitruv.tools/deepverification"),
          StandardCharsets.UTF_8);
    }
    Path reaction = inputs.resolve("custom.reactions");
    Files.writeString(reaction,
        Files.readString(RESOURCES.resolve("consistency/templateReactions.reactions"), StandardCharsets.UTF_8)
            .replace("http://vitruv.tools/methodologisttemplate", "http://vitruv.tools/deepverification")
            .replace("reactions: model2Model2", "reactions: customModelToModel2"), StandardCharsets.UTF_8);
    Path output = OUTPUT.resolve("custom package with spaces").toAbsolutePath();
    var vsum = new CLI().run(new String[] {"-m", metamodels(inputs), "-f", output.toString(),
        "-u", "default", "-r", reaction.toString()});
    assertThat(vsum).isPresent();
    assertGeneratedTestPassed(output);
    GeneratedServerAssertions.assertStartsAndStops(output, packageName, false);
    assertThat(output.resolve("vsum/src/main/java/VSUMExample.java"))
        .content().contains("package custom.deepverification.vsum;");
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("model", new XMIResourceFactoryImpl());
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("model2", new XMIResourceFactoryImpl());
    Path models = output.resolve("models");
    Files.createDirectories(models);
    ProjectMarker.markAsProjectRootFolder(models);
    EFactory factory = (EFactory) FileUtils.CLASS_LOADER
        .loadClass(packageName + ".model.model.ModelFactory").getField("eINSTANCE").get(null);
    var root = factory.create((EClass) factory.getEPackage().getEClassifier("System"));
    var selector = vsum.get().createSelector(ViewTypeFactory.createIdentityMappingViewType("deep"));
    selector.getSelectableElements().forEach(element -> selector.setSelected(element, true));
    var view = selector.createView().withChangeDerivingTrait();
    view.registerRoot(root, URI.createFileURI(models.resolve("custom.model").toString()));
    view.commitChanges();
    var selected = vsum.get().createSelector(ViewTypeFactory.createIdentityMappingViewType("verify"));
    selected.getSelectableElements().forEach(element -> selected.setSelected(element, true));
    assertThat(selected.createView().getRootObjects()).extracting(object -> object.eClass().getName())
        .containsExactlyInAnyOrder("System", "Root");
  }

  private static void assertGeneratedTestPassed(Path output) throws Exception {
    Path reports = output.resolve("vsum/target/surefire-reports");
    try (var files = Files.list(reports)) {
      Path report = files.filter(path -> path.toString().endsWith("VSUMExampleTest.txt"))
          .findFirst().orElseThrow();
      assertThat(Files.readString(report)).contains("Tests run: 1, Failures: 0, Errors: 0, Skipped: 0");
    }
  }

  private static String metamodels(Path directory) {
    return directory.resolve("model.ecore") + "," + directory.resolve("model.genmodel")
        + ";" + directory.resolve("model2.ecore") + "," + directory.resolve("model2.genmodel");
  }
}
