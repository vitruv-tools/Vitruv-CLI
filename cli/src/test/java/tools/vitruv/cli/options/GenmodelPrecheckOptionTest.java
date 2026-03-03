package tools.vitruv.cli.options;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;

import tools.vitruv.cli.configuration.MetamodelLocation;
import tools.vitruv.cli.configuration.VitruvConfiguration;

class GenmodelPrecheckOptionTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void registerEmfFactories() {
        GenModelPackage.eINSTANCE.eClass();
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("genmodel", new XMIResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new XMIResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
    }

    @Test
    void prepare_whenOptionNotPresent_doesNothing() throws Exception {
        VitruvConfiguration cfg = new VitruvConfiguration();
        cfg.addMetamodelLocations(new MetamodelLocation(new File("a.ecore"), new File("a.genmodel"), "uri", "/x"));

        CommandLine cmd = parse(new String[] {});
        assertDoesNotThrow(() -> new GenmodelPrecheckOption().prepare(cmd, cfg));
    }

    @Test
    void prepare_withPgButNoMetamodels_throws() throws Exception {
        VitruvConfiguration cfg = new VitruvConfiguration();
        CommandLine cmd = parse(new String[] {"-pg"});

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> new GenmodelPrecheckOption().prepare(cmd, cfg));
        assertTrue(ex.getMessage().contains("No metamodels configured"));
    }

    @Test
    void prepare_whenGenmodelFileMissing_reportsError() throws Exception {
        VitruvConfiguration cfg = new VitruvConfiguration();
        File missing = tempDir.resolve("missing.genmodel").toFile();
        cfg.addMetamodelLocations(new MetamodelLocation(tempDir.resolve("m.ecore").toFile(), missing, "uri", "/x"));

        CommandLine cmd = parse(new String[] {"-pg"});

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> new GenmodelPrecheckOption().prepare(cmd, cfg));
        assertTrue(ex.getMessage().contains("Genmodel file does not exist"));
    }

    @Test
    void prepare_missingModelPluginId_reportsError() throws Exception {
        File gm = writeGenmodel("missing-plugin.genmodel", genmodelXml(null, "/x/target/generated-sources/ecore", "x"));

        VitruvConfiguration cfg = new VitruvConfiguration();
        cfg.addMetamodelLocations(new MetamodelLocation(tempDir.resolve("m.ecore").toFile(), gm, "uri", "/x"));

        CommandLine cmd = parse(new String[] {"-pg"});

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> new GenmodelPrecheckOption().prepare(cmd, cfg));
        assertTrue(ex.getMessage().contains("modelPluginID is missing/blank"));
    }

    @Test
    void prepare_noGenPackages_reportsError() throws Exception {
        File gm = writeGenmodel("no-genpackages.genmodel", genmodelXml("x", "/x/target/generated-sources/ecore", null));

        VitruvConfiguration cfg = new VitruvConfiguration();
        cfg.addMetamodelLocations(new MetamodelLocation(tempDir.resolve("m.ecore").toFile(), gm, "uri", "/x"));

        CommandLine cmd = parse(new String[] {"-pg"});

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> new GenmodelPrecheckOption().prepare(cmd, cfg));
        assertTrue(ex.getMessage().toLowerCase().contains("no genpackages found"));
    }

    @Test
    void prepare_basePackageMissing_reportsError() throws Exception {
        writeEcore();

        String xml = genmodelXmlWithGenPackages(
                "x",
                "/x/target/generated-sources/ecore",
                genPackagesXmlWithoutBasePackage());

        File gm = writeGenmodel("base-missing.genmodel", xml);

        VitruvConfiguration cfg = new VitruvConfiguration();
        cfg.addMetamodelLocations(new MetamodelLocation(tempDir.resolve("m.ecore").toFile(), gm, "uri", "/x"));

        CommandLine cmd = parse(new String[] {"-pg"});

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> new GenmodelPrecheckOption().prepare(cmd, cfg));

        assertTrue(ex.getMessage().contains("basePackage is missing"), ex.getMessage());
    }

    @Test
    void prepare_modelDirectoryMissing_reportsError() throws Exception {
        File gm = writeGenmodel("modeldir-missing.genmodel", genmodelXml("x", null, "x"));

        VitruvConfiguration cfg = new VitruvConfiguration();
        cfg.addMetamodelLocations(new MetamodelLocation(tempDir.resolve("m.ecore").toFile(), gm, "uri", "/x"));

        CommandLine cmd = parse(new String[] {"-pg"});

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> new GenmodelPrecheckOption().prepare(cmd, cfg));
        assertTrue(ex.getMessage().contains("modelDirectory is missing"));
    }

    @Test
    void prepare_modelDirectoryMismatch_reportsError() throws Exception {
        File gm = writeGenmodel("modeldir-mismatch.genmodel",
                genmodelXml("x", "/wrong/target/generated-sources/ecore", "x"));

        VitruvConfiguration cfg = new VitruvConfiguration();
        cfg.addMetamodelLocations(new MetamodelLocation(tempDir.resolve("m.ecore").toFile(), gm, "uri", "/x"));

        CommandLine cmd = parse(new String[] {"-pg"});

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> new GenmodelPrecheckOption().prepare(cmd, cfg));
        assertTrue(ex.getMessage().contains("modelDirectory must be"));
    }

    @Test
    void prepare_basePackageMismatch_reportsError() throws Exception {
        writeEcore();

        String xml = genmodelXmlWithGenPackages(
                "x",
                "/x/target/generated-sources/ecore",
                genPackagesXmlWithBasePackage("y"));

        File gm = writeGenmodel("base-mismatch.genmodel", xml);

        VitruvConfiguration cfg = new VitruvConfiguration();
        cfg.addMetamodelLocations(new MetamodelLocation(tempDir.resolve("m.ecore").toFile(), gm, "uri", "/x"));

        CommandLine cmd = parse(new String[] {"-pg"});

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> new GenmodelPrecheckOption().prepare(cmd, cfg));

        assertTrue(ex.getMessage().contains("basePackage must equal modelPluginID"), ex.getMessage());
    }

    @Test
    void prepare_compliancePresent_reportsError_whenGetterExists() throws Exception {
        Assumptions.assumeTrue(hasComplianceGetter());

        writeEcore();

        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<genmodel:GenModel xmi:version=\"2.0\"\n"
                        + "  xmlns:xmi=\"http://www.omg.org/XMI\"\n"
                        + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                        + "  xmlns:ecore=\"http://www.eclipse.org/emf/2002/Ecore\"\n"
                        + "  xmlns:genmodel=\"http://www.eclipse.org/emf/2002/GenModel\""
                        + " modelDirectory=\"/x/target/generated-sources/ecore\""
                        + " modelPluginID=\"x\""
                        + " complianceLevel=\"1.4\""
                        + ">\n"
                        + "  <foreignModel>model.ecore</foreignModel>\n"
                        + genPackagesXmlWithBasePackage("x")
                        + "</genmodel:GenModel>\n";

        File gm = writeGenmodel("compliance.genmodel", xml);

        VitruvConfiguration cfg = new VitruvConfiguration();
        cfg.addMetamodelLocations(new MetamodelLocation(tempDir.resolve("m.ecore").toFile(), gm, "uri", "/x"));

        CommandLine cmd = parse(new String[] {"-pg"});

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> new GenmodelPrecheckOption().prepare(cmd, cfg));

        assertTrue(ex.getMessage().contains("complianceLevel"), ex.getMessage());
    }

    private CommandLine parse(String[] args) throws Exception {
        Options opts = new Options();
        opts.addOption(new GenmodelPrecheckOption());
        return new DefaultParser().parse(opts, args);
    }

    private File writeGenmodel(String name, String content) throws Exception {
        Path p = tempDir.resolve(name);
        Files.writeString(p, content, StandardCharsets.UTF_8);
        return p.toFile();
    }

    private boolean hasComplianceGetter() {
        try {
            GenModel.class.getMethod("getComplianceLevel");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean complianceGetterReturnTypeIsPrimitive() {
        try {
            return GenModel.class.getMethod("getComplianceLevel").getReturnType().isPrimitive();
        } catch (Throwable t) {
            return false;
        }
    }

    private String genmodelXml(String modelPluginId, String modelDirectory, String basePackage) {
        String md = modelDirectory == null ? "" : (" modelDirectory=\"" + modelDirectory + "\"");
        String mp = modelPluginId == null ? "" : (" modelPluginID=\"" + modelPluginId + "\"");

        String genPackages =
                basePackage == null
                        ? ""
                        : genPackagesXmlWithBasePackage(basePackage);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<genmodel:GenModel xmi:version=\"2.0\"\n"
                + "  xmlns:xmi=\"http://www.omg.org/XMI\"\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "  xmlns:ecore=\"http://www.eclipse.org/emf/2002/Ecore\"\n"
                + "  xmlns:genmodel=\"http://www.eclipse.org/emf/2002/GenModel\""
                + md
                + mp
                + ">\n"
                + genPackages
                + "</genmodel:GenModel>\n";
    }

    private String genPackagesXmlWithBasePackage(String basePackage) {
        return "  <genPackages prefix=\"Model\""
                + " basePackage=\"" + basePackage + "\""
                + " ecorePackage=\"model.ecore#/\"/>\n";
    }

    private String genPackagesXmlWithoutBasePackage() {
        return "  <genPackages prefix=\"Model\""
                + " ecorePackage=\"model.ecore#/\"/>\n";
    }

    private void write(String content) throws Exception {
        Path p = tempDir.resolve("model.ecore");
        Files.writeString(p, content, StandardCharsets.UTF_8);
    }

    private void writeEcore() throws Exception {
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
        write(ecore);
    }

    private String genmodelXmlWithGenPackages(String modelPluginId, String modelDirectory, String genPackagesXml) {
        String md = modelDirectory == null ? "" : (" modelDirectory=\"" + modelDirectory + "\"");
        String mp = modelPluginId == null ? "" : (" modelPluginID=\"" + modelPluginId + "\"");

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<genmodel:GenModel xmi:version=\"2.0\"\n"
                + "  xmlns:xmi=\"http://www.omg.org/XMI\"\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "  xmlns:ecore=\"http://www.eclipse.org/emf/2002/Ecore\"\n"
                + "  xmlns:genmodel=\"http://www.eclipse.org/emf/2002/GenModel\""
                + md
                + mp
                + ">\n"
                + "  <foreignModel>model.ecore</foreignModel>\n"
                + (genPackagesXml == null ? "" : genPackagesXml)
                + "</genmodel:GenModel>\n";
    }
}