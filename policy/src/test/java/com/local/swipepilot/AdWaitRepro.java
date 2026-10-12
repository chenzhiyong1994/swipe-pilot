package com.local.swipepilot;
import java.util.*;
/** Synthetic evidence modelled on the reported symptoms, not a captured accessibility tree. */
public class AdWaitRepro {
    public static void main(String[] args){
        SwipePolicy p=new SwipePolicy();p.reset(0,15);
        List<String> page=Arrays.asList("领取优惠券","广告","00:43","15");
        for(int t=0;t<=30000;t+=500){p.observe(t,AdDetector.detect(page,""));if(t%10000==0)System.out.println("shopping t="+t/1000+"s: "+p.hold(t));}
        p.reset(0,15);
        for(int t=0;t<=20000;t+=500){p.observe(t,AdDetector.detect(Arrays.asList("广告倒计时 15 秒"),""));if(t%5000==0)System.out.println("fixed15 t="+t/1000+"s: "+p.hold(t));}
    }
}
