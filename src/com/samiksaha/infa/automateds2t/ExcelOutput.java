/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samiksaha.infa.automateds2t;

/**
 *
 * @author Samik
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.samiksaha.infa.automateds2t.Mapping.TableField;

public class ExcelOutput {

    Workbook wb;
    Sheet ws;
    int usedRow;
    int usedCol;
    Font captionFont, headerFont;
    CellStyle captionStyle, headerStyle;
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
        captionFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        
        captionStyle = wb.createCellStyle();
        captionStyle.setFont(captionFont);
        captionStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        captionStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        
        headerFont = wb.createFont();
        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        
        headerStyle = wb.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.BROWN.getIndex());
        headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        

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
        Row row = ws.createRow(usedRow+1);
        Cell cell = row.createCell(0);
        cell.setCellStyle(headerStyle);
        cell.setCellValue(header);
        ws.addMergedRegion(new CellRangeAddress(usedRow+1,usedRow+1,0, 3));
        
        usedRow+=1;
    }

    public void addFieldMappingsHeader(){
        Cell cell;
        Row row = ws.createRow(usedRow++);
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
