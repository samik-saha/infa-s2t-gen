package com.samiksaha.infa.automateds2t;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MappingTest {

	private static Document xmlDocument;
	private static XPath xPath;
	private static NodeList mappingNodeList;
	private static Mapping mapping;

	@BeforeClass
	public static void initialize() {
		File file = new File("./test/res/m_sample_straight_move.XML");
		try {
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			builderFactory.setValidating(true);
			builderFactory.setFeature("http://xml.org/sax/features/namespaces", false);
			builderFactory.setFeature("http://xml.org/sax/features/validation", false);
			builderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
			builderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			
			xmlDocument = builder.parse(file);
			xPath = XPathFactory.newInstance().newXPath();

			mappingNodeList = xmlDocument.getElementsByTagName("MAPPING");
			
			mapping = new Mapping(null, mappingNodeList.item(0));
			mapping.loadMappingDetails();
		} catch (IOException | SAXException | ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public final void testMappingName() {
		assertEquals("m_sample_straight_move", mapping.getName()); 
	}

	@Test
	public final void testMappingDescription() {
		assertEquals("Test Description", mapping.getDescription());
	}
	
	@Test
	public final void testTransformationCount() {
		assertEquals(4, mapping.getTransformationCount());
	}
	
	@Test
	public final void testTargetTableNames() {
		ArrayList<String> tgtTableNames = mapping.getTargetTableNames();
		assertEquals("test_Target", tgtTableNames.get(0));
	}
}
