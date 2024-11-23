package schedule_sks;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DatabaseReader {
    private final String url;
    private final String user;
    private final String password;

    public DatabaseReader (String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public List<Timetable> GetDataFromDatabase() throws SQLException{
        List<Timetable> timetable = new ArrayList<>();
        
        String sqlQuery = "SELECT * FROM schedule";

        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
                ResultSet set = statement.executeQuery();
                System.out.println(set);

                while (set.next()) {
                    String subject = set.getString("subject");
                    String teacher = set.getString("teacher_name");
                    String dayString = set.getString("day");
                    String group_of_students = set.getString("group_of_students");
                    int time_of_lesson = set.getInt("time_of_lesson");
                    String classroom = set.getString("classroom_number");
                    String week_type_of_lesson = set.getString("week_type");
                    String form_of_studying = set.getString("form_of_studying");

                    DayOfWeek day = DayOfWeek.valueOf(dayString);

                    Numerator weekType;
                    switch (week_type_of_lesson.toLowerCase()) {
                        case "чисельник":
                           weekType = Numerator.Chyselnik;
                           break;
                        case "знаменник":
                           weekType = Numerator.Znamennyk;
                           break;
                        default:
                           weekType = Numerator.BOTH;
                    }

                    FormOfStudying form = FormOfStudying.NONE;
                    switch (form_of_studying.toLowerCase()) {
                        case "offline":
                           form = FormOfStudying.OFFLINE;
                           break;
                        case "online":
                           form = FormOfStudying.ONLINE;
                           break;
                    }

                    Timetable entry = new Timetable(day, time_of_lesson, subject, teacher, classroom, group_of_students, form, weekType);
                    timetable.add(entry);
                }
             }
        return timetable;
    }

}
