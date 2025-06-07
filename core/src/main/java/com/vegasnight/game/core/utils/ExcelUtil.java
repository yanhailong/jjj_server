package com.vegasnight.game.core.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/6 17:34
 */
public class ExcelUtil {
    public static Map<String, List<String[]>> readExcelFile(File file) {
        Map<String, List<String[]>> sheetValues = new HashMap<>();
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            XSSFWorkbook workbook = new XSSFWorkbook(fileInputStream);
            int size = workbook.getNumberOfSheets();
            for (int i = 0; i < size; i++) {
                XSSFSheet sheet = workbook.getSheetAt(i);
                //标签名
                String sheetName = sheet.getSheetName();
                //获取行数
                int rowLen = sheet.getLastRowNum();
                List<String[]> rowValues = new ArrayList<>();
                for (int rowNum = sheet.getFirstRowNum(); rowNum <= rowLen; rowNum++) {
                    XSSFRow nameRow = sheet.getRow(rowNum);
                    if (nameRow == null) {
                        System.err.println("Excel 读取警告,有空行出现，file=" + file.getName() + ",sheet=" + sheetName + ",rowNum=" + (rowNum + 1));
                        rowValues.add(new String[0]);
                        continue;
                    }
                    rowValues.add(readRow(nameRow));
                }
                sheetValues.put(sheetName, rowValues);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (Exception e) {
                }
            }
        }
        return sheetValues;
    }

    public static String[] readRow(XSSFRow row) {
        int lastCellNum = row.getLastCellNum();
        List<String> values = new ArrayList<>();
        for (int cellNum = row.getFirstCellNum(); cellNum < lastCellNum; cellNum++) {
            try {
                XSSFCell cell = row.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String name = getCellValue(cell);
                values.add(name);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("" + cellNum);
            }
        }
        return values.toArray(new String[0]);
    }

    public static String getCellValue(Cell cell) {
        String value;

        switch (cell.getCellType()) {
            case STRING: // 字符串
                value = cell.getStringCellValue();
                break;
            case BOOLEAN: // Boolean
                value = cell.getBooleanCellValue() + "";
                break;
            case NUMERIC: // 公式
                value = cell.getNumericCellValue() + "";
                break;
            case BLANK: // 空值
            default:
                value = "";
                break;
        }
        return value;
    }
}
