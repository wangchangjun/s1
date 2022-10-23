package com.example.s1.model;

public class TickData {
    public float cp;
    public float accVol;
    public static final int SIZE = 4 * 2;
    public TickData(float cp_, float accVol_) {
        cp = cp_;
        accVol = accVol_;
    }
}