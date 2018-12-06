/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samiksaha.infa.automateds2t;

/**
 *
 * @author Samik Saha (samiksaha88@gmail.com)
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.samiksaha.infa.automateds2t.Mapping.Lookup;
import com.samiksaha.infa.automateds2t.Mapping.TableField;

public class ExcelOutput {

    Workbook wb;
    Sheet ws;
    int usedRow;
    int usedCol;
    Font captionFont, headerFont;
    CellStyle captionStyle, headerStyle, subHeaderStyle;
    FileOutputStream fileOut;
    Logger logger;

    public ExcelOutput(File file) {
    	logger = Logger.getLogger(ExcelOutput.class.getName());
        try {
            wb = new XSSFWorkbook();
            fileOut = new FileOutputStream(file);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ExcelOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void createWorksheet(String sheetName) {
        String safeName = WorkbookUtil.createSafeSheetName(sheetName);
        ws = wb.createSheet(safeName);

        //Configure fonts and styles
        captionFont = wb.createFont();
        captionFont.setFontHeightInPoints((short) 14);
        captionFont.setBold(true);
        
        captionStyle = wb.createCellStyle();
        captionStyle.setFont(captionFont);
        captionStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        captionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        
        headerStyle = wb.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.BROWN.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        subHeaderStyle = wb.createCellStyle();
        subHeaderStyle.setFont(headerFont);
        subHeaderStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        subHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        

        Row row = ws.createRow((short) 0);
        Cell cell = row.createCell((short) 0);
        cell.setCellStyle(captionStyle);
        cell.setCellValue("Mapping Document for " + sheetName);
        CellRangeAddress region = new CellRangeAddress(0, 1, 0, 10);
        ws.addMergedRegion(region);

        ws.setColumnWidth(3, 50*256);

        //Set used row and column
        usedRow = 2;
        usedCol = 2;
    }

    public void addBlankRow() {
        usedRow += 1;
    }

    public void addHeader(String header) {
        Row row = ws.createRow(usedRow);
        Cell cell = row.createCell(0);
        cell.setCellStyle(headerStyle);
        cell.setCellValue(header);
        ws.addMergedRegion(new CellRangeAddress(usedRow,usedRow,0, 3));
        
        usedRow+=1;
    }
    
    public void addSectionHeader(String header) {
        Row row = ws.createRow(usedRow);
        Cell cell = row.createCell(0);
        CellStyle cs = wb.createCellStyle();
        cs.setFillForegroundColor(IndexedColors.GOLD.index);
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cell.setCellStyle(cs);
        cell.setCellValue(header);
        ws.addMergedRegion(new CellRangeAddress(usedRow,usedRow,0, 4));
        
        usedRow+=1;
    }
    
    public void addSubHeader(String header) {
        Row row = ws.createRow(usedRow);
        Cell cell = row.createCell(0);
        cell.setCellStyle(subHeaderStyle);
        cell.setCellValue(header);
        ws.addMergedRegion(new CellRangeAddress(usedRow,usedRow,0, 3));
        
        usedRow+=1;
    }

    public void addFieldMappingsHeader(){
        Cell cell;
        Row row = ws.createRow(usedRow);
        cell = row.createCell(0);
        cell.setCellStyle(headerStyle);
        cell.setCellValue("Source Table");
        cell = row.createCell(1);
        cell.setCellStyle(headerStyle);
        cell.setCellValue("Source Field");
        cell = row.createCell(2);
        cell.setCellStyle(headerStyle);
        cell.setCellValue("Source Data Type");
        cell = row.createCell(3);
        cell.setCellStyle(headerStyle);
        cell.setCellValue("Transformation/Conversion Rules");
        cell = row.createCell(4);
        cell.setCellStyle(headerStyle);
        cell.setCellValue("Target Table Name");
        cell = row.createCell(5);
        cell.setCellStyle(headerStyle);
        cell.setCellValue("Target Field Name");
        cell = row.createCell(6);
        cell.setCellStyle(headerStyle);
        cell.setCellValue("Target Data Type/Len");
        cell = row.createCell(7);
        cell.setCellStyle(headerStyle);
        cell.setCellValue("Nullable");
        cell = row.createCell(8);
        cell.setCellStyle(headerStyle);
        cell.setCellValue("Key Type");
        usedRow+=1;
    }
    
    public void addFieldMappingRow(ArrayList<TableField> srcTblFlds,
            String tgtTbl,
            String tgtFld,
            String logic,
            String tgtFldType,
            String tgtFldNullable,
            String tgtFldKeyType){
        Mapping.TableField srcTblFld;
        
        int startRow = usedRow;
        
        Row row = ws.createRow(startRow);
        Cell cell = row.createCell(0);

        for(int i =0;i<srcTblFlds.size();i++){
            srcTblFld = (Mapping.TableField)srcTblFlds.get(i);
            logger.log(Level.INFO, "Writing source field for " + tgtFld + ": "+srcTblFld.fldName);
            row = ws.createRow(usedRow);
            cell = row.createCell(0);
            cell.setCellValue(srcTblFld.tblName);
            cell = row.createCell(1);
            cell.setCellValue(srcTblFld.fldName);
            cell = row.createCell(2);
            cell.setCellValue(srcTblFld.fldType);
            usedRow+=1; 
        }
        
        if (srcTblFlds.size()>0) usedRow--;
        //if (startRow==usedRow+1) usedRow+=1;
        
       

        CellStyle style = wb.createCellStyle();
        style.setWrapText(true); 
        row = ws.getRow(startRow);
        cell = row.createCell(3);
        cell.setCellStyle(style);
        cell.setCellValue(logic);
        
        logger.log(Level.INFO,"Creating merged region: start row:"+startRow+" end row:"+usedRow);

        ws.addMergedRegion(new CellRangeAddress(startRow,usedRow,3,3));
        cell = row.createCell(4);
        cell.setCellValue(tgtTbl);
        ws.addMergedRegion(new CellRangeAddress(startRow,usedRow,4,4));
        cell = row.createCell(5);
        cell.setCellValue(tgtFld);
        ws.addMergedRegion(new CellRangeAddress(startRow,usedRow,5,5));
        cell = row.createCell(6);
        cell.setCellValue(tgtFldType);
        ws.addMergedRegion(new CellRangeAddress(startRow,usedRow,6,6));
        cell = row.createCell(7);
        cell.setCellValue(tgtFldNullable);
        ws.addMergedRegion(new CellRangeAddress(startRow,usedRow,7,7));
        cell = row.createCell(8);
        cell.setCellValue(tgtFldKeyType);
        ws.addMergedRegion(new CellRangeAddress(startRow,usedRow,8,8));
        
        usedRow++;
        
    }
    
    public void addQuery(String query){
    	int nLines = query.split("\\n").length;
    	addSubHeader("Query");
    	Row row = ws.createRow(usedRow);
    	Cell cell= row.createCell(0);
    	CellStyle cs = wb.createCellStyle();
    	cs.setWrapText(true);
    	cell.setCellStyle(cs);
    	cell.setCellValue(query);
    	ws.addMergedRegion(new CellRangeAddress(usedRow,usedRow+nLines,0,3));
    	usedRow+=nLines+1;
    }
    
    public void writeCell(String text, int width){
    	Row row = ws.createRow(usedRow);
    	Cell cell = row.createCell(0);
    	cell.setCellValue(text);
    	ws.addMergedRegion(new CellRangeAddress(usedRow,usedRow,0,width));
    	
    	usedRow+=1;
    }
    
    public void addLookupDetails(HashMap<String, Lookup> lookups){
    	logger.log(Level.INFO,"Writing lookups: "+lookups.values().size()+ " lookups found");
    	Lookup lookup;
    	if(lookups.values().size()>0){
    		addSectionHeader("Lookup Details");
    	}
    	
    	Iterator<Lookup> iter = lookups.values().iterator();
    	while(iter.hasNext()){
    		lookup = iter.next();
    		logger.log(Level.INFO, "Writing lookup details for "+lookup.lkpName );
    		addHeader(lookup.lkpName);
    		if (lookup.lkpCondition != null && !lookup.lkpCondition.isEmpty()){
    			addSubHeader("Lookup Condition");
        		writeCell(lookup.lkpCondition,3);
    		}
    		if(lookup.lkpQuery != null && !lookup.lkpQuery.isEmpty()){
    			addQuery(lookup.lkpQuery);
    		}else{
    			addSubHeader("Lookup table");
    			writeCell(lookup.lkpTblName,3);
    		}
    		addBlankRow();
    	}
    }
    
    
    public void close() {
        try {
        	ws.autoSizeColumn(0);
        	ws.autoSizeColumn(1);
        	ws.autoSizeColumn(2);
        	ws.autoSizeColumn(4);
        	ws.autoSizeColumn(5);
        	ws.autoSizeColumn(6);
        	ws.autoSizeColumn(7);
        	ws.autoSizeColumn(8);
            wb.write(fileOut);
            fileOut.close();
        } catch (IOException ex) {
            Logger.getLogger(ExcelOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
