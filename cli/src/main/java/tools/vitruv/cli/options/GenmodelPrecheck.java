package tools.vitruv.cli.options;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

/**
 * Validates GenModel files for compatibility with MWE2 generator workflows.
 * <p>
 * This checker ensures that GenModel files meet specific requirements:
 * <ul>
 *   <li>The {@code complianceLevel} attribute must be removed</li>
 *   <li>The {@code basePackage} must equal {@code modelPluginID}</li>
 *   <li>The {@code modelDirectory} must follow the pattern {@code /<modelPluginID>/target/generated-sources/ecore}</li>
 *   <li>All {@code foreignModel} entries must reference existing files</li>
 *   <li>All GenPackages must have resolved {@code ecorePackage} references</li>
 * </ul>
 */
public final class GenmodelPrecheck {

    /**
     * Represents a validation issue found in a GenModel file.
     * <p>
     * Issues can be errors or warnings. Warnings are prefixed with "WARNING:" in the message.
     */
    public static final class Issue {
        public final File file;
        public final String message;

        public Issue(File file, String message) {
            this.file = file;
            this.message = message;
        }

        @Override
        public String toString() {
            return file + ": " + message;
        }
    }

    private static final Pattern COMPLIANCE_ATTR = Pattern.compile("\\bcompliance(Level)?\\s*=");
    private static final Pattern EDIT_DIRECTORY_ATTR = Pattern.compile("\\beditDirectory\\s*=");
    private static final Pattern EDITOR_DIRECTORY_ATTR = Pattern.compile("\\beditorDirectory\\s*=");
    private static final Pattern TESTS_DIRECTORY_ATTR = Pattern.compile("\\btestsDirectory\\s*=");
    private static final Pattern EDIT_PLUGIN_ID_ATTR = Pattern.compile("\\beditPluginID\\s*=");
    private static final Pattern EDITOR_PLUGIN_ID_ATTR = Pattern.compile("\\beditorPluginID\\s*=");
    private static final Pattern TESTS_PLUGIN_ID_ATTR = Pattern.compile("\\btestsPluginID\\s*=");

    /**
     * Validates a GenModel file against MWE2 workflow requirements.
     * <p>
     * Validation stops early if {@code modelPluginID} is missing or blank, as other checks depend on it.
     *
     * @param genmodelFile the GenModel file to validate
     * @return a list of validation issues (empty if valid)
     * @throws IOException if the file cannot be read or loaded
     * @throws IllegalArgumentException if {@code genmodelFile} is null
     */
    public List<Issue> check(File genmodelFile) throws IOException {
        if (genmodelFile == null) {
            throw new IllegalArgumentException("genmodelFile must not be null");
        }

        String xml = Files.readString(genmodelFile.toPath(), StandardCharsets.UTF_8);
        GenModel genModel = load(genmodelFile);

        List<Issue> issues = new ArrayList<>();

        String modelPluginId = safeTrim(genModel.getModelPluginID());
        if (modelPluginId.isEmpty()) {
            issues.add(new Issue(genmodelFile, "modelPluginID is missing/blank."));
            return issues;
        }

        validateComplianceRemoved(genmodelFile, xml, issues);
        validateForeignModelsExist(genmodelFile, genModel, issues);
        validateBasePackageEqualsModelPluginId(genmodelFile, genModel, modelPluginId, issues);
        validateModelDirectory(genmodelFile, genModel, modelPluginId, issues);
        validateEcorePackageResolves(genmodelFile, genModel, issues);
        warnIfEditEditorTestsGenerationConfigured(genmodelFile, genModel, xml, issues);

        return issues;
    }

    /**
     * Loads a GenModel from the specified file.
     *
     * @param genmodelFile the file to load
     * @return the loaded GenModel
     * @throws IOException if loading fails or the file is not a valid GenModel
     */
    private GenModel load(File genmodelFile) throws IOException {
        ResourceSet rs = new ResourceSetImpl();
        rs.getPackageRegistry().put(GenModelPackage.eNS_URI, GenModelPackage.eINSTANCE);

        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("genmodel", new XMIResourceFactoryImpl());
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());

        try {
            URI uri = URI.createFileURI(genmodelFile.getAbsolutePath());
            Resource r = rs.getResource(uri, true);
            r.load(null);

            Object root = r.getContents().isEmpty() ? null : r.getContents().get(0);
            if (!(root instanceof GenModel gm)) {
                throw new IOException("Not a GenModel: " + genmodelFile.getAbsolutePath());
            }
            return gm;
        } catch (RuntimeException e) {
            throw new IOException("Failed to load genmodel: " + genmodelFile.getAbsolutePath(), e);
        }
    }

    /**
     * Checks that the {@code complianceLevel} attribute has been removed from the XML.
     */
    private void validateComplianceRemoved(File genmodelFile, String xml, List<Issue> issues) {
        if (COMPLIANCE_ATTR.matcher(xml).find()) {
            issues.add(new Issue(genmodelFile, "Remove complianceLevel/compliance (must be deleted)."));
        }
    }

    /**
     * Validates that all {@code foreignModel} entries reference existing files.
     * Issues a warning if no foreign models are specified.
     */
    private void validateForeignModelsExist(File genmodelFile, GenModel genModel, List<Issue> issues) {
        List<String> foreignModels = genModel.getForeignModel();
        if (foreignModels == null || foreignModels.isEmpty()) {
            issues.add(new Issue(genmodelFile, "WARNING: No foreignModel entries found (often expected)."));
            return;
        }

        File baseDir = genmodelFile.getParentFile();
        for (String fm : foreignModels) {
            String name = safeTrim(fm);
            if (name.isEmpty()) {
                issues.add(new Issue(genmodelFile, "foreignModel entry is blank."));
                continue;
            }

            File candidate = new File(baseDir, name);
            if (!candidate.exists()) {
                issues.add(new Issue(genmodelFile, "foreignModel does not exist: " + candidate.getAbsolutePath()));
            }
        }
    }

    /**
     * Validates that each GenPackage's {@code basePackage} equals the {@code modelPluginID}.
     */
    private void validateBasePackageEqualsModelPluginId(
            File genmodelFile, GenModel genModel, String modelPluginId, List<Issue> issues) {

        List<GenPackage> genPackages = genModel.getGenPackages();
        if (genPackages == null || genPackages.isEmpty()) {
            issues.add(new Issue(genmodelFile, "No genPackages found in genmodel."));
            return;
        }

        for (GenPackage gp : genPackages) {
            String basePackage = safeTrim(gp.getBasePackage());
            String gpName = safeTrim(gp.getPackageName());
            String label = gpName.isEmpty() ? "<unnamed GenPackage>" : gpName;

            if (basePackage.isEmpty()) {
                issues.add(new Issue(genmodelFile,
                        "basePackage is missing for genPackage " + label + " (must equal modelPluginID)."));
            } else if (!basePackage.equals(modelPluginId)) {
                issues.add(new Issue(genmodelFile,
                        "basePackage must equal modelPluginID for genPackage "
                                + label
                                + ". Found basePackage="
                                + basePackage
                                + ", modelPluginID="
                                + modelPluginId
                                + "."));
            }
        }
    }

    /**
     * Validates that {@code modelDirectory} follows the expected pattern:
     * {@code /<modelPluginID>/target/generated-sources/ecore}.
     * <p>
     * Path separators are normalized before comparison.
     */
    private void validateModelDirectory(
            File genmodelFile, GenModel genModel, String modelPluginId, List<Issue> issues) {

        String expectedModelDirectory = normalize("/" + modelPluginId + "/target/generated-sources/ecore");
        String modelDirectory = normalize(safeTrim(genModel.getModelDirectory()));

        if (modelDirectory.isEmpty()) {
            issues.add(new Issue(genmodelFile, "modelDirectory is missing."));
        } else if (!modelDirectory.equals(expectedModelDirectory)) {
            issues.add(new Issue(genmodelFile,
                    "modelDirectory must be '"
                            + expectedModelDirectory
                            + "'. Found: '"
                            + safeTrim(genModel.getModelDirectory())
                            + "'."));
        }
    }

    /**
     * Validates that all GenPackages have resolved {@code ecorePackage} references.
     */
    private void validateEcorePackageResolves(File genmodelFile, GenModel genModel, List<Issue> issues) {
        List<GenPackage> genPackages = genModel.getGenPackages();
        if (genPackages == null || genPackages.isEmpty()) {
            return;
        }

        for (GenPackage gp : genPackages) {
            EPackage ep = gp.getEcorePackage();
            String gpName = safeTrim(gp.getPackageName());
            String label = gpName.isEmpty() ? "<unnamed GenPackage>" : gpName;

            if (ep == null) {
                issues.add(new Issue(genmodelFile,
                        "GenPackage " + label + " has no resolved ecorePackage (check ecorePackage=\"...\" and foreignModel)."));
            }
        }
    }

    /**
     * Issues warnings if edit/editor/tests generation is configured.
     * These settings are typically unwanted in CLI/MWE2 templates.
     */
    private void warnIfEditEditorTestsGenerationConfigured(
            File genmodelFile, GenModel genModel, String xml, List<Issue> issues) {

        if (EDIT_DIRECTORY_ATTR.matcher(xml).find()) {
            warnIfNonBlank(genmodelFile, issues, "editDirectory", reflectString(genModel, "getEditDirectory"));
        }
        if (EDITOR_DIRECTORY_ATTR.matcher(xml).find()) {
            warnIfNonBlank(genmodelFile, issues, "editorDirectory", reflectString(genModel, "getEditorDirectory"));
        }
        if (TESTS_DIRECTORY_ATTR.matcher(xml).find()) {
            warnIfNonBlank(genmodelFile, issues, "testsDirectory", reflectString(genModel, "getTestsDirectory"));
        }
        if (EDIT_PLUGIN_ID_ATTR.matcher(xml).find()) {
            warnIfNonBlank(genmodelFile, issues, "editPluginID", reflectString(genModel, "getEditPluginID"));
        }
        if (EDITOR_PLUGIN_ID_ATTR.matcher(xml).find()) {
            warnIfNonBlank(genmodelFile, issues, "editorPluginID", reflectString(genModel, "getEditorPluginID"));
        }
        if (TESTS_PLUGIN_ID_ATTR.matcher(xml).find()) {
            warnIfNonBlank(genmodelFile, issues, "testsPluginID", reflectString(genModel, "getTestsPluginID"));
        }
    }

    private void warnIfNonBlank(File file, List<Issue> issues, String field, String value) {
        String v = safeTrim(value);
        if (!v.isEmpty() && !"null".equalsIgnoreCase(v)) {
            issues.add(new Issue(file,
                    "WARNING: " + field + " is set to '" + v + "' (usually unwanted in CLI/MWE2 templates)."));
        }
    }

    /**
     * Invokes a getter method via reflection, returning empty string if not found.
     */
    private String reflectString(Object target, String methodName) {
        try {
            Object v = target.getClass().getMethod(methodName).invoke(target);
            return v == null ? "" : String.valueOf(v);
        } catch (ReflectiveOperationException e) {
            return "";
        }
    }

    /**
     * Safely trims a string, treating null as empty string.
     */
    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Normalizes path separators and collapses multiple slashes.
     */
    private String normalize(String s) {
        return s.replace("\\", "/").replaceAll("/+", "/").trim();
    }
}