package tools.vitruv.cli.options;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

/** Validates and standardizes GenModel files for MWE2 workflow compatibility. */
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

  private static final Set<String> ATTRS_TO_REMOVE =
          Set.of(
                  "complianceLevel",
                  "compliance",
                  "editDirectory",
                  "editorDirectory",
                  "testsDirectory",
                  "editPluginID",
                  "editorPluginID",
                  "testsPluginID");

  /**
   * Inspects a GenModel file and reports the changes that would be applied without modifying it.
   *
   * @param genmodelFile the GenModel file to inspect
   * @return the list of detected issues and planned changes
   */
  public List<Issue> inspect(File genmodelFile) {
    return analyze(genmodelFile, false);
  }

  /**
   * Processes a GenModel file and applies the required changes.
   *
   * @param genmodelFile the GenModel file to process
   * @return the list of detected issues and applied changes
   */
  public List<Issue> process(File genmodelFile) {
    return analyze(genmodelFile, true);
  }

  /**
   * Analyzes a GenModel file and optionally applies changes.
   *
   * @param genmodelFile the GenModel file to analyze
   * @param applyChanges whether the detected changes should be written back to disk
   * @return the list of detected issues and applied or planned changes
   */
  public List<Issue> analyze(File genmodelFile, boolean applyChanges) {
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

    final String strippedXml;
    try {
      strippedXml = stripAttributesWithStax(originalXml, ATTRS_TO_REMOVE);
    } catch (Exception e) {
      throw new IllegalArgumentException(
              "Could not strip attributes from genmodel XML: " + genmodelFile.getAbsolutePath(), e);
    }

    List<Issue> issues = new ArrayList<>();

    if (!originalXml.equals(strippedXml)) {
      issues.add(
              new Issue(
                      genmodelFile,
                      (applyChanges ? "Removed attributes: " : "Would remove attributes: ")
                              + String.join(", ", ATTRS_TO_REMOVE)));

      if (applyChanges) {
        try {
          Files.writeString(genmodelFile.toPath(), strippedXml, StandardCharsets.UTF_8);
        } catch (IOException e) {
          throw new IllegalArgumentException(
                  "Could not write genmodel file: " + genmodelFile.getAbsolutePath(), e);
        }
      }
    }

    ResourceSet resourceSet = createResourceSet();
    URI uri = URI.createFileURI(genmodelFile.getAbsolutePath());
    Resource resource = loadResource(resourceSet, uri, applyChanges ? null : strippedXml, genmodelFile);

    if (resource.getContents().isEmpty() || !(resource.getContents().get(0) instanceof GenModel)) {
      throw new IllegalArgumentException("Not a valid GenModel: " + genmodelFile.getAbsolutePath());
    }

    GenModel genModel = (GenModel) resource.getContents().get(0);

    String modelPluginId = safeTrim(genModel.getModelPluginID());
    if (modelPluginId.isEmpty()) {
      throw new IllegalArgumentException(
              "GenModel has missing/blank modelPluginID: " + genmodelFile.getAbsolutePath());
    }

    enforceBasePackageEqualsModelPluginId(
            genmodelFile, genModel, modelPluginId, issues, applyChanges);
    enforceModelDirectory(genmodelFile, genModel, modelPluginId, issues, applyChanges);
    enforceForeignModel(genmodelFile, genModel, issues, applyChanges);
    enforceCreationIcons(genmodelFile, genModel, issues, applyChanges);

    if (applyChanges) {
      try {
        resource.save(null);
      } catch (IOException e) {
        throw new IllegalArgumentException(
                "Could not save genmodel file: " + genmodelFile.getAbsolutePath(), e);
      }
    }

    return issues;
  }

  /**
   * Removes the provided attributes from the XML using StAX.
   *
   * @param xml the XML source
   * @param attributeLocalNamesToRemove the local attribute names to remove
   * @return the XML without the specified attributes
   */
  public String stripAttributesWithStax(String xml, Set<String> attributeLocalNamesToRemove) {
    try {
      XMLInputFactory inFactory = XMLInputFactory.newFactory();
      inFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
      if (inFactory.isPropertySupported(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES)) {
        inFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
      }
      inFactory.setXMLResolver(
              (publicID, systemID, baseURI, namespace) -> {
                throw new XMLStreamException("External entity resolution disabled");
              });
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

          @SuppressWarnings("unchecked")
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
   * Ensures creationIcons is false.
   *
   * @param genmodelFile the source file
   * @param genModel the GenModel to inspect or mutate
   * @param issues the issue accumulator
   * @param applyChanges whether the change should be applied
   */
  public void enforceCreationIcons(
          File genmodelFile, GenModel genModel, List<Issue> issues, boolean applyChanges) {
    if (genModel.isCreationIcons()) {
      if (applyChanges) {
        genModel.setCreationIcons(false);
        issues.add(new Issue(genmodelFile, "Set creationIcons=false."));
      } else {
        issues.add(new Issue(genmodelFile, "Would set creationIcons=false."));
      }
    }
  }

  /**
   * Ensures a foreignModel entry exists.
   *
   * @param genmodelFile the source file
   * @param genModel the GenModel to inspect or mutate
   * @param issues the issue accumulator
   * @param applyChanges whether the change should be applied
   */
  public void enforceForeignModel(
          File genmodelFile, GenModel genModel, List<Issue> issues, boolean applyChanges) {
    List<String> foreignModels = genModel.getForeignModel();

    if (foreignModels == null || foreignModels.isEmpty()) {
      String defaultModel = genmodelFile.getName().replace(".genmodel", ".ecore");
      if (applyChanges) {
        genModel.getForeignModel().add(defaultModel);
        issues.add(new Issue(genmodelFile, "Added missing foreignModel entry: " + defaultModel));
      } else {
        issues.add(new Issue(genmodelFile, "Would add missing foreignModel entry: " + defaultModel));
      }
    }
  }

  /**
   * Ensures basePackage equals modelPluginId for all GenPackages.
   *
   * @param genmodelFile the source file
   * @param genModel the GenModel to inspect or mutate
   * @param modelPluginId the expected plugin id
   * @param issues the issue accumulator
   * @param applyChanges whether the change should be applied
   */
  public void enforceBasePackageEqualsModelPluginId(
          File genmodelFile,
          GenModel genModel,
          String modelPluginId,
          List<Issue> issues,
          boolean applyChanges) {

    List<GenPackage> genPackages = genModel.getGenPackages();
    for (GenPackage genPackage : genPackages) {
      String before = safeTrim(genPackage.getBasePackage());
      String gpName = safeTrim(genPackage.getPackageName());
      String label = gpName.isEmpty() ? "<unnamed GenPackage>" : gpName;

      if (!modelPluginId.equals(before)) {
        if (applyChanges) {
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
        } else {
          if (before.isEmpty()) {
            issues.add(
                    new Issue(
                            genmodelFile,
                            "Would set basePackage for genPackage "
                                    + label
                                    + " to '"
                                    + modelPluginId
                                    + "'."));
          } else {
            issues.add(
                    new Issue(
                            genmodelFile,
                            "Would change basePackage for genPackage "
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
  }

  /**
   * Ensures modelDirectory follows the required pattern.
   *
   * @param genmodelFile the source file
   * @param genModel the GenModel to inspect or mutate
   * @param modelPluginId the plugin id used to compute the expected directory
   * @param issues the issue accumulator
   * @param applyChanges whether the change should be applied
   */
  public void enforceModelDirectory(
          File genmodelFile,
          GenModel genModel,
          String modelPluginId,
          List<Issue> issues,
          boolean applyChanges) {

    String expected = normalize("/" + modelPluginId + "/target/generated-sources/ecore");
    String beforeRaw = genModel.getModelDirectory();
    String before = normalize(safeTrim(beforeRaw));

    if (before.isEmpty()) {
      if (applyChanges) {
        genModel.setModelDirectory(expected);
        issues.add(new Issue(genmodelFile, "Set modelDirectory to '" + expected + "'."));
      } else {
        issues.add(new Issue(genmodelFile, "Would set modelDirectory to '" + expected + "'."));
      }
    } else if (!before.equals(expected)) {
      if (applyChanges) {
        genModel.setModelDirectory(expected);
        issues.add(
                new Issue(
                        genmodelFile,
                        "Changed modelDirectory from '" + beforeRaw + "' to '" + expected + "'."));
      } else {
        issues.add(
                new Issue(
                        genmodelFile,
                        "Would change modelDirectory from '" + beforeRaw + "' to '" + expected + "'."));
      }
    }
  }

  /**
   * Safely trims a string, treating null as empty string.
   *
   * @param s the input string
   * @return the trimmed string or empty string
   */
  public String safeTrim(String s) {
    return s == null ? "" : s.trim();
  }

  /**
   * Normalizes a path by converting separators and collapsing repeated slashes.
   *
   * @param s the input path
   * @return the normalized path
   */
  public String normalize(String s) {
    return s.replace("\\", "/").replaceAll("/+", "/").trim();
  }

  /**
   * Creates the EMF ResourceSet used for loading GenModel resources.
   *
   * @return the configured ResourceSet
   */
  public ResourceSet createResourceSet() {
    ResourceSet resourceSet = new ResourceSetImpl();
    resourceSet.getPackageRegistry().put(GenModelPackage.eNS_URI, GenModelPackage.eINSTANCE);
    resourceSet
            .getResourceFactoryRegistry()
            .getExtensionToFactoryMap()
            .put("genmodel", new XMIResourceFactoryImpl());
    return resourceSet;
  }

  /**
   * Loads a GenModel resource either from disk or from an in-memory XML string.
   *
   * @param resourceSet the ResourceSet to use
   * @param uri the file URI of the GenModel
   * @param xmlOverride optional XML content to load instead of the file on disk
   * @param genmodelFile the source file for error reporting
   * @return the loaded Resource
   */
  public Resource loadResource(
          ResourceSet resourceSet, URI uri, String xmlOverride, File genmodelFile) {
    Resource resource;
    try {
      if (xmlOverride == null) {
        resource = resourceSet.getResource(uri, true);
        resource.load(null);
      } else {
        resource = resourceSet.createResource(uri);
        resource.load(
                new ByteArrayInputStream(xmlOverride.getBytes(StandardCharsets.UTF_8)), null);
      }
      return resource;
    } catch (IOException e) {
      throw new IllegalArgumentException(
              "Could not load genmodel file: " + genmodelFile.getAbsolutePath(), e);
    }
  }
}