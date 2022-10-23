package com.example.s1.model;


public class WcjDateTime {
    public WcjDateTime(int date, int time){
        wcjDate = date;
        wcjTime = time;
    }
    public boolean equals(WcjDateTime rhs){
        return rhs.wcjTime == wcjTime && rhs.wcjDate == wcjDate;
    }
    public int wcjDate;  // date: 00:00 对齐
    public int wcjTime;  // time: 0~24*60
}
