package com.example.s1.model;


public class DbDayK {
    public DbDayK(float op, float hp, float lp, float cp, float vol , float precp){
        this.hp = hp;
        this.lp = lp;
        this.op = op;
        this.cp = cp;
        this.vol = vol;
        this.precp = precp;
    }
    public DbDayK(DbDayK dk){
        hp = dk.hp;
        lp = dk.lp;
        cp = dk.cp;
        op = dk.op;
        vol = dk.vol;
        precp = dk.precp;
    }
    public static final int SIZE = 4*6;
    public float hp, lp, cp, op, vol, precp;
}