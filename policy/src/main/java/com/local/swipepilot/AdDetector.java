package com.local.swipepilot;

import java.util.*;
import java.util.regex.*;

/** Conservative control-text parser: never join an ad badge to a number elsewhere on screen. */
public final class AdDetector {
    private static final String NUMBER="(\\d{1,3})";
    private static final String UNIT="(?:秒|s|seconds?)";
    private static final String ACTION="(?:跳过(?:广告)?|关闭(?:广告)?|翻页)";
    private static final Pattern[] TIMERS={
        compile("^(?:广告|ad|advertisement)\\s*(?:倒计时|剩余|还剩)?\\s*[:：·-]?\\s*"+NUMBER+"\\s*"+UNIT+"(?:\\s*后\\s*(?:即可|可以|可)?\\s*"+ACTION+")?[。.!]?$"),
        compile("^(?:剩余|还剩)?\\s*"+NUMBER+"\\s*"+UNIT+"\\s*后\\s*(?:即可|可以|可)?\\s*"+ACTION+"[。.!]?$"),
        compile("^(?:跳过(?:广告)?|关闭广告)\\s*[:：·-]?\\s*"+NUMBER+"\\s*(?:"+UNIT+")?[。.!]?$"),
        compile("^(?:skip|close)(?:\\s+(?:ad|advertisement))?\\s+in\\s+"+NUMBER+"\\s*(?:s|seconds?)[.!]?$"),
        compile("^(?:观看|看完)广告\\s*"+NUMBER+"\\s*秒\\s*(?:后)?\\s*(?:即可|可以|可)?(?:得|获得|领取)?(?:奖励|解锁本集|继续播放)[。.!]?$"),
        compile("^"+NUMBER+"\\s*秒\\s*后\\s*广告结束[。.!]?$")
    };
    private static final Pattern AD_CLOCK=compile("^(?:广告|ad|advertisement)\\s*(?:倒计时|剩余)?\\s*[:：·-]?\\s*([0-9]{1,2})[:：]([0-5][0-9])$");
    private static final Pattern ACTIVE=compile("^(?:即将播放广告|广告(?:正在播放|播放中|加载中)|advertisement (?:loading|playing))(?:[，,。… .]*(?:请稍候|请稍后|please wait))?[。.!…]*$");
    public static final class Result {
        public final boolean ad;
        public final int seconds;
        Result(boolean ad,int seconds){this.ad=ad;this.seconds=seconds;}
    }
    private static Pattern compile(String value){return Pattern.compile(value,Pattern.CASE_INSENSITIVE);}
    private static String normalize(String text){
        return text.trim().replace('廣','广').replace('過','过').replace('閉','闭').replace('計','计').replace('時','时').replace('餘','余').replace('還','还').replaceAll("\\s+"," ");
    }
    public static Result detect(List<String> lines,String keywords){
        boolean active=false;int seconds=-1;
        for(String raw:lines){
            String line=normalize(raw);
            // Whole readable items only: titles, subtitles and sentences mentioning an ad are not controls.
            if(line.length()>64)continue;
            if(ACTIVE.matcher(line).matches())active=true;
            for(Pattern timer:TIMERS){Matcher m=timer.matcher(line);if(m.matches())seconds=Math.max(seconds,Integer.parseInt(m.group(1)));}
            Matcher clock=AD_CLOCK.matcher(line);
            if(clock.matches())seconds=Math.max(seconds,Integer.parseInt(clock.group(1))*60+Integer.parseInt(clock.group(2)));
            // Optional labels must be followed immediately by seconds in the same readable item.
            // Legacy broad keywords no longer classify an entire page as an ad.
            if(keywords!=null)for(String rawKey:keywords.split("[,，\\n]")){
                String key=normalize(rawKey);if(key.isEmpty())continue;
                Matcher custom=compile("^"+Pattern.quote(key)+"\\s*[:：·-]?\\s*"+NUMBER+"\\s*"+UNIT+"$").matcher(line);
                if(custom.matches())seconds=Math.max(seconds,Integer.parseInt(custom.group(1)));
            }
        }
        return new Result(active||seconds>=0,Math.min(seconds,600));
    }
}
