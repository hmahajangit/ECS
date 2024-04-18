package com.siemens.nextwork.admin.excel;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.siemens.nextwork.admin.dto.DevPathExcelDTO;

public class DevPathExcelExporter {
	private XSSFWorkbook workbook;
	private XSSFSheet sheet;
	private List<DevPathExcelDTO> devPathExcelDTOList;
	int rowId=0;

	public DevPathExcelExporter(List<DevPathExcelDTO> devPathExcelDTO,int rowId, XSSFSheet sheet,XSSFWorkbook workbook) {
		this.devPathExcelDTOList = devPathExcelDTO;
		this.rowId=rowId;
		this.workbook=workbook;
		this.sheet=sheet;
	}


	public void writeHeaderLine() {

		rowId+=5;
		Row row = sheet.createRow(rowId);

		CellStyle style = workbook.createCellStyle();
		XSSFFont font = workbook.createFont();
		font.setBold(true);
		style.setFont(font);
		createCell(row, 1, "Transformation Paths details", style); 
		sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 15));
		rowId+=2;
		row = sheet.createRow(rowId);
		createCell(row, 1, " ", style); 
		sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 15));
		row = sheet.createRow(rowId++);
		createCell(row, 1, "Transformation Paths", style(workbook)); 
		sheet.addMergedRegion(new CellRangeAddress(rowId-1, rowId-1, 1, 6));
		
		row = sheet.createRow(rowId++);
		createCell(row, 1, "Status Quo Job Profile", style);   
		createCell(row, 2, "Grip Code Status Quo Job profile", style);       
		createCell(row, 3, "Future job profile", style);    
		createCell(row, 4, "Grip Code Future Job profile", style);
		createCell(row, 5, "Measure", style);
		createCell(row, 6, "Affected HCs", style);


		writeDataLines(rowId);

	}

	private void createCell(Row row, int columnCount, Object value, CellStyle style) {
		Cell cell = row.createCell(columnCount);
		if (value instanceof Integer) {
			cell.setCellValue((Integer) value);
		} else if (value instanceof Boolean) {
			cell.setCellValue((Boolean) value);
		}else {
			cell.setCellValue((String) value);
		}
		cell.setCellStyle(style);
	}
	

	private void writeDataLines(int rowCount) {

		CellStyle style = workbook.createCellStyle();
		XSSFFont font = workbook.createFont();
		font.setFontHeight(12);
		style.setFont(font);
		for (DevPathExcelDTO devPathExcelDTO : devPathExcelDTOList) {
			Row row = sheet.createRow(rowCount++);
			createCell(row, 1, devPathExcelDTO.getStatusQuoJobProfile(), style);
			createCell(row, 2, devPathExcelDTO.getGripCodeStatusQuoJobProfile(), style);
			createCell(row, 3, devPathExcelDTO.getFutureStateJobProfile(), style);
			createCell(row, 4, devPathExcelDTO.getGripCodeFutureStateJobProfile(), style);
			createCell(row, 5, devPathExcelDTO.getMeasure(), style);
			createCell(row, 6, devPathExcelDTO.getAssignedHC(), style);
			
		}

	}

	public void export(HttpServletResponse response) throws IOException {
		writeHeaderLine();
		

		ServletOutputStream outputStream = response.getOutputStream();
		workbook.write(outputStream);
		workbook.close();
		outputStream.close();
	}
	
	CellStyle style(XSSFWorkbook workbook)
	{
		XSSFCellStyle styleBold = workbook.createCellStyle();
		XSSFFont fontBold = workbook.createFont();
		fontBold.setBold(true);
		styleBold.setFont(fontBold);
		  styleBold.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
		  styleBold.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return styleBold;
		
	}
	CellStyle styleColourLight(XSSFWorkbook workbook)
	{
		XSSFCellStyle styleBold = workbook.createCellStyle();
		XSSFFont fontBold = workbook.createFont();
		fontBold.setBold(true);
		styleBold.setFont(fontBold);
		
		  styleBold.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE1.getIndex());
		  styleBold.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return styleBold;
		
	}
	
}