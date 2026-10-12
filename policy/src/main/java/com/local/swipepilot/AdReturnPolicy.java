package com.local.swipepilot;

import java.util.List;

/** A preview must precede a stable reward. No game title or fixed ad duration is used. */
public final class AdReturnPolicy {
    private static final long DEPARTURE_LIMIT=30000, REWARD_LIMIT=120000, REWARD_STABLE=700;
    private int phase; // 0 watching, 1 preview, 2 ad, 3 return requested
    private long deadline,rewardSince=-1;
    private boolean previewLatched;
    public void reset(){phase=0;deadline=0;rewardSince=-1;previewLatched=false;}
    public void cancel(){phase=0;deadline=0;rewardSince=-1;previewLatched=true;}
    public boolean pending(){return phase==1||phase==2;}
    public boolean returning(){return phase==3;}
    public static boolean contains(List<String> lines,String phrase){
        StringBuilder text=new StringBuilder();for(String line:lines)text.append(line.replaceAll("\\s+",""));
        return text.indexOf(phrase)>=0;
    }
    public boolean observe(long now,boolean targetVisible,boolean systemSurface,boolean preview,boolean reward,boolean adPage){
        if(systemSurface){cancel();return false;}
        if(phase==3)return false;
        if(phase!=0&&now>=deadline){cancel();return false;}
        boolean fresh=targetVisible&&preview&&!previewLatched;
        if(targetVisible){if(preview)previewLatched=true;else previewLatched=false;}
        if(fresh&&phase==0){phase=1;deadline=now+DEPARTURE_LIMIT;rewardSince=-1;}
        if(phase==1&&!preview&&(!targetVisible||adPage||reward)){phase=2;deadline=now+REWARD_LIMIT;}
        if(phase!=2||preview||!reward){rewardSince=-1;return false;}
        if(rewardSince<0)rewardSince=now;
        if(now-rewardSince<REWARD_STABLE)return false;
        phase=3;return true;
    }
    public String status(){return phase==1?"已识别广告预告，等待跳转":phase==2?"等待广告奖励完成":phase==3?"奖励已完成，正在返回":"正常播放，等待广告";}
}
