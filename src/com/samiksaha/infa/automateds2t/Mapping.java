/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samiksaha.infa.automateds2t;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	ArrayList<String> targetInstancesForS2T;
	ArrayList<TableField> srcTblFld;

	public class SQ {
		ArrayList<String> sqQuery = new ArrayList<String>();
		ArrayList<String> sqFilter = new ArrayList<String>();
	}

	public class S2TRow {
		String tgtTbl;
		String tgtFld;
		String logic;
		String tgtFldType;
		String tgtFldKeyType;
		String tgtFldNullable;
		ArrayList<TableField> S2TsrcTblFld;

		public S2TRow() {
			this.S2TsrcTblFld = new ArrayList<TableField>();
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
	
	public class Instance{
		String name;
		boolean reusable;
		String trfName;
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
		targetTables = findTargetTables();
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
					"./INSTANCE", mappingNode, XPathConstants.NODESET);
			trfCount = trfNodeList.getLength();
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
		ArrayList<String> instanceNameList = new ArrayList<String>();
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

	private ArrayList<String> findTargetTables() {
		ArrayList<String> targetTableNames = new ArrayList<String>();
		String targetTableName;
		String targetInstance;

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
						s2tRow.logic = getLogic(fromInstanceField);
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

	private String getLogic(InstanceField instanceField) {		
		String logic = "";
		String trfLogic = "";
		
		if (instanceField.instanceName==null) return logic;
		
		Logger.getLogger(Mapping.class.getName())
		.log(Level.INFO, "Getting logic for "+instanceField.instanceName+"."+instanceField.field);

		Instance instance = getInstance(instanceField.instanceName);
		Node trfNode = getTrfNode(instance);
		
		if (instanceField.instanceType.equals("Source Definition")){
			Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
					instance.instanceType+" : "+instance.name+" : "+instance.trfName);
			getSourceFields(instanceField);
			return "";
		}else if (instanceField.instanceType.equals("Expression")){
			Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
					instance.instanceType+" : "+instance.name+" : "+instance.trfName);
			trfLogic = getLogicFromEXP(trfNode, instanceField.field);
			//TODO - Unconnected lookup
		}
		
		System.out.println("trfLogic="+trfLogic);
		logic = trfLogic;
		
		if (trfLogic.isEmpty()) trfLogic = instanceField.field;
		
		Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
				"Looking for input ports for "+instanceField.field+" in "+instance.name);
		ArrayList<String> inPorts = getInPorts(trfNode,instance.instanceType,instanceField.field,trfLogic);
		
		
		for (int i=0;i<inPorts.size();i++){
			String inPort = inPorts.get(i);
			InstanceField frmInstFld = getFromField(instanceField.instanceName, inPort);
			
			if (frmInstFld.instanceName == null) continue;
			if (!frmInstFld.field.equals(inPort)){
				logic = inPort + "=" +frmInstFld.field + (logic.isEmpty()?"":"\r\n"+logic);
			}
			
			trfLogic = getLogic(frmInstFld);
			
			if (!trfLogic.isEmpty()){
				logic = trfLogic + (logic.isEmpty()?"":"\r\n"+logic);
			}
		}
		return logic;
	}
	
	private ArrayList<String> getInPorts(Node trfNode, String trfType, String fldNm, String fldExp ){
		String trfName = trfNode.getAttributes().getNamedItem("NAME").getNodeValue();//for logging purpose
		Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
				"Getting input ports for "+fldNm);
		
		//TODO - getInPorts for UNION and Mapplet
		
		if (trfType.equals("Router")){
			return getInPortsROUTER(trfNode, fldNm);
		}else if (trfType.equals("Custom Transformation")){
			return getInPortsUNION(trfNode, fldNm);
		}
		
		ArrayList<String> inPorts = new ArrayList<String>();
		String inPort;
		
		try {
			String xPathExpr = "./TRANSFORMFIELD[@PORTTYPE='INPUT/OUTPUT' or @PORTTYPE='INPUT']";
			NodeList inpPortList = (NodeList) xPath
					.evaluate(xPathExpr,trfNode,XPathConstants.NODESET);
			for (int i=0;i<inpPortList.getLength();i++){
				Node inPortNode = inpPortList.item(i);
				inPort = inPortNode.getAttributes()
						.getNamedItem("NAME").getNodeValue();
				Pattern pattern = Pattern.compile("\\b"+inPort+"\\b");
				Matcher matcher = pattern.matcher(fldExp);
				if (matcher.find()){
					Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
							"Found input port: "+trfName+"."+inPort+" for "+fldNm);
					inPorts.add(inPort);
				}
			}
		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null,
					ex);
		}
		
		if (inPorts.isEmpty())
			Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
					"No input ports found in "+trfType+" "+trfName+" for "+fldNm);
		return inPorts;
	}
	
	private ArrayList<String> getInPortsROUTER(Node trfNode, String fldNm){
		String trfName = trfNode.getAttributes().getNamedItem("NAME").getNodeValue();//for logging purpose
		ArrayList<String> inPorts = new ArrayList<String>();
		String inPort;
		try {
			String xPathExpr = "./TRANSFORMFIELD[@PORTTYPE='OUTPUT' and @NAME='"+fldNm+"']/@REF_FIELD";
			inPort = (String) xPath.evaluate(xPathExpr,trfNode,XPathConstants.STRING);
			Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
					"Found input port: "+trfName+"."+inPort+" for "+fldNm);
			inPorts.add(inPort);
		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null,
					ex);
		}
		return inPorts;
	}
	
	private ArrayList<String> getInPortsUNION(Node trfNode, String fldNm){
		String trfName = trfNode.getAttributes().getNamedItem("NAME").getNodeValue();//for logging purpose
		ArrayList<String> inPorts = new ArrayList<String>();
		String inPort;
		try {
			String xPathExpr = "./FIELDDEPENDENCY[@OUTPUTFIELD='"+fldNm+"']/@INPUTFIELD";
			inPort = (String) xPath.evaluate(xPathExpr,trfNode,XPathConstants.STRING);
			Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
					"Found input port: "+trfName+"."+inPort+" for "+fldNm);
			inPorts.add(inPort);
		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null,
					ex);
		}
		return inPorts;
	}
	
	private String getLogicFromEXP(Node trfNode, String field){
		Logger.getLogger(Mapping.class.getName()).log(Level.INFO,"Getting logic from exp for "+field);
		String logic="";
		try {
			Node trfFldNode = (Node) xPath.evaluate("./TRANSFORMFIELD[@NAME='"+field+"']",trfNode,XPathConstants.NODE);
			String fldType = trfFldNode.getAttributes().getNamedItem("PORTTYPE").getNodeValue();
			System.out.println(fldType);
			if (fldType.equals("OUTPUT")){
					String expStr = trfFldNode.getAttributes().getNamedItem("EXPRESSION").getNodeValue();
					logic=field + " = " + expStr + "\r\n";
					String varExp = getLogicFromEXPVar(trfNode, field, expStr);
					if (!varExp.isEmpty()){
						logic = varExp + "\r\n" + logic;
					}
			}
		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null,
					ex);
		}
		return logic;
	}
	
	private String getLogicFromEXPVar(Node trfNode, String fldName, String fldExp){
		String logic = "";
		try {
			String xPathExpr = "./TRANSFORMFIELD[@PORTTYPE='LOCAL VARIABLE' and @NAME!='"+fldName+"']";
			NodeList varPortList = (NodeList) xPath
					.evaluate(xPathExpr,trfNode,XPathConstants.NODESET);
			
			for (int i=0;i<varPortList.getLength();i++){
				Node varPort = varPortList.item(i);
				String varPortName = varPort.getAttributes()
						.getNamedItem("NAME").getNodeValue();
				Pattern pattern = Pattern.compile("\\b"+varPortName+"\\b");
				Matcher matcher = pattern.matcher(fldExp);
				
				if (matcher.find()){
					String varExp = varPort.getAttributes()
							.getNamedItem("EXPRESSION").getNodeValue();
					if (!logic.isEmpty())
						logic = varPortName + " = " + varExp + "\r\n"+logic;
					else
						logic =  varPortName + " = " + varExp;
					
					String x = getLogicFromEXPVar(trfNode, varPortName, varExp);
					if (!x.isEmpty()){
						logic = x + "\r\n" + logic;
					}
				}
			}
		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null,
					ex);
		}
		
		return logic;
	}
	
	private Instance getInstance(String instanceName){
		Instance instance = new Instance();
		try{
			String xPathExprInstance = ".//INSTANCE[@NAME='"+instanceName+"']";
			Node instanceNode = (Node) xPath.evaluate(xPathExprInstance,mappingNode,XPathConstants.NODE);
			
			instance.name=instanceNode.getAttributes()
					.getNamedItem("NAME").getNodeValue();
			instance.trfName=instanceNode.getAttributes()
					.getNamedItem("TRANSFORMATION_NAME").getNodeValue();
			instance.instanceType=instanceNode.getAttributes()
					.getNamedItem("TRANSFORMATION_TYPE").getNodeValue();
			Node node = instanceNode.getAttributes().getNamedItem("REUSABLE");
			if (node !=null)
				instance.reusable=node.getNodeValue().equals("YES")?true:false;
		}catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null,
					ex);
		} 
		
		return instance;
	}
	
	private Node getTrfNode(Instance instance){
		Node trfNode = null;
		if (instance.reusable){
			//TODO
		}else{
			try{
				String xPathExpr = ".//TRANSFORMATION[@NAME='"+instance.trfName + "']";
				trfNode = (Node) xPath.evaluate(xPathExpr, mappingNode,
						XPathConstants.NODE);
			}catch (XPathExpressionException ex) {
				Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null,
						ex);
			} 
			
		}
		return trfNode;
	}
	
	/***
	 * Finds the source definition field details
	 * 
	 * @param instanceField
	 */
	private void getSourceFields(InstanceField instanceField){
		TableField srcFld = new TableField();
		
		Logger.getLogger(Mapping.class.getName()).log(Level.INFO, "Source Definition :"+instanceField.instanceName+" Field: "+instanceField.field);
		
		try {
			/* Get transformation name(table name) from Instance node */
			String xPathExprTrfName = "./INSTANCE[@NAME='" + instanceField.instanceName+ "']/@TRANSFORMATION_NAME";
			String trfName = (String) xPath.evaluate(xPathExprTrfName, mappingNode, XPathConstants.STRING);
			
			String xPathExprSrcNode = "/POWERMART/REPOSITORY/FOLDER/SOURCE[@NAME='" + trfName + "']/SOURCEFIELD[@NAME='"+instanceField.field + "']";
			Node srcFldNode = (Node) xPath.evaluate(xPathExprSrcNode, MainWindow.xmlDocument,XPathConstants.NODE);
			
			srcFld.fldName=instanceField.field;
			srcFld.tblName=trfName;
			String srcFldDataType = (String) xPath.evaluate("./@DATATYPE",
					srcFldNode, XPathConstants.STRING);
			String srcFldPrec = (String) xPath.evaluate("./@PRECISION",
					srcFldNode, XPathConstants.STRING);
			String srcFldScale = (String) xPath.evaluate("./@SCALE",
					srcFldNode, XPathConstants.STRING);
			srcFld.fldType = srcFldDataType
					+ "("
					+ srcFldPrec
					+ (srcFldScale.equals("0") ? "" : "," + srcFldScale)
					+ ")";
			srcTblFld.add(srcFld);
		}catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null,
					ex);
		} 
		
		
	}


}
