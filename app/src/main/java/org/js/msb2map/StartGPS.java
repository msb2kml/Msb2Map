package org.js.msb2map;

import android.location.Location;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StartGPS {

    String pathStartGPS;
    String patrnLat="lat=\"(-?[0-9.-]+)\"";
    String patrnLon="lon=\"(-?[0-9.-]+)\"";
    String patrnNam="<name>(.+)</name>";
    String patrnEle="<ele>(.+)</ele>";
    String patrnTim="<time>(.+)</time>";
    Pattern pLat;
    Pattern pLon;
    Pattern pNam;
    Pattern pEle;
    Pattern pTim;

    public StartGPS(String path){
        pathStartGPS=path;
        pLat=Pattern.compile(patrnLat);
        pLon=Pattern.compile(patrnLon);
        pNam=Pattern.compile(patrnNam);
        pEle=Pattern.compile(patrnEle);
        pTim=Pattern.compile(patrnTim);
    }

    public void writeSG(Map<String,Location> startPoints){
        String name;
        Location loc;
        if (pathStartGPS==null || startPoints.isEmpty()) return;
        try{
             FileWriter outGpx = new FileWriter(pathStartGPS);
             outGpx.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
             outGpx.write("<gpx version=\"1.0\"\n");
             outGpx.write("   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
             outGpx.write("   creator=\"Msb2Kml\"\n");
             outGpx.write("   xmlns=\"http://www.topografix.com/GPX/1/0\"\n");
             outGpx.write("   xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0");
             outGpx.write("http://www.topografix.com/GPX/1/0/gpx.xsd\">\n");
             SortedSet<String> keys=new TreeSet<>();
             keys.addAll(startPoints.keySet());
             Iterator<String> itr=keys.iterator();
             while (itr.hasNext()){
                name=itr.next();
                loc=startPoints.get(name);
                writeWpt(outGpx,name,loc);
             }
             outGpx.write("</gpx>\n");
             outGpx.close();
        }catch (IOException e){
            return;
        }
    }

    public void writeWpt(FileWriter outGpx,String name, Location loc){
        try {
            outGpx.write(" <wpt ");
            outGpx.write(String.format(Locale.ENGLISH,"lat=\"%.8f\" ",loc.getLatitude()));
            outGpx.write(String.format(Locale.ENGLISH,"lon=\"%.8f\">\n",loc.getLongitude()));
            outGpx.write(String.format(Locale.ENGLISH,"  <ele>%.3f</ele>\n",loc.getAltitude()));
            outGpx.write("  <name>"+name+"</name>\n");
            outGpx.write(" </wpt>\n");
        }catch (IOException e) {
            return;
        }
    }

    public Map<String,Location> readSG(){
        boolean lkWpt=true;
        boolean lkEwpt=false;
        boolean lkCwpt=false;
        Integer len;
        Integer curnt;
        String expr="";
        Location loc=null;
        String name=null;
        Double alt=null;
        Map<String,Location> startPoints=new HashMap<>();
        File sg=new File(pathStartGPS);
        if (!sg.exists()) return startPoints;
        try {
            BufferedReader f=new BufferedReader(new FileReader(sg));
            String line="";
            while (line!=null) {
                line = f.readLine();
                if (line == null) continue;
                len = line.length();
                curnt = 0;
                while (curnt < len) {
                    if (lkWpt) {
                        Integer i = line.indexOf("<wpt", curnt);
                        if (i < 0) {
                            curnt = len;
                            continue;
                        }
                        expr = "";
                        curnt=i+4;
                        lkWpt=false;
                        lkEwpt=true;
                        loc=null;
                    } else if (lkEwpt){
                        Integer j=line.indexOf(">",curnt);
                        if (j<0){
                            expr=expr+line.substring(curnt);
                            curnt=len;
                        } else {
                            expr=expr+line.substring(curnt,j);
                            curnt=j+1;
                            loc=LatLon(expr);
                            lkEwpt=false;
                            if (loc!=null){
                                lkCwpt=true;
                                name=null;
                                expr="";
                                loc.setAltitude(0);
                            } else {
                                lkWpt=true;
                            }
                        }
                    } else if (lkCwpt){
                        Integer i=line.indexOf("</wpt>",curnt);
                        if (i<0) {
                            String tname=rName(line.substring(curnt));
                            if (tname!=null) name=tname;
                            alt=rEle(line.substring(curnt));
                            if (alt!=null) loc.setAltitude(alt);
                            curnt=len;
                            continue;
                        }
                        if (i>curnt) {
                            String tname=rName(line.substring(curnt,i));
                            if (tname!=null) name=tname;
                            alt=rEle(line.substring(curnt,i));
                            if (alt!=null) loc.setAltitude(alt);
                        }
                        lkCwpt=false;
                        lkWpt=true;
                        if (name!=null){
                            startPoints.put(name,loc);
                            name=null;
                        }
                        curnt=i+6;
                    }
                }

            }
            f.close();
        } catch (Exception e){ return startPoints; }
        return startPoints;
    }

    private Location LatLon(String expr){
        Location loc=new Location("");
        Double lat=null;
        Double lon=null;
        Matcher m;
        m=pLat.matcher(expr);
        if (m.find()){
            try {
                lat=Double.parseDouble(m.group(1));
            } catch (NumberFormatException e){
                return null;
            }
        } else return null;
        m=pLon.matcher(expr);
        if (m.find()){
            try {
                lon=Double.parseDouble(m.group(1));
            } catch (NumberFormatException e){
                return null;
            }
        } else return null;
        if (lat>180 || lat<-180 || lon>180 || lon<-180) return null;
        loc.setLongitude(lon);
        loc.setLatitude(lat);
        return loc;
    }

    private String rName(String expr){
        String name;
        Matcher m;
        m=pNam.matcher(expr);
        if (m.find()){
            name=m.group(1);
            name.trim();
            if (name.length()>0) return name;
            return null;
        }
        return null;
    }

    private Double rEle(String expr){
        Double alt;
        Matcher m;
        m=pEle.matcher(expr);
        if (m.find()){
            try {
                alt=Double.parseDouble(m.group(1));
                return alt;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Long rTime(String expr){
        Matcher m;
        Long tim;
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        m=pTim.matcher(expr);
        if (m.find()){
            try {
                Date date=sdf.parse(m.group(1));
                tim=date.getTime();
                return tim;
            } catch (Exception e){
                return null;
            }
        } else return null;
    }

    public ArrayList<Location> readTrack(String nameGPX){
        ArrayList<Location> result=null;
        Location loc=null;
        Integer len;
        Integer curnt;
        boolean lkTpt=true;
        boolean lkEtpt=false;
        boolean lkCtpt=false;
        String expr="";
        Double alt=null;
        Long tim=null;
        File track=new File(nameGPX);
        if (!track.exists()) return null;
        try {
            BufferedReader f=new BufferedReader((new FileReader(track)));
            String line="";
            while (line!=null) {
                line = f.readLine();
                if (line == null) continue;
                len = line.length();
                curnt = 0;
                while (curnt < len) {
                    if (lkTpt) {
                        int i = line.indexOf("<trkpt", curnt);
                        if (i < 0) {
                            curnt = len;
                            continue;
                        }
                        expr = "";
                        curnt = i + 6;
                        lkTpt = false;
                        lkEtpt = true;
                        loc = null;
                    } else if (lkEtpt) {
                        int j = line.indexOf(">", curnt);
                        if (j < 0) {
                            expr = expr + line.substring(curnt);
                            curnt = len;
                        } else {
                            expr = expr + line.substring(curnt, j);
                            curnt = j + 1;
                            loc = LatLon(expr);
                            lkEtpt = false;
                            if (loc != null) {
                                lkCtpt = true;
                                expr = "";
                                loc.setAltitude(0d);
                            } else {
                                lkTpt = true;
                            }
                        }
                    } else if (lkCtpt) {
                        int i = line.indexOf("</trkpt>", curnt);
                        if (i < 0) {
                            alt = rEle(line.substring(curnt));
                            tim = rTime(line.substring(curnt));
                            if (alt != null) loc.setAltitude(alt);
                            if (tim != null) loc.setTime(tim);
                            curnt = len;
                        } else {
                            alt = rEle(line.substring(curnt, i));
                            tim = rTime(line.substring(curnt, i));
                            if (alt != null) loc.setAltitude(alt);
                            if (tim != null) loc.setTime(tim);
                            curnt = i + 8;
                            lkCtpt = false;
                            lkTpt = true;
                            if (result == null) {
                                result = new ArrayList<>();
                                result.add(0, loc);
                                result.add(1, loc);
                            }
                            result.set(1, loc);
                            loc = null;
                        }
                    }
                }
            }
            f.close();
        } catch (Exception e) { return null; }
        return  result;
    }




}
