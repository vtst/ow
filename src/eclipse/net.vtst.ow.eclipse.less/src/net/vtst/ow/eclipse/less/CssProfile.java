// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

// This class allows parsing CSS profile files (stored as XML files, see 
// ../data/css-profiles), and to give access to their contents via a small
// API.
// Static members and methods of this class manage the registry of CssProfiles.
public class CssProfile {
    
  @SuppressWarnings("unused")
  private String id;
  private String filename;
  @SuppressWarnings("unused")
  private String name;

  // For performance reasons, CssProfiles are loaded lazily.  The field
  // status stores the current status of a profile.
  private int status = STATUS_INITIALIZED;
  private static final int STATUS_INITIALIZED = 0;
  private static final int STATUS_LOADED = 1;
  private static final int STATUS_ERROR = 2;
  
  // **************************************************************************
  // Data-structures containing the information about a CSS profile.

  public class NumberDef {
    // This list contains the *values*, not the names.
    Set<String> units = new HashSet<String>();
  }
  private Map<String, NumberDef> numbers = new HashMap<String, NumberDef>();

  private Map<String, String> pseudoClasses;
  private Map<String, String> pseudoElements;
  @SuppressWarnings("unused")
  private Map<String, String> categories;
  private Map<String, String> functions;
  private Map<String, String> units;
  private Map<String, String> keywords;
  
  public class PropertyDef {
    // Names of the contained elements, mainly for avoiding recursion in parsing
    private Set<String> properties = new HashSet<String>();
    private Set<String> containers = new HashSet<String>();
    // These lists contains the *values*, not the names.
    public Set<String> keywords = new HashSet<String>();
    public Set<String> functions = new HashSet<String>();
    public Set<String> units = new HashSet<String>();
    public Set<String> strings = new HashSet<String>();
    // Attributes
    public String separator = null;
    public String inherited = null;
    public String mediagroup = null;
    public String category = null;
  }
  private Map<String, PropertyDef> properties;
  @SuppressWarnings("unused")
  private Map<String, PropertyDef> descriptors;
  
  // Used for parsing only
  private Map<String, Element> containerElements;
  private Map<String, Element> descriptorElements;
  private Map<String, Element> propertyElements;
  
  public class RuleDef {
    // These lists contains the *values*, not the names.
    Set<String> descriptors = new HashSet<String>();
    Set<String> properties = new HashSet<String>();
    Set<String> pseudoClasses = new HashSet<String>();
    Set<String> pseudoElements = new HashSet<String>();
    Set<String> selectorExpressions = new HashSet<String>();
  }
  private RuleDef pageRuleDef;
  private RuleDef fontfaceRuleDef;
  private RuleDef styleRuleDef;
  
  public static final int PAGE_RULE = 0;
  public static final int FONTFACE_RULE = 1;
  public static final int STYLE_RULE = 2;

  // **************************************************************************
  // Constructor

  public CssProfile(String id, String name) {
	  this.id = id;
	  this.name = name;
	  this.filename = "cssprofile-" + id + ".xml";
  }
  
  // **************************************************************************
  // Methods to get the information from the profile
  
  // This returns the descriptors for font-face rules, properties for other
  // rules.
  public Set<String> getProperties(int ruleType) {
    switch (ruleType) {
    case PAGE_RULE: return this.pageRuleDef.properties;
    case FONTFACE_RULE: return this.fontfaceRuleDef.descriptors;
    case STYLE_RULE: return this.styleRuleDef.properties;
    default: return null;
    }
  }
  
  public PropertyDef getProperty(String name) {
    return this.properties.get(name);
  }
  
  
  // **************************************************************************
  // Methods to load the profile information in memory

  // Get the text contents of the first element child which has a given tag name
  // Returns null if there is no such child.
  private String getChildTextContent(Node node, String tagName) {
    NodeList nodeList = node.getChildNodes();
    for (int i = 0; i < nodeList.getLength(); ++i) {
      Node child = nodeList.item(i);
      if (tagName.equals(child.getNodeName())) return child.getTextContent();
    }
    return null;
  }
  
  // --------------------------------------------------------------------------
  // Parsing of rule definitions

  private void parseRuleDef(RuleDef def, Element elt) {
    NodeList children = elt.getChildNodes();
    for (int i = 0; i < children.getLength(); ++i) {
      Node childNode = children.item(i);
      if (!(childNode instanceof Element)) continue;
      Element childElt = (Element) childNode;
      String nodeName = childElt.getNodeName();
      String name = childElt.getAttribute("name");
      if (name == null) continue;
      if ("descriptor".equals(nodeName)) {
        def.descriptors.add(name);
      } else if ("pseudo-class".equals(nodeName)) {
        String value = this.pseudoClasses.get(name);
        if (value != null) def.pseudoClasses.add(value);        
      } else if ("pseudo-element".equals(nodeName)) {
        String value = this.pseudoElements.get(name);
        if (value != null) def.pseudoElements.add(value);        
      } else if ("property".equals(nodeName)) {
        def.properties.add(name);
      } else if ("selector-expression".equals(nodeName)) {
        def.selectorExpressions.add(name);
      } 
    }
  }
  
  private RuleDef parseRuleDef(Document doc, String tagName) {
    NodeList nodeList = doc.getElementsByTagName(tagName);
    RuleDef def = new RuleDef();
    for (int i = 0; i < nodeList.getLength(); ++i) {
      parseRuleDef(def, (Element) nodeList.item(i));
    }
    return def;
  }

  private Map<String, Element> parseElementMap(Document doc, String tagName) {
    NodeList nodeList = doc.getElementsByTagName(tagName);
    Map<String, Element> map = new HashMap<String, Element>();
    for (int i = 0; i < nodeList.getLength(); ++i) {
      Element elt = (Element) nodeList.item(i);
      String name = elt.getAttribute("name");
      if (name != null) map.put(name, elt);
    }
    return map;
  }

  // --------------------------------------------------------------------------
  // Parsing of descriptor and property definitions
  // Descriptor and definitions are parsed in two steps. First, the DOM elements
  // stored in a Map by parseElementMap, and then the element are converted
  // into PropertyDef by parsePropertyDef(Rec) and parsePropertyDefsFromMap.

  // This function resolves cross-references by recursive calls.
  private void parsePropertyDefRec(Element elt, PropertyDef def) {
    NodeList children = elt.getChildNodes();
    for (int i = 0; i < children.getLength(); ++i) {
      Node childNode = children.item(i);
      if (!(childNode instanceof Element)) continue;
      Element childElt = (Element) childNode;
      String nodeName = childElt.getNodeName();
      String name = childElt.getAttribute("name");
      if (name == null) continue;
      if ("keyword".equals(nodeName)) {
        String value = keywords.get(name);
        if (value != null) def.keywords.add(value);
      } else if ("property".equals(nodeName)) {
        if (!def.properties.contains(name)) {
          Element refElement = propertyElements.get(name);
          if (refElement != null) {
            def.properties.add(name);
            parsePropertyDefRec(refElement, def);
          }
        }
      } else if ("separator".equals(nodeName)) {
        def.separator = name;
      } else if ("string".equals(nodeName)) {
        def.strings.add(name);
      } else if ("function".equals(nodeName)) {
        String value = functions.get(name);
        if (value != null) def.functions.add(value);        
      } else if ("number".equals(nodeName)) {
        NumberDef numberDef = numbers.get(name);
        if (numberDef != null) def.units.addAll(numberDef.units);
      } else if ("container".equals(nodeName)) {
        if (!def.containers.contains(name)) {
          Element refElement = containerElements.get(name);
          if (refElement != null) {
            def.containers.add(name);
            parsePropertyDefRec(refElement, def);
          }
        }
      }
    }
    if (elt.hasAttribute("inherited")) def.inherited = elt.getAttribute("inherited");
    if (elt.hasAttribute("mediagroup")) def.mediagroup = elt.getAttribute("mediagroup");
    if (elt.hasAttribute("category")) def.category = elt.getAttribute("category");
  }
  
  private PropertyDef parsePropertyDef(Element elt) {
    PropertyDef def = new PropertyDef();
    parsePropertyDefRec(elt, def);
    return def;
  }

  private Map<String, PropertyDef> parsePropertyDefsFromMap(Map<String, Element> elementMap) {
    Map<String, PropertyDef> map = new HashMap<String, PropertyDef>();
    for (Map.Entry<String, Element> entry: elementMap.entrySet()) {
      map.put(entry.getKey(), parsePropertyDef(entry.getValue()));
    }
    return map;
  }
  
  private void setPropertyDefsFromDoc(Document doc) {
    this.containerElements = parseElementMap(doc, "container-def");
    this.descriptorElements = parseElementMap(doc, "descriptor-def");
    this.propertyElements = parseElementMap(doc, "property-def");
    
    this.descriptors = parsePropertyDefsFromMap(this.descriptorElements);
    this.properties = parsePropertyDefsFromMap(this.propertyElements);
    
    this.propertyElements = null;
    this.containerElements = null;    
  }

  // --------------------------------------------------------------------------
  // Parsing of number definitions

  private Map<String, NumberDef> parseNumberDefs(Document doc) {
    Map<String, NumberDef> map = new HashMap<String, NumberDef>();
    NodeList nodeList = doc.getElementsByTagName("number-def");
    for (int i = 0; i < nodeList.getLength(); ++i) {
      Element elt = (Element) nodeList.item(i);
      String name = elt.getAttribute("name");
      if (name == null) continue;
      NumberDef def = new NumberDef();
      NodeList children = elt.getElementsByTagName("unit");
      for (int j = 0; j < children.getLength(); ++j) {
        Element child = (Element) children.item(j);
        String unitValue = units.get(child.getAttribute("name"));
        if (unitValue == null) continue;
        def.units.add(unitValue);
      }
      map.put(name, def);
    }
    return map;
  }
  
    // --------------------------------------------------------------------------
  // Parsing of name to value mappings
  
  private Map<String, String> parseNameToValues(
      Document doc, String defElementName, String valueElementName) {
    Map<String, String> map = new HashMap<String, String>();
    NodeList nodeList = doc.getElementsByTagName(defElementName);
    for (int i = 0; i < nodeList.getLength(); ++i) {
      Element elt = (Element) nodeList.item(i);
      String name = elt.getAttribute("name");
      if (name == null) continue;
      String value = getChildTextContent(elt, valueElementName);
      if (value == null) continue;
      map.put(name, value);
    }
    return map;
  }
      
  // --------------------------------------------------------------------------
  // Main parsing function

  private void initializeFromDocument(Document doc) {
    this.pseudoClasses = parseNameToValues(doc, "pseudo-class-def", "selector-value");
    this.pseudoElements = parseNameToValues(doc, "pseudo-element-def", "selector-value");
    this.categories = parseNameToValues(doc, "category-def", "caption");
    this.functions = parseNameToValues(doc, "function-def", "function-value");
    this.units = parseNameToValues(doc, "unit-def", "unit-value");
    this.keywords = parseNameToValues(doc, "keyword-def", "keyword-value");
    this.numbers = parseNumberDefs(doc);
    setPropertyDefsFromDoc(doc);
    this.pageRuleDef = parseRuleDef(doc, "page-rule-def");
    this.fontfaceRuleDef = parseRuleDef(doc, "fontface-rule-def");
    this.styleRuleDef = parseRuleDef(doc, "style-rule-def");
  }
  
  private void load() {
    if (this.status != STATUS_INITIALIZED) return;
	  Bundle bundle = Platform.getBundle(LessRuntimeModule.PLUGIN_ID);
	  if (bundle == null) {
	    this.status = STATUS_ERROR;
	    return;
	  }

    try {
      InputStream input = FileLocator.openStream(bundle, (new Path(DATA_DIRECTORY)).append(this.filename), false);
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(input);
      this.initializeFromDocument(doc);
      input.close();
      this.status = STATUS_LOADED;
    } catch (IOException e) {
      this.status = STATUS_ERROR;
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      this.status = STATUS_ERROR;
      e.printStackTrace();
    } catch (SAXException e) {
      this.status = STATUS_ERROR;
      e.printStackTrace();
    }

  }

  // **************************************************************************
  // Static functions (registry)

  private static final String DATA_DIRECTORY = "data/css-profiles";
  private static final String DEFAULT_ID = "css2";
  private static final Map<String, CssProfile> registry = new HashMap<String, CssProfile>();
  
  private static void addToRegistry(String id, String name) {
    registry.put(id, new CssProfile(id, name));
  }
  
  public static void initializeRegistry() {
    if (!registry.isEmpty()) return;
    addToRegistry("css1", "CSS 1");
    addToRegistry("css2", "CSS 3");
    addToRegistry("css3", "CSS 3");
    addToRegistry("mobile1_0", "Mobile 1.0");
    addToRegistry("wap", "WAP");
  }

  public static CssProfile get(String id) {
    CssProfile profile = registry.get(id);
    if (profile != null) profile.load();
    return profile;
  }
  
  public static CssProfile getDefault() {
    return get(DEFAULT_ID);
  }
}
