package tools.vitruv.cli.options;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelFactory;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenmodelPrecheckTest {

    @TempDir Path tempDir;

    private File writeRaw(String name, String content) throws IOException {
        Path p = tempDir.resolve(name);
        Files.writeString(p, content, StandardCharsets.UTF_8);
        return p.toFile();
    }

    private File writeEcore(String name) throws IOException {
        String ecore =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<ecore:EPackage xmi:version=\"2.0\"\n"
                        + "  xmlns:xmi=\"http://www.omg.org/XMI\"\n"
                        + "  xmlns:ecore=\"http://www.eclipse.org/emf/2002/Ecore\"\n"
                        + "  name=\"model\" nsURI=\"http://example/model\" nsPrefix=\"model\"/>\n";
        return writeRaw(name, ecore);
    }

    private ResourceSet newResourceSetWithFactories() {
        ResourceSet rs = new ResourceSetImpl();
        rs.getPackageRegistry().put(GenModelPackage.eNS_URI, GenModelPackage.eINSTANCE);
        rs.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);

        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("genmodel", new XMIResourceFactoryImpl());
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new XMIResourceFactoryImpl());
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
        return rs;
    }

    private EPackage loadEPackage(File ecoreFile) throws IOException {
        ResourceSet rs = newResourceSetWithFactories();
        URI uri = URI.createFileURI(ecoreFile.getAbsolutePath());
        Resource r = rs.getResource(uri, true);
        try {
            r.load(null);
        } catch (RuntimeException e) {
            throw new IOException("Failed to load ecore: " + ecoreFile.getAbsolutePath(), e);
        }
        if (r.getContents().isEmpty() || !(r.getContents().get(0) instanceof EPackage ep)) {
            throw new IOException("Not an EPackage: " + ecoreFile.getAbsolutePath());
        }
        return ep;
    }

    private File writeGenmodel(
            String fileName,
            String modelPluginId,
            String modelDirectory,
            List<String> foreignModels,
            List<GenPackage> genPackages)
            throws IOException {

        ResourceSet rs = newResourceSetWithFactories();

        GenModel gm = GenModelFactory.eINSTANCE.createGenModel();
        if (modelPluginId != null) {
            gm.setModelPluginID(modelPluginId);
        }
        if (modelDirectory != null) {
            gm.setModelDirectory(modelDirectory);
        }
        if (foreignModels != null) {
            gm.getForeignModel().addAll(foreignModels);
        }
        if (genPackages != null) {
            gm.getGenPackages().addAll(genPackages);
        }

        Path out = tempDir.resolve(fileName);
        URI outUri = URI.createFileURI(out.toFile().getAbsolutePath());
        Resource r = rs.createResource(outUri);
        r.getContents().add(gm);
        r.save(null);

        return out.toFile();
    }

    private GenPackage newGenPackage(String basePackage, EPackage ePackageOrNull) {
        GenPackage gp = GenModelFactory.eINSTANCE.createGenPackage();
        if (basePackage != null) {
            gp.setBasePackage(basePackage);
        }
        gp.setPrefix("Model");
        gp.setEcorePackage(ePackageOrNull);
        return gp;
    }

    private boolean hasGetter(String method) {
        try {
            Class.forName("org.eclipse.emf.codegen.ecore.genmodel.GenModel").getMethod(method);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private void injectComplianceLevelAttribute(File genmodelFile, String value) throws IOException {
        String xml = Files.readString(genmodelFile.toPath(), StandardCharsets.UTF_8);
        int idx = xml.indexOf("<genmodel:GenModel");
        if (idx < 0) {
            idx = xml.indexOf("<GenModel");
        }
        if (idx < 0) {
            throw new IOException("Could not find GenModel root element to inject complianceLevel");
        }

        int endOfStartTag = xml.indexOf(">", idx);
        if (endOfStartTag < 0) {
            throw new IOException("Invalid XML (no closing > for root start tag)");
        }

        String before = xml.substring(0, endOfStartTag);
        String after = xml.substring(endOfStartTag);

        if (before.contains("complianceLevel=")) {
            return;
        }

        String injected = before + " complianceLevel=\"" + value + "\"" + after;
        Files.writeString(genmodelFile.toPath(), injected, StandardCharsets.UTF_8);
    }

    @Test
    void check_nullFile_throwsIllegalArgumentException() {
        GenmodelPrecheck precheck = new GenmodelPrecheck();
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> precheck.check(null));
        assertEquals("genmodelFile must not be null", ex.getMessage());
    }

    @Test
    void check_notAGenModel_throwsIOException() throws Exception {
        File f = writeRaw("not-genmodel.xml", "<x/>");
        GenmodelPrecheck precheck = new GenmodelPrecheck();
        IOException ex = assertThrows(IOException.class, () -> precheck.check(f));
        assertTrue(ex.getMessage().contains("Failed to load genmodel") || ex.getMessage().contains("Not a GenModel"));
    }

    @Test
    void check_missingModelPluginId_returnsSingleIssueAndStops() throws Exception {
        File ecore = writeEcore("model.ecore");
        EPackage ep = loadEPackage(ecore);

        File genmodel =
                writeGenmodel(
                        "missing-plugin.genmodel",
                        "   ",
                        "/x/target/generated-sources/ecore",
                        List.of(ecore.getName()),
                        List.of(newGenPackage("x", ep)));

        injectComplianceLevelAttribute(genmodel, "1.4");

        GenmodelPrecheck precheck = new GenmodelPrecheck();
        List<GenmodelPrecheck.Issue> issues = precheck.check(genmodel);

        assertEquals(1, issues.size(), issues.toString());
        assertTrue(issues.get(0).message.contains("modelPluginID is missing/blank."));
    }

    @Test
    void check_complianceLevelPresent_reportsIssue() throws Exception {
        File ecore = writeEcore("model.ecore");
        EPackage ep = loadEPackage(ecore);

        File genmodel =
                writeGenmodel(
                        "compliance.genmodel",
                        "x",
                        "/x/target/generated-sources/ecore",
                        List.of(ecore.getName()),
                        List.of(newGenPackage("x", ep)));

        injectComplianceLevelAttribute(genmodel, "1.4");

        GenmodelPrecheck precheck = new GenmodelPrecheck();
        List<GenmodelPrecheck.Issue> issues = precheck.check(genmodel);

        assertTrue(
                issues.stream().anyMatch(i -> i.message.contains("Remove complianceLevel/compliance")));
    }

    @Test
    void check_noForeignModel_reportsWarning() throws Exception {
        File ecore = writeEcore("model.ecore");
        EPackage ep = loadEPackage(ecore);

        File genmodel =
                writeGenmodel(
                        "no-foreign.genmodel",
                        "x",
                        "/x/target/generated-sources/ecore",
                        List.of(),
                        List.of(newGenPackage("x", ep)));

        GenmodelPrecheck precheck = new GenmodelPrecheck();
        List<GenmodelPrecheck.Issue> issues = precheck.check(genmodel);

        assertTrue(
                issues.stream().anyMatch(i -> i.message.contains("WARNING: No foreignModel entries found")));
    }

    @Test
    void check_blankForeignModel_reportsIssue() throws Exception {
        File ecore = writeEcore("model.ecore");
        EPackage ep = loadEPackage(ecore);

        File genmodel =
                writeGenmodel(
                        "blank-foreign.genmodel",
                        "x",
                        "/x/target/generated-sources/ecore",
                        List.of("   "),
                        List.of(newGenPackage("x", ep)));

        GenmodelPrecheck precheck = new GenmodelPrecheck();
        List<GenmodelPrecheck.Issue> issues = precheck.check(genmodel);

        assertTrue(issues.stream().anyMatch(i -> i.message.contains("foreignModel entry is blank")));
    }

    @Test
    void check_missingForeignModelFile_reportsIssue() throws Exception {
        File ecore = writeEcore("model.ecore");
        EPackage ep = loadEPackage(ecore);

        File genmodel =
                writeGenmodel(
                        "missing-foreign-file.genmodel",
                        "x",
                        "/x/target/generated-sources/ecore",
                        List.of("missing.ecore"),
                        List.of(newGenPackage("x", ep)));

        GenmodelPrecheck precheck = new GenmodelPrecheck();
        List<GenmodelPrecheck.Issue> issues = precheck.check(genmodel);

        assertTrue(issues.stream().anyMatch(i -> i.message.contains("foreignModel does not exist")));
    }

    @Test
    void check_existingForeignModelFile_doesNotReportMissingForeignModel() throws Exception {
        File ecore = writeEcore("model.ecore");
        EPackage ep = loadEPackage(ecore);

        File genmodel =
                writeGenmodel(
                        "foreign-exists.genmodel",
                        "x",
                        "/x/target/generated-sources/ecore",
                        List.of(ecore.getName()),
                        List.of(newGenPackage("x", ep)));

        GenmodelPrecheck precheck = new GenmodelPrecheck();
        List<GenmodelPrecheck.Issue> issues = precheck.check(genmodel);

        assertTrue(issues.stream().noneMatch(i -> i.message.contains("foreignModel does not exist")));
    }

    @Test
    void check_noGenPackages_reportsIssue() throws Exception {
        File genmodel =
                writeGenmodel(
                        "no-genpackages.genmodel",
                        "x",
                        "/x/target/generated-sources/ecore",
                        List.of(),
                        List.of());

        GenmodelPrecheck precheck = new GenmodelPrecheck();
        List<GenmodelPrecheck.Issue> issues = precheck.check(genmodel);

        assertTrue(issues.stream().anyMatch(i -> i.message.contains("No genPackages found in genmodel")));
    }

    @Test
    void check_genPackageMissingBasePackage_reportsIssue() throws Exception {
        File ecore = writeEcore("model.ecore");
        EPackage ep = loadEPackage(ecore);

        File genmodel =
                writeGenmodel(
                        "missing-basepackage.genmodel",
                        "x",
                        "/x/target/generated-sources/ecore",
                        List.of(ecore.getName()),
                        List.of(newGenPackage("   ", ep)));

        GenmodelPrecheck precheck = new GenmodelPrecheck();
        List<GenmodelPrecheck.Issue> issues = precheck.check(genmodel);

        assertTrue(issues.stream().anyMatch(i -> i.message.contains("basePackage is missing")));
    }

    @Test
    void check_genPackageBasePackageMismatch_reportsIssue() throws Exception {
        File ecore = writeEcore("model.ecore");
        EPackage ep = loadEPackage(ecore);

        File genmodel =
                writeGenmodel(
                        "basepackage-mismatch.genmodel",
                        "x",
                        "/x/target/generated-sources/ecore",
                        List.of(ecore.getName()),
                        List.of(newGenPackage("y", ep)));

        GenmodelPrecheck precheck = new GenmodelPrecheck();
        List<GenmodelPrecheck.Issue> issues = precheck.check(genmodel);

        assertTrue(issues.stream().anyMatch(i -> i.message.contains("basePackage must equal modelPluginID")));
    }

    @Test
    void check_modelDirectoryMissing_reportsIssue() throws Exception {
        File ecore = writeEcore("model.ecore");
        EPackage ep = loadEPackage(ecore);

        File genmodel =
                writeGenmodel(
                        "missing-modeldir.genmodel",
                        "x",
                        "   ",
                        List.of(ecore.getName()),
                        List.of(newGenPackage("x", ep)));

        GenmodelPrecheck precheck = new GenmodelPrecheck();
        List<GenmodelPrecheck.Issue> issues = precheck.check(genmodel);

        assertTrue(issues.stream().anyMatch(i -> i.message.contains("modelDirectory is missing")));
    }

    @Test
    void check_modelDirectoryMismatch_reportsIssue() throws Exception {
        File ecore = writeEcore("model.ecore");
        EPackage ep = loadEPackage(ecore);

        File genmodel =
                writeGenmodel(
                        "modeldir-mismatch.genmodel",
                        "x",
                        "/wrong/target/generated-sources/ecore",
                        List.of(ecore.getName()),
                        List.of(newGenPackage("x", ep)));

        GenmodelPrecheck precheck = new GenmodelPrecheck();
        List<GenmodelPrecheck.Issue> issues = precheck.check(genmodel);

        assertTrue(
                issues.stream().anyMatch(i -> i.message.contains("modelDirectory must be '/x/target/generated-sources/ecore'")),
                issues.toString());
    }

    @Test
    void check_modelDirectoryNormalization_allowsDuplicateSlashes() throws Exception {
        File ecore = writeEcore("model.ecore");
        EPackage ep = loadEPackage(ecore);

        File genmodel =
                writeGenmodel(
                        "modeldir-normalization.genmodel",
                        "x",
                        "///x//target///generated-sources//ecore",
                        List.of(ecore.getName()),
                        List.of(newGenPackage("x", ep)));

        GenmodelPrecheck precheck = new GenmodelPrecheck();
        List<GenmodelPrecheck.Issue> issues = precheck.check(genmodel);

        assertTrue(issues.stream().noneMatch(i -> i.message.startsWith("modelDirectory must be")));
        assertTrue(issues.stream().noneMatch(i -> i.message.contains("modelDirectory is missing")));
    }

    @Test
    void check_ecorePackageUnresolved_reportsIssue() throws Exception {
        File ecore = writeEcore("model.ecore");
        loadEPackage(ecore);

        GenPackage gp = newGenPackage("x", null);

        File genmodel =
                writeGenmodel(
                        "ecore-unresolved.genmodel",
                        "x",
                        "/x/target/generated-sources/ecore",
                        List.of(ecore.getName()),
                        List.of(gp));

        GenmodelPrecheck precheck = new GenmodelPrecheck();
        List<GenmodelPrecheck.Issue> issues = precheck.check(genmodel);

        assertTrue(issues.stream().anyMatch(i -> i.message.contains("has no resolved ecorePackage")), issues.toString());
    }

    @Test
    void check_validGenmodel_hasNoErrors_allowsWarnings() throws Exception {
        File ecore = writeEcore("model.ecore");
        EPackage ep = loadEPackage(ecore);

        String plugin = "tools.vitruv.methodologisttemplate.model";

        File genmodel =
                writeGenmodel(
                        "valid.genmodel",
                        plugin,
                        "/" + plugin + "/target/generated-sources/ecore",
                        List.of(ecore.getName()),
                        List.of(newGenPackage(plugin, ep)));

        GenmodelPrecheck precheck = new GenmodelPrecheck();
        List<GenmodelPrecheck.Issue> issues = precheck.check(genmodel);

        List<GenmodelPrecheck.Issue> errors =
                issues.stream()
                        .filter(i -> !i.message.startsWith("WARNING:"))
                        .toList();

        assertEquals(0, errors.size(), issues.toString());
    }

    @Test
    void check_warnsOnEditEditorTestsFields_whenGettersExist_andValuesAreSet() throws Exception {
        boolean any =
                hasGetter("getEditDirectory")
                        || hasGetter("getEditorDirectory")
                        || hasGetter("getTestsDirectory")
                        || hasGetter("getEditPluginID")
                        || hasGetter("getEditorPluginID")
                        || hasGetter("getTestsPluginID");

        Assumptions.assumeTrue(any);

        File ecore = writeEcore("model.ecore");
        EPackage ep = loadEPackage(ecore);

        ResourceSet rs = newResourceSetWithFactories();
        GenModel gm = GenModelFactory.eINSTANCE.createGenModel();
        gm.setModelPluginID("x");
        gm.setModelDirectory("/x/target/generated-sources/ecore");
        gm.getForeignModel().add(ecore.getName());
        gm.getGenPackages().add(newGenPackage("x", ep));

        setIfExists(gm, "setEditDirectory", String.class, "/x/edit");
        setIfExists(gm, "setEditorDirectory", String.class, "/x/editor");
        setIfExists(gm, "setTestsDirectory", String.class, "/x/tests");
        setIfExists(gm, "setEditPluginID", String.class, "x.edit");
        setIfExists(gm, "setEditorPluginID", String.class, "x.editor");
        setIfExists(gm, "setTestsPluginID", String.class, "x.tests");

        File genmodelFile = tempDir.resolve("warn-edit-editor-tests.genmodel").toFile();
        URI outUri = URI.createFileURI(genmodelFile.getAbsolutePath());
        Resource r = rs.createResource(outUri);
        r.getContents().add(gm);
        r.save(null);

        GenmodelPrecheck precheck = new GenmodelPrecheck();
        List<GenmodelPrecheck.Issue> issues = precheck.check(genmodelFile);

        assertTrue(
                issues.stream().anyMatch(i -> i.message.contains("WARNING:")),
                issues.toString());
    }

    private void setIfExists(Object target, String method, Class<?> argType, Object value) {
        try {
            target.getClass().getMethod(method, argType).invoke(target, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}