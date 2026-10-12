package com.local.swipepilot;

/** Session lifetime is independent of gesture and manual pause state. */
public final class SessionPolicy {
    public static final long AWAY_LIMIT=120000, LOCK_LIMIT=30000;
    private long deadline, awaySince=-1, lockedSince=-1;
    public void start(long now,int minutes){deadline=minutes>0?now+minutes*60000L:0;awaySince=lockedSince=-1;}
    public String update(long now,boolean targetVisible,boolean unlocked){
        // Check old timers before clearing them, including after device sleep.
        if(deadline>0&&now>=deadline)return "使用时间到了，已自动停止";
        if(lockedSince>=0&&now-lockedSince>=LOCK_LIMIT)return "锁屏超过 30 秒，已自动停止";
        if(awaySince>=0&&now-awaySince>=AWAY_LIMIT)return "离开超过 2 分钟，已自动停止";
        if(unlocked)lockedSince=-1;else if(lockedSince<0)lockedSince=now;
        if(targetVisible&&unlocked)awaySince=-1;else if(awaySince<0)awaySince=now;
        return null;
    }
    public long remainingMinutes(long now){return deadline==0?-1:Math.max(0,(deadline-now+59999)/60000);}
}
