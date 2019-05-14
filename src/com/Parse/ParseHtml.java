package com.Parse;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


/**
 * @AUTHOR : QYL
 * @DATE : 2018/11/6
 * @TIME : 9:41
 * @VERSION : 3.1
 * @DESC :Parse the HTML and Store database
 */
public class ParseHtml {
    public static Set ExistId(){
        Set<String> idSet = new HashSet<>();
        Connection connection = MyConnection.getConn();
        PreparedStatement preparedStatement = null;
        String sql = "SELECT id from system_overview;";
        try{
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next())
                idSet.add(rs.getString(1));
        }catch (Exception e) {
            e.printStackTrace();
        }
        return idSet;
    }
    public static void main(String[] args) throws SQLException {
        Set<String> idSet = new HashSet<>();
        idSet = ExistId();
        int flag1, flag2;// 用于判断页面类型
        int flag_NC = 1; //用于判断页面是否有效
        Document document = null;
        List<String> htmlAll = new ArrayList<String>();
        String html2 = "https://www.spec.org/power_ssj2008/results/power_ssj2008.html";
        try {
            document = Jsoup.connect(html2).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Elements hrefs = document.select("a[href]");
        for (Element elem : hrefs) {                        //找出此页面中所有html的链接，存入htmlAll
            if (elem.attr("abs:href").endsWith(".html")) {
                htmlAll.add(elem.attr("abs:href"));
            }
        }
        Connection connection = MyConnection.getConn(); //数据库建立链接
        PreparedStatement preparedStatement = null;
        //      Document document = null;
        for (int p = 1; p < htmlAll.size(); p++) {
            String html = htmlAll.get(p);
            //System.out.println(html);
            String id = html.substring(67, 81);//8张表用的ID
            id = id.replaceAll("-", "");
            if(idSet.contains(id)) //如果数据库中包含了此条记录，则跳过
                continue;
            String str;//转义用
            String sql = null; //存sql语句
            int x = 0, y = 0, z = 0; //x记列，y记行，z记List中的位数
            int[] arr = new int[12];//	记录表的位置
            try {
                document = Jsoup.connect(html).get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Element body = document.body();
            Elements tables = body.select("table");
            Elements uls = body.select("ul");
            Elements jugle_type = body.select("#aggregateSutData"); //判断页面是哪一种类型，一共有三种类型的页面
            Elements flag = jugle_type.select("table");     //判断页面是哪一种类型
            if (flag.size() == 0) {
                flag1 = 1;//没有#aggregateSutData
            } else {
                flag1 = 0;//有#aggregateSutData
            }
            jugle_type = body.select("#resultsSummary");// 用id号判断页面是否NC
            flag = jugle_type.select("table");
            if (flag.size() == 0) {
                flag2 = 1;//没有#resultsSummary
            } else {
                flag2 = 0;//有#resultsSummary
            }
            if (flag1 == 1 && flag2 == 0) {
                arr = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};//有效页面第一类
                flag_NC = 1;
            }
            if (flag1 == 0 && flag2 == 0) {
                arr = new int[]{0, 1, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};//有效页面第二类
                flag_NC = 1;
            }
            if (flag1 == 1 && flag2 == 1) {
                arr = new int[]{0, 1, 1, 2, 3, 4, 5, 6, 7, 8, 10, 11};//无效页面第一类
                flag_NC = 0;
            }
            if (flag1 == 0 && flag2 == 1) {
                arr = new int[]{0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};//无效页面第二类
                flag_NC = 0;
            }
            Elements trs = tables.get(arr[0]).select("tr");
            List<String> tdStr = new ArrayList<String>();  //java List 存数据
            tdStr.add(id);
            for (int i = 0; i < trs.size(); i++) {            /*系统概述*/
                Elements tds = trs.get(i).select("td");
                for (int j = 0; j < tds.size(); j++) {
                    String td = tds.get(j).text();
                    System.out.println(td);
                    td = td.replaceAll("\'", "\\\\'");//转义单引号
                    tdStr.add(td);
                }
            }
            sql = "INSERT INTO pastehtml.system_overview  (id,Machine_Type,SPECpower_ssj2008,Test_Sponsor,SPEC_License,Test_Method,Tested_By,Test_Location,Test_Date,Hardware_Availability,Software_Availability,Publication,System_Source,System_Designation,Power_Provisioning) VALUES  ('" + tdStr.get(0) + "','" + tdStr.get(1) + "','" + tdStr.get(2) + "','" + tdStr.get(4) + "','" + tdStr.get(6) + "','" + tdStr.get(8) + "','" + tdStr.get(10) + "','" + tdStr.get(12) + "','" + tdStr.get(14) + "','" + tdStr.get(16) + "','" + tdStr.get(18) + "','" + tdStr.get(20) + "','" + tdStr.get(22) + "','" + tdStr.get(24) + "','" + tdStr.get(26) + "')";
            System.out.println(sql);
            preparedStatement = connection.prepareStatement(sql); //运行SQL语句
            preparedStatement.executeUpdate();
            System.out.println("1-----------------------------------------------------------------");
            if (flag_NC == 1) {    //NC界面没有Benchmak Results Summary
                List<String> tdStr2 = new ArrayList<String>();
                trs = tables.get(arr[1]).select("tr");            /*Benchmak Results Summary*/
                tdStr2.add(id);
                double flagPower = 1; //算EP用的，每张表的第一行power保存下来
                double maxPower = 0;
                double minPowerGap = 0;
                double sumPowerGap = 1;
                for (int i = 0; i < trs.size(); i++) {
                    Elements tds = trs.get(i).select("td");
                    for (int j = 0; j < tds.size(); j++) {
                        String td = tds.get(j).text();
                        tdStr2.add(td);
                        z++;
                        x++;
                        if (x == 5 && y < 10) {
                            if (flagPower == 1) {
                                maxPower = Double.parseDouble(tdStr2.get(z - 1).replaceAll(",",""));
                                sql = "INSERT INTO pastehtml.Benchmark_Results_Summary(id,Target_Load,Actual_Load,ssj_ops,Average_Active_Power,Performance_to_Power_Ratio,powerNorm) VALUES ('" + tdStr2.get(0) + "','" + tdStr2.get(z - 4) + "','" + tdStr2.get(z - 3) + "','" + tdStr2.get(z - 2) + "','" + tdStr2.get(z - 1) + "','" + tdStr2.get(z) + "',1)";
                                flagPower = 0;
                            }else {
                                double tmpPower = Double.parseDouble(tdStr2.get(z - 1).replaceAll(",",""))/maxPower;
                                sumPowerGap += tmpPower;
                                sql = "INSERT INTO pastehtml.Benchmark_Results_Summary(id,Target_Load,Actual_Load,ssj_ops,Average_Active_Power,Performance_to_Power_Ratio,powerNorm) VALUES ('" + tdStr2.get(0) + "','" + tdStr2.get(z - 4) + "','" + tdStr2.get(z - 3) + "','" + tdStr2.get(z - 2) + "','" + tdStr2.get(z - 1) + "','" + tdStr2.get(z) + "','" + tmpPower + "')";
                            }
                            preparedStatement = connection.prepareStatement(sql); //运行SQL语句
                            preparedStatement.executeUpdate();
                            x = 0;
                            y++;
                        }
                    }
                }
                minPowerGap = Double.parseDouble(tdStr2.get(z - 3).replaceAll(",","")) / maxPower;
                sumPowerGap += minPowerGap;
                double EP = 0.05 * (2 * sumPowerGap - 1 - minPowerGap);
                EP = 1 - (EP - 0.5) / 0.5;
                sql = "INSERT INTO pastehtml.Benchmark_Results_Summary(id,Target_Load,Actual_Load,ssj_ops,Average_Active_Power,Performance_to_Power_Ratio,∑ssj_ops÷∑power,powerNorm) VALUES ('" + tdStr2.get(0) + "','" + tdStr2.get(z - 5) + "','" + tdStr2.get(z - 5) + "','" + tdStr2.get(z - 4) + "','" + tdStr2.get(z - 3) + "','" + tdStr2.get(z - 2) + "','" + tdStr2.get(z) + "','" + EP + "')";
                preparedStatement = connection.prepareStatement(sql); //运行SQL语句
                preparedStatement.executeUpdate();
            }
            System.out.println("2-----------------------------------------------------------------");
            trs = tables.get(arr[2]).select("tr");        /* Set:'sut' */
            List<String> tdStr3 = new ArrayList<String>();
            tdStr3.add(id);
            for (int i = 0; i < trs.size(); i++) {
                Elements tds = trs.get(i).select("td");
                for (int j = 0; j < tds.size(); j++) {
                    String td = tds.get(j).text();
                    System.out.println(td);
                    td = td.replaceAll("\'", "\\\\'");//转义单引号
                    tdStr3.add(td);
                }
            }
            sql = "INSERT INTO pastehtml.sut(id,Set_Identifier,Set_Description,of_Identical_Nodes,Comment) VALUES('" + tdStr3.get(0) + "','" + tdStr3.get(2) + "','" + tdStr3.get(4) + "','" + tdStr3.get(6) + "','" + tdStr3.get(8) + "')";
            preparedStatement = connection.prepareStatement(sql); //运行SQL语句
            preparedStatement.executeUpdate();
            System.out.println("3-----------------------------------------------------------------");
            trs = tables.get(arr[3]).select("tr");    /* Set:'sut' 中的Hardware*/
            for (int i = 0; i < trs.size(); i++) {
                Elements tds = trs.get(i).select("td");
                for (int j = 0; j < tds.size(); j++) {
                    String td = tds.get(j).text();
                    System.out.println(td);
                    td = td.replaceAll("\'", "\\\\'");//转义单引号
                    tdStr3.add(td);
                }
            }
            sql = "UPDATE pastehtml.sut SET Hardware_Vendor='" + tdStr3.get(10) + "',Model='" + tdStr3.get(12) + "',Form_Factor='" + tdStr3.get(14) + "',CPU_Name='" + tdStr3.get(16) + "',CPU_Characteristics='" + tdStr3.get(18) + "',CPU_Frequency_MHz='" + tdStr3.get(20) + "',CPU_Enabled_s='" + tdStr3.get(22) + "',Hardware_Threads='" + tdStr3.get(24) + "',CPU_Orderable_s='" + tdStr3.get(26) + "',Primary_Cache='" + tdStr3.get(28) + "',Secondary_CacheSet='" + tdStr3.get(30) + "',Tertiary_Cache='" + tdStr3.get(32) + "',Other_Cache='" + tdStr3.get(34) + "',Memory_Amount_GB='" + tdStr3.get(36) + "',and_size_of_DIMM='" + tdStr3.get(38) + "',Memory_Details='" + tdStr3.get(40) + "',Power_Supply_Quantity_and_Rating_W='" + tdStr3.get(42) + "',Power_Supply_Details='" + tdStr3.get(44) + "',Disk_Drive='" + tdStr3.get(46) + "',Disk_Controller='" + tdStr3.get(48) + "',and_type_of_Network_Interface_Cards_Installed='" + tdStr3.get(50) + "',NICs_Enabled_in_Firmware_and_OS_and_Connected='" + tdStr3.get(52) + "',Network_Speed_Mbit='" + tdStr3.get(54) + "',Keyboard='" + tdStr3.get(56) + "',Mouse='" + tdStr3.get(58) + "',Monitor='" + tdStr3.get(60) + "',Optical_Drives='" + tdStr3.get(62) + "',Other_Hardware='" + tdStr3.get(64) + "' where id='" + tdStr3.get(0) + "'";
            preparedStatement = connection.prepareStatement(sql); //运行SQL语句
            preparedStatement.executeUpdate();
            System.out.println("4-----------------------------------------------------------------");
            trs = tables.get(arr[4]).select("tr");    /* Set:'sut'中的Software*/
            for (int i = 0; i < trs.size(); i++) {
                Elements tds = trs.get(i).select("td");
                for (int j = 0; j < tds.size(); j++) {
                    String td = tds.get(j).text();
                    System.out.println(td);
                    td = td.replaceAll("\'", "\\\\'");//转义单引号
                    tdStr3.add(td);
                }
            }
            sql = "UPDATE pastehtml.sut SET Power_Management='" + tdStr3.get(66) + "',Operating_System='" + tdStr3.get(68) + "',OS_Version='" + tdStr3.get(70) + "',Filesystem='" + tdStr3.get(72) + "',JVM_Vendor='" + tdStr3.get(74) + "',JVM_Version='" + tdStr3.get(76) + "',JVM_Commandline_Options='" + tdStr3.get(78) + "',JVM_Affinity='" + tdStr3.get(80) + "',JVM_Instances='" + tdStr3.get(82) + "',JVM_Initial_Heap_MB='" + tdStr3.get(84) + "',JVM_Maximum_Heap_MB='" + tdStr3.get(86) + "',JVM_Address_Bits='" + tdStr3.get(88) + "',Boot_Firmware_Version='" + tdStr3.get(90) + "',Management_Firmware_Version='" + tdStr3.get(92) + "',Workload_Version='" + tdStr3.get(94) + "',Director_Location='" + tdStr3.get(96) + "',Other_Software='" + tdStr3.get(98) + "'WHERE id='" + tdStr3.get(0) + "'";
            preparedStatement = connection.prepareStatement(sql); //运行SQL语句
            preparedStatement.executeUpdate();
            System.out.println("5-----------------------------------------------------------------");
            List<String> tdStr4 = new ArrayList<String>();
            tdStr4.add(id);
            z = 0;
            Elements divs = body.select("#boot_firmware");/*Boot Firmware Settings 有的页面有，有的页面为空*/
            Element div = divs.get(0);
            uls = div.select("ul");
            if (uls.size() == 0) {
                System.out.println("Null");
                sql = "INSERT INTO pastehtml.boot_firmware_settings(id,content) VALUES('" + tdStr4.get(0) + "','NULL') ";
                preparedStatement = connection.prepareStatement(sql); //运行SQL语句
                preparedStatement.executeUpdate();
            } else {
                System.out.println(uls.size());
                Elements lis = uls.get(0).select("li");
                System.out.println(lis.size());
                for (int i = 0; i < lis.size(); i++) {
                    String tds = lis.get(i).text();
                    System.out.println(tds);
                    tdStr4.add(tds);
                    z++;
                    str = tdStr4.get(z);
                    str = str.replaceAll("\'", "\\\\'");//转义单引号
                    str = str.replaceAll("\"", "\\\\\"");//转义双引号
                    sql = "INSERT INTO pastehtml.boot_firmware_settings(id,content) VALUES('" + tdStr4.get(0) + "','" + str + "') ";
                    System.out.println(sql);
                    preparedStatement = connection.prepareStatement(sql); //运行SQL语句
                    preparedStatement.executeUpdate();
                }
            }
            System.out.println("6-----------------------------------------------------------------");
            List<String> tdStr5 = new ArrayList<String>();
            tdStr5.add(id);
            z = 0;
            divs = body.select("#tuning"); /*System Under Test Notes 有的页面有，有的页面为空*/
            div = divs.get(0);
            uls = div.select("ul");
            if (uls.size() == 0) {
                System.out.println("Null");
                sql = "INSERT INTO pastehtml.system_under_test_notes(id,content) VALUES('" + tdStr5.get(0) + "','NULL') ";
                System.out.println(sql);
                preparedStatement = connection.prepareStatement(sql); //运行SQL语句
                preparedStatement.executeUpdate();
            } else {
                Elements lis = uls.get(0).select("li");
                for (int i = 0; i < lis.size(); i++) {
                    String tds = lis.get(i).text();
                    System.out.println(tds);
                    tdStr5.add(tds);
                    z++;
                    str = tdStr5.get(z);
                    str = str.replaceAll("\'", "\\\\'");//转义单引号
                    str = str.replaceAll("\"", "\\\\\"");//转义双引号
                    sql = "INSERT INTO pastehtml.system_under_test_notes(id,content) VALUES('" + tdStr5.get(0) + "','" + str + "') ";
                    System.out.println(sql);
                    preparedStatement = connection.prepareStatement(sql); //运行SQL语句
                    preparedStatement.executeUpdate();
                }
            }
            System.out.println("7-----------------------------------------------------------------");
            trs = tables.get(arr[5]).select("tr");    /* Control system 中的Hardware */
            List<String> tdStr6 = new ArrayList<String>();
            tdStr6.add(id);
            for (int i = 0; i < trs.size(); i++) {
                Elements tds = trs.get(i).select("td");
                for (int j = 0; j < tds.size(); j++) {
                    String td = tds.get(j).text();
                    System.out.println(td);
                    td = td.replaceAll("\'", "");//转义单引号
                    tdStr6.add(td);
                }
            }
            sql = "INSERT INTO pastehtml.controller_system(id,Hardware_Vendor,Model,CPU_Description,Memory_amount_GB) VALUES('" + tdStr6.get(0) + "','" + tdStr6.get(2) + "','" + tdStr6.get(4) + "','" + tdStr6.get(6) + "','" + tdStr6.get(8) + "')";
            preparedStatement = connection.prepareStatement(sql); //运行SQL语句
            preparedStatement.executeUpdate();
            System.out.println("8-----------------------------------------------------------------");
            trs = tables.get(arr[6]).select("tr");    /* Control system 中的  software */
            for (int i = 0; i < trs.size(); i++) {
                Elements tds = trs.get(i).select("td");
                for (int j = 0; j < tds.size(); j++) {
                    String td = tds.get(j).text();
                    System.out.println(td);
                    td = td.replaceAll("\'", "\\\\'");//转义单引号
                    tdStr6.add(td);
                }
            }
            sql = "UPDATE pastehtml.controller_system SET Operating_System='" + tdStr6.get(10) + "',JVM_Vendor='" + tdStr6.get(12) + "',JVM_Version='" + tdStr6.get(14) + "',CCS_Version='" + tdStr6.get(16) + "' WHERE id='" + tdStr6.get(0) + "'";
            preparedStatement = connection.prepareStatement(sql); //运行SQL语句
            preparedStatement.executeUpdate();
            System.out.println("9-----------------------------------------------------------------");
            trs = tables.get(arr[7]).select("tr");/*Measurement Devices*/
            List<String> tdStr7 = new ArrayList<String>();
            tdStr7.add(id);
            for (int i = 0; i < trs.size(); i++) {
                Elements tds = trs.get(i).select("td");
                for (int j = 0; j < tds.size(); j++) {
                    String td = tds.get(j).text();
                    System.out.println(td);
                    td = td.replaceAll("\'", "\\\\'");//转义单引号
                    tdStr7.add(td);
                }
            }
            sql = "INSERT INTO pastehtml.measurement_devices(id,pwr1_Hardware_Vendor,pwr1_Model,pwr1_Serial_Number,pwr1_Connectivity,pwr1_Input_Connection,pwr1_Metrology_Institute,pwr1_Accredited_by,pwr1_Calibration_Label,pwr1_Date_of_Calibration,pwr1_PTDaemon_Host_System,pwr1_PTDaemon_Host_OS,pwr1_PTDaemon_Version,pwr1_Setup_Description) VALUES ('" + tdStr7.get(0) + "','" + tdStr7.get(2) + "','" + tdStr7.get(4) + "','" + tdStr7.get(6) + "','" + tdStr7.get(8) + "','" + tdStr7.get(10) + "','" + tdStr7.get(12) + "','" + tdStr7.get(14) + "','" + tdStr7.get(16) + "','" + tdStr7.get(18) + "','" + tdStr7.get(20) + "','" + tdStr7.get(22) + "','" + tdStr7.get(24) + "','" + tdStr7.get(26) + "')";
            preparedStatement = connection.prepareStatement(sql); //运行SQL语句
            preparedStatement.executeUpdate();
            System.out.println("10-----------------------------------------------------------------");
            trs = tables.get(arr[8]).select("tr");/*Measurement Devices*/
            for (int i = 0; i < trs.size(); i++) {
                Elements tds = trs.get(i).select("td");
                for (int j = 0; j < tds.size(); j++) {
                    String td = tds.get(j).text();
                    System.out.println(td);
                    td = td.replaceAll("\'", "\\\\'");//转义单引号
                    tdStr7.add(td);
                }
            }
            sql = "UPDATE pastehtml.measurement_devices SET temp1_Hardware_Vendor='" + tdStr7.get(28) + "',temp1_Model='" + tdStr7.get(30) + "',temp1_Driver_Version='" + tdStr7.get(32) + "',temp1_Connectivity='" + tdStr7.get(34) + "',temp1_PTDaemon_Host_System='" + tdStr7.get(36) + "',temp1_PTDaemon_Host_OS='" + tdStr7.get(38) + "',temp1_Setup_Description='" + tdStr7.get(40) + "' WHERE id='" + tdStr7.get(0) + "'";
            preparedStatement = connection.prepareStatement(sql); //运行SQL语句
            preparedStatement.executeUpdate();
            System.out.println("11-----------------------------------------------------------------");
            if (flag_NC == 1) {
                trs = tables.get(arr[9]).select("tr"); /*Aggregate Eletrical and Environmental Data中的第一个表格*/
                x = z = 0; //x记列，y记行，z记List中的位数
                List<String> tdStr8 = new ArrayList<String>();
                tdStr8.add(id);
                for (int i = 0; i < trs.size(); i++) {
                    Elements tds = trs.get(i).select("td");
                    for (int j = 0; j < tds.size(); j++) {
                        String td = tds.get(j).text();
                        System.out.println(td);
                        td = td.replaceAll("\'", "\\\\'");
                        tdStr8.add(td);
                        z++;
                        x++;
                        if (x == 3) {
                            sql = "INSERT INTO pastehtml.Aggregate_Electrical_and_Environmental_Data(id,Target_Load,Average_Active_Power_W,Minimum_Ambient_Temperature_°C) VALUES('" + tdStr8.get(0) + "','" + tdStr8.get(z - 2) + "','" + tdStr8.get(z - 1) + "','" + tdStr8.get(z) + "')";
                            preparedStatement = connection.prepareStatement(sql); //运行SQL语句
                            preparedStatement.executeUpdate();
                            x = 0;
                            y++;
                        }
                    }
                }
                System.out.println("12-----------------------------------------------------------------");
                trs = tables.get(arr[10]).select("tr");/*Aggregate Eletrical and Environmental Data中的第二个表格*/
                for (int i = 0; i < trs.size(); i++) {
                    Elements tds = trs.get(i).select("td");
                    for (int j = 0; j < tds.size(); j++) {
                        String td = tds.get(j).text();
                        System.out.println(td);
                        td = td.replaceAll("\'", "\\\\'");
                        tdStr8.add(td);
                        z++;
                    }
                }
                sql = "INSERT INTO  pastehtml.Aggregate_Electrical_and_Environmental_Data(id,Line_Standard,Minimum_Temperature_°C,Elevation_m) VALUES('" + tdStr8.get(0) + "','" + tdStr8.get(z - 2) + "','" + tdStr8.get(z - 1) + "','" + tdStr8.get(z) + "')";
                preparedStatement = connection.prepareStatement(sql); //运行SQL语句
                preparedStatement.executeUpdate();
            }
            System.out.println("13-----------------------------------------------------------------");
            if (flag_NC == 1) {
                x = y = z = 0;//x记列，y记行，z记List中的位数
                List<String> tdStr9 = new ArrayList<String>();
                tdStr9.add(id);
                trs = tables.get(arr[11]).select("tr"); /*Aggregate Performance Data*/
                for (int i = 0; i < trs.size(); i++) {
                    Elements tds = trs.get(i).select("td");
                    for (int j = 0; j < tds.size(); j++) {
                        String td = tds.get(j).text();
                        System.out.println(td);
                        tdStr9.add(td);
                        z++;
                        x++;
                        if (x == 4 && y != 3) {
                            sql = "INSERT INTO pastehtml.aggregate_performance_data(id,Target_Load,Actual_Load,ssj_ops_Target,ssj_Actual) VALUES ('" + tdStr9.get(0) + "','" + tdStr9.get(z - 3) + "','" + tdStr9.get(z - 2) + "','" + tdStr9.get(z - 1) + "','" + tdStr9.get(z) + "')";
                            preparedStatement = connection.prepareStatement(sql); //运行SQL语句
                            preparedStatement.executeUpdate();
                            x = 0;
                            y++;
                        }
                        if (y == 3 && z == 13) {
                            sql = "INSERT INTO pastehtml.aggregate_performance_data(id,Target_Load,Actual_Load,ssj_ops_Target,ssj_Actual) VALUES('" + tdStr9.get(0) + "','" + tdStr9.get(z) + "','" + tdStr9.get(z) + "','" + tdStr9.get(z) + "','" + tdStr9.get(z) + "')";
                            preparedStatement = connection.prepareStatement(sql); //运行SQL语句
                            preparedStatement.executeUpdate();
                            y++;
                            x = 0;
                        }
                    }
                }
            }
            if (flag_NC == 0) {
                sql = "UPDATE pastehtml.system_overview SET compliment=0 WHERE id = '" + id + "'";
                preparedStatement = connection.prepareStatement(sql); //运行SQL语句
                preparedStatement.executeUpdate();
            }
            System.out.println("14-----------------------------------------------------------------");
        }
        System.out.println("Finish");
    }
}

