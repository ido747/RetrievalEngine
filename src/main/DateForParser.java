package main;

import java.util.HashMap;

public class DateForParser {

    private HashMap<String,String> date;

    public DateForParser() {

        date = new HashMap<>();
        initDate();

    }

        public void initDate(){

            date.put("January", "01");
            date.put("JAN", "01");
            date.put("JANUARY","01");
            date.put("Jan", "01");

            date.put("February","02");
            date.put("FEBRUARY", "02");
            date.put("Feb", "02");
            date.put("FEB", "02");

            date.put("Mar", "03");
            date.put("March", "03");
            date.put("MARCH", "03");
            date.put("MAR","03");

            date.put("Apr", "04");
            date.put("April", "04");
            date.put("APRIL", "04");
            date.put("APR", "04");

            date.put("MAY","05");
            date.put("May","05");

            date.put("June","06");
            date.put("Jun","06");
            date.put("JUN","06");
            date.put("JUNE","06");

            date.put("July","07");
            date.put("JULY","07");
            date.put("Jul","07");
            date.put("JUL","07");

            date.put("August","08");
            date.put("AUGUST","08");
            date.put("Aug","08");
            date.put("AUG","08");

            date.put("SEPTEMBER","09");
            date.put("September","09");
            date.put("Sep","09");
            date.put("SEP","09");

            date.put("October","10");
            date.put("OCTOBER","10");
            date.put("Oct","10");
            date.put("OCT","10");

            date.put("November","11");
            date.put("NOV","11");
            date.put("Nov","11");
            date.put("NOVEMBER","11");

            date.put("Dec","12");
            date.put("December","12");
            date.put("DECEMBER","12");
            date.put("DEC","12");
    }

    public HashMap<String, String> getDate() {
        return date;
    }
}
