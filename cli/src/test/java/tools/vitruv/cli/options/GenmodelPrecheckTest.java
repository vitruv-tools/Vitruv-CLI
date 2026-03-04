package tools.vitruv.cli.options;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelFactory;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenmodelPrecheckTest {

  @TempDir Path tempDir;

  @BeforeAll
  static void initEmf() {
    GenModelPackage.eINSTANCE.eClass();
  }

  @Test
  void safeTrim_null_returnsEmpty() {
    GenmodelPrecheck svc = new GenmodelPrecheck();
    assertEquals("", svc.safeTrim(null));
  }

  @Test
  void safeTrim_trimsWhitespace() {
    GenmodelPrecheck svc = new GenmodelPrecheck();
    assertEquals("x", svc.safeTrim("  x \n"));
  }

  @Test
  void normalize_convertsBackslashes_collapsesSlashes_trims() {
    GenmodelPrecheck svc = new GenmodelPrecheck();
    assertEquals("/a/b/c", svc.normalize("  \\\\a\\\\b////c  "));
  }

  @Test
  void stripAttributesWithStax_removesOnlySpecifiedAttributes() {
    GenmodelPrecheck svc = new GenmodelPrecheck();

    String xml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <root a="1" b="2" c="3"></root>
            """;

    String out = svc.stripAttributesWithStax(xml, Set.of("b", "c"));

    assertTrue(out.contains("a=\"1\""), out);
    assertFalse(out.contains("b=\"2\""), out);
    assertFalse(out.contains("c=\"3\""), out);
  }

  @Test
  void stripAttributesWithStax_keepsNamespacesAndOtherAttributes() {
    GenmodelPrecheck svc = new GenmodelPrecheck();

    String xml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <genmodel:GenModel xmi:version="2.0"
              xmlns:xmi="http://www.omg.org/XMI"
              xmlns:genmodel="http://www.eclipse.org/emf/2002/GenModel"
              xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
              complianceLevel="1.4">
              <child foo="bar"/>
            </genmodel:GenModel>
            """;

    String out = svc.stripAttributesWithStax(xml, Set.of("complianceLevel"));

    assertTrue(out.contains("xmlns:genmodel="), out);
    assertTrue(out.contains("xmlns:xmi="), out);
    assertTrue(out.contains("xmi:version=\"2.0\""), out);
    assertTrue(out.contains("foo=\"bar\""), out);
    assertFalse(out.contains("complianceLevel="), out);
  }


  @Test
  void enforceCreationIcons_whenTrue_setsFalse_andAddsIssue() {
    GenmodelPrecheck svc = new GenmodelPrecheck();
    GenModel gm = GenModelFactory.eINSTANCE.createGenModel();
    gm.setCreationIcons(true);

    List<GenmodelPrecheck.Issue> issues = new ArrayList<>();
    svc.enforceCreationIcons(new File("x.genmodel"), gm, issues);

    assertFalse(gm.isCreationIcons());
    assertEquals(1, issues.size());
    assertTrue(issues.get(0).message.contains("creationIcons=false"));
  }

  @Test
  void enforceCreationIcons_whenAlreadyFalse_noIssue() {
    GenmodelPrecheck svc = new GenmodelPrecheck();
    GenModel gm = GenModelFactory.eINSTANCE.createGenModel();
    gm.setCreationIcons(false);

    List<GenmodelPrecheck.Issue> issues = new ArrayList<>();
    svc.enforceCreationIcons(new File("x.genmodel"), gm, issues);

    assertFalse(gm.isCreationIcons());
    assertEquals(0, issues.size());
  }

  @Test
  void enforceForeignModel_whenMissing_addsDefaultEntry_andIssue() {
    GenmodelPrecheck svc = new GenmodelPrecheck();
    GenModel gm = GenModelFactory.eINSTANCE.createGenModel();
    gm.getForeignModel().clear();

    File f = new File("my.genmodel");
    List<GenmodelPrecheck.Issue> issues = new ArrayList<>();

    svc.enforceForeignModel(f, gm, issues);

    assertEquals(List.of("my.ecore"), gm.getForeignModel());
    assertEquals(1, issues.size());
    assertTrue(issues.get(0).message.contains("Added missing foreignModel"));
    assertTrue(issues.get(0).message.contains("my.ecore"));
  }

  @Test
  void enforceForeignModel_whenAlreadyPresent_noChange_noIssue() {
    GenmodelPrecheck svc = new GenmodelPrecheck();
    GenModel gm = GenModelFactory.eINSTANCE.createGenModel();
    gm.getForeignModel().clear();
    gm.getForeignModel().add("model.ecore");

    List<GenmodelPrecheck.Issue> issues = new ArrayList<>();
    svc.enforceForeignModel(new File("x.genmodel"), gm, issues);

    assertEquals(List.of("model.ecore"), gm.getForeignModel());
    assertEquals(0, issues.size());
  }


  @Test
  void enforceBasePackage_whenEmpty_sets_andAddsIssue() {
    GenmodelPrecheck svc = new GenmodelPrecheck();

    GenModel gm = GenModelFactory.eINSTANCE.createGenModel();
    GenPackage gp = GenModelFactory.eINSTANCE.createGenPackage();
    gp.setBasePackage("");
    gm.getGenPackages().add(gp);

    List<GenmodelPrecheck.Issue> issues = new ArrayList<>();
    svc.enforceBasePackageEqualsModelPluginId(new File("x.genmodel"), gm, "my.plugin", issues);

    assertEquals("my.plugin", gp.getBasePackage());
    assertEquals(1, issues.size());
    assertTrue(issues.get(0).message.contains("Set basePackage"));
  }

  @Test
  void enforceBasePackage_whenMismatch_overwrites_andAddsIssue() {
    GenmodelPrecheck svc = new GenmodelPrecheck();

    GenModel gm = GenModelFactory.eINSTANCE.createGenModel();
    GenPackage gp = GenModelFactory.eINSTANCE.createGenPackage();
    gp.setBasePackage("wrong");
    gm.getGenPackages().add(gp);

    List<GenmodelPrecheck.Issue> issues = new ArrayList<>();
    svc.enforceBasePackageEqualsModelPluginId(new File("x.genmodel"), gm, "my.plugin", issues);

    assertEquals("my.plugin", gp.getBasePackage());
    assertEquals(1, issues.size());
    assertTrue(issues.get(0).message.contains("Changed basePackage"));
  }

  @Test
  void enforceBasePackage_whenAlreadyCorrect_noIssue() {
    GenmodelPrecheck svc = new GenmodelPrecheck();

    GenModel gm = GenModelFactory.eINSTANCE.createGenModel();
    GenPackage gp = GenModelFactory.eINSTANCE.createGenPackage();
    gp.setBasePackage("my.plugin");
    gm.getGenPackages().add(gp);

    List<GenmodelPrecheck.Issue> issues = new ArrayList<>();
    svc.enforceBasePackageEqualsModelPluginId(new File("x.genmodel"), gm, "my.plugin", issues);

    assertEquals("my.plugin", gp.getBasePackage());
    assertEquals(0, issues.size());
  }

  @Test
  void enforceModelDirectory_whenAlreadyCorrect_noIssue() {
    GenmodelPrecheck svc = new GenmodelPrecheck();

    GenModel gm = GenModelFactory.eINSTANCE.createGenModel();
    gm.setModelDirectory("/p/target/generated-sources/ecore");

    List<GenmodelPrecheck.Issue> issues = new ArrayList<>();
    svc.enforceModelDirectory(new File("x.genmodel"), gm, "p", issues);

    assertEquals("/p/target/generated-sources/ecore", svc.normalize(gm.getModelDirectory()));
    assertEquals(0, issues.size());
  }

  @Test
  void enforceModelDirectory_whenMismatch_overwrites_andAddsOneIssue() {
    GenmodelPrecheck svc = new GenmodelPrecheck();

    GenModel gm = GenModelFactory.eINSTANCE.createGenModel();
    gm.setModelDirectory("/wrong/dir");

    List<GenmodelPrecheck.Issue> issues = new ArrayList<>();
    svc.enforceModelDirectory(new File("x.genmodel"), gm, "p", issues);

    assertEquals("/p/target/generated-sources/ecore", svc.normalize(gm.getModelDirectory()));
    assertEquals(1, issues.size());
    assertTrue(issues.get(0).message.contains("Changed modelDirectory"));
  }

  @Test
  void enforceModelDirectory_whenBlank_setsExpected_andAddsTwoIssues_currentBehavior() {
    // Your method currently has two independent if statements, so blank triggers both.
    GenmodelPrecheck svc = new GenmodelPrecheck();

    GenModel gm = GenModelFactory.eINSTANCE.createGenModel();
    gm.setModelDirectory("   ");

    List<GenmodelPrecheck.Issue> issues = new ArrayList<>();
    svc.enforceModelDirectory(new File("x.genmodel"), gm, "p", issues);

    assertEquals("/p/target/generated-sources/ecore", svc.normalize(gm.getModelDirectory()));
    assertEquals(2, issues.size(), "Blank currently produces 2 issues due to double-if.");
    assertTrue(issues.get(0).message.contains("Set modelDirectory"));
    assertTrue(issues.get(1).message.contains("Changed modelDirectory"));
  }

  @Test
  void process_withAllScenarios_stripsAttrs_andPersistsEmfChanges() throws Exception {
    writeEcore(tempDir.resolve("model.ecore"));

    File genmodelFile = tempDir.resolve("all.genmodel").toFile();

    String xml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <genmodel:GenModel xmi:version="2.0"
              xmlns:xmi="http://www.omg.org/XMI"
              xmlns:genmodel="http://www.eclipse.org/emf/2002/GenModel"
              xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
              modelPluginID="p"
              modelDirectory="/WRONG/dir"
              creationIcons="true"
              complianceLevel="1.4"
              editDirectory="/p.edit/src"
              editorDirectory="/p.editor/src"
              testsDirectory="/p.tests/src"
              editPluginID="p.edit"
              editorPluginID="p.editor"
              testsPluginID="p.tests">
              <genPackages prefix="Model" basePackage="WRONG.BASE" ecorePackage="model.ecore#/"/>
            </genmodel:GenModel>
            """;

    Files.writeString(genmodelFile.toPath(), xml, StandardCharsets.UTF_8);

    GenmodelPrecheck svc = new GenmodelPrecheck();
    List<GenmodelPrecheck.Issue> issues = svc.process(genmodelFile);

    String updatedXml = Files.readString(genmodelFile.toPath(), StandardCharsets.UTF_8);
    assertFalse(updatedXml.contains("complianceLevel="), updatedXml);
    assertFalse(updatedXml.contains("editDirectory="), updatedXml);
    assertFalse(updatedXml.contains("editorDirectory="), updatedXml);
    assertFalse(updatedXml.contains("testsDirectory="), updatedXml);
    assertFalse(updatedXml.contains("editPluginID="), updatedXml);
    assertFalse(updatedXml.contains("editorPluginID="), updatedXml);
    assertFalse(updatedXml.contains("testsPluginID="), updatedXml);

    GenModel reloaded = loadGenModel(genmodelFile.toPath());

    assertFalse(reloaded.isCreationIcons());
    assertTrue(reloaded.getForeignModel().contains("all.ecore"));
    assertEquals("p", reloaded.getGenPackages().get(0).getBasePackage());
    assertEquals("/p/target/generated-sources/ecore", svc.normalize(reloaded.getModelDirectory()));

    String dump = issues.toString();
    assertTrue(dump.contains("Removed attributes"), dump);
    assertTrue(dump.contains("basePackage"), dump);
    assertTrue(dump.contains("modelDirectory"), dump);
    assertTrue(dump.contains("foreignModel"), dump);
    assertTrue(dump.contains("creationIcons"), dump);
  }

  private static void writeEcore(Path p) throws Exception {
    String ecore =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <ecore:EPackage
              xmlns:xmi="http://www.omg.org/XMI"
              xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
              name="model"
              nsURI="http://example/model"
              nsPrefix="model">
            </ecore:EPackage>
            """;
    Files.writeString(p, ecore, StandardCharsets.UTF_8);
  }

  private static GenModel loadGenModel(Path genmodelPath) throws Exception {
    ResourceSet rs = new ResourceSetImpl();
    rs.getPackageRegistry().put(GenModelPackage.eNS_URI, GenModelPackage.eINSTANCE);
    rs.getResourceFactoryRegistry()
            .getExtensionToFactoryMap()
            .put("genmodel", new XMIResourceFactoryImpl());

    URI uri = URI.createFileURI(genmodelPath.toFile().getAbsolutePath());
    Resource r = rs.getResource(uri, true);
    r.load(null);
    return (GenModel) r.getContents().get(0);
  }
}