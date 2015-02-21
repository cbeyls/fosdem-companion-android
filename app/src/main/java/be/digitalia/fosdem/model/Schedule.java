package be.digitalia.fosdem.model;

import be.digitalia.fosdem.db.DatabaseHelper;

/**
 * Created by Abhishek on 20/02/15.
 */
public class Schedule {

    private String title;
    private String time;
    private String date;
    private String information;
    private int id;

    public Schedule(String title, String time, String date, String information, int id) {
        this.title = title;
        this.time = time;
        this.date = date;
        this.information = information;
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getTime() {
        return time;
    }

    public String getDate() {
        return date;
    }

    public String getInformation() {
        return information;
    }

    public int getId() {
        return id;
    }

    public String generateSqlQuery() {
        String query = String.format("INSERT INTO %s VALUES (%d, '%s', '%s', '%s', '%s');", DatabaseHelper.TABLE_NAME_SCHEDULE, id, title, information, time, date);
        return query;
    }


}
