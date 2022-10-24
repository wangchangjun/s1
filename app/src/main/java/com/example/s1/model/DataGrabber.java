package com.example.s1.model;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;



public class DataGrabber {
    public StocksInfo stockInfo;

    public  DataGrabber(){
        memStocksData = new MemData[CONSTANT.MAX_STOCKS_NUM];
        for(int i = 0; i < CONSTANT.MAX_STOCKS_NUM; ++i)
            memStocksData[i] = new MemData();

        // 1. load stocks' codes and names
        String infoFilePath = CONSTANT.PATH + CONSTANT.STOCK_INFO_FILE_NAME;
        if( new File(infoFilePath).exists() ){
            try {
                stockInfo = new StocksInfo(infoFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            StocksInfo.downloadInfo();
        }
        assert stockInfo.codeNames.size() > 0;

        // 2. load data from database file
        String filePath = CONSTANT.PATH + CONSTANT.DB_FILE_NAME;
        if(new File(filePath).exists()){
            try {
                database = new Database(CONSTANT.PATH + CONSTANT.DB_FILE_NAME);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Database.ReadValue[][] retVal = database.read(CONSTANT.DAYS_READ_FROM_DB);
            if(retVal == null)
                return;
            //int num = database.capacity > stockInfo.codeNames.size() ? stockInfo.codeNames.size():database.capacity;
            for(int si = 0; si < database.capacity; ++si) {
                MemData data = new MemData();
                data.add(retVal, si);
                memStocksData[si] = data;
            }
        }
        else{
            // 1. create a database file
            try {
                database = new Database(filePath, CONSTANT.MAX_STOCKS_NUM );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void save(int nowDateTime) throws Database.WrongTimeStamp {
        database.write(nowDateTime, stockInfo.codeNames.size(), new Database.WriteCb() {
            @Override
            public TickData[] getTickData(int stocki) {
                assert memStocksData[stocki] != null;
                if(memStocksData[stocki].tickData.size() == 0) return null;
                return memStocksData[stocki].tickData.get_n(-1);
            }

            @Override
            public DbDayK getDayK(int stocki) {
                assert memStocksData[stocki] != null;
                if(memStocksData[stocki].dayK.size() == 0) return null;
                return memStocksData[stocki].dayK.get_n(-1).dbDayk;
            }

            @Override
            public boolean hasNewData(int si){
                return memStocksData[si].hasNewData;
            }
        });
    }

    public Database database;
    public MemData[] memStocksData ;
    StringBuilder reqCodesBuffer = new StringBuilder(CONSTANT.MAX_STOCKS_NUM*(10+1) + 2);
    void makeReqText(WcjDateTime dt, int beginIndex, int endIndex ){
        reqCodesBuffer.setLength(0);
        reqCodesBuffer.append("q=");
        for(int si = beginIndex; si < endIndex; ++si){
            boolean fuquan = needFuquan( memStocksData[si], dt );
            if (!fuquan)
                reqCodesBuffer.append("s_");
            reqCodesBuffer.append(stockInfo.codeNames.get(si)[0]);
            reqCodesBuffer.append(",");
        }
    }

    private static boolean needFuquan(MemData data, WcjDateTime dt){ //ok
        return data.dayK.size() == 0 || WcjTime.cmpDay(data.dayK.get_n(-1).timeStamp, dt.wcjDate) < 0 || (dt.wcjTime-CONSTANT.SAMPLING_INTERVAL)%30==0 ;
    }

    private static String request(String url, String params){
        PrintWriter out = null;
        BufferedReader in = null;
        StringBuilder result = new StringBuilder();
        try {
            URL reqUrl = new URL(url);
            // 建立连接
            URLConnection conn = reqUrl.openConnection();
            //设置请求头
            conn.setRequestProperty("Content-Type", "text/html;charset=utf-8");
            conn.setDoOutput(true); //设置为true才可以使用conn.getOutputStream().write()
            conn.setDoInput(true); //才可以使用conn.getInputStream().read();

            //写入参数
            out = new PrintWriter(conn.getOutputStream());
            out.print(params);//设置参数，可以直接写&参数，也可以直接传入拼接好的
            out.flush();// flush输出流的缓冲

            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "gbk"));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }finally {// 使用finally块来关闭输出流、输入流
            try {
                if (out != null)                     out.close();
                if (in != null)                     in.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result.toString();
    }

/* 长格式-------------------------------------
 0: 未知
 1: 股票名字
 2: 股票代码
 3: 当前价格
 4: 昨收
 5: 今开
 6: 成交量（手）
 7: 外盘
 8: 内盘
 9: 买一
10: 买一量（手）
11-18: 买二 买五
19: 卖一
20: 卖一量
21-28: 卖二 卖五
29: 最近逐笔成交
30: 时间
31: 涨跌
32: 涨跌%
33: 最高
34: 最低
35: 价格/成交量（手）/成交额
36: 成交量（手）
37: 成交额（万）
38: 换手率
39: 市盈率
40:
41: 最高
42: 最低
43: 振幅
44: 流通市值
45: 总市值
46: 市净率
47: 涨停价
48: 跌停价
*/
/* 短格式----------------------------------------
 0: 未知
 1: 股票名称
 2: 股票代码
 3: 当前价格
 4: 涨跌
 5: 涨跌%
 6: 成交量（手）
 7: 成交额（万）
 8:
 9: 总市值
 */

    private int parseWcjTime(String text){ // 20220107154118
        return WcjTime.create(
                Integer.parseInt(text.substring(0,4)),
                Integer.parseInt(text.substring(4,6)),
                Integer.parseInt(text.substring(6,8)),
                Integer.parseInt(text.substring(8,10)),
                Integer.parseInt(text.substring(10,12))
        );
    }


    // return samplingTime
    boolean download_debug(WcjDateTime dt){
        boolean longData = ((dt.wcjTime-3) % 30 == 0);
        int tick_index = WcjTime.tickIndexOfADay(dt.wcjTime);
        dataDownload.clear();
        float v = 10.0f;
        for(int i = 0; i < stockInfo.codeNames.size(); ++i, v += 1.0f) {
            if (longData) { // 长格式
                int timeStamp = dt.wcjTime + dt.wcjDate;
                int mins = WcjTime.toMinutesOfADay(timeStamp);
                if (mins > 11 * 60 + 30 && mins < 13 * 60)
                    timeStamp -= mins - (11 * 60 + 30);
                else if (mins > 15 * 60)
                    timeStamp -= mins - 15 * 60;
                float cp = v + tick_index*2; //Float.parseFloat(items[3]);
                float precp = v-1.0f; //Float.parseFloat(items[4]);
                float op = v+1.0f+ tick_index*2; //Float.parseFloat(items[5]);
                float hp = v+1.0f+ tick_index*2; //Float.parseFloat(items[33]);
                float lp = v-1.0f+ tick_index*2; //Float.parseFloat(items[34]);
                float vol = v*10000+tick_index*v*10; // Float.parseFloat(items[36]);
                dataDownload.add(new DataDownloadItem(stockInfo.codeNames.get(i)[0], new MemDayK(new DbDayK(op, hp, lp, cp, vol, precp), timeStamp)));
            }
            else{ //if (items.length >= 10) { // 短格式
                float cp = v + tick_index*2; //Float.parseFloat(items[3]);
                float accVol = v*100+tick_index*v*10;// Float.parseFloat(items[6]);
                if (cp == 0.0f || accVol == 0.0f) continue;
                dataDownload.add(new DataDownloadItem(stockInfo.codeNames.get(i)[0], new TickData(cp, accVol)));
            }
        }
        return true; //CONSTANT.SAMPLING_TIME_TABLE[tick_index];
   }

    // 无效股票数据，直接丢弃没有加入list
    boolean download(int samplingDate, int tryTimes){//ok
        int tryCount = 0;
        String text = null;
        do{
            text = request(CONSTANT.URL, reqCodesBuffer.toString());
            if(++ tryCount >= tryTimes) return  false;
        } while( text == null);
        String[] stockText = text.split(";");
        dataDownload.clear();
        for(String s: stockText){
            String[] items = s.split("~");
            if(items.length >= 49){ // 长格式
                int timeStamp = parseWcjTime(items[30]);
                int mins = WcjTime.toMinutesOfADay(timeStamp);
                if( WcjTime.cmpDay(timeStamp, samplingDate ) < 0 || mins < 9*60+30 )
                    continue;
                if(  mins > 11*60+30 && mins < 13*60  )
                    timeStamp -= mins-11*60+30;
                else if( mins > 15*60 )
                    timeStamp -= mins -  15*60;
                float cp = Float.parseFloat(items[3]);
                if(cp == 0.0f) continue; // cp == 0 : 不存在的股票
                float precp = Float.parseFloat(items[4]);
                float op = Float.parseFloat(items[5]);
                float hp = Float.parseFloat(items[33]);
                float lp = Float.parseFloat(items[34]);
                float vol = Float.parseFloat(items[36]);
                dataDownload.add(new DataDownloadItem(items[0].substring(2, 10), new MemDayK(new DbDayK(op, hp, lp, cp, vol, precp), timeStamp)));
            }
            else if(items.length >= 10){ // 短格式
                float cp = Float.parseFloat(items[3]);
                float accVol = Float.parseFloat(items[6]);
                //float totalMemory = Float.parseFloat(items[9]); // 总市值 亿元
                if(cp == 0.0f || accVol == 0.0f) continue;
                dataDownload.add(new DataDownloadItem(items[0].substring(4, 12),new TickData(cp, accVol)));
            }
        }
        return true;
    }


    public static boolean downloadInfo(ArrayList<String[]> cns, ArrayList<StocksInfo.OtherInfo> info, ArrayList<String> possibleCodes, int tryTimes){//ok
        StringBuilder sb = new StringBuilder(CONSTANT.BATCH_SIZE*10);

        for(int i = 0; i < possibleCodes.size(); i+=CONSTANT.BATCH_SIZE){
            sb.setLength(0);
            sb.append("q=");
            int end = i+CONSTANT.BATCH_SIZE;
            end = end > possibleCodes.size()? possibleCodes.size(): end;
            for( int j = i; j < end; ++j){
                sb.append(possibleCodes.get(j));sb.append(",");
            }
            int tryCount = 0;
            String text = null;
            do{
                text = request(CONSTANT.URL, sb.toString());
                if(++ tryCount >= tryTimes) return  false;
            } while( text == null);
            String[] stockText = text.split(";");
            for(String s: stockText){
                String[] items = s.split("~");
                if(items.length >= 49){ // 长格式
                    float cp = Float.parseFloat(items[3]);
                    if(cp == 0.0f) continue; // cp == 0 : 不存在的股票
                    float currentMoney = 0.0f;
                    float totalMoney = 0.0f;
                    float PE = 0.0f;
                    if(!items[44].isEmpty())
                        currentMoney = Float.parseFloat(items[44]); // 流通市值
                    if(!items[45].isEmpty())
                        totalMoney = Float.parseFloat(items[45]); // 总市值
                    if(!items[39].isEmpty())
                    PE = Float.parseFloat(items[39]);
                    cns.add(new String[] {items[0].substring(2, 10), items[1]} );
                    info.add(new StocksInfo.OtherInfo(currentMoney, totalMoney, PE));
                }
            }
        }

        return true;
    }

    static class DataDownloadItem {
        DataDownloadItem(String code_, MemDayK memDayK_){
            code = code_;
            memDayK = memDayK_;
            daykValid = true;
        }
        DataDownloadItem(String code_, TickData tickData_){
            code = code_;
            tickData = tickData_;
            daykValid = false;
        }
        boolean daykValid;
        String code;
        TickData tickData;
        MemDayK memDayK;
    }

    ArrayList<DataDownloadItem> dataDownload = new ArrayList<>(CONSTANT.MAX_STOCKS_NUM);

    public void grab(WcjDateTime nowDateTime, ArrayList<Algo> algos) { //ok
        for(int i = 0; i < stockInfo.codeNames.size(); i += CONSTANT.BATCH_SIZE){
            int endIndex = i + CONSTANT.BATCH_SIZE;
            endIndex = endIndex > stockInfo.codeNames.size() ? stockInfo.codeNames.size(): endIndex;
            makeReqText(nowDateTime, i, endIndex);
            boolean ok;
            ok = download(nowDateTime.wcjDate, 3);  // return dataDownload
            //ok = download_debug(nowDateTime);  // return dataDownload
            if(!ok)               continue;
            for(DataDownloadItem ddl: dataDownload){
                Integer index = stockInfo.CODE_INDEX.get(ddl.code);
                MemData siData = memStocksData[index];
                assert siData != null;
                // 1. add new data to memory-data
                siData.add(ddl, nowDateTime);
                // 2. run algos
                for(Algo a: algos) {
                    a.iterate(stockInfo, memStocksData, index);
                }
            }
        }
    }
}
