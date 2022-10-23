package com.example.s1.model;

import java.time.DayOfWeek;

public class CONSTANT {
    public static final int[] MA_CYCLES = {6, 12, 24, 64};
    public static final int MAX_STOCKS_NUM = 5000;
    public static final int SAMPLING_INTERVAL = 3; // min
    public static final int DAYS_READ_FROM_DB = 24;
    public static final int MAX_MEM_K_ITEMS = DAYS_READ_FROM_DB*4; // 内存中各个K线的最大个数都是一样的
    public static final int MAX_MEM_TICK_DAYS = DAYS_READ_FROM_DB; // 分笔数据在内存中最大天数，它与K线最大个数不同
    public static final String STOCK_INFO_FILE_NAME = "stocks_codes.csv";
    public static final String DB_FILE_NAME = "stocks_data.dat";
    public static final String PATH = "/wcj_stock";
    public static final String URL = "http://qt.gtimg.cn";

    public static int[] SAMPLING_TIME_TABLE = createSamplingTimeTable();
    static int[] createSamplingTimeTable(){
        int[] tab = new int[CONSTANT.TICKS_A_DAY];
        int beginTime = 9*60+30+SAMPLING_INTERVAL;
        for(int i = 0; i < CONSTANT.TICKS_A_DAY/2; ++i)
            tab[i] = beginTime + i*CONSTANT.SAMPLING_INTERVAL;
        beginTime = 13*60+SAMPLING_INTERVAL;
        for(int i = 0; i < CONSTANT.TICKS_A_DAY/2; ++i)
            tab[i+CONSTANT.TICKS_A_DAY/2] = beginTime + i*CONSTANT.SAMPLING_INTERVAL;
        return tab;
    }

    public static final int TICKS_A_DAY = 240 / SAMPLING_INTERVAL;
    public static final int BATCH_SIZE = 200;

    public static WcjDateTime getNextSamplingTime(WcjDateTime currDt) {
        DayOfWeek dofw = WcjTime.toLocalDateTime(currDt.wcjDate).getDayOfWeek();
        int d , t;
        if( dofw == DayOfWeek.SATURDAY ) {
            d = currDt.wcjDate + 2 * 24*60;
            t = SAMPLING_TIME_TABLE[0];
            return new WcjDateTime(d, t);
        }
        else if( dofw == DayOfWeek.SUNDAY) {
            d = currDt.wcjDate + 24*60;
            t = SAMPLING_TIME_TABLE[0];
            return new WcjDateTime(d, t);
        }

        for( int i = 0; i < SAMPLING_TIME_TABLE.length; ++i) {
            if (currDt.wcjTime < SAMPLING_TIME_TABLE[i]) {
                t = SAMPLING_TIME_TABLE[i];
                return new WcjDateTime(currDt.wcjDate, t);
            }
        }
        return new WcjDateTime(currDt.wcjDate + 24*60, SAMPLING_TIME_TABLE[0]);
    }
}
