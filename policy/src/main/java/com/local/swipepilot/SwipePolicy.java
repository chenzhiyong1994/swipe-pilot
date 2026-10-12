package com.local.swipepilot;

/** All times are monotonic milliseconds; never replay missed gestures. */
public final class SwipePolicy {
    public long nextSwipe, adUntil, manualUntil;
    private long ignoreAdsUntil;
    private int lastTimerSeconds=-1;
    private boolean unknownAd, sawTimer;
    private long adAbsentSince = -1;
    public void reset(long now, int interval) {
        nextSwipe = now + interval * 1000L;
        adUntil = manualUntil = ignoreAdsUntil = 0; unknownAd = sawTimer = false; lastTimerSeconds=-1; adAbsentSince = -1;
    }
    public void defer(long now, int interval) { nextSwipe = now + interval * 1000L; }
    public void waitManually(long now, int seconds) { ignoreAdsUntil=0; manualUntil = now + seconds * 1000L; }
    public void dismissAd(long now,int interval){
        adUntil=manualUntil=0;unknownAd=sawTimer=false;lastTimerSeconds=-1;adAbsentSince=-1;
        ignoreAdsUntil=now+Math.max(60000,interval*1000L+5000);defer(now,interval);
    }
    public void gestureCompleted(long now){
        if(ignoreAdsUntil>now)ignoreAdsUntil=now+1500;
        clearAutomatic();
    }
    public void observe(long now, AdDetector.Result result) {
        if(now<ignoreAdsUntil)return;
        if (result.ad) {
            adAbsentSince = -1;
            if (result.seconds >= 0) {
                // Repeated screen text is one observation, not a new waiting request.
                // Changed countdown values can resynchronise a real paused/restarted ad.
                if(!sawTimer||result.seconds!=lastTimerSeconds)adUntil=now+result.seconds*1000L+1500;
                lastTimerSeconds=result.seconds;sawTimer=true;unknownAd=false;
            } else if (!sawTimer) unknownAd = true;
        } else {
            if (adAbsentSince < 0) adAbsentSince = now;
            if (now - adAbsentSince >= 2000)clearAutomatic();
        }
    }
    public void clearAutomatic(){unknownAd=sawTimer=false;lastTimerSeconds=-1;adUntil=0;adAbsentSince=-1;}
    public String hold(long now) {
        if (manualUntil > now) return "手动广告等待 " + ((manualUntil - now + 999) / 1000) + " 秒";
        if (adUntil > now) return "广告倒计时约 " + ((adUntil - now + 999) / 1000) + " 秒";
        if (unknownAd) return "发现广告，等待页面结束";
        return null;
    }
    public boolean ready(long now) { return now >= nextSwipe && hold(now) == null; }
}
