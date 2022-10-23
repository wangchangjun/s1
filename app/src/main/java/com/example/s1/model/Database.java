package com.example.s1.model;


import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Database {
    static final int MAX_DAYS = 256;
    static final int PER_STOCK_PER_DAY_SIZE = TickData.SIZE* CONSTANT.TICKS_A_DAY + DbDayK.SIZE;
    // code name is put in another file.
    static final int TOTAL_ADDR = 0, TOTAL_LEN = 4 ;                     // total num of stocks
    static final int HEAD_ADDR = TOTAL_ADDR + TOTAL_LEN, HEAD_LEN = 4*2;  // head, tail,
    static final int TIME_ADDR = HEAD_ADDR + HEAD_LEN, TIME_LEN = 4*MAX_DAYS; // time[]  4*256
    static final int DATA_ADDR = TIME_ADDR + TIME_LEN;
    int ALL_STOCKS_PER_DAY_SIZE() { return 4+ PER_STOCK_PER_DAY_SIZE *capacity; }
    int ALL_STOCKS_ALL_DAYS_SIZE() {     return ALL_STOCKS_PER_DAY_SIZE() * MAX_DAYS;    }


    int capacity; // 从数据库文件读出来的
    public int getCapacity(){return capacity;}

    MappedByteBuffer headBuf, timeBuf, dataBuf;
    RandomAccessFile file;

    // 无Db文件，第一次创建，调用此ctor
    public Database(String path, int capacity_) throws IOException {
        capacity = capacity_;
        file = new RandomAccessFile(path,"rw" );
        file.setLength(0);
        file.writeInt(capacity);
        file.writeInt(0);
        file.writeInt(0);
        FileChannel ch = file.getChannel();
        headBuf = ch.map(FileChannel.MapMode.READ_WRITE, HEAD_ADDR, HEAD_LEN);
        timeBuf = ch.map(FileChannel.MapMode.READ_WRITE, TIME_ADDR, TIME_LEN);
        dataBuf = ch.map(FileChannel.MapMode.READ_WRITE, DATA_ADDR, ALL_STOCKS_ALL_DAYS_SIZE());
        loop = new Loop(MAX_DAYS, 0,0);
    }

    //  存在Db文件，打开它，调用此ctor
    public Database(String path) throws IOException {
        file = new RandomAccessFile(path,"rw" );
        capacity = file.readInt();
        FileChannel ch = file.getChannel();
        headBuf = ch.map(FileChannel.MapMode.READ_WRITE, HEAD_ADDR, HEAD_LEN);
        timeBuf = ch.map(FileChannel.MapMode.READ_WRITE, TIME_ADDR, TIME_LEN);
        dataBuf = ch.map(FileChannel.MapMode.READ_WRITE, DATA_ADDR, ALL_STOCKS_ALL_DAYS_SIZE());
        int h = headBuf.getInt(0);
        int t = headBuf.getInt(1);
        loop = new Loop(MAX_DAYS, h, t);
    }

    public interface  WriteCb{
        TickData[] getTickData(int stocki);
        DbDayK getDayK(int stocki);
        boolean hasNewData(int si);
    }

    public class WrongTimeStamp extends Exception{
        public String toString() {
            return "wrong time stamp";
        }
    }
    public void write(int latestSamplingDateTime/*最后一个采样时间对应的datetime*/, int totalStocks, WriteCb writeCb) throws WrongTimeStamp {
        int index = -1;
        if(loop.size() > 0) { // 非空数据库
            int tailIndex = loop.indexOf_n(-1);
            int tailTime = timeBuf.getInt( timePosOf(tailIndex) );
            int dif = WcjTime.cmpDay(latestSamplingDateTime, tailTime );
            if ( dif == 0 )
                index = tailIndex;
            else if(dif < 0)
                throw new WrongTimeStamp();
        }
        if(index == -1) {
            index = loop.push();
            headBuf.putInt(0, loop.head);
            headBuf.putInt(1, loop.tail);
            timeBuf.putInt(timePosOf(index), latestSamplingDateTime);
        }

        int pos = dataPosOf(index);
        dataBuf.position(pos);
        dataBuf.putInt(totalStocks);
        pos += 4; // 越过totalStocks变量，之后才是数据
        int upBound = totalStocks < capacity? totalStocks: capacity;
        for(int si  = 0; si < upBound; ++si){
            if( !writeCb.hasNewData(si) )
                continue;
            DbDayK dk = writeCb.getDayK(si);
            if (dk == null || dk.op <= 0.0f  )
                continue;
            dataBuf.position(pos + si * PER_STOCK_PER_DAY_SIZE);
            dataBuf.putFloat(dk.op);
            dataBuf.putFloat(dk.hp);
            dataBuf.putFloat(dk.lp);
            dataBuf.putFloat(dk.cp);
            dataBuf.putFloat(dk.vol);
            dataBuf.putFloat(dk.precp);

            TickData[] td = writeCb.getTickData(si);
            for(int i = 0; i < CONSTANT.TICKS_A_DAY; ++i) {
                if(td[i] ==  null || td[i].cp <= 0.0f) {
                    dataBuf.putFloat(0.0f);
                    dataBuf.putFloat(0.0f);
                }
                else {
                    dataBuf.putFloat(td[i].cp);
                    dataBuf.putFloat(td[i].accVol);
                }
            }
        }
    }

    // indexs: 要读的股票在CODE_NAME[]中的索引组成的数组
    public static class ReadValue{
        public ReadValue(DbDayK dk_, TickData[] td_, int dateTime){
            dk = dk_;
            td = td_;
            latestDateTime = dateTime;
        }
        DbDayK dk;
        TickData[] td; // td[tick_i]
        int latestDateTime;
    }

    //ReadValue[day_i][si]
    public  ReadValue[][] read( int days) {
        int s = loop.size();
        if(s == 0) return null;
        days = days <= s? days: s;
        int startIndex = loop.indexOf_n(-days);
        int dataPos = dataPosOf(startIndex);
        int timePos = timePosOf(startIndex);
        ReadValue[][] retValue = new ReadValue[days][capacity];

        for(int di = 0; di < days; ++di, timePos += 4, dataPos += ALL_STOCKS_PER_DAY_SIZE()) {
            int timeStamp = timeBuf.getInt(timePos);
            dataBuf.position(dataPos);
            int totalStocks = dataBuf.getInt();
            for (int si = 0; si < totalStocks; ++si) {
                dataBuf.position( dataPos + 4 + PER_STOCK_PER_DAY_SIZE *si );
                float op = dataBuf.getFloat();
                if (op <= 0.0f)  continue;
                DbDayK dk = new DbDayK(
                        op,
                        dataBuf.getFloat(),
                        dataBuf.getFloat(),
                        dataBuf.getFloat(),
                        dataBuf.getFloat(),
                        dataBuf.getFloat()
                );
                int t = 0;
                TickData[] td = new TickData[CONSTANT.TICKS_A_DAY];
                for(int tdi = 0; tdi < CONSTANT.TICKS_A_DAY; ++tdi){
                    float cp = dataBuf.getFloat();
                    //System.out.println(cp);
                    float accVol = dataBuf.getFloat();
                    if(cp <= 0.0f)
                        continue;
                    td[tdi] = new TickData(cp, accVol);
                    t = CONSTANT.SAMPLING_TIME_TABLE[tdi];
                }
                retValue[di][si] = new ReadValue(dk, td, timeStamp-WcjTime.toMinutesOfADay(timeStamp)+t);
            }
        }
        return  retValue;
    }

    public void close() throws IOException {
        file.close();
    }

    private int timePosOf(int index){
        return index * 4 ;
    }
    private int dataPosOf(int index){
        return index * ALL_STOCKS_PER_DAY_SIZE();
    }

    int findIndex(int wcjTime){
        int len = loop.size();
        for(int i = len-1; i >= 0; --i){
            int index = loop.indexOf(i);
            int ti = timeBuf.getInt(timePosOf(index));
            if (ti <= wcjTime)        return index;
        }
        return -1;
    }
    Loop loop;
    private static class Loop{
        Loop(int capacity_, int head_, int tail_){
            capacity = capacity_;
            tail = tail_;
            head = head_;
        }

        int push(){
            int orgTail = tail;
            tail = (tail + 1)%capacity;
            if (tail == head) head = (head+1)%capacity;
            return orgTail;
        }

        int indexOf(int i){
            return (head + i)%capacity;
        }
        int indexOf_n(int i){
            return (tail + i + capacity) % capacity;
        }
        int size() {
            return (tail - head)%capacity;
        }
        int capacity;
        int tail, head;
    }
    public static boolean writeText(String str) {
        String filepath = Environment.getExternalStorageDirectory() + CONSTANT.PATH;
        File file = new File(filepath);
        if (!file.exists())
            file.mkdirs();
        file = new File(filepath + CONSTANT.DB_FILE_NAME);

        try {
            if (!file.exists())
                file.createNewFile();
            RandomAccessFile raf = new RandomAccessFile(file,"rw");
            raf.seek(file.length());
            raf.write(str.getBytes());
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}

