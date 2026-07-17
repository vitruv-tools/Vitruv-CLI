package tools.vitruv.cli.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.cli.configuration.MetamodelLocation;
import tools.vitruv.cli.configuration.VitruvConfiguration;

class GenmodelPrecheckOptionTest {

  @TempDir Path tempDir;

  private GenmodelPrecheckOption option;
  private InputStream originalIn;
  private PrintStream originalOut;
  private ByteArrayOutputStream capturedOut;

  @BeforeAll
  static void initEmf() {
    GenModelPackage.eINSTANCE.eClass();
  }

  @BeforeEach
  void setup() {
    option = new GenmodelPrecheckOption();
    originalIn = System.in;
    originalOut = System.out;
    capturedOut = new ByteArrayOutputStream();
    System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
  }

  @AfterEach
  void restoreStreams() {
    System.setIn(originalIn);
    System.setOut(originalOut);
  }

  private CommandLine parse(String... args) throws Exception {
    Options options = new Options();
    options.addOption(option);
    return new DefaultParser().parse(options, args);
  }

  private String writeEcore(String fileName) throws Exception {
    Path ecorePath = tempDir.resolve(fileName);
    String ecore =
        """
            <?xml version="1.0" encoding="UTF-8"?>
            <ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
              xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="model"
              nsURI="http://example/model" nsPrefix="model">
            </ecore:EPackage>
            """;
    Files.writeString(ecorePath, ecore, StandardCharsets.UTF_8);
    return ecorePath.toString();
  }

  private String writeGenmodel(String fileName, String modelDirectory) throws Exception {
    Path genmodelPath = tempDir.resolve(fileName);
    String genmodel =
        """
            <?xml version="1.0" encoding="UTF-8"?>
            <genmodel:GenModel xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
              xmlns:genmodel="http://www.eclipse.org/emf/2002/GenModel"
              xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
              modelPluginID="p" modelDirectory="%s" creationIcons="false">
              <foreignModel>model.ecore</foreignModel>
              <genPackages prefix="Model" basePackage="p" ecorePackage="model.ecore#/"/>
            </genmodel:GenModel>
            """
            .formatted(modelDirectory);
    Files.writeString(genmodelPath, genmodel, StandardCharsets.UTF_8);
    return genmodelPath.toString();
  }

  private VitruvConfiguration configurationWith(String ecorePath, String genmodelPath) {
    VitruvConfiguration configuration = new VitruvConfiguration();
    configuration.setLocalPath(tempDir);
    configuration.addMetamodelLocations(
        new MetamodelLocation(
            new java.io.File(ecorePath), new java.io.File(genmodelPath), "", ""));
    return configuration;
  }

  @Test
  void prepare_cleanGenmodel_printsCleanStatus() throws Exception {
    String ecorePath = writeEcore("model.ecore");
    String genmodelPath = writeGenmodel("model.genmodel", "/p/target/generated-sources/ecore");
    VitruvConfiguration configuration = configurationWith(ecorePath, genmodelPath);

    CommandLine cmd = parse("-pg");
    option.prepare(cmd, configuration);

    assertTrue(capturedOut.toString(StandardCharsets.UTF_8).contains("GENMODEL_PRECHECK_STATUS: CLEAN"));
  }

  @Test
  void prepare_issuesFoundWithClosedStdin_doesNotThrow_printsIssuesFoundStatus() throws Exception {
    String ecorePath = writeEcore("model.ecore");
    String genmodelPath = writeGenmodel("model.genmodel", "/wrong/dir");
    VitruvConfiguration configuration = configurationWith(ecorePath, genmodelPath);

    System.setIn(new ByteArrayInputStream(new byte[0]));

    CommandLine cmd = parse("-pg");
    option.prepare(cmd, configuration);

    assertTrue(
        capturedOut.toString(StandardCharsets.UTF_8).contains("GENMODEL_PRECHECK_STATUS: ISSUES_FOUND"));
  }

  @Test
  void prepare_issuesFoundWithConfirmation_appliesFixes_printsFixesAppliedStatus() throws Exception {
    String ecorePath = writeEcore("model.ecore");
    String genmodelPath = writeGenmodel("model.genmodel", "/wrong/dir");
    VitruvConfiguration configuration = configurationWith(ecorePath, genmodelPath);

    System.setIn(new ByteArrayInputStream("y\n".getBytes(StandardCharsets.UTF_8)));

    CommandLine cmd = parse("-pg");
    option.prepare(cmd, configuration);

    assertTrue(
        capturedOut.toString(StandardCharsets.UTF_8).contains("GENMODEL_PRECHECK_STATUS: FIXES_APPLIED"));
    String updatedGenmodel = Files.readString(Path.of(genmodelPath), StandardCharsets.UTF_8);
    assertTrue(updatedGenmodel.contains("/p/target/generated-sources/ecore"));
  }

  @Test
  void askForFixConfirmation_noStdinAvailable_returnsFalse() {
    System.setIn(new ByteArrayInputStream(new byte[0]));
    assertFalse(option.askForFixConfirmation());
  }

  @Test
  void askForFixConfirmation_yesInput_returnsTrue() {
    System.setIn(new ByteArrayInputStream("y\n".getBytes(StandardCharsets.UTF_8)));
    assertTrue(option.askForFixConfirmation());
  }

  @Test
  void askForFixConfirmation_noInput_returnsFalse() {
    System.setIn(new ByteArrayInputStream("n\n".getBytes(StandardCharsets.UTF_8)));
    assertFalse(option.askForFixConfirmation());
  }

  @Test
  void prepare_optionNotPresent_doesNothing() throws Exception {
    VitruvConfiguration configuration = new VitruvConfiguration();
    CommandLine cmd = parse();
    option.prepare(cmd, configuration);
    assertEquals("", capturedOut.toString(StandardCharsets.UTF_8));
  }
}
