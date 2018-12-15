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

import com.samiksaha.infa.automateds2t.Mapping.S2TForTargetInstance;
import com.samiksaha.infa.automateds2t.Mapping.S2TRow;
import com.samiksaha.infa.automateds2t.Mapping.TableField;

public class MappingTest {

	private static Document xmlDocument;
	private static XPath xPath;
	private static NodeList mappingNodeList;
	private static Mapping mapping;
	static ArrayList<S2TForTargetInstance> s2t;

	@BeforeClass
	public static void initialize() {
		File file = new File("./src/test/com/samiksaha/infa/automateds2t/res/m_sample_straight_move.XML");
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
			s2t = mapping.createS2T();
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
	
	@Test
	public final void testSQQuery() {
		String actualQuery=mapping.getSQQuery().sqQuery.get(0);
		String expectedQuery = "select 1 from dual";
		assertEquals(expectedQuery, actualQuery);
	}
	
	@Test
	public final void testTargetInstanceList() {
		//<TARGETLOADORDER ORDER ="1" TARGETINSTANCE ="test_Target"/>
		String expectedTargetInstanceName="test_Target";
		int expectedTargetInstanceOrder=1;
		String actualTargetInstanceName=mapping.getTargetInstanceList().get(0).name;
		int actualTargetInstanceOrder=mapping.getTargetInstanceList().get(0).order;
		assertEquals(expectedTargetInstanceOrder, actualTargetInstanceOrder);
		assertEquals(expectedTargetInstanceName,actualTargetInstanceName);
	}
	
	@Test
	public final void testCreateS2T() {
		
		S2TRow expectedS2TRow= mapping.new S2TRow();
		TableField srcTblFld = mapping.new TableField();
		expectedS2TRow.tgtFld="ID";
		expectedS2TRow.tgtFldKeyType="string(100)";
		expectedS2TRow.tgtTbl="test_Target";
		srcTblFld.fldName="DUMMY";
		srcTblFld.tblName="DUAL";
		srcTblFld.fldType="varchar2(1)";
		expectedS2TRow.S2TsrcTblFld.add(srcTblFld);
		expectedS2TRow.tgtFldNullable="NULL";
		expectedS2TRow.logic="ID=DUMMY";
		System.out.println(s2t.get(0).s2tRows.size());
		
		//TODO
	}
	
    @Test
    public final void testGetSourceTableNames() {
    	String expectedSourceTableName="DUAL";
    	//String actualSourceTableName=mapping.getSourceTableNames().get(0);
    	//assertEquals(expectedSourceTableName,actualSourceTableName);
    }
}
