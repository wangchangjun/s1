package com.example.s1.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public class WcjTime {

    public static int create(int y, int m, int d, int h, int M){
        LocalDateTime dt = LocalDateTime.of(y,m,d,h,M,0);
        return (int)(dt.toEpochSecond( OffsetDateTime.now().getOffset() ) / 60L);
    }
    public static LocalDateTime toLocalDateTime(int wcjDateTime){    // wcjTime to LocalDateTime
        long localSecond = wcjDateTime + OffsetDateTime.now().getOffset().getTotalSeconds()/60L;  // overflow caught later
        long localEpochDay = Math.floorDiv(localSecond, 24*60);
        int secsOfDay = Math.floorMod(localSecond, 24*60);
        LocalDate date = LocalDate.ofEpochDay(localEpochDay);
        LocalTime time = LocalTime.ofSecondOfDay(secsOfDay*60);
        return  LocalDateTime.of(date, time);
    }
    public static int toMinutesOfADay(int wcjDateTime){
        LocalDateTime dt = toLocalDateTime(wcjDateTime);
        return (int) (dt.toLocalTime().toSecondOfDay()/60L);
    }
    // get part of time -------------------------------------------------------------------------------
//    public static int getDayOfYear(int wcjDateTime){ // OK
//        return toLocalDateTime(wcjDateTime).getDayOfYear();
//    }
//    public static int getDayOfMonth(int wcjTime) {
//        return toLocalDateTime(wcjTime).getDayOfMonth();
//    }
//    public static int getHour(int wcjDateTime){ // OK
//        return toLocalDateTime(wcjDateTime).getHour();
//    }
//    public static int getMinute(int wcjDateTime){
//        return toLocalDateTime(wcjDateTime).getMinute();
//    }
    // return (days_of_difference * 24 * 60)
    public static int cmpDay(int wcjDateTime1, int wcjDateTime2){ // OK 比较两个时间的所在的“天”， 谁大谁小， 大返>0, 小 返 < 0
        LocalDateTime d1 = toLocalDateTime(wcjDateTime1);
        LocalDateTime d2 = toLocalDateTime(wcjDateTime2);
        int dm1 = wcjDateTime1-toMinutesOfADay(wcjDateTime1);
        int dm2 = wcjDateTime2-toMinutesOfADay(wcjDateTime2);
        return dm1 - dm2;
    }
    // return (hours_of_difference * 60)
    static int hi(int wcjTimeOnly){
        int mi1 = 0;
        if( wcjTimeOnly <= 10*60+30 ) mi1 = 0;
        else if( wcjTimeOnly <= 11*60+30) mi1 = 1;
        else if( wcjTimeOnly <= 14*60) mi1 = 2;
        else mi1 = 3;
        return mi1;
    }
    public static int cmpHour (int wcjDateTime1, int wcjDateTime2 ){
        int m1 = WcjTime.toMinutesOfADay(wcjDateTime1);
        int day1 = wcjDateTime1 - m1;
        int m2 = WcjTime.toMinutesOfADay(wcjDateTime2);
        int day2 = wcjDateTime1 - m2;

        if (day1 > day2)
            return 1;
        int mi1 = hi(m1);
        int mi2 = hi(m2);
        return mi1 - mi2;
    }

    public static int tickIndexOfADay(int samplingTime){

        if( samplingTime > 13*60) {
            int ind = (samplingTime - 13 * 60 - CONSTANT.SAMPLING_INTERVAL) / CONSTANT.SAMPLING_INTERVAL + 2 * 60 / CONSTANT.SAMPLING_INTERVAL;
            return ind >= CONSTANT.TICKS_A_DAY ? CONSTANT.TICKS_A_DAY - 1 : ind;
        }
        else {
            int ind =(samplingTime - 9 * 60 - 30 - CONSTANT.SAMPLING_INTERVAL) / CONSTANT.SAMPLING_INTERVAL;
            return ind < 0? 0:ind;
        }
    }

    // get now time -------------------------------------------------------------------
    public static int nowDate(){ // ok // 对齐到00:00的分钟数
        LocalDateTime ldt = LocalDateTime.now();
        return (int)((ldt.toEpochSecond( OffsetDateTime.now().getOffset() ) - ldt.toLocalTime().toSecondOfDay()) /60L);
    }
    public static int nowDateTime() { // ok
        return (int)(LocalDateTime.now().toEpochSecond( OffsetDateTime.now().getOffset() )/60L);
    }
    public static int nowTime() { // ok
        return (int) (LocalTime.now().toSecondOfDay()/60L);
    }
    public static LocalTime localTime(){
        return LocalTime.now();
    }
    public static LocalDate localDate(){
        return LocalDate.now();
    }


    // formate time ---------------------------------------------------------------------------
    public static String toString(int wcjDateTime){
        return toString(wcjDateTime, TimeFmt.YMD_HM);
    }

    public static String toString_forTimeOnly(int wcjTime) {
        return String.format("%02d:%02d", Math.floorDiv(wcjTime, 60), Math.floorMod(wcjTime, 60));
    }
    public enum TimeFmt{
        YMD_HM, MD_HM, YMD, HM;
        public static int index(TimeFmt fmt){
            switch (fmt){
                case YMD_HM: return 0;
                case MD_HM: return 1;
                case YMD: return 2;
                case HM: return 3;
            }
            return 0;
        }
    }

    public static String toString(int wcjDateTime, TimeFmt fmt ){
        LocalDateTime ldt = toLocalDateTime(wcjDateTime);
        switch (fmt) {
            case YMD: // date
                return String.format("%4d-%02d-%02d", ldt.getYear(), ldt.getMonth().getValue());
            case HM: // time
                return String.format("%02d:%02d", ldt.getHour(), ldt.getMinute());
            case YMD_HM: // date_time
                return String.format("%4d-%02d-%02d %02d:%02d", ldt.getYear(), ldt.getMonth().getValue(), ldt.getDayOfMonth(), ldt.getHour(), ldt.getMinute());
            case MD_HM:
                return String.format("%02d-%02d %02d:%02d", ldt.getMonth().getValue(), ldt.getDayOfMonth(), ldt.getHour(), ldt.getMinute());
        }
        return null;
    }
}
