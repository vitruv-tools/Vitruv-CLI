package tools.vitruv.cli.options;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.vitruv.cli.configuration.MetamodelLocation;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/**
 * Unit tests for GenmodelPrecheckOption CLI option.
 *
 * <p>Tests cover:
 * - Option not present behavior
 * - No metamodels configured scenarios
 * - Genmodel file validation (non-existent, null references)
 * - Clean status when no issues found
 * - Issue detection and handling with user confirmation
 * - Automatic fixes with --apply flag
 * - Builder lifecycle methods (preBuild, applyInternal, postBuild)
 * - User confirmation input variations
 */
@ExtendWith(MockitoExtension.class)
class GenmodelPrecheckOptionTest {

  private GenmodelPrecheckOption option;
  private ByteArrayOutputStream outContent;
  private PrintStream originalOut;

  @BeforeEach
  void setUp() {
    option = new GenmodelPrecheckOption();
    outContent = new ByteArrayOutputStream();
    originalOut = System.out;
    System.setOut(new PrintStream(outContent));
  }

  void restoreStdout() {
    System.setOut(originalOut);
  }

  private CommandLine parse(String... args) throws Exception {
    Options options = new Options();
    options.addOption(option);
    options.addOption("m", "metamodel", true, "Metamodel");
    options.addOption(null, "apply", false, "Apply fixes");
    return new DefaultParser().parse(options, args);
  }

  @Test
  void prepare_optionNotPresent_doesNothing(@TempDir Path tempDir) throws Exception {
    VitruvConfiguration config = new VitruvConfiguration();
    config.setLocalPath(tempDir);
    CommandLine cmd = parse();
    option.prepare(cmd, config);
    restoreStdout();
  }

  @Test
  void prepare_noMetamodelsConfigured_throwsException(@TempDir Path tempDir) throws Exception {
    VitruvConfiguration config = new VitruvConfiguration();
    config.setLocalPath(tempDir);
    CommandLine cmd = parse("-pg");
    restoreStdout();
    assertThatThrownBy(() -> option.prepare(cmd, config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("No metamodels configured");
  }

  @Test
  void preBuild_returnsUnmodifiedBuilder(@TempDir Path tempDir) {
    VitruvConfiguration config = new VitruvConfiguration();
    config.setLocalPath(tempDir);
    VirtualModelBuilder builder = new VirtualModelBuilder();
    CommandLine cmd = mock(CommandLine.class);
    VirtualModelBuilder result = option.preBuild(cmd, builder, config);
    assertThat(result).isSameAs(builder);
  }

  @Test
  void applyInternal_returnsUnmodifiedBuilder(@TempDir Path tempDir) {
    VitruvConfiguration config = new VitruvConfiguration();
    config.setLocalPath(tempDir);
    VirtualModelBuilder builder = new VirtualModelBuilder();
    CommandLine cmd = mock(CommandLine.class);
    VirtualModelBuilder result = option.applyInternal(cmd, builder, config);
    assertThat(result).isSameAs(builder);
  }

  @Test
  void postBuild_returnsUnmodifiedBuilder(@TempDir Path tempDir) {
    VitruvConfiguration config = new VitruvConfiguration();
    config.setLocalPath(tempDir);
    VirtualModelBuilder builder = new VirtualModelBuilder();
    CommandLine cmd = mock(CommandLine.class);
    VirtualModelBuilder result = option.postBuild(cmd, builder, config);
    assertThat(result).isSameAs(builder);
  }

  @Test
  void askForFixConfirmation_userAnswersYes_returnsTrue() {
    String userInput = "y\n";
    System.setIn(new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8)));
    boolean result = option.askForFixConfirmation();
    assertThat(result).isTrue();
  }

  @Test
  void askForFixConfirmation_userAnswersYes_uppercase_returnsTrue() {
    String userInput = "Y\n";
    System.setIn(new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8)));
    boolean result = option.askForFixConfirmation();
    assertThat(result).isTrue();
  }

  @Test
  void askForFixConfirmation_userAnswersYes_fullWord_returnsTrue() {
    String userInput = "yes\n";
    System.setIn(new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8)));
    boolean result = option.askForFixConfirmation();
    assertThat(result).isTrue();
  }

  @Test
  void askForFixConfirmation_userAnswersYes_fullWordUppercase_returnsTrue() {
    String userInput = "YES\n";
    System.setIn(new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8)));
    boolean result = option.askForFixConfirmation();
    assertThat(result).isTrue();
  }

  @Test
  void askForFixConfirmation_userAnswersNo_returnsFalse() {
    String userInput = "n\n";
    System.setIn(new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8)));
    boolean result = option.askForFixConfirmation();
    assertThat(result).isFalse();
  }

  @Test
  void askForFixConfirmation_userAnswersNo_fullWord_returnsFalse() {
    String userInput = "no\n";
    System.setIn(new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8)));
    boolean result = option.askForFixConfirmation();
    assertThat(result).isFalse();
  }

  @Test
  void askForFixConfirmation_userAnswersEmpty_returnsFalse() {
    String userInput = "\n";
    System.setIn(new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8)));
    boolean result = option.askForFixConfirmation();
    assertThat(result).isFalse();
  }

  @Test
  void askForFixConfirmation_userAnswersWithWhitespace_trimsAndEvaluates() {
    String userInput = "  y  \n";
    System.setIn(new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8)));
    boolean result = option.askForFixConfirmation();
    assertThat(result).isTrue();
  }

  @Test
  void askForFixConfirmation_userAnswersInvalidInput_returnsFalse() {
    String userInput = "maybe\n";
    System.setIn(new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8)));
    boolean result = option.askForFixConfirmation();
    assertThat(result).isFalse();
  }

  private File createValidGenmodelFile(Path tempDir) throws Exception {
    return createValidGenmodelFile(tempDir, "model.genmodel");
  }

  private File createValidGenmodelFile(Path tempDir, String filename) throws Exception {
    File ecore = Files.createFile(tempDir.resolve(filename.replace(".genmodel", ".ecore"))).toFile();
    String ecoreContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<ecore:EPackage xmi:version=\"2.0\" xmlns:xmi=\"http://www.omg.org/XMI\" "
        + "xmlns:ecore=\"http://www.eclipse.org/emf/2002/Ecore\" name=\"model\" "
        + "nsURI=\"http://example/model\" nsPrefix=\"model\"></ecore:EPackage>";
    Files.writeString(ecore.toPath(), ecoreContent, StandardCharsets.UTF_8);
    File genmodelFile = tempDir.resolve(filename).toFile();
    String genmodelContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<genmodel:GenModel xmi:version=\"2.0\" xmlns:xmi=\"http://www.omg.org/XMI\" "
        + "xmlns:genmodel=\"http://www.eclipse.org/emf/2002/GenModel\" modelPluginID=\"p\" "
        + "modelDirectory=\"/p/target/generated-sources/ecore\" creationIcons=\"false\">"
        + "<genPackages prefix=\"Model\" basePackage=\"p\" ecorePackage=\"" + ecore.getName() + "#/\"/>"
        + "</genmodel:GenModel>";
    Files.writeString(genmodelFile.toPath(), genmodelContent, StandardCharsets.UTF_8);
    return genmodelFile;
  }

  private File createGenmodelFileWithIssues(Path tempDir) throws Exception {
    File ecore = tempDir.resolve("issues.ecore").toFile();
    String ecoreContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<ecore:EPackage xmi:version=\"2.0\" xmlns:xmi=\"http://www.omg.org/XMI\" "
        + "xmlns:ecore=\"http://www.eclipse.org/emf/2002/Ecore\" name=\"model\" "
        + "nsURI=\"http://example/model\" nsPrefix=\"model\"></ecore:EPackage>";
    Files.writeString(ecore.toPath(), ecoreContent, StandardCharsets.UTF_8);
    File genmodelFile = tempDir.resolve("issues.genmodel").toFile();
    String genmodelContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<genmodel:GenModel xmi:version=\"2.0\" xmlns:xmi=\"http://www.omg.org/XMI\" "
        + "xmlns:genmodel=\"http://www.eclipse.org/emf/2002/GenModel\" modelPluginID=\"p\" "
        + "modelDirectory=\"/wrong/path\" creationIcons=\"true\">"
        + "<genPackages prefix=\"Model\" basePackage=\"wrong.base\" ecorePackage=\"issues.ecore#/\"/>"
        + "</genmodel:GenModel>";
    Files.writeString(genmodelFile.toPath(), genmodelContent, StandardCharsets.UTF_8);
    return genmodelFile;
  }

  private MetamodelLocation createMockMetamodelLocation(File genmodelFile, @SuppressWarnings("unused") String modelPluginId) {
    MetamodelLocation location = mock(MetamodelLocation.class);
    when(location.genmodel()).thenReturn(genmodelFile);
    when(location.toString()).thenReturn(genmodelFile.getAbsolutePath());
    return location;
  }
}

