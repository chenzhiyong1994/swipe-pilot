package com.local.swipepilot;

import java.util.Arrays;

/** Shared deterministic policy checks; runs with JUnit or only a JDK. */
public final class PolicyTest {
    static int count;
    static void check(boolean yes,String reason){count++;if(!yes)throw new AssertionError(reason);}
    static AdDetector.Result detect(String... lines){return AdDetector.detect(Arrays.asList(lines),"");}
    private static final String[] shopping={"领取优惠券","奖励","解锁新剧情","倒计时","广告","7天无理由退货","50人夸它好想买","店铺有1万+回头客","¥119.9","54","43","15","00:43","立即查看"};

    public static void main(String[] args){
        count=0;
        detectionTests();
        waitingTests();
        sessionTests();
        returnTests();
        popupTests();
        System.out.println("PASS: "+count+" assertions");
    }

    static void detectionTests(){
        check(!detect("视频 00:32","23 秒").ad,"Video durations are not ads");
        check(detect("广告","5 秒后可跳过").seconds==5,"Chinese timer");
        check(detect("Sponsored","Skip in 12s").seconds==12,"English timer");
        check(!detect("广告","00:28").ad,"Separate video clock is not an ad timer");
        check(detect("广告 00:28").seconds==28,"Clock explicitly labelled as an ad timer");
        check(detect("跳过 9").seconds==9,"Skip number");
        check(!detect("广告","8").ad,"Standalone number cannot become a split countdown without structural proof");
        check(!detect("广告","立即查看").ad,"An ordinary ad badge does not prevent swiping");
        check(detect("广告加载中").ad&&detect("广告加载中").seconds==-1,"Explicit ad loading state");
        check(AdDetector.detect(Arrays.asList("推广内容 12秒"),"推广内容").seconds==12,"Custom keyword");
        check(!detect("download","shadow").ad,"No substring ad matching");
        check(!detect(shopping).ad,"Reported shopping-video layout cannot combine labels and unrelated numbers into an ad");
        check(!detect("广告","00:45","点赞 54","评论 17","领取","15秒").ad,"Neither 45-second video length nor 15-second text becomes an ad countdown");
        check(!detect("奖励","领取","解锁","可关闭","倒计时","skip","reward","sponsored").ad,"Broad legacy keywords no longer pause playback");
        check(!detect("广告等待","结束等待","广告倒计时约 45 秒","手动广告等待 15 秒","✓ 计时翻页","17").ad,"Expanded overlay captions cannot recognise themselves as ads");
        check(!detect("视频标题：广告剩余15秒","这条视频讲广告倒计时 40 秒怎么做").ad,"Descriptions mentioning ads are not countdown controls");
        check(detect("广告倒计时 5 秒","00:43","119","广告","54").seconds==5,"Unrelated larger values cannot lengthen a valid timer");
        check(detect("廣告剩餘 7 秒").seconds==7,"Traditional Chinese explicit countdown");
        check(detect("广告 · 8 秒后可翻页").seconds==8,"Practice and inline blocked-swipe countdown");
        check(detect("15秒后可跳过广告").seconds==15,"Inline skip-ad countdown");
        check(detect("Close in 10 seconds").seconds==10,"English close countdown");
        check(detect("观看广告15秒后可领取奖励").seconds==15,"Complete explicit watch-ad reward countdown remains supported");
        check(detect("即将播放广告").ad,"Explicit impending advertisement remains protected");
        check(!AdDetector.detect(Arrays.asList("推广内容","12秒","50"),"推广内容").ad,"Custom keyword cannot capture other nodes' numbers");
        check(!AdDetector.detect(Arrays.asList("领取优惠券","15秒"),"领取").ad,"Legacy custom keyword alone no longer pauses");
    }

    static void waitingTests(){
        SwipePolicy p=new SwipePolicy();p.reset(0,15);
        check(!p.ready(14999)&&p.ready(15000),"Interval boundary");
        p.observe(15000,detect("广告倒计时 10 秒"));check(!p.ready(16000),"Countdown blocks gesture");
        p.observe(25000,detect("普通视频"));check(!p.ready(26000),"Grace after timer");
        check(p.ready(26500),"Countdown expiry releases");
        p.reset(0,3);p.observe(3000,detect("广告加载中"));check(!p.ready(100000),"Unknown ad has no blind timeout");
        p.observe(100000,detect("普通视频"));check(!p.ready(101999),"Requires stable absence");
        p.observe(102000,detect("普通视频"));check(p.ready(102000),"Ad disappeared");
        p.waitManually(102000,35);check(!p.ready(136999)&&p.ready(137000),"Manual wait");
        p.defer(137000,30);check(!p.ready(166999)&&p.ready(167000),"Fresh interval after resume");
        p.reset(0,3);p.observe(4000,detect("广告倒计时 1 秒"));p.observe(6000,detect("广告倒计时 1 秒"));check(p.ready(6500)&&p.ready(8000),"Unchanged timer cannot extend its first deadline");
        p.reset(0,3);p.observe(3000,detect("广告倒计时 0 秒"));p.observe(4000,detect("广告倒计时 0 秒"));check(p.ready(4500),"Zero does not extend forever");
        p.reset(0,15);p.observe(1000,detect("广告倒计时 45 秒"));p.waitManually(1000,15);p.dismissAd(2000,15);
        check(p.hold(2000)==null,"Override clears automatic and manual ad waits");
        p.observe(2500,detect("广告倒计时 45 秒"));check(p.hold(2500)==null,"Same false-positive screen cannot immediately restart ad wait");
        check(!p.ready(16999)&&p.ready(17000),"Override resumes normal interval without immediate gesture");
        p.gestureCompleted(17450);p.observe(18000,detect("广告倒计时 8 秒"));check(p.hold(18000)==null,"Transition grace after overridden gesture");
        p.observe(19000,detect("广告倒计时 8 秒"));check(p.hold(19000)!=null,"New page ads protected again after transition");
        p.dismissAd(20000,15);p.waitManually(21000,15);check(p.hold(21000)!=null,"Explicit manual wait still works after override");
        p.reset(0,15);p.observe(1000,detect("广告加载中"));p.dismissAd(2000,15);check(p.hold(2000)==null,"Override clears unknown-duration ad");
        p.observe(62000,detect("广告加载中"));check(p.hold(62000)!=null,"Override bounded even if no gesture completes");
        p.reset(0,3);long end=0;
        for(int t=0;t<=60000;t+=500){p.observe(t,detect("广告倒计时 15 秒"));if(t==0)end=p.adUntil;check(p.adUntil==end,"Fixed 15 cannot keep renewing 17-second wait at "+t);}
        check(p.hold(16500)==null&&p.ready(60000),"Fixed 15 ends once instead of remaining at 17");
        p.reset(0,3);for(int t=0;t<=60000;t+=500)p.observe(t,detect("广告倒计时 43 秒"));
        check(p.adUntil==44500&&p.ready(60000),"Reported 45-second display also expires once");
        p.reset(0,3);p.observe(0,detect("广告倒计时 10 秒"));p.observe(5000,detect("广告倒计时 5 秒"));
        check(p.adUntil==11500,"Real decrement retains expected deadline");
        p.observe(9000,detect("广告倒计时 4 秒"));check(p.adUntil==14500&&!p.ready(14000),"Changed countdown resynchronises a real delayed advertisement");
        p.observe(10000,detect("广告倒计时 0 秒"));p.observe(11000,detect("广告倒计时 0 秒"));
        check(p.ready(11500),"Real zero releases after buffer, not old 10-second prediction");
        p.reset(0,3);p.observe(0,detect("广告倒计时 40 秒"));p.observe(1000,detect("普通视频"));p.observe(3000,detect("普通视频"));
        check(p.hold(3000)==null,"Stable removal clears obsolete ad countdown immediately");
        p.reset(0,3);p.observe(0,detect("广告倒计时 40 秒"));p.waitManually(1000,15);p.clearAutomatic();
        check(p.hold(2000).startsWith("手动"),"Disabling automatic recognition preserves explicit manual wait");
        check(p.hold(16000)==null,"Cleared automatic state cannot outlive manual timer");
        p.reset(0,3);for(int t=0;t<=20000;t+=500)p.observe(t,detect(shopping));
        check(p.hold(20000)==null&&p.ready(20000),"Reported normal page keeps normal swipe schedule across repeated scans");
    }

    static void sessionTests(){
        SessionPolicy session=new SessionPolicy();session.start(0,30);
        check(session.update(0,true,true)==null,"Session starts in target");
        check(session.update(1000,false,true)==null,"Leaving immediately pauses without ending session");
        check(session.update(120999,true,true)==null,"Return before two minutes remains active");
        session.update(130000,false,true);
        check(session.update(250000,true,true)!=null,"Returning after two minutes cannot bypass expiry");
        session.start(0,30);session.update(1000,false,false);
        check(session.update(30999,false,false)==null,"Lock grace is thirty seconds");
        check(session.update(31000,true,true)!=null,"Wake at lock timeout still stops");
        session.start(0,30);session.update(1000,false,false);check(session.update(10000,true,true)==null,"Short lock may resume");
        check(session.update(1799999,true,true)==null&&session.update(1800000,true,true)!=null,"Thirty-minute limit includes time spent watching");
        session.start(0,5);session.update(1000,false,true);session.update(60000,false,true);
        check(session.update(121000,false,true)!=null,"Switching between other apps does not restart away timer");
        session.start(0,5);check(session.remainingMinutes(0)==5&&session.remainingMinutes(299999)==1,"Remaining time rounds up");
        session.start(0,0);check(session.remainingMinutes(0)==-1,"Disabled timer has no remaining-minute display");
        check(session.update(1800000,true,true)==null&&session.update(86400000,true,true)==null,"Unlimited viewing survives thirty minutes and a full day");
        session.update(86401000,false,true);check(session.update(86521000,true,true)!=null,"Unlimited viewing still stops after a prolonged absence");
        session.start(0,0);session.update(1000,false,false);check(session.update(31000,true,true)!=null,"Unlimited viewing still stops after a long lock");
    }

    static void returnTests(){
        AdReturnPolicy r=new AdReturnPolicy();
        check(!r.observe(0,false,false,false,true,true),"Unrelated game's reward cannot initiate a return");
        check(!r.observe(100,true,false,true,false,false)&&r.pending(),"Target preview arms a cycle");
        check(!r.observe(200,false,false,false,false,true),"Game title is irrelevant to waiting");
        check(!r.observe(15000,false,false,false,true,true),"Reward must be stable");
        check(!r.observe(15699,false,false,false,true,true),"Reward stability boundary");
        check(r.observe(15700,false,false,false,true,true)&&r.returning(),"Stable reward requests return once");
        check(!r.observe(17000,false,false,false,true,true),"Repeated reward cannot trigger repeat task navigation");
        r.cancel();r.observe(18000,true,false,false,false,false);
        check(!r.pending(),"Returning normally cannot start a new ad cycle");
        r.observe(19000,true,false,true,false,false);check(r.pending(),"A later episode can arm a fresh cycle");
        r.observe(20000,false,true,false,false,false);
        check(!r.observe(21000,false,false,false,true,true)&&!r.pending(),"Manual home or recents cancels pending return");
        r.reset();r.observe(0,true,false,true,false,false);
        check(!r.observe(30000,false,false,false,true,true)&&!r.pending(),"Stale preview does not arm unrelated later app");
        r.reset();r.observe(0,true,false,true,false,false);r.observe(1000,false,false,false,false,true);
        check(!r.observe(121000,false,false,false,true,true)&&!r.pending(),"Abandoned ad cycle expires");
        r.reset();r.observe(0,true,false,true,false,false);r.observe(1000,true,false,false,false,true);
        r.observe(2000,true,false,false,true,true);
        check(r.observe(2700,true,false,false,true,true),"Shared host package can still contain another ad task");
        r.reset();r.observe(0,true,false,true,true,false);
        check(!r.observe(1000,true,false,true,true,false),"Preview itself cannot trigger return even with reward text");
        r.observe(2000,false,false,false,true,true);r.observe(2300,false,false,false,false,true);
        check(!r.observe(3000,false,false,false,true,true)&&r.observe(3700,false,false,false,true,true),"Reward disappearance resets stabilization");
        r.cancel();check(!r.observe(4000,true,false,true,false,false)&&!r.pending(),"Cancel suppresses same visible preview");
        r.observe(4500,true,false,false,false,false);r.observe(5000,true,false,true,false,false);
        check(r.pending(),"Cancel does not disable future episodes");
        r.reset();check(!r.pending()&&!r.returning(),"Mode switch and stop reset pending work");
        check(AdReturnPolicy.contains(Arrays.asList("即将播放", "广告"),"即将播放广告"),"Split accessible preview nodes");
        check(AdReturnPolicy.contains(Arrays.asList("已获得 奖励"),"已获得奖励"),"Whitespace-tolerant reward recognition");
        check(!AdReturnPolicy.contains(Arrays.asList("未获得奖励", "浏览2秒得奖励", "加载99%"),"已获得奖励"),"No return from unfinished rewards or game loading");
    }

    static void popupTests(){
        check(PopupPolicy.matches(Arrays.asList("睡前小福利 看视频赚钱","打卡即可得金豆","打卡赚钱")),"Screenshot 33 popup");
        check(PopupPolicy.matches(Arrays.asList("睡前小福利", "看视频赚钱","开始赚钱即可获得1088金豆","提醒我19点后打卡赚钱")),"Screenshot 11 popup, split heading");
        check(PopupPolicy.matches(Arrays.asList("睡前小福利 看视频赚钱","已攒 3440 记得到点来领奖励哦","提醒我明天6点领3440金豆")),"Screenshot 22 popup");
        check(PopupPolicy.matches(Arrays.asList("睡前小福利 看视频赚钱","开始赚钱即可获得999金豆")),"Reward amount must not be hard-coded");
        check(!PopupPolicy.matches(Arrays.asList("看视频赚钱","打卡赚钱","奖励","金豆","关闭")),"Ordinary reward pages do not match");
        check(!PopupPolicy.matches(Arrays.asList("睡前小福利 看视频赚钱","普通视频")),"Heading alone does not match");
        check(!PopupPolicy.matches(Arrays.asList("轻滑助手","发现淘宝弹窗，10 秒后关闭","关闭弹窗")),"Own overlay does not match");
        PopupPolicy sheet=new PopupPolicy();
        check(!sheet.observe(0,false)&&!sheet.shouldClose(60000),"No popup means no closure");
        check(sheet.observe(1000,true)&&sheet.remaining(1000)==10,"First popup starts ten-second wait");
        for(int t=1500;t<11000;t+=500){sheet.observe(t,true);check(!sheet.shouldClose(t),"Repeated scans never close early");}
        check(sheet.remaining(10999)==1&&!sheet.shouldClose(10999)&&sheet.shouldClose(11000),"Ten-second boundary, no timer reset");
        sheet.attempted(11000);
        check(!sheet.shouldClose(12999)&&sheet.shouldClose(13000),"No rapid repeated clicks");
        sheet.attempted(13000);check(sheet.exhausted()&&!sheet.shouldClose(60000),"At most two verified-close attempts");
        check(sheet.observe(13500,false)&&!sheet.observe(14000,false),"Wait for stable disappearance before resuming");
        check(sheet.observe(15000,true)&&sheet.remaining(15000)==10&&!sheet.exhausted(),"New popup gets a fresh wait");
        sheet.observe(16000,false);check(sheet.observe(16300,true)&&sheet.remaining(16300)==9,"Brief node loss does not restart timer or resume flips");
        sheet.observe(17000,false);check(!sheet.observe(17500,false)&&!sheet.shouldClose(60000),"Early disappearance cancels pending close");
        sheet.observe(70000,true);sheet.reset();check(!sheet.shouldClose(80000)&&!sheet.observe(80000,false),"Leaving, locking, pausing, and stopping cancel pending action");
    }
}
