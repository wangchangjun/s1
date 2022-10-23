package com.example.s1.model;


import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * usage:
 * if( stockInfoFile NOT exist) StocksInfo.createCodeNameFile(path, name);
 * StocksInfo si = new StocksInfo(path, fileName);
 *
 * int index = si.CODE_INDEX.get("sh600001");
 * for(String[] cn: si.codeNames)
 *  cn[0] ...
 *
 */




public class StocksInfo {

    public static final String[] ZHISHU_ICODES = {
            "sh000001",
            "sh000002",
            "sh000003",
            "sh000008",
            "sh000009",
            "sh000010",
            "sh000011",
            "sh000012",
            "sh000016",
            "sh000017",
            "sh000300",

            "sz399001",
            "sz399002",
            "sz399003",
            "sz399004",
            "sz399005",
            "sz399006",
            "sz399100",
            "sz399101",
            "sz399106",
            "sz399107",
            "sz399108",
            "sz399333",
            "sz399606",
    };

    public static final String[] ZHISHU_NAMES = {
            "上证指数" ,
            "Ａ股指数" ,
            "Ｂ股指数" ,
            "综合指数" ,
            "上证380" ,
            "上证180" ,
            "基金指数" ,
            "国债指数" ,
            "上证50" ,
            "新综指" ,
            "沪深300" ,
            "深证成指" ,
            "深成指R" ,
            "成份Ｂ指" ,
            "深证100R" ,
            "中小板指" ,
            "创业板指" ,
            "新 指 数" ,
            "中小板综" ,
            "深证综指" ,
            "深证Ａ指" ,
            "深证Ｂ指" ,
            "中小板R" ,
            "创业板R"};


    public static ArrayList<String> makePossibleCodes(){
        int icode;
        ArrayList<String> sb = new ArrayList<>(7100);
        for ( icode = 0; icode <= 2999; ++icode) {
            sb.add (String.format("sz%06d", icode));
        }
        for ( icode = 300000; icode <= 300999; ++icode){
            sb.add(String.format("sz%06d", icode));
        }
        for ( icode = 600000; icode <= 601999; ++icode){
            sb.add(String.format("sh%06d", icode));
        }
        for ( icode = 603000; icode <= 603999; ++icode){
            sb.add(String.format("sh%06d", icode));
        }
        return sb;
    }


    public static class OtherInfo{
        OtherInfo(float c, float t, float pe){
            currentMoney = c;
            totalMoney = t;
            PE = pe;
        }
        float currentMoney; // 流通市值
        float totalMoney; //  总市值
        float PE;
    }

    public static  void downloadInfo(){
        ArrayList<String> pc = StocksInfo.makePossibleCodes();
        ArrayList<String[]> cns = new ArrayList<>(CONSTANT.MAX_STOCKS_NUM);
        ArrayList<OtherInfo> info = new ArrayList<>(CONSTANT.MAX_STOCKS_NUM);
        if(!DataGrabber.downloadInfo(cns, info, pc, 3))
            return;
        try {
            StocksInfo.save( cns, info, CONSTANT.PATH+CONSTANT.STOCK_INFO_FILE_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save(ArrayList<String[]> codeNames, ArrayList<OtherInfo> otherInfo, String path) throws IOException {
        String str = System.getProperty("line.separator");
        BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "gbk"));
        int i = 0;
        for(String[] s: codeNames){
            fw.write(s[0]);
            fw.write(",");
            fw.write(s[1]);
            fw.write(",");
            fw.write(Float.toString(otherInfo.get(i).currentMoney));
            fw.write(",");
            fw.write(Float.toString(otherInfo.get(i).totalMoney));
            fw.write(",");
            fw.write(Float.toString(otherInfo.get(i).PE));
            fw.write(str);
            ++i;
        }
        fw.close();
    }

    public StocksInfo(String path) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path), "gbk"));
        String str;
        while ((str = in.readLine()) != null) {
            String[] s = str.split(",");
            if(s.length < 2) continue;
            codeNames.add(s);
        }
        in.close();
        int i = 0;
        for (String[] c: codeNames) {
            CODE_INDEX.put(c[0], i++);
        }
    }

    private ArrayList<String[]> load(String path){
        return null;
    }
    public  ArrayList<String[]> codeNames = new ArrayList<>(CONSTANT.MAX_STOCKS_NUM);
    public HashMap<String, Integer> CODE_INDEX = new HashMap<>(CONSTANT.MAX_STOCKS_NUM);
}
