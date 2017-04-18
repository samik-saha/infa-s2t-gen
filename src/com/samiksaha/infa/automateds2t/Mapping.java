/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samiksaha.infa.automateds2t;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingWorker;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.samiksaha.infa.automateds2t.Mapping.Lookup;
import com.samiksaha.infa.automateds2t.Mapping.S2TRow;
import com.samiksaha.infa.automateds2t.Mapping.TableField;

/**
 * <b>Mapping.java</b>
 * This class represents a Informatica mapping and all associated properties and 
 * methods associated with it
 * 
 * @author Samik Saha
 * 
 */
public class Mapping extends SwingWorker<Void, Void> {
	
	MainWindow mainWindow;
	/**
	 * mappingNode contains the XML node for the mapping. Mapping node does not
	 * contain reusable transformations or mapplet definitions. For those use
	 * xmlDocument static variable of MainWindow
	 */
	private Node mappingNode;
	private XPath xPath;

	/**
	 * Stores current mapping name
	 */
	private String mappingName;
	private String mappingDescription;
	private int transformationCount;
	private ArrayList<String> targetTables;

	/**
	 * Contains all target instances in the mapping
	 */
	private ArrayList<String> targetInstances;

	/**
	 * Stores the target instances user selected for preparing S2T. Set by
	 * MainWindow before calling createS2T.
	 */
	private ArrayList<String> targetInstancesForS2T;

	/**
	 * Stores mapplet input ports. As mapplet input ports cannot be derived
	 * directly they are added on encountering input transformation while
	 * backtracking a mapplet in method "getLogicFromMappletTransformation"
	 */
	private ArrayList<String> mpltInPorts;
	
	
	public HashMap<String, Lookup> lookups;

	public void setTargetInstancesForS2T(ArrayList<String> targetInstancesForS2T) {
		this.targetInstancesForS2T = targetInstancesForS2T;
	}

	ArrayList<TableField> srcTblFld;

	public class SQ {
		ArrayList<String> sqQuery = new ArrayList<String>();
		ArrayList<String> sqFilter = new ArrayList<String>();
	}

	public class Lookup{
		String lkpName;
		String lkpTblName;
        String lkpQuery;
        String lkpSrcFilter;
        String lkpCondition;
        String returnField;
        Boolean connected;
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

	public class Instance {
		String name;
		boolean reusable;
		String trfName;
		String instanceType;
	}

	public Mapping(MainWindow mw, Node mappingNode) {
		this.mainWindow = mw;
		this.mappingNode = mappingNode;
		xPath = XPathFactory.newInstance().newXPath();

		try {
			mappingName = (String) xPath.compile("./@NAME").evaluate(mappingNode, XPathConstants.STRING);
			mappingDescription = (String) xPath.compile("./@DESCRIPTION").evaluate(mappingNode, XPathConstants.STRING);

		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null, ex);
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

	public ArrayList<String> getTargetTableNames() {
		return targetTables;
	}

	public int countTransformations() {
		int trfCount = 0;

		try {
			NodeList trfNodeList = (NodeList) xPath.evaluate("./INSTANCE", mappingNode, XPathConstants.NODESET);
			trfCount = trfNodeList.getLength();
		} catch (XPathExpressionException e) {
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
					"./INSTANCE[@TRANSFORMATION_TYPE='" + transformationType + "']/@NAME", mappingNode,
					XPathConstants.NODESET);
			for (int i = 0; i < instanceList.getLength(); i++) {
				instanceName = instanceList.item(i).getFirstChild().getNodeValue();
				if (!instanceNameList.contains(instanceName)) {
					instanceNameList.add(instanceName);
				}
			}
		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null, ex);
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
				targetTableName = (String) xPath
						.evaluate(
								"./INSTANCE[@NAME='" + targetInstance + "' and @TRANSFORMATION_TYPE="
										+ "'Target Definition']/@TRANSFORMATION_NAME",
								mappingNode, XPathConstants.STRING);
				if (!targetTableNames.contains(targetTableName))
					targetTableNames.add(targetTableName);
			} catch (XPathExpressionException ex) {
				Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null, ex);
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
			sqTrfNodeList = (NodeList) xPath.evaluate("./TRANSFORMATION[@TYPE='Source Qualifier']", mappingNode,
					XPathConstants.NODESET);
			for (int i = 0; i < sqTrfNodeList.getLength(); i++) {
				sqTrfNode = sqTrfNodeList.item(i);
				sqQuery = (String) xPath.evaluate("./TABLEATTRIBUTE[@NAME='Sql Query']/@VALUE", sqTrfNode,
						XPathConstants.STRING);
				sq.sqQuery.add(sqQuery);
				sqFilter = (String) xPath.evaluate("./TABLEATTRIBUTE[@NAME='Source Filter']/@VALUE", sqTrfNode,
						XPathConstants.STRING);
				sq.sqFilter.add(sqFilter);
			}
		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null, ex);
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
		
		/* Initialize Lookup array */
		lookups = new HashMap<>();

		/* Initialize source table/fields array */
		srcTblFld = new ArrayList<TableField>();
		
		ListIterator<String> iter = targetInstancesForS2T.listIterator();

		while (iter.hasNext()) {
			targetInstanceName = (String) iter.next();
			s2tTargetInstance = new ArrayList<S2TRow>();
			Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
					"Processing target instance " + targetInstanceName);

			try {
				String tgtTbl = (String) xPath
						.evaluate(
								"./INSTANCE[@NAME='" + targetInstanceName + "' and @TRANSFORMATION_TYPE="
										+ "'Target Definition']/@TRANSFORMATION_NAME",
								mappingNode, XPathConstants.STRING);

				Logger.getLogger(Mapping.class.getName()).log(Level.INFO, "Found target table: " + tgtTbl);

				/* For shortcuts get target name */
				String shortcutTgtTbl = (String) xPath.evaluate(
						"//SHORTCUT[@NAME='" + tgtTbl + "' and @OBJECTSUBTYPE=" + "'Target Definition']/@REFOBJECTNAME",
						MainWindow.xmlDocument, XPathConstants.STRING);

				if (!shortcutTgtTbl.isEmpty()) {
					tgtTbl = shortcutTgtTbl;
					Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
							"Found Shortcut object reference: " + tgtTbl);
				}

				targetFields = (NodeList) xPath.evaluate("//TARGET[@NAME='" + tgtTbl + "']/TARGETFIELD",
						MainWindow.xmlDocument, XPathConstants.NODESET);

				Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
						"No. of target fields found: " + targetFields.getLength());
				
				int nTgtFlds = targetFields.getLength();

				for (int i = 0; i < nTgtFlds; i++) {
					s2tRow = new S2TRow();
					s2tRow.tgtTbl = tgtTbl;
					targetField = targetFields.item(i);
					s2tRow.tgtFld = (String) xPath.evaluate("./@NAME", targetField, XPathConstants.STRING);
					s2tRow.tgtFldKeyType = (String) xPath.evaluate("./@KEYTYPE", targetField, XPathConstants.STRING);
					s2tRow.tgtFldNullable = (String) xPath.evaluate("./@NULLABLE", targetField, XPathConstants.STRING);
					tgtFldDataType = (String) xPath.evaluate("./@DATATYPE", targetField, XPathConstants.STRING);
					tgtFldPrec = (String) xPath.evaluate("./@PRECISION", targetField, XPathConstants.STRING);
					tgtFldScale = (String) xPath.evaluate("./@SCALE", targetField, XPathConstants.STRING);
					s2tRow.tgtFldType = tgtFldDataType + "(" + tgtFldPrec
							+ (tgtFldScale.equals("0") ? "" : "," + tgtFldScale) + ")";

					Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
							"Backtracking target field: " + s2tRow.tgtFld);

					srcTblFld.clear();

					InstanceField fromInstanceField = getFromField(targetInstanceName, s2tRow.tgtFld);
					
					s2tRow.S2TsrcTblFld.addAll(srcTblFld);
					
					if (fromInstanceField != null) {
						s2tRow.logic = getLogic(fromInstanceField);
						
						s2tRow.S2TsrcTblFld.addAll(srcTblFld);
						
						
						if (!s2tRow.tgtFld.equals(fromInstanceField.field)){				
							s2tRow.logic = s2tRow.logic + "\r\n" + s2tRow.tgtFld +"="+fromInstanceField.field;
						}
						
						/*
						 * Optimize/compact the logic removing unnecessary redundancy generating 
						 * during backtracking.
						 */
						// Remove blank lines 
						s2tRow.logic = s2tRow.logic.replaceAll("(?m)^\\s+", "");

						System.out.println(s2tRow.logic);

						String optimLogic = "";
						
						// Split lines
						String logicLines[] = s2tRow.logic.split("\\r?\\n");
						String mergedLine = "";
						String curLineSplit[];
						
						// Nothing to compact if only one line
						if (logicLines.length == 1){	
							optimLogic = s2tRow.logic;
						}
						else {
							/* Loop until the 1 line before last line so that we have one more line
							 * to look for for possible merge
							 */
							for (int j = 0; j < logicLines.length - 1; j++) {
								/* If the last line was merged take the merged line as current line */
								if (!mergedLine.isEmpty()) {
									curLineSplit = mergedLine.split("=", 2);
								} else
									curLineSplit = logicLines[j].split("=", 2);

								String nextLineSplit[] = logicLines[j + 1].split("=", 2);

								/* If the LHS of current line is equal to RHS of next line
								 * then merge the lines together
								 */
								if (nextLineSplit.length == 2 && curLineSplit[0].trim().equals(nextLineSplit[1].trim())) {
									mergedLine = nextLineSplit[0] + "=" + curLineSplit[1];
								} else if (!mergedLine.isEmpty()) {
									optimLogic = ((!optimLogic.isEmpty()) ? optimLogic + "\r\n" : "") + mergedLine;
									mergedLine = "";
								} else {
									optimLogic = ((!optimLogic.isEmpty()) ? optimLogic + "\r\n" : "") + logicLines[j];
									
									/* As we are looping 1 line less than the last, include the last line if not
									 * merged with the line before
									 */
									if (j == logicLines.length - 2) {
										optimLogic = ((!optimLogic.isEmpty()) ? optimLogic + "\r\n" : "")
												+ logicLines[j + 1];
									}
								}
							}
							
							/* If the last line was merged, it did not get a chance to be added to the
							 * optimized logic, add it now.
							 */
							if (!mergedLine.isEmpty())
								optimLogic = ((!optimLogic.isEmpty()) ? optimLogic + "\r\n" : "") + mergedLine;
						}
						
						/*
						 * Check if the logic is one line and is just source field equals to target field,
						 * then just write "Straight Move"
						 */
						if (optimLogic.split("\\r\\n").length == 1){
							String[] logicSplit = optimLogic.split("=", 2);
							if (logicSplit.length == 2 && s2tRow.S2TsrcTblFld.size()>0 &&
									logicSplit[0].trim().equals(s2tRow.S2TsrcTblFld.get(0).fldName.trim()) &&
									logicSplit[1].trim().equals(s2tRow.tgtFld.trim())){
								optimLogic = "Straight Move";
							}
						}
						
						System.out.println("Optimized\n" + optimLogic);

						s2tRow.logic = optimLogic;

						if (s2tRow.logic.equals(""))
							s2tRow.logic = "Straight Move";
					} else {
						s2tRow.logic = "";
					}

					

					s2tTargetInstance.add(s2tRow);
					// if (i>5)break;
					setProgress((int)(5+((float)i/nTgtFlds)*90));
					
				}
			} catch (XPathExpressionException ex) {
				Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null, ex);
			}
			s2tAllTargetInstances.add(s2tTargetInstance);
		}

		Logger.getLogger(Mapping.class.getName()).log(Level.INFO, s2tAllTargetInstances.get(0).get(0).tgtFld);
		setProgress(100);
		return s2tAllTargetInstances;
	}

	private InstanceField getFromField(String toInstance, String toField) {
		InstanceField instFld = new InstanceField();
		Node connectorNode;

		try {
			String xPathExpr = "./CONNECTOR[@TOFIELD='" + toField + "' and @TOINSTANCE='" + toInstance + "']";
			connectorNode = (Node) xPath.evaluate(xPathExpr, mappingNode, XPathConstants.NODE);
			if (connectorNode != null) {
				instFld.field = connectorNode.getAttributes().getNamedItem("FROMFIELD").getNodeValue();
				instFld.instanceName = connectorNode.getAttributes().getNamedItem("FROMINSTANCE").getNodeValue();
				instFld.instanceType = connectorNode.getAttributes().getNamedItem("FROMINSTANCETYPE").getNodeValue();
			}
		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null, ex);
		}
		return instFld;
	}

	private InstanceField getFromFieldForMapplet(Node mpltNode, String toFld, String toInst) {
		InstanceField frmInstFld = new InstanceField();
		try {
			frmInstFld.field = xPath.evaluate(
					"string(//CONNECTOR[@TOFIELD='" + toFld + "' and @TOINSTANCE='" + toInst + "']/@FROMFIELD)",
					mpltNode);
			frmInstFld.instanceName = xPath.evaluate(
					"string(//CONNECTOR[@TOFIELD='" + toFld + "' and @TOINSTANCE='" + toInst + "']/@FROMINSTANCE)",
					mpltNode);
			frmInstFld.instanceType = xPath.evaluate(
					"string(//CONNECTOR[@TOFIELD='" + toFld + "' and @TOINSTANCE='" + toInst + "']/@FROMINSTANCETYPE)",
					mpltNode);

		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}

		return frmInstFld;
	}

	/**
	 * Recursive function to get the logic of populating a port in an instance.
	 * Backtracks until the source definition is reached, and prepends all the
	 * 'logic' found along the way.
	 * 
	 * @param instanceField
	 * @return
	 */
	private String getLogic(InstanceField instanceField) {
		String logic = "";
		String trfLogic = "";

		if (instanceField.instanceName == null)
			return logic;

		Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
				"Getting logic for " + instanceField.instanceName + "." + instanceField.field);

		Instance instance = getInstance(instanceField.instanceName);
		Node trfNode = getTrfNode(instance);

		if (instanceField.instanceType.equals("Source Definition")) {
			Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
					instance.instanceType + " : " + instance.name + " : " + instance.trfName);
			getSourceFields(instanceField);
			return "";
		} else if (instanceField.instanceType.equals("Expression")) {
			Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
					instance.instanceType + " : " + instance.name + " : " + instance.trfName);
			trfLogic = getLogicFromEXP(trfNode, instanceField.field);
			
			//Check for any unconnected lookup in the logic
			Pattern pattern= Pattern.compile(":LKP\\.[_A-Za-z0-9]*\\(");
			Matcher matcher= pattern.matcher(trfLogic);
			while(matcher.find()){
				String lkpStr = matcher.group();
				String lkpName = lkpStr.substring(5,lkpStr.length()-1);
				if (!lookups.containsKey(lkpName)){
					getUnconnectedLookup(lkpName);
				}
			}
		} else if (instanceField.instanceType.equals("Lookup Procedure")) {
			Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
					instance.instanceType + " : " + instance.name + " : " + instance.trfName);
			trfLogic = getLogicFromConnectedLookup(trfNode, instanceField.field);
		}
		else if (instanceField.instanceType.equals("Mapplet")) {
			Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
					instance.instanceType + " : " + instance.name + " : " + instance.trfName);
			trfLogic = getLogicFromMapplet(trfNode, instanceField.field);
		} else if (instanceField.instanceType.equals("Router")){
			ArrayList<String> inPorts = getInPortsROUTER(trfNode, instanceField.field);
			if (!inPorts.isEmpty())	trfLogic = instanceField.field + "=" + inPorts.get(0);
		}

		System.out.println("trfLogic=" + trfLogic);
		logic = trfLogic;

		if (trfLogic.isEmpty())
			trfLogic = instanceField.field;

		Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
				"Looking for input ports for " + instanceField.field + " in " + instance.name);
		ArrayList<String> inPorts = getInPorts(trfNode, instance.instanceType, instanceField.field, trfLogic);

		for (int i = 0; i < inPorts.size(); i++) {
			String inPort = inPorts.get(i);
			InstanceField frmInstFld = getFromField(instanceField.instanceName, inPort);

			if (frmInstFld.instanceName == null)
				continue;
			if (!frmInstFld.field.equals(inPort)) {
				logic = inPort + "=" + frmInstFld.field + (logic.isEmpty() ? "" : "\r\n" + logic);
			}

			trfLogic = getLogic(frmInstFld);

			if (!trfLogic.isEmpty()) {
				logic = trfLogic + (logic.isEmpty() ? "" : "\r\n" + logic);
			}
		}
		return logic;
	}

	private ArrayList<String> getInPorts(Node trfNode, String trfType, String fldNm, String fldExp) {
		String trfName = trfNode.getAttributes().getNamedItem("NAME").getNodeValue();// for
																						// logging
																						// purpose
		Logger.getLogger(Mapping.class.getName()).log(Level.INFO, "Getting input ports for " + fldNm);

		// TODO - getInPorts for UNION and Mapplet

		if (trfType.equals("Router")) {
			return getInPortsROUTER(trfNode, fldNm);
		} else if (trfType.equals("Custom Transformation")) {
			return getInPortsUNION(trfNode, fldNm);
		} else if (trfType.equals("Mapplet")) {
			return mpltInPorts;
		}

		ArrayList<String> inPorts = new ArrayList<String>();
		String inPort;

		try {
			String xPathExpr = "./TRANSFORMFIELD[@PORTTYPE='INPUT/OUTPUT' or @PORTTYPE='INPUT']";
			NodeList inpPortList = (NodeList) xPath.evaluate(xPathExpr, trfNode, XPathConstants.NODESET);
			for (int i = 0; i < inpPortList.getLength(); i++) {
				Node inPortNode = inpPortList.item(i);
				inPort = inPortNode.getAttributes().getNamedItem("NAME").getNodeValue();
				Pattern pattern = Pattern.compile("\\b" + inPort + "\\b");
				Matcher matcher = pattern.matcher(fldExp);
				if (matcher.find()) {
					Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
							"Found input port: " + trfName + "." + inPort + " for " + fldNm);
					inPorts.add(inPort);
				}
			}
		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null, ex);
		}

		if (inPorts.isEmpty())
			Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
					"No input ports found in " + trfType + " " + trfName + " for " + fldNm);
		return inPorts;
	}

	private ArrayList<String> getInPortsROUTER(Node trfNode, String fldNm) {
		String trfName = trfNode.getAttributes().getNamedItem("NAME").getNodeValue();// for
																						// logging
																						// purpose
		ArrayList<String> inPorts = new ArrayList<String>();
		String inPort;
		try {
			String xPathExpr = "./TRANSFORMFIELD[@PORTTYPE='OUTPUT' and @NAME='" + fldNm + "']/@REF_FIELD";
			inPort = (String) xPath.evaluate(xPathExpr, trfNode, XPathConstants.STRING);
			Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
					"Found input port: " + trfName + "." + inPort + " for " + fldNm);
			inPorts.add(inPort);
		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null, ex);
		}
		return inPorts;
	}

	private ArrayList<String> getInPortsUNION(Node trfNode, String fldNm) {
		String trfName = trfNode.getAttributes().getNamedItem("NAME").getNodeValue();// for
																						// logging
																						// purpose
		ArrayList<String> inPorts = new ArrayList<String>();
		String inPort;
		try {
			String xPathExpr = "./FIELDDEPENDENCY[@OUTPUTFIELD='" + fldNm + "']/@INPUTFIELD";
			inPort = (String) xPath.evaluate(xPathExpr, trfNode, XPathConstants.STRING);
			Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
					"Found input port: " + trfName + "." + inPort + " for " + fldNm);
			inPorts.add(inPort);
		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null, ex);
		}
		return inPorts;
	}

	private String getLogicFromEXP(Node trfNode, String field) {
		Logger.getLogger(Mapping.class.getName()).log(Level.INFO, "Getting logic from exp for " + field);
		String logic = "";
		try {
			Node trfFldNode = (Node) xPath.evaluate("./TRANSFORMFIELD[@NAME='" + field + "']", trfNode,
					XPathConstants.NODE);
			String fldType = trfFldNode.getAttributes().getNamedItem("PORTTYPE").getNodeValue();
			System.out.println(fldType);
			if (fldType.equals("OUTPUT")) {
				String expStr = trfFldNode.getAttributes().getNamedItem("EXPRESSION").getNodeValue();
				logic = field + " = " + expStr + "\r\n";
				String varExp = getLogicFromEXPVar(trfNode, field, expStr);
				if (!varExp.isEmpty()) {
					logic = varExp + "\r\n" + logic;
				}
			}
		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null, ex);
		}
		return logic;
	}

	private String getLogicFromEXPVar(Node trfNode, String fldName, String fldExp) {
		String logic = "";
		try {
			String xPathExpr = "./TRANSFORMFIELD[@PORTTYPE='LOCAL VARIABLE' and @NAME!='" + fldName + "']";
			NodeList varPortList = (NodeList) xPath.evaluate(xPathExpr, trfNode, XPathConstants.NODESET);

			for (int i = 0; i < varPortList.getLength(); i++) {
				Node varPort = varPortList.item(i);
				String varPortName = varPort.getAttributes().getNamedItem("NAME").getNodeValue();
				Pattern pattern = Pattern.compile("\\b" + varPortName + "\\b");
				Matcher matcher = pattern.matcher(fldExp);

				if (matcher.find()) {
					String varExp = varPort.getAttributes().getNamedItem("EXPRESSION").getNodeValue();
					if (!logic.isEmpty())
						logic = varPortName + " = " + varExp + "\r\n" + logic;
					else
						logic = varPortName + " = " + varExp;

					String x = getLogicFromEXPVar(trfNode, varPortName, varExp);
					if (!x.isEmpty()) {
						logic = x + "\r\n" + logic;
					}
				}
			}
		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null, ex);
		}

		return logic;
	}

	private void getUnconnectedLookup (String lkpInstName){
		Instance lkpInst = getInstance(lkpInstName);
		if (lkpInst.name == null) return;
		Node lkpTrfNode = getTrfNode(lkpInst);
		if (lkpTrfNode == null ) return;
		
		Lookup lookup = new Lookup();
		
		try {
			lookup.lkpName = xPath.evaluate("string(//TRANSFORMATION/@NAME)", lkpTrfNode);
			lookup.lkpQuery = xPath.evaluate("string(./TABLEATTRIBUTE[@NAME='Lookup Sql Override']/@VALUE)", lkpTrfNode);
			lookup.lkpCondition = xPath.evaluate("string(./TABLEATTRIBUTE[@NAME='Lookup condition']/@VALUE)", lkpTrfNode);
			lookup.lkpTblName = xPath.evaluate("string(./TABLEATTRIBUTE[@NAME='Lookup table name']/@VALUE)", lkpTrfNode);
			lookup.lkpSrcFilter = xPath.evaluate("string(./TABLEATTRIBUTE[@NAME='Lookup Source Filter']/@VALUE)", lkpTrfNode);
			lookup.connected = false;
			
			if (!lookups.containsKey(lookup.lkpName)){
				lookups.put(lookup.lkpName, lookup);
			}
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		
	}
	
	private String getLogicFromConnectedLookup(Node trfNode, String fldName){
		
		if(!trfNode.getAttributes().getNamedItem("TYPE").getNodeValue().equals("Lookup Procedure")) return "";
		
		try {
			String portType = xPath.evaluate("string(./TRANSFORMFIELD[@NAME='"+fldName+"']/@PORTTYPE)", trfNode);
			if (portType.equals("INPUT/OUTPUT")) return "";
			
			String lkpName = xPath.evaluate("string(//TRANSFORMATION/@NAME)", trfNode);
			String sqlOverride = xPath.evaluate("string(./TABLEATTRIBUTE[@NAME='Lookup Sql Override']/@VALUE)", trfNode);
			
			if (!(sqlOverride.isEmpty() || sqlOverride == null)){
				Lookup lkp = new Lookup();
				lkp.lkpName=lkpName;
				lkp.lkpQuery=sqlOverride;
				lkp.connected=true;
				if(!lookups.containsKey(lkpName)){
					lookups.put(lkpName, lkp);
				}
			}
			
			String lkpTable = xPath.evaluate("./TABLEATTRIBUTE[@NAME='Lookup table name']/@VALUE", trfNode);
			String srcType = xPath.evaluate("./TABLEATTRIBUTE[@NAME='Source Type']/@VALUE", trfNode);
			if (srcType.equals("Flat File")){
				lkpTable = "a flat file " + lkpTable;
			}
			
			String lkpCondition = xPath.evaluate("./TABLEATTRIBUTE[@NAME='Lookup condition']/@VALUE", trfNode);
			return "Lookup" +
		            ((!sqlOverride.isEmpty())? " (" + lkpName + ")": "") +
		            " on " +
		            lkpTable +
		            ((!sqlOverride.isEmpty())? " (query provided below)": "") +
		            " with " + lkpCondition + " and return " + fldName;
		
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	
	private String getLogicFromAGG(Node trfNode, String fldName){
		NodeList groupByPorts;
		String groupByPortsCSV = "";
		String expStr;
		try {
			String portType = xPath.evaluate("string(./TRANSFORMFIELD[@NAME='" + fldName + "']/@PORTTYPE)", trfNode);
			if (portType.equals("OUTPUT")){
				//Get group by ports
				groupByPorts = (NodeList) xPath.evaluate("./TRANSFORMFIELD[@EXPRESSIONTYPE='GROUPBY']/@NAME", trfNode, XPathConstants.NODESET);
				for(int i = 0;i<groupByPorts.getLength();i++){
					groupByPortsCSV = groupByPortsCSV + groupByPorts.item(i).getNodeValue() + ",";
				}
				
				//Get the expression for the output port
				expStr = xPath.evaluate("string(./TRANSFORMFIELD[@NAME='" + fldName + "']/@EXPRESSION)", trfNode);
				
				//The expression might contain some variables, if so get the formula from those var ports also
				String logicFromVarPorts = getLogicFromAGGVar(trfNode, fldName, expStr);
				
				//Return the final logic
				return "Group by on " + groupByPortsCSV + "\r\n" +
		                ((!logicFromVarPorts.isEmpty())? logicFromVarPorts + "\r\n":"") +
		                fldName + " = " + expStr + "\r\n" + " ";
			}
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "";
	}
	
	private String getLogicFromAGGVar(Node trfNode, String fldName, String fldExp) {
		// TODO Auto-generated method stub
		String expStr;
		String varPortName;
		String aggVarLogic="";
		
		try {
			NodeList varPorts = (NodeList) xPath.evaluate("//TRANSFORMATION/TRANSFORMFIELD[@PORTTYPE='LOCAL VARIABLE']",trfNode, XPathConstants.NODESET);
			for (int i = 0; i < varPorts.getLength(); i++){
				varPortName = varPorts.item(i).getAttributes().getNamedItem("NAME").getNodeValue();
				Pattern pattern = Pattern.compile("\b"+varPortName+"\b");
				Matcher matcher = pattern.matcher(fldExp);
				if (matcher.matches()){
					expStr = varPorts.item(i).getAttributes().getNamedItem("EXPRESSION").getNodeValue();
					
					aggVarLogic = fldName + " = " + expStr;
					
					//The expression for this variable in turn may contain other variable ports.
					//Call the function recursively
					
					String x = getLogicFromAGGVar(trfNode, varPortName, expStr);
					if (!x.isEmpty() && x != null){
						aggVarLogic = x + "\r\n" +aggVarLogic;
					}
				}
				
			}
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
		
		return aggVarLogic;
	}

	private String getLogicFromMapplet(Node trfNode, String fldName) {
		InstanceField outInstFld = new InstanceField();
		Instance outInst = new Instance();

		try {
			outInstFld.field = fldName;
			outInstFld.instanceName = (String) xPath.evaluate(
					"//MAPPLET/CONNECTOR[@TOFIELD='" + fldName
							+ "' and @TOINSTANCETYPE='Output Transformation']/@TOINSTANCE",
					mappingNode, XPathConstants.STRING);
			outInstFld.instanceType = "Output Transformation";

			outInst.name = outInstFld.instanceName;
			outInst.instanceType = outInstFld.instanceType;
			outInst.trfName = xPath.evaluate(
					"string(//MAPPLET/INSTANCE[@NAME='" + outInstFld.instanceName + "']/@TRANSFORMATION_NAME)",
					mappingNode);
			System.out.println("mapplet transformation name:" + outInst.trfName);
			outInst.reusable = xPath
					.evaluate("string(//MAPPLET/INSTANCE[@NAME='" + outInstFld.instanceName + "']/@REUSABLE)",
							mappingNode)
					.equals("NO");

			mpltInPorts = new ArrayList<>();

			return getLogicFromMappletTransformation(trfNode, fldName, outInst);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "";
	}

	private Node getTrfNodeForMapplet(Node mpltNode, Instance instance) {
		try {
			String trfName = xPath.evaluate("string(//INSTANCE[@NAME='" + instance.name + "' and @TRANSFORMATION_TYPE='"
					+ instance.instanceType + "']/@TRANSFORMATION_NAME)", mpltNode);
			System.out.println("instanceName=" + instance.name + " trfName =" + trfName);

			Node trfNode = (Node) xPath.evaluate("//TRANSFORMATION[@NAME='" + trfName + "']", mpltNode,
					XPathConstants.NODE);
			return trfNode;
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private String getLogicFromMappletTransformation(Node mpltNode, String fldName, Instance inst) {
		Node trfNode;
		String trfLogic = "";
		String mpltLogic = "";
		ArrayList<String> inports = new ArrayList();
		InstanceField frmInstFld = new InstanceField();
		Instance frmInst = new Instance();
		Node prevTrfNode;

		trfNode = getTrfNodeForMapplet(mpltNode, inst);
		if (trfNode == null) {
			System.out.println("trfNode is null");
		}

		switch (inst.instanceType) {
		case "Input Transformation":
			if (!mpltInPorts.contains(fldName))
				mpltInPorts.add(fldName);
			return "";
		case "Output Transformation":
			trfLogic = "";
			break;
		case "Expression":
			trfLogic = getLogicFromEXP(trfNode, fldName);
			break;
		case "Lookup Procedure":
			trfLogic = getLogicFromConnectedLookup(trfNode, fldName);
		case "Aggregator":
			// TODO update agg method when available
			trfLogic = "";
		}

		mpltLogic = trfLogic;

		if (trfLogic.isEmpty())
			trfLogic = fldName;

		inports = getInPorts(trfNode, inst.instanceType, fldName, trfLogic);

		for (int i = 0; i < inports.size(); i++) {
			String inport = inports.get(i);
			frmInstFld = getFromFieldForMapplet(mpltNode, inport, inst.name);
			if (frmInstFld.instanceName.equals(inport)) {
				mpltLogic = inport + " = " + frmInstFld.field + (!mpltLogic.isEmpty() ? "\r" + mpltLogic : "");
			}

			if (frmInstFld.instanceName == null)
				continue;

			frmInst.name = frmInstFld.instanceName;
			frmInst.instanceType = frmInstFld.instanceType;

			prevTrfNode = getTrfNodeForMapplet(trfNode, frmInst);

			trfLogic = getLogicFromMappletTransformation(mpltNode, frmInstFld.field, frmInst);

			if (trfLogic != null) {
				mpltLogic = trfLogic + (!mpltLogic.isEmpty() ? "\r" + mpltLogic : "");
			}

		}
		return mpltLogic;
	}

	private Instance getInstance(String instanceName) {
		Instance instance = new Instance();
		try {
			String xPathExprInstance = ".//INSTANCE[@NAME='" + instanceName + "']";
			Node instanceNode = (Node) xPath.evaluate(xPathExprInstance, mappingNode, XPathConstants.NODE);

			instance.name = instanceNode.getAttributes().getNamedItem("NAME").getNodeValue();
			instance.trfName = instanceNode.getAttributes().getNamedItem("TRANSFORMATION_NAME").getNodeValue();
			instance.instanceType = instanceNode.getAttributes().getNamedItem("TRANSFORMATION_TYPE").getNodeValue();
			Node node = instanceNode.getAttributes().getNamedItem("REUSABLE");
			if (node != null)
				instance.reusable = node.getNodeValue().equals("YES") ? true : false;
		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null, ex);
		}

		return instance;
	}

	private Node getTrfNode(Instance instance) {
		NodeList folderList;
		NodeList folderContentList;

		Node trfNode = null;

		/* For shortcuts get reference name */
		try {
			String shortcutTrfName = (String) xPath.evaluate(
					"//SHORTCUT[@NAME='" + instance.trfName + "']/@REFOBJECTNAME", MainWindow.xmlDocument,
					XPathConstants.STRING);
			System.out.println("Shortcut Transformation Name" + shortcutTrfName);
			if (!shortcutTrfName.isEmpty()) {
				instance.trfName = shortcutTrfName;
			}

		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (instance.reusable) {
			folderList = MainWindow.xmlDocument.getElementsByTagName("FOLDER");
			for (int i = 0; i < folderList.getLength(); i++) {
				folderContentList = folderList.item(i).getChildNodes();
				for (int j = 0; j < folderContentList.getLength(); j++) {
					Node folderContent = folderContentList.item(j);
					if (folderContent.getNodeName().equals("#text"))
						continue;
					System.out.println(folderContent.getNodeName());
					if (folderContent.getAttributes().getNamedItem("NAME").getNodeValue().equals(instance.trfName)) {
						/*
						 * WARNING: if the XML doc contains more than one FOLDER
						 * and there are reusable(transformations) of same name
						 * in two or more folders, this will return the one
						 * found in the first folder
						 */
						trfNode = folderContent;
						System.out.println(folderContent.getAttributes().getNamedItem("NAME").getNodeValue());
						break;
					}
				}
			}

		} else {
			try {
				String xPathExpr = ".//TRANSFORMATION[@NAME='" + instance.trfName + "']";
				trfNode = (Node) xPath.evaluate(xPathExpr, mappingNode, XPathConstants.NODE);
			} catch (XPathExpressionException ex) {
				Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null, ex);
			}

		}
		return trfNode;
	}

	/***
	 * Finds the source definition field details
	 * 
	 * @param instanceField
	 */
	private void getSourceFields(InstanceField instanceField) {
		TableField srcFld = new TableField();

		Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
				"Source Definition :" + instanceField.instanceName + " Field: " + instanceField.field);

		try {
			/* Get transformation name(table name) from Instance node */
			String xPathExprTrfName = "./INSTANCE[@NAME='" + instanceField.instanceName + "']/@TRANSFORMATION_NAME";
			String trfName = (String) xPath.evaluate(xPathExprTrfName, mappingNode, XPathConstants.STRING);

			/* For shortcuts get source name */
			String shortcutSrcTbl = (String) xPath.evaluate(
					"//SHORTCUT[@NAME='" + trfName + "' and @OBJECTSUBTYPE=" + "'Source Definition']/@REFOBJECTNAME",
					MainWindow.xmlDocument, XPathConstants.STRING);

			if (!shortcutSrcTbl.isEmpty()) {
				trfName = shortcutSrcTbl;
				Logger.getLogger(Mapping.class.getName()).log(Level.INFO,
						"Found Shortcut object reference: " + trfName);
			}

			String xPathExprSrcNode = "/POWERMART/REPOSITORY/FOLDER/SOURCE[@NAME='" + trfName + "']/SOURCEFIELD[@NAME='"
					+ instanceField.field + "']";
			Node srcFldNode = (Node) xPath.evaluate(xPathExprSrcNode, MainWindow.xmlDocument, XPathConstants.NODE);

			srcFld.fldName = instanceField.field;
			srcFld.tblName = trfName;
			String srcFldDataType = (String) xPath.evaluate("./@DATATYPE", srcFldNode, XPathConstants.STRING);
			String srcFldPrec = (String) xPath.evaluate("./@PRECISION", srcFldNode, XPathConstants.STRING);
			String srcFldScale = (String) xPath.evaluate("./@SCALE", srcFldNode, XPathConstants.STRING);
			srcFld.fldType = srcFldDataType + "(" + srcFldPrec + (srcFldScale.equals("0") ? "" : "," + srcFldScale)
					+ ")";
			srcTblFld.add(srcFld);
		} catch (XPathExpressionException ex) {
			Logger.getLogger(Mapping.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	@Override
	protected Void doInBackground() throws Exception {
		setProgress(5);
		ArrayList<ArrayList<S2TRow>> s2t = createS2T();
		setProgress(100);
		mainWindow.outputToExcel(s2t);
		mainWindow.enableUserInteraction();
		return null;
	}

}
