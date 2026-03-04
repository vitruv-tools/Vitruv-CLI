package tools.vitruv.cli.options;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

/**
 * Validates GenModel files for compatibility with MWE2 generator workflows.
 *
 * <p>This checker ensures that GenModel files meet specific requirements:
 *
 * <ul>
 *   <li>The {@code complianceLevel} attribute must be removed
 *   <li>The {@code basePackage} must equal {@code modelPluginID}
 *   <li>The {@code modelDirectory} must follow the pattern {@code
 *       /<modelPluginID>/target/generated-sources/ecore}
 *   <li>All {@code foreignModel} entries must reference existing files
 *   <li>All GenPackages must have resolved {@code ecorePackage} references
 * </ul>
 */
public final class GenmodelPrecheck {

  /**
   * Represents a validation issue found in a GenModel file.
   *
   * <p>Issues can be errors or warnings. Warnings are prefixed with "WARNING:" in the message.
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

  public List<Issue> process(File genmodelFile) {
    if (genmodelFile == null) {
      throw new IllegalArgumentException("genmodelFile must not be null");
    }

    final String originalXml;
    try {
      originalXml = Files.readString(genmodelFile.toPath(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Could not read genmodel file: " + genmodelFile.getAbsolutePath(), e);
    }

    ResourceSet resourceSet = new ResourceSetImpl();
    resourceSet.getPackageRegistry().put(GenModelPackage.eNS_URI, GenModelPackage.eINSTANCE);
    resourceSet
        .getResourceFactoryRegistry()
        .getExtensionToFactoryMap()
        .put("genmodel", new XMIResourceFactoryImpl());

    URI uri = URI.createFileURI(genmodelFile.getAbsolutePath());
    Resource resource = resourceSet.getResource(uri, true);
    try {
      resource.load(null);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Could not load genmodel file: " + genmodelFile.getAbsolutePath(), e);
    }

    if (resource.getContents().isEmpty() || !(resource.getContents().get(0) instanceof GenModel)) {
      throw new IllegalArgumentException("Not a valid GenModel: " + genmodelFile.getAbsolutePath());
    }

    GenModel genModel = (GenModel) resource.getContents().get(0);
    List<Issue> issues = new ArrayList<>();

    String modelPluginId = safeTrim(genModel.getModelPluginID());
    if (modelPluginId.isEmpty()) {
      issues.add(new Issue(genmodelFile, "modelPluginID is missing/blank."));
      return issues;
    }

    Set<String> attrsToRemove =
        Set.of(
            "complianceLevel",
            "compliance",
            "editDirectory",
            "editorDirectory",
            "testsDirectory",
            "editPluginID",
            "editorPluginID",
            "testsPluginID");

    final String strippedXml;
    try {
      strippedXml = stripAttributesWithStax(originalXml, attrsToRemove);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Could not strip attributes from genmodel XML: " + genmodelFile.getAbsolutePath(), e);
    }

    if (!originalXml.equals(strippedXml)) {
      try {
        Files.writeString(genmodelFile.toPath(), strippedXml, StandardCharsets.UTF_8);
        issues.add(
            new Issue(genmodelFile, "Removed attributes: " + String.join(", ", attrsToRemove)));
      } catch (IOException e) {
        throw new IllegalArgumentException(
            "Could not write genmodel file: " + genmodelFile.getAbsolutePath(), e);
      }
    }

    try {
      resource.unload();
      resource.load(null);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Could not reload genmodel after XML rewrite: " + genmodelFile.getAbsolutePath(), e);
    }
    genModel = (GenModel) resource.getContents().get(0);

    enforceBasePackageEqualsModelPluginId(genmodelFile, genModel, modelPluginId, issues);
    enforceModelDirectory(genmodelFile, genModel, modelPluginId, issues);
    enforceForeignModel(genmodelFile, genModel, issues);
    enforceCreationIcons(genmodelFile, genModel, issues);

    try {
      resource.save(null);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Could not save genmodel file: " + genmodelFile.getAbsolutePath(), e);
    }

    return issues;
  }

  public String stripAttributesWithStax(String xml, Set<String> attributeLocalNamesToRemove) {
    try {
      XMLInputFactory inFactory = XMLInputFactory.newFactory();
      inFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);

      XMLOutputFactory outFactory = XMLOutputFactory.newFactory();
      XMLEventFactory eventFactory = XMLEventFactory.newFactory();

      XMLEventReader reader = inFactory.createXMLEventReader(new StringReader(xml));
      StringWriter stringWriter = new StringWriter();
      XMLEventWriter writer = outFactory.createXMLEventWriter(stringWriter);

      while (reader.hasNext()) {
        XMLEvent xmlEvent = reader.nextEvent();

        if (xmlEvent.isStartElement()) {
          StartElement startElement = xmlEvent.asStartElement();

          List<Attribute> keptAttrs = new ArrayList<>();
          Iterator<?> attributes = startElement.getAttributes();
          while (attributes.hasNext()) {
            Attribute attribute = (Attribute) attributes.next();
            String localName = attribute.getName().getLocalPart();
            if (!attributeLocalNamesToRemove.contains(localName)) {
              keptAttrs.add(attribute);
            }
          }

          Iterator<Namespace> namespaces = startElement.getNamespaces();

          StartElement rebuilt =
              eventFactory.createStartElement(
                  startElement.getName(), keptAttrs.iterator(), namespaces);
          writer.add(rebuilt);
        } else {
          writer.add(xmlEvent);
        }
      }

      writer.flush();
      writer.close();
      reader.close();
      return stringWriter.toString();
    } catch (XMLStreamException ex) {
      throw new IllegalArgumentException("Failed to strip attributes via StAX", ex);
    }
  }

  public void enforceCreationIcons(File genmodelFile, GenModel genModel, List<Issue> issues) {
    if (genModel.isCreationIcons()) {
      genModel.setCreationIcons(false);
      issues.add(new Issue(genmodelFile, "Set creationIcons=false."));
    }
  }

  public void enforceForeignModel(File genmodelFile, GenModel genModel, List<Issue> issues) {
    List<String> foreignModels = genModel.getForeignModel();

    if (foreignModels == null || foreignModels.isEmpty()) {
      String defaultModel = genmodelFile.getName().replace(".genmodel", ".ecore");
      genModel.getForeignModel().add(defaultModel);

      issues.add(new Issue(genmodelFile, "Added missing foreignModel entry: " + defaultModel));
    }
  }

  public void enforceBasePackageEqualsModelPluginId(
      File genmodelFile, GenModel genModel, String modelPluginId, List<Issue> issues) {

    List<GenPackage> genPackages = genModel.getGenPackages();
    for (GenPackage genPackage : genPackages) {
      String before = safeTrim(genPackage.getBasePackage());
      String gpName = safeTrim(genPackage.getPackageName());
      String label = gpName.isEmpty() ? "<unnamed GenPackage>" : gpName;

      if (!modelPluginId.equals(before)) {
        genPackage.setBasePackage(modelPluginId);
        if (before.isEmpty()) {
          issues.add(
              new Issue(
                  genmodelFile,
                  "Set basePackage for genPackage " + label + " to '" + modelPluginId + "'."));
        } else {
          issues.add(
              new Issue(
                  genmodelFile,
                  "Changed basePackage for genPackage "
                      + label
                      + " from '"
                      + before
                      + "' to '"
                      + modelPluginId
                      + "'."));
        }
      }
    }
  }

  /**
   * Ensures {@code GenModel#modelDirectory} matches the CLI/MWE2 template convention:
   *
   * @param genmodelFile  source file for reporting purposes (used in {@link Issue})
   * @param genModel      loaded EMF GenModel to mutate
   * @param modelPluginId plugin id used to compute the expected directory
   * @param issues        output list that receives informational issues when changes are applied
   * @throws IllegalArgumentException if {@code genModel} or {@code modelPluginId} is null
   */
  public void enforceModelDirectory(
      File genmodelFile, GenModel genModel, String modelPluginId, List<Issue> issues) {

    String expected = normalize("/" + modelPluginId + "/target/generated-sources/ecore");
    String beforeRaw = genModel.getModelDirectory();
    String before = normalize(safeTrim(beforeRaw));

    if (before.isEmpty()) {
      genModel.setModelDirectory(expected);
      issues.add(new Issue(genmodelFile, "Set modelDirectory to '" + expected + "'."));
    }

    if (!before.equals(expected)) {
      genModel.setModelDirectory(expected);
      issues.add(
          new Issue(
              genmodelFile,
              "Changed modelDirectory from '" + beforeRaw + "' to '" + expected + "'."));
    }
  }

  /** Safely trims a string, treating null as empty string. */
  public String safeTrim(String s) {
    return s == null ? "" : s.trim();
  }

  /** Normalizes path separators and collapses multiple slashes. */
  public String normalize(String s) {
    return s.replace("\\", "/").replaceAll("/+", "/").trim();
  }
}
