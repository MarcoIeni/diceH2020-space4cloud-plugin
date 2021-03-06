/*
Copyright 2017 Arlind Rufi
Copyright 2017 Gianmario Pozzi
Copyright 2017 Giorgio Pea

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package it.polimi.diceH2020.plugin.control;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class JSonReader {
	private static final String INSTANCES = "instances";
	private static final String GENERIC_SIZE = "genericSize";
	private static final String PROVIDER = "provider";
	private String provider;
	private Map<String, Long> classNumVM;
	private Map<String, String> classTypeVM;
	private List<String> classes;
	private Map<String, String> idClassUmlFile;
	private String filePath;

	public JSonReader(String filePath) {
		setProvider("");
		setClassNumVM(new HashMap<String, Long>());
		setClassTypeVM(new HashMap<String, String>());
		classes = new ArrayList<String>();
		this.filePath = filePath;
		idClassUmlFile = new HashMap<String, String>();
	}

	// Takes as input the file path of the JSon file
	public void read() {
		// read the json file
		FileReader reader;

		try {
			reader = new FileReader(filePath);

			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
			provider = (String) jsonObject.get("provider");
			JSONArray lang = (JSONArray) jsonObject.get("lstSolutions");
			Iterator<?> i = lang.iterator();

			// take each value from the json array separately
			while (i.hasNext()) {
				JSONObject innerObj = (JSONObject) i.next();
				String idClass = (String) innerObj.get("id");
				long numVm = (long) innerObj.get("numberVM");
				JSONObject jsonOb = (JSONObject) innerObj.get("typeVMselected");
				String type = (String) jsonOb.get("id");
				this.classNumVM.put(idClass, numVm);
				this.classTypeVM.put(idClass, type);
				this.classes.add(idClass);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private void writeUml(String filePath, String id) {
		File inputFile = new File(filePath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		Document doc = null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(inputFile);
		} catch (SAXException | ParserConfigurationException | IOException e) {
			e.printStackTrace();
		}

		Element root = doc.getDocumentElement();
		root.normalize();
		NodeList packegedElement = root.getElementsByTagName("packagedElement");

		Node firstPackEl = packegedElement.item(0);
		String deviceId;
		if (firstPackEl.getNodeType() == Node.ELEMENT_NODE) {
			Element pElement = (Element) firstPackEl;
			deviceId = pElement.getAttribute("xmi:id");
		} else {
			System.err.println("NO DEVICE ID IN THE DDSM UML FILE. THE DDSM FILE WILL NOT BE OVERWRITTEN");
			return;
		}

		NodeList nodes = root.getElementsByTagName("DDSM:DdsmVMsCluster");
		Node n = searchElement(nodes, deviceId);

		if (n == null) {
			System.err.println("NO DEVICE ID MATCHING IN THE DDSM UML FILE. THE DDSM FILE WILL NOT BE OVERWRITTEN");
		} else {
			setAttribute(n, INSTANCES, classNumVM.get(id).toString());
			setAttribute(n, GENERIC_SIZE, classTypeVM.get(id));
			setAttribute(n, PROVIDER, provider);

			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer;
			try {
				transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(new File(filePath));
				transformer.transform(source, result);
			} catch (TransformerException e) {
				e.printStackTrace();
			}
		}
	}

	public void write() {
		for (String id : this.classes) {
			this.writeUml(this.idClassUmlFile.get(id), id);
		}
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public Map<String, Long> getClassNumVM() {
		return classNumVM;
	}

	public void setClassNumVM(Map<String, Long> classNumVM) {
		this.classNumVM = classNumVM;
	}

	public Map<String, String> getClassTypeVM() {
		return classTypeVM;
	}

	public void setClassTypeVM(Map<String, String> classTypeVM) {
		this.classTypeVM = classTypeVM;
	}

	public void createMap(String jsonFilePath) {
		FileReader reader;

		try {
			reader = new FileReader(jsonFilePath);
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
			Set<?> keys = jsonObject.keySet();

			for (Object s : keys) {
				this.idClassUmlFile.put((String) s, (String) jsonObject.get(s));
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static boolean findAttribute(NamedNodeMap element, String attribute) {
		Boolean result = false;

		try {
			Node value = element.getNamedItem(attribute);
			if (value != null) {
				result = true;
			}
		} catch (Exception e) {
		}

		return result;
	}

	private static Node searchElement(NodeList nodes, String deviceId) {
		Node n = null;
		String baseDevice;
		Element nElement = null;
		for (int i = 0; i < nodes.getLength(); i++) {
			n = nodes.item(i);
			nElement = (Element) n;
			baseDevice = nElement.getAttribute("base_Device");
			if (deviceId.equals(baseDevice)) {
				return n;
			}
		}
		return null;
	}

	/**
	 * set the attribute named as "name" in the node "n" with a given value
	 * 
	 * @param n
	 *            the node in which you want to set the attribute
	 * @param name
	 *            the name of the attribute
	 * @param value
	 *            the value of the attribute
	 */
	private static void setAttribute(Node n, String name, String value) {
		NamedNodeMap attributes = n.getAttributes();
		if (!JSonReader.findAttribute(attributes, name)) {
			Element el = (Element) n;
			el.setAttribute(name, value);
		} else {
			Node nodeAttrType1 = attributes.getNamedItem(name);
			nodeAttrType1.setTextContent(value);
		}
	}
}
