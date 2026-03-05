package tools.vitruv.cli.options;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.Set;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

/** Validates GenModel files for MWE2 workflow compatibility. */
public final class GenmodelPrecheck {

  /** Represents a validation issue found in a GenModel file. */
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

  /**
   * Validates and corrects a GenModel file for MWE2 compatibility.
   *
   * @param genmodelFile the GenModel file to process
   * @return a list of issues found and corrected
   */
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

    List<Issue> issues = new ArrayList<>();

    if (!originalXml.equals(strippedXml)) {
      try {
        Files.writeString(genmodelFile.toPath(), strippedXml, StandardCharsets.UTF_8);
        issues.add(new Issue(genmodelFile, "Removed attributes: " + String.join(", ", attrsToRemove)));
      } catch (IOException e) {
        throw new IllegalArgumentException(
                "Could not write genmodel file: " + genmodelFile.getAbsolutePath(), e);
      }
    }

    // Now it’s safe to load via EMF because unknown attrs are already gone
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

    String modelPluginId = safeTrim(genModel.getModelPluginID());
    if (modelPluginId.isEmpty()) {
      issues.add(new Issue(genmodelFile, "modelPluginID is missing/blank."));
      return issues;
    }

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

  /**
   * Removes specified XML attributes from the given XML string using StAX parser.
   *
   * @param xml the XML content
   * @param attributeLocalNamesToRemove set of attribute names to remove
   * @return XML string with specified attributes removed
   */
  public String stripAttributesWithStax(String xml, Set<String> attributeLocalNamesToRemove) {
    try {
      XMLInputFactory inFactory = XMLInputFactory.newFactory();
      inFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
      inFactory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
      inFactory.setXMLResolver(
          (publicID, systemID, baseURI, namespace) -> {
            throw new XMLStreamException("External entity resolution disabled");
          });
      inFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);

      XMLOutputFactory outFactory = XMLOutputFactory.newFactory();
      XMLEventFactory eventFactory = XMLEventFactory.newFactory();
      inFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
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

  /**
   * Ensures creationIcons is set to false in the GenModel.
   *
   * @param genmodelFile the GenModel file
   * @param genModel the GenModel to modify
   * @param issues the issue list to collect changes
   */
  public void enforceCreationIcons(File genmodelFile, GenModel genModel, List<Issue> issues) {
    if (genModel.isCreationIcons()) {
      genModel.setCreationIcons(false);
      issues.add(new Issue(genmodelFile, "Set creationIcons=false."));
    }
  }

  /**
   * Ensures foreignModel entry exists, adding default if missing.
   *
   * @param genmodelFile the GenModel file
   * @param genModel the GenModel to modify
   * @param issues the issue list to collect changes
   */
  public void enforceForeignModel(File genmodelFile, GenModel genModel, List<Issue> issues) {
    List<String> foreignModels = genModel.getForeignModel();

    if (foreignModels == null || foreignModels.isEmpty()) {
      String defaultModel = genmodelFile.getName().replace(".genmodel", ".ecore");
      genModel.getForeignModel().add(defaultModel);

      issues.add(new Issue(genmodelFile, "Added missing foreignModel entry: " + defaultModel));
    }
  }

  /**
   * Ensures basePackage equals modelPluginId for all GenPackages.
   *
   * @param genmodelFile the GenModel file
   * @param genModel the GenModel to modify
   * @param modelPluginId the expected plugin ID
   * @param issues the issue list to collect changes
   */
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
   * Ensures modelDirectory follows the required pattern.
   *
   * @param genmodelFile the GenModel file
   * @param genModel the GenModel to modify
   * @param modelPluginId the plugin ID for computing the expected directory
   * @param issues the issue list to collect changes
   */
  public void enforceModelDirectory(
      File genmodelFile, GenModel genModel, String modelPluginId, List<Issue> issues) {

    String expected = normalize("/" + modelPluginId + "/target/generated-sources/ecore");
    String beforeRaw = genModel.getModelDirectory();
    String before = normalize(safeTrim(beforeRaw));

    if (before.isEmpty()) {
      genModel.setModelDirectory(expected);
      issues.add(new Issue(genmodelFile, "Set modelDirectory to '" + expected + "'."));
    } else if (!before.equals(expected)) {
      genModel.setModelDirectory(expected);
      issues.add(
          new Issue(
              genmodelFile,
              "Changed modelDirectory from '" + beforeRaw + "' to '" + expected + "'."));
    }
  }

  /**
   * Safely trims a string, treating null as empty string.
   *
   * @param s the string to trim
   * @return the trimmed string or empty string if null
   */
  public String safeTrim(String s) {
    return s == null ? "" : s.trim();
  }

  /**
   * Normalizes path separators and collapses multiple slashes.
   *
   * @param s the path string
   * @return the normalized path
   */
  public String normalize(String s) {
    return s.replace("\\", "/").replaceAll("/+", "/").trim();
  }
}
