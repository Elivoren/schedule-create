package schedule_sks;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils.Null;
import org.apache.poi.ss.formula.functions.Column;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTRowImpl;
import org.apache.poi.xssf.usermodel.TextDirection;

public class ScheduleForSKS {
  static final String[] Alldays = {"Понеділок", "Вівторок", "Середа", "Четвер", "П'ятниця"};
  public static void main(String[] args) throws IOException, SQLException{
    DatabaseReader reader = new DatabaseReader("jdbc:mysql://localhost/sks", "root", "root");
    List<Timetable> timetable = reader.GetDataFromDatabase();
    DataFormatter formatter = new DataFormatter();
    List<String> lecturers = timetable.stream()
       .map(Timetable::getTeacher)
       .distinct() // Ensure unique teacher names
       .toList(); 
    try(Workbook wb = new XSSFWorkbook()){
      Sheet sh = wb.createSheet("Лист 1");
      Row r1 = sh.createRow(0);
      Cell days = r1.createCell(0);
      Cell lessons = r1.createCell(1);
      CellStyle cellStyle = wb.createCellStyle();
      cellStyle.setRotation((short) 90);
      cellStyle.setAlignment(HorizontalAlignment.CENTER);
      days.setCellStyle(cellStyle);
      days.setCellValue(new XSSFRichTextString("Дні"));
      lessons.setCellStyle(cellStyle);
      lessons.setCellValue("Пари");
      sh.addMergedRegion(new CellRangeAddress(0, 2, 0, 0));
      sh.addMergedRegion(new CellRangeAddress(0, 2, 1, 1));
      for(int i=1; i <= Alldays.length; i++){
        Row r = sh.createRow(3+14*(i-1));
        Cell xcell = r.createCell(0);
        xcell.setCellStyle(cellStyle);
        xcell.setCellValue(new XSSFRichTextString(Alldays[i-1]));
        sh.addMergedRegion(new CellRangeAddress(3+14*(i-1), 3+14*(i-1)+13, 0, 0));
      }
      int columnIndex = 2;
      for (String teacher : lecturers) {
        Cell teacherCell = r1.createCell(columnIndex++);
        teacherCell.setCellValue(new XSSFRichTextString(teacher));
        for (Timetable table: timetable){
            for (int i = 2; i < r1.getLastCellNum(); i++) {
                if (table.getTeacher().equals(formatter.formatCellValue(r1.getCell(i)))){
                    int dayIndex = 3;
                    switch (table.getDay()) {
                        case M:
                            break;
                        case T:
                            dayIndex += 14*1;
                            break; 
                        case W:
                            dayIndex += 14*2;
                            break;
                        case S:
                            dayIndex += 14*3;
                            break;
                        case F:
                            dayIndex += 14*4;
                            break;
                        default:
                            System.out.println("Unexpected day: " + table.getDay());
                            continue;
                    }
                    dayIndex += (table.getTime_of_lesson() - 1) * 2;
                    if (table.getWeek_type_of_lesson() == Numerator.Chyselnik) {
                        Row dayr = sh.getRow(dayIndex);
                        if (dayr == null){
                            dayr = sh.createRow(dayIndex);
                        }
                        Cell dayc = dayr.createCell(i);
                        dayc.setCellValue(table.getForm_of_studying() + " " + table.getSubject() + " " + table.getGroup_of_students());
                        System.out.println(dayc.getRow());
                    }
                    else if (table.getWeek_type_of_lesson() == Numerator.Znamennyk) {
                        Row dayr = sh.getRow(dayIndex + 1);
                        if (dayr == null){
                            dayr = sh.createRow(dayIndex);
                        }
                        Cell dayc = dayr.createCell(i);
                        dayc.setCellValue(table.getForm_of_studying() + " " + table.getSubject() + " " + table.getGroup_of_students());
                        System.out.println(dayc.getRow());
                    }
                    else {
                        Row rday = sh.getRow(dayIndex);
                        if (rday == null){
                            rday = sh.createRow(dayIndex);
                        }
                        Cell dayc = rday.createCell(i);
                        dayc.setCellValue(table.getForm_of_studying() + " " + table.getSubject() + " " + table.getGroup_of_students());
                        System.out.println(dayc.getStringCellValue());
                        boolean isAlreadyMerged = false;
                        for (int j = 0; j < sh.getNumMergedRegions(); j++) {
                            CellRangeAddress region = sh.getMergedRegion(j);
                            if (region.isInRange(dayIndex, teacherCell.getColumnIndex())) {
                                isAlreadyMerged = true;
                                break;
                            }
                        
                        }
                        if (!isAlreadyMerged) {
                            if (dayIndex % 2 != 0) {
                                sh.addMergedRegion(new CellRangeAddress(dayIndex, dayIndex + 1, teacherCell.getColumnIndex(), teacherCell.getColumnIndex()));
                            } else {
                                sh.addMergedRegion(new CellRangeAddress(dayIndex - 1, dayIndex, teacherCell.getColumnIndex(), teacherCell.getColumnIndex()));
                            }
                        }
                    }
                }
            }
          }
      }
      try(FileOutputStream fout = new FileOutputStream("ScheduleSKS.xlsx")){
        wb.write(fout); 
      }
    }
  }

  private static void print_data (Timetable table) {
    System.out.println(table.getTeacher() + table.getClassroom() + table.getDay() + table.getForm_of_studying() + table.getSubject() + table.getWeek_type_of_lesson());
  }
}


