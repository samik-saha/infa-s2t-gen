package com.samiksaha.infa.automateds2t;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import com.samiksaha.infa.automateds2t.Mapping.Lookup;
import com.samiksaha.infa.automateds2t.Mapping.S2TForTargetInstance;
import com.samiksaha.infa.automateds2t.Mapping.S2TRow;

public class S2TGenerator extends SwingWorker<Void, String> {
	MainWindow mainWindow;
	ArrayList<String> mappingList;
	ExcelOutput xlOutput;
	Logger logger;

	public S2TGenerator(MainWindow mainWindow, ArrayList<String> mappingList, ExcelOutput xlOutput){
		this.mappingList = mappingList;
		this.xlOutput = xlOutput;
		this.mainWindow = mainWindow;
		logger = Logger.getLogger(S2TGenerator.class.getName());
	}
	
	@Override
	protected Void doInBackground() throws Exception {
		setProgress(5);
		for(int i = 0; i < mappingList.size(); i++){
			String mappingName = mappingList.get(i);
			Mapping mapping = MainWindow.mappingObjectList.get(mappingName);
			if (mapping == null){
				mapping = new Mapping(mainWindow, mainWindow.mappingNodeNamedList.get(mappingName));
			}
			if (mapping.getTargetInstanceList() == null){
				mapping.loadMappingDetails();
			}
			
			ArrayList<S2TForTargetInstance> s2t = mapping.createS2T();
			outputToExcel(mappingName, mapping.getSQQuery(),mapping.getSourceTableNames(),mapping.lookups,s2t);
			
		}
		xlOutput.close();
		return null;
	}
	
	@Override
	protected void done() {
		super.done();
		if (isCancelled()){
			mainWindow.setStatusMessage("Cancelled S2T generation");
		} else{
			mainWindow.setStatusMessage("Done");
		}
		mainWindow.enableUserInteraction();
	}
	
	public void outputToExcel(String mappingName,
			Mapping.SQ srcQueries,
			ArrayList<String> sourceTables,
			HashMap<String, Lookup> lookups,
			ArrayList<S2TForTargetInstance> s2t) {
		xlOutput.createWorksheet(mappingName);
		logger.log(Level.INFO, "In method outputToExcel");
		xlOutput.addBlankRow();
		boolean isSourceQueryPresent = false;
		xlOutput.addHeader("Source");
		for (int i = 0; i < srcQueries.sqQuery.size(); i++) {
			if (!srcQueries.sqQuery.get(i).isEmpty()) {
				isSourceQueryPresent = true;
				xlOutput.addQuery(srcQueries.sqQuery.get(i));
				xlOutput.addBlankRow();
			}
		}

		for (int i = 0; i < srcQueries.sqFilter.size(); i++) {
			if (!(srcQueries.sqFilter.get(i).trim().isEmpty() || srcQueries.sqFilter.get(i) == null)) {
				isSourceQueryPresent = true;
				xlOutput.writeCell("Source Filter:", 3);
				xlOutput.addQuery(srcQueries.sqFilter.get(i));
			}
		}

		if (isSourceQueryPresent == false) {
			for (int i = 0; i < sourceTables.size(); i++) {
				xlOutput.writeCell(sourceTables.get(i), 3);
			}
		}

		xlOutput.addBlankRow();

		ListIterator<S2TForTargetInstance> iter = s2t.listIterator();
		while (iter.hasNext()) {
			S2TForTargetInstance s2tForTargetInstance = iter.next();
			logger.log(Level.INFO, "Writing mapping for target instance " + s2tForTargetInstance.targetInstanceName);
			xlOutput.addSectionHeader("Field level mapping for: " + s2tForTargetInstance.targetInstanceName);
			xlOutput.addFieldMappingsHeader();
			ArrayList<S2TRow> s2tRows = s2tForTargetInstance.s2tRows;
			logger.log(Level.INFO, "No. of rows: " + s2tForTargetInstance.s2tRows.size());
			ListIterator<S2TRow> iter1 = s2tRows.listIterator();
			while (iter1.hasNext()) {
				S2TRow s2tRow = (S2TRow) iter1.next();
				xlOutput.addFieldMappingRow(s2tRow.S2TsrcTblFld, s2tRow.tgtTbl, s2tRow.tgtFld, s2tRow.logic,
						s2tRow.tgtFldType, s2tRow.tgtFldNullable, s2tRow.tgtFldKeyType);
				/*
				 * System.out.println(s2tRow.tgtTbl + s2tRow.tgtFld +
				 * s2tRow.logic + s2tRow.tgtFldType + s2tRow.tgtFldNullable +
				 * s2tRow.tgtFldKeyType+" source:");
				 */

			}

		}
		xlOutput.addBlankRow();
		xlOutput.addLookupDetails(lookups);
	}

	static void updateProgress(){
		
	}

}
