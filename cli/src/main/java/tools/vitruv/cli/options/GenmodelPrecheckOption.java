package tools.vitruv.cli.options;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import tools.vitruv.cli.configuration.MetamodelLocation;
import tools.vitruv.cli.configuration.VitruvConfiguration;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/**
 * CLI option that prechecks provided {@code .genmodel} files for compatibility with the MWE2
 * generator workflow.
 *
 * <p>Rules (from Arne):
 *
 * <ul>
 *   <li>{@code compliance} must be deleted (commonly stored as {@code complianceLevel})
 *   <li>{@code basePackage} must be equal to {@code modelPluginID}
 *   <li>{@code modelDirectory} must be {@code /<modelPluginID>/target/generated-sources/ecore}
 * </ul>
 *
 * <p>If any rule is violated, this option fails fast with a detailed error message.
 */
public class GenmodelPrecheckOption extends VitruvCLIOption {

    private static final String OPT = "pg";

    /**
     * Creates the {@code --precheck-genmodel} option.
     */
    public GenmodelPrecheckOption() {
        super(
                OPT,
                "precheck-genmodel",
                false,
                "Precheck .genmodel files for MWE2 build compatibility (fails fast on issues).");
        this.setArgs(0);
        this.setOptionalArg(true);
    }

    /**
     * Performs the precheck during the prepare phase.
     *
     * @param cmd the parsed command line
     * @param configuration the current CLI configuration
     * @throws IllegalArgumentException if precheck fails
     */
    @Override
    public void prepare(CommandLine cmd, VitruvConfiguration configuration) {
        if (!cmd.hasOption(OPT)) return;

        List<MetamodelLocation> locations = configuration.getMetaModelLocations();
        if (locations == null || locations.isEmpty()) {
            throw new IllegalArgumentException(
                    "No metamodels configured. Provide -m/--metamodel before running --precheck-genmodel.");
        }

        List<String> errors = new ArrayList<>();

        for (MetamodelLocation loc : locations) {
            File genmodelFile = loc.genmodel();
            if (genmodelFile == null) {
                errors.add("Metamodel has no genmodel file reference: " + loc);
                continue;
            }
            if (!genmodelFile.exists()) {
                errors.add("Genmodel file does not exist: " + genmodelFile.getAbsolutePath());
                continue;
            }

            try {
                GenModel gm = loadGenModel(genmodelFile.toPath());

                String modelPluginId = safeTrim(gm.getModelPluginID());
                if (modelPluginId.isEmpty()) {
                    errors.add(genmodelFile.getName() + ": modelPluginID is missing/blank.");
                    continue;
                }

                validateBasePackageEqualsModelPluginId(genmodelFile, gm, modelPluginId, errors);
                validateModelDirectory(genmodelFile, gm, modelPluginId, errors);
                validateComplianceRemoved(genmodelFile, errors);

            } catch (IOException e) {
                errors.add(
                        "Failed to read genmodel '" + genmodelFile.getAbsolutePath() + "': " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Genmodel precheck failed:\n- " + String.join("\n- ", errors));
        }
    }

    /**
     * No-op in this option.
     *
     * @param cmd the parsed command line
     * @param builder the virtual model builder
     * @param configuration the current CLI configuration
     * @return the unchanged builder
     */
    @Override
    public VirtualModelBuilder preBuild(
            CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
        return builder;
    }

    /**
     * No-op in this option.
     *
     * @param cmd the parsed command line
     * @param builder the virtual model builder
     * @param configuration the current CLI configuration
     * @return the unchanged builder
     */
    @Override
    public VirtualModelBuilder applyInternal(
            CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
        return builder;
    }

    /**
     * No-op in this option.
     *
     * @param cmd the parsed command line
     * @param builder the virtual model builder
     * @param configuration the current CLI configuration
     * @return the unchanged builder
     */
    @Override
    public VirtualModelBuilder postBuild(
            CommandLine cmd, VirtualModelBuilder builder, VitruvConfiguration configuration) {
        return builder;
    }

    /**
     * Loads a {@link GenModel} from the given path using EMF resources.
     *
     * @param genmodelPath path to the {@code .genmodel} file
     * @return loaded {@link GenModel}
     * @throws IOException if the file cannot be loaded or is not a valid genmodel
     */
    private GenModel loadGenModel(Path genmodelPath) throws IOException {
        ResourceSet rs = new ResourceSetImpl();
        rs.getPackageRegistry().put(GenModelPackage.eNS_URI, GenModelPackage.eINSTANCE);

        rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put("genmodel", new org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl());
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put("xmi", new org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl());
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put("*", new org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl());

        try {
            URI uri = URI.createFileURI(genmodelPath.toAbsolutePath().toString());
            Resource r = rs.getResource(uri, true);
            r.load(null);

            if (r.getContents().isEmpty() || !(r.getContents().get(0) instanceof GenModel gm)) {
                throw new IOException("File is not a valid EMF GenModel: " + genmodelPath);
            }
            return gm;
        } catch (RuntimeException e) {
            throw new IOException("Failed to load genmodel: " + genmodelPath, e);
        }
    }

    /**
     * Validates that every {@link GenPackage} has a {@code basePackage} equal to {@code modelPluginID}.
     *
     * @param genmodelFile the genmodel file being checked
     * @param gm the loaded {@link GenModel}
     * @param modelPluginId the required model plugin id
     * @param errors error accumulator
     */
    private void validateBasePackageEqualsModelPluginId(
            File genmodelFile, GenModel gm, String modelPluginId, List<String> errors) {
        List<GenPackage> genPackages = gm.getGenPackages();
        if (genPackages == null || genPackages.isEmpty()) {
            errors.add(genmodelFile.getName() + ": no genPackages found in genmodel.");
            return;
        }

        for (GenPackage gp : genPackages) {
            String basePackage = safeTrim(gp.getBasePackage());
            String gpName = safeTrim(gp.getPackageName());
            String gpLabel = gpName.isEmpty() ? "<unnamed GenPackage>" : gpName;

            if (basePackage.isEmpty()) {
                errors.add(
                        genmodelFile.getName()
                                + ": genPackage "
                                + gpLabel
                                + " basePackage is missing (must equal modelPluginID="
                                + modelPluginId
                                + ").");
            } else if (!basePackage.equals(modelPluginId)) {
                errors.add(
                        genmodelFile.getName()
                                + ": genPackage "
                                + gpLabel
                                + " basePackage must equal modelPluginID. Found basePackage="
                                + basePackage
                                + ", modelPluginID="
                                + modelPluginId
                                + ".");
            }
        }
    }

    /**
     * Validates that {@code modelDirectory} equals {@code /<modelPluginID>/target/generated-sources/ecore}.
     *
     * @param genmodelFile the genmodel file being checked
     * @param gm the loaded {@link GenModel}
     * @param modelPluginId the required model plugin id
     * @param errors error accumulator
     */
    private void validateModelDirectory(
            File genmodelFile, GenModel gm, String modelPluginId, List<String> errors) {
        String expectedModelDirectory = normalizeDir("/" + modelPluginId + "/target/generated-sources/ecore");
        String modelDirectory = normalizeDir(safeTrim(gm.getModelDirectory()));

        if (modelDirectory.isEmpty()) {
            errors.add(
                    genmodelFile.getName()
                            + ": modelDirectory is missing (expected "
                            + expectedModelDirectory
                            + ").");
        } else if (!modelDirectory.equals(expectedModelDirectory)) {
            errors.add(
                    genmodelFile.getName()
                            + ": modelDirectory must be '"
                            + expectedModelDirectory
                            + "'. Found '"
                            + safeTrim(gm.getModelDirectory())
                            + "'.");
        }
    }

    /**
     * Validates that {@code complianceLevel} is not set on the genmodel, if the EMF version exposes it.
     *
     * @param genmodelFile the genmodel file being checked
     * @param gm the loaded {@link GenModel}
     * @param errors error accumulator
     */
    private void validateComplianceRemoved(File genmodelFile, List<String> errors) {
        try {
            String xml = Files.readString(genmodelFile.toPath());
            if (xml.contains("complianceLevel=") || xml.contains("compliance=")) {
                errors.add(
                        genmodelFile.getName()
                                + ": complianceLevel must be removed (attribute still present in file).");
            }
        } catch (IOException e) {
            errors.add(
                    genmodelFile.getName()
                            + ": Could not read file for complianceLevel check: "
                            + e.getMessage());
        }
    }

    /**
     * Trims a possibly null string.
     *
     * @param s the input string
     * @return trimmed string or empty string if null
     */
    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Normalizes directory strings to forward slashes and collapses repeated slashes.
     *
     * @param s the input path string
     * @return normalized path string
     */
    private String normalizeDir(String s) {
        return s.replace("\\", "/").replaceAll("/+", "/").trim();
    }
}