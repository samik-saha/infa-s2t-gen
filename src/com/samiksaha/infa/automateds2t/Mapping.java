/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samiksaha.infa.automateds2t;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Samik
 */
public class Mapping {

	private Node mappingNode;
	private XPath xPath;
	private String mappingName;
	private String mappingDescription;
	private int transformationCount;
	ArrayList<String> targetTables;
	ArrayList<String> targetInstances;
	ArrayList targetInstancesForS2T;
	ArrayList srcTblFld;

	public class SQ {
		ArrayList sqQuery = new ArrayList();
		ArrayList sqFilter = new ArrayList();
	}

	public class S2TRow {
		String tgtTbl;
		String tgtFld;
		String logic;
		String tgtFldType;
		String tgtFldKeyType;
		String tgtFldNullable;
		ArrayList S2TsrcTblFld;

		public S2TRow() {
			this.S2TsrcTblFld = new ArrayList();
		}
	}

	public class TableField {
		String tblName;
		String fldName;
		String fldType;
	}

	public class InstanceField {
		String field;
		String instanceName;
		String instanceType;
	}

	public Mapping(Node mappingNode) {
		this.mappingNode = mappingNode;
		xPath = XPathFactory.newInstance().newXPath();

		try {
			mappingName = (String) xPath.compile("./@NAME").evaluate(
					mappingNode, XPathConstants.STRING);
			mappingDescription = (String) xPath.compile("./@DESCRIPTION").evaluate(
					mappingNode, XPathConstants.STRING);

		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null,
					ex);
		}
		targetInstances = getInstanceList("Target Definition");
		targetInstancesForS2T = getInstanceList("Target Definition");
		targetTables = findTargerTables();
		transformationCount = countTransformations();
	}

	public String getName() {
		return mappingName;
	}

	public String getDescription() {
		return mappingDescription;
	}

	public int getTransformationCount() {
		return transformationCount;
	}
	
	public ArrayList<String> getTargetTableNames(){
		return targetTables;
	}

	public int countTransformations() {
		int trfCount = 0;

		try {
			NodeList trfNodeList = (NodeList) xPath.evaluate(
					"./TRANSFORMATION", mappingNode, XPathConstants.NODESET);
			//trfCount = trfNodeList.getLength();
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return trfCount;
	}

	public ArrayList<String> getTargetInstanceList() {
		return targetInstances;
	}

	private ArrayList<String> getInstanceList(String transformationType) {
		ArrayList instanceNameList = new ArrayList();
		String instanceName;

		try {
			NodeList instanceList = (NodeList) xPath.evaluate(
					"./INSTANCE[@TRANSFORMATION_TYPE='" + transformationType
							+ "']/@NAME", mappingNode, XPathConstants.NODESET);
			for (int i = 0; i < instanceList.getLength(); i++) {
				instanceName = instanceList.item(i).getFirstChild()
						.getNodeValue();
				if (!instanceNameList.contains(instanceName)) {
					instanceNameList.add(instanceName);
				}
			}
		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null,
					ex);
		}
		return instanceNameList;
	}

	private ArrayList<String> findTargerTables() {
		ArrayList targetTableNames = new ArrayList();
		String targetTableName;
		String targetInstance;
		srcTblFld = new ArrayList();

		ListIterator iter = targetInstancesForS2T.listIterator();
		while (iter.hasNext()) {
			targetInstance = (String) iter.next();
			try {
				targetTableName = (String) xPath.evaluate("./INSTANCE[@NAME='"
						+ targetInstance + "' and @TRANSFORMATION_TYPE="
						+ "'Target Definition']/@TRANSFORMATION_NAME",
						mappingNode, XPathConstants.STRING);
				if (!targetTableNames.contains(targetTableName))
					targetTableNames.add(targetTableName);
			} catch (XPathExpressionException ex) {
				Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE,
						null, ex);
			}
		}
		return targetTableNames;
	}

	public SQ getSQQuery() {
		SQ sq = new SQ();
		NodeList sqTrfNodeList;
		Node sqTrfNode;
		String sqQuery;
		String sqFilter;

		try {
			sqTrfNodeList = (NodeList) xPath.evaluate(
					"./TRANSFORMATION[@TYPE='Source Qualifier']", mappingNode,
					XPathConstants.NODESET);
			for (int i = 0; i < sqTrfNodeList.getLength(); i++) {
				sqTrfNode = sqTrfNodeList.item(i);
				sqQuery = (String) xPath.evaluate(
						"./TABLEATTRIBUTE[@NAME='Sql Query']/@VALUE",
						sqTrfNode, XPathConstants.STRING);
				sq.sqQuery.add(sqQuery);
				sqFilter = (String) xPath.evaluate(
						"./TABLEATTRIBUTE[@NAME='Source Filter']/@VALUE",
						sqTrfNode, XPathConstants.STRING);
				sq.sqFilter.add(sqFilter);
			}
		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null,
					ex);
		}

		return sq;
	}

	public ArrayList<ArrayList<S2TRow>> createS2T() {
		S2TRow s2tRow;
		ArrayList<S2TRow> s2tTargetInstance;
		ArrayList<ArrayList<S2TRow>> s2tAllTargetInstances = new ArrayList<ArrayList<S2TRow>>();
		String targetInstanceName;
		String targetTableName;
		NodeList targetFields;
		Node targetField;
		String tgtFldDataType;
		String tgtFldPrec;
		String tgtFldScale;

		srcTblFld = new ArrayList();
		ListIterator iter = targetInstancesForS2T.listIterator();

		while (iter.hasNext()) {
			targetInstanceName = (String) iter.next();
			s2tTargetInstance = new ArrayList<S2TRow>();
			Logger.getLogger(Mapping.class.getName()).log(Level.INFO,"Processing target instance "+targetInstanceName);
			
			
			try {
				String tgtTbl = (String) xPath.evaluate("./INSTANCE[@NAME='"
						+ targetInstanceName + "' and @TRANSFORMATION_TYPE="
						+ "'Target Definition']/@TRANSFORMATION_NAME",
						mappingNode, XPathConstants.STRING);

				targetFields = (NodeList) xPath.evaluate("//TARGET[@NAME='"
						+ tgtTbl + "']/TARGETFIELD",
						MainWindow.xmlDocument, XPathConstants.NODESET);
				
				for (int i = 0; i < targetFields.getLength(); i++) {
					s2tRow = new S2TRow();
					s2tRow.tgtTbl=tgtTbl;
					targetField = targetFields.item(i);
					s2tRow.tgtFld = (String) xPath.evaluate("./@NAME",
							targetField, XPathConstants.STRING);
					s2tRow.tgtFldKeyType = (String) xPath.evaluate(
							"./@KEYTYPE", targetField, XPathConstants.STRING);
					s2tRow.tgtFldNullable = (String) xPath.evaluate(
							"./@NULLABLE", targetField, XPathConstants.STRING);
					tgtFldDataType = (String) xPath.evaluate("./@DATATYPE",
							targetField, XPathConstants.STRING);
					tgtFldPrec = (String) xPath.evaluate("./@PRECISION",
							targetField, XPathConstants.STRING);
					tgtFldScale = (String) xPath.evaluate("./@SCALE",
							targetField, XPathConstants.STRING);
					s2tRow.tgtFldType = tgtFldDataType
							+ "("
							+ tgtFldPrec
							+ (tgtFldScale.equals("0") ? "" : "," + tgtFldScale)
							+ ")";

					srcTblFld.clear();

					InstanceField fromInstanceField = getFromField(
							targetInstanceName, s2tRow.tgtFld);

					if (fromInstanceField != null) {
						s2tRow.logic = getLogic(fromInstanceField.instanceName,
								fromInstanceField.field);
						if (s2tRow.logic.equals(""))
							s2tRow.logic = "Straight Move";
					} else {
						s2tRow.logic = "";
					}

					s2tRow.S2TsrcTblFld.addAll(srcTblFld);

					
					s2tTargetInstance.add(s2tRow);
				}
			} catch (XPathExpressionException ex) {
				Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE,
						null, ex);
			}
			s2tAllTargetInstances.add(s2tTargetInstance);
		}
		
		Logger.getLogger(Mapping.class.getName()).log(Level.INFO,s2tAllTargetInstances.get(0).get(0).tgtFld);

		return s2tAllTargetInstances;
	}

	private InstanceField getFromField(String toInstance, String toField) {
		InstanceField instFld = new InstanceField();
		Node connectorNode;

		try {
			String xPathExpr = "./CONNECTOR[@TOFIELD='" + toField
					+ "' and @TOINSTANCE='" + toInstance + "']";
			connectorNode = (Node) xPath.evaluate(xPathExpr, mappingNode,
					XPathConstants.NODE);
			if (connectorNode !=null){
				instFld.field = connectorNode.getAttributes()
						.getNamedItem("FROMFIELD").getNodeValue();
				instFld.instanceName = connectorNode.getAttributes()
						.getNamedItem("FROMINSTANCE").getNodeValue();
				instFld.instanceType = connectorNode.getAttributes()
						.getNamedItem("FROMINSTANCETYPE").getNodeValue();
			}
		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null,
					ex);
		} 
		return instFld;
	}

	private String getLogic(String instanceName, String fieldName) {
		String trfLogic = "";

		// TODO
		return trfLogic;
	}
}
