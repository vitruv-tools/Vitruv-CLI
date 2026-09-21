package tools.vitruv.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.cli.ParseException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.utils.ProjectMarker;
import tools.vitruv.cli.options.FileUtils;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;

class CLITest {
  private static final Path TEST_RESOURCES = Path.of("src/test/resources");
  private static final Path REACTIONS_DIRECTORY = TEST_RESOURCES.resolve("consistency");
  private static final Path MODEL_TO_MODEL2_REACTIONS =
      REACTIONS_DIRECTORY.resolve("templateReactions.reactions");
  private static final Path MODEL2_TO_MODEL_REACTIONS =
      REACTIONS_DIRECTORY.resolve("templateReactions2.reactions");
  private static final Path OUTPUT_ROOT = Path.of("target/cli-test");

  private static final String MODEL_FACTORY =
      "tools.vitruv.methodologisttemplate.model.model.ModelFactory";
  private static final String MODEL2_FACTORY =
      "tools.vitruv.methodologisttemplate.model.model2.Model2Factory";

  @BeforeAll
  static void registerResourceFactories() {
    var extensionToFactoryMap = Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap();
    extensionToFactoryMap.put("model", new XMIResourceFactoryImpl());
    extensionToFactoryMap.put("model2", new XMIResourceFactoryImpl());
  }

  @Test
  void singleReactionFileInRelativeFolderPropagatesChanges() throws Exception {
    Path folder = cleanFolder(OUTPUT_ROOT.resolve("single-reaction"));

    Optional<VirtualModel> vsum =
        new CLI()
            .run(
                new String[] {
                  "-m", metamodelsIn(TEST_RESOURCES.resolve("model")),
                  "-f", folder.toString(),
                  "-u", "default",
                  "-r", MODEL_TO_MODEL2_REACTIONS.toString()
                });

    assertThat(vsum).isPresent();
    assertGeneratedVsumTestWasExecuted(folder);
    assertThat(folder.resolve("consistency/src/main/reactions/templateReactions.reactions"))
        .exists();
    assertRootIsPropagated(vsum.get(), folder, MODEL_FACTORY, "System", "model", "Root");
  }

  @Test
  void reactionInOppositeDirectionPropagatesChanges() throws Exception {
    Path folder = cleanFolder(OUTPUT_ROOT.resolve("opposite-reaction"));

    Optional<VirtualModel> vsum =
        new CLI()
            .run(
                new String[] {
                  "--metamodel", metamodelsIn(TEST_RESOURCES.resolve("model")),
                  "--folder", folder.toString(),
                  "--userinteractor", "default",
                  "--reaction", MODEL2_TO_MODEL_REACTIONS.toString()
                });

    assertThat(vsum).isPresent();
    assertGeneratedVsumTestWasExecuted(folder);
    assertRootIsPropagated(vsum.get(), folder, MODEL2_FACTORY, "Root", "model2", "System");
  }

  @Test
  void reactionsDirectoryWithPrecheckInAbsoluteFolderPropagatesChanges() throws Exception {
    Path folder = cleanFolder(OUTPUT_ROOT.resolve("reactions-directory")).toAbsolutePath();
    // the precheck modifies the genmodels, so it must not operate on the test resources
    Path metamodels = copyMetamodelsTo(cleanFolder(OUTPUT_ROOT.resolve("metamodels-directory")));

    Optional<VirtualModel> vsum =
        new CLI()
            .run(
                new String[] {
                  "-pg",
                  "--apply",
                  "-m", metamodelsIn(metamodels.toAbsolutePath()),
                  "-f", folder.toString(),
                  "-u", "default",
                  "-rs", REACTIONS_DIRECTORY.toAbsolutePath().toString()
                });

    assertThat(vsum).isPresent();
    assertGeneratedVsumTestWasExecuted(folder);
    assertThat(folder.resolve("consistency/src/main/reactions"))
        .isDirectoryContaining("glob:**templateReactions.reactions")
        .isDirectoryContaining("glob:**templateReactions2.reactions");
    assertRootIsPropagated(vsum.get(), folder, MODEL_FACTORY, "System", "model", "Root");
  }

  @Test
  void precheckWithoutFolderDoesNotGenerateProject() throws Exception {
    Path metamodels = copyMetamodelsTo(cleanFolder(OUTPUT_ROOT.resolve("metamodels-precheck")));

    Optional<VirtualModel> vsum =
        new CLI().run(new String[] {"-pg", "--apply", "-m", metamodelsIn(metamodels)});

    assertThat(vsum).isEmpty();
  }

  @Test
  void missingMetamodelOptionFails() {
    String[] args = {
      "-f", OUTPUT_ROOT.resolve("missing-metamodel").toString(),
      "-u", "default",
      "-r", MODEL_TO_MODEL2_REACTIONS.toString()
    };

    assertThatThrownBy(() -> new CLI().run(args))
        .isInstanceOf(ParseException.class)
        .hasMessageContaining("Missing required option: m");
    assertThat(OUTPUT_ROOT.resolve("missing-metamodel")).doesNotExist();
  }

  @Test
  void reactionFileAndReactionsDirectoryAreMutuallyExclusive() {
    String[] args = {
      "-m", metamodelsIn(TEST_RESOURCES.resolve("model")),
      "-f", OUTPUT_ROOT.resolve("exclusive-reactions").toString(),
      "-u", "default",
      "-r", MODEL_TO_MODEL2_REACTIONS.toString(),
      "-rs", REACTIONS_DIRECTORY.toString()
    };

    assertThatThrownBy(() -> new CLI().run(args))
        .isInstanceOf(ParseException.class)
        .hasMessageContaining("mutually exclusive");
  }

  @Test
  void missingReactionsDirectoryFails() {
    String[] args = {
      "-m", metamodelsIn(TEST_RESOURCES.resolve("model")),
      "-f", OUTPUT_ROOT.resolve("missing-reactions").toString(),
      "-u", "default",
      "-rs", OUTPUT_ROOT.resolve("does-not-exist").toString()
    };

    assertThatThrownBy(() -> new CLI().run(args))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not exist");
  }

  @Test
  void mainDoesNotThrowOnInvalidArguments() {
    assertThatCode(() -> CLI.main(new String[] {"-u", "default"})).doesNotThrowAnyException();
  }

  private static String metamodelsIn(Path directory) {
    return directory.resolve("model.ecore")
        + ","
        + directory.resolve("model.genmodel")
        + ";"
        + directory.resolve("model2.ecore")
        + ","
        + directory.resolve("model2.genmodel");
  }

  private static Path copyMetamodelsTo(Path directory) throws IOException {
    Files.createDirectories(directory);
    for (String file : List.of("model.ecore", "model.genmodel", "model2.ecore", "model2.genmodel")) {
      Files.copy(
          TEST_RESOURCES.resolve("model").resolve(file),
          directory.resolve(file),
          StandardCopyOption.REPLACE_EXISTING);
    }
    return directory;
  }

  private static Path cleanFolder(Path folder) throws IOException {
    if (Files.exists(folder)) {
      try (Stream<Path> paths = Files.walk(folder)) {
        for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
          Files.delete(path);
        }
      }
    }
    return folder;
  }

  /**
   * The generated project contains a test interacting with the generated V-SUM, which is executed
   * by the Maven build the CLI triggers.
   */
  private static void assertGeneratedVsumTestWasExecuted(Path folder) throws IOException {
    Path reports = folder.resolve("vsum/target/surefire-reports");
    assertThat(reports).isDirectoryContaining("glob:**VSUMExampleTest.txt");
    try (Stream<Path> files = Files.list(reports)) {
      Path report =
          files.filter(file -> file.toString().endsWith("VSUMExampleTest.txt")).findFirst().get();
      assertThat(Files.readString(report))
          .contains("Tests run: 1, Failures: 0, Errors: 0, Skipped: 0");
    }
  }

  /**
   * Registers a root element in the V-SUM built by the CLI and validates that the reactions create
   * the corresponding root element in the other model.
   */
  private static void assertRootIsPropagated(
      VirtualModel vsum,
      Path folder,
      String factoryName,
      String rootClassName,
      String fileExtension,
      String propagatedRootClassName)
      throws Exception {
    Path modelsFolder = folder.resolve("models").toAbsolutePath();
    Files.createDirectories(modelsFolder);
    ProjectMarker.markAsProjectRootFolder(modelsFolder);

    EFactory factory =
        (EFactory) FileUtils.CLASS_LOADER.loadClass(factoryName).getField("eINSTANCE").get(null);
    EObject root = factory.create((EClass) factory.getEPackage().getEClassifier(rootClassName));

    CommittableView view = createDefaultView(vsum).withChangeDerivingTrait();
    view.registerRoot(
        root, URI.createFileURI(modelsFolder.resolve("example." + fileExtension).toString()));
    view.commitChanges();

    assertThat(createDefaultView(vsum).getRootObjects())
        .extracting(rootObject -> rootObject.eClass().getName())
        .containsExactlyInAnyOrder(rootClassName, propagatedRootClassName);
  }

  private static View createDefaultView(VirtualModel vsum) {
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
    selector.getSelectableElements().forEach(element -> selector.setSelected(element, true));
    return selector.createView();
  }
}
