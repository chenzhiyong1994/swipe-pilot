package com.local.swipepilot;

import java.util.List;

/** Only the supplied Taobao bedtime reward sheet, never generic reward text. */
public final class PopupPolicy {
    private long since=-1,absentSince=-1,lastAttempt=-1;
    private int attempts;
    public static boolean matches(List<String> lines){
        StringBuilder all=new StringBuilder();for(String s:lines)all.append(s.replaceAll("\\s+",""));
        String text=all.toString();
        boolean title=text.contains("睡前小福利")&&text.contains("看视频赚钱");
        boolean content=text.contains("打卡即可得金豆")||text.contains("记得到点来领奖励")||text.matches("(?s).*开始赚钱即可获得\\d+金豆.*");
        boolean action=text.contains("打卡赚钱")||text.matches("(?s).*提醒我.*领\\d+金豆.*");
        return title&&(content||action);
    }
    public void reset(){since=absentSince=lastAttempt=-1;attempts=0;}
    public boolean observe(long now,boolean visible){
        if(visible){if(since<0)since=now;absentSince=-1;return true;}
        if(since<0)return false;
        if(absentSince<0)absentSince=now;
        if(now-absentSince<500)return true;
        reset();return false;
    }
    public long remaining(long now){return since<0?10:Math.max(0,(10000-(now-since)+999)/1000);}
    public boolean shouldClose(long now){return since>=0&&remaining(now)==0&&attempts<2&&(lastAttempt<0||now-lastAttempt>=2000);}
    public void attempted(long now){lastAttempt=now;attempts++;}
    public boolean exhausted(){return attempts>=2;}
}
