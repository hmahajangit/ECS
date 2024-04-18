package com.siemens.nextwork.admin.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;

public class TestSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

	private final List<String> rows = new ArrayList<>();
    private final StringBuilder sb = new StringBuilder();

    public void startSheet(String sheetName) {
    	// START SHEET
   
    }

    @Override
    public void endSheet() {
    	if(sb.length() > 0) sb.setLength(sb.length() - 1);
    	sb.setLength(0);
    }

    @Override
    public void startRow(int rowNum) {
        sb.append("{");
    }

    @Override
    public void endRow(int rowNum) {
    	if(sb.length() > 0) sb.setLength(sb.length() - 1);
    	if(sb.length() > 0) {
    		sb.append("}");
    		rows.add(sb.toString());
    	}else {
    		rows.add("BLANK");
    	}
    	sb.setLength(0);
    }

    @Override
    public void cell(String cellReference, String formattedValue, XSSFComment comment) {
    	if(formattedValue.isBlank()) formattedValue = "æææ";
    	 sb.append("\"").append(cellReference).append("\" : \"").append(formattedValue).append("\",");
    }

    @Override
    public void headerFooter(String text, boolean isHeader, String tagName) {
    	// Header Footer
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    public List<String> getRows(){
    	return rows;
    }
}
