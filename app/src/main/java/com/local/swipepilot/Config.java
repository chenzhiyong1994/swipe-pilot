package com.local.swipepilot;
import android.content.*;
import java.util.*;

final class Config {
    final SharedPreferences prefs;
    Config(Context c) {
        prefs=c.getSharedPreferences("config",Context.MODE_PRIVATE);
        if(prefs.getInt("defaultsVersion",0)<2){
            SharedPreferences.Editor e=prefs.edit();
            for(Map.Entry<String,?> item:prefs.getAll().entrySet()){
                String k=item.getKey();Object value=item.getValue();
                if(k.endsWith(":maxInterval")&&Integer.valueOf(30).equals(value))e.putInt(k,20);
                if(k.endsWith(":wait")&&Integer.valueOf(35).equals(value))e.putInt(k,15);
            }
            e.putInt("defaultsVersion",2).apply();
        }
    }
    String target(){return prefs.getString("target","");}
    String name(){return prefs.getString("name","未选择");}
    void target(String pkg,String name){prefs.edit().putString("target",pkg).putString("name",name).apply();}
    String key(String k){return target()+":"+k;}
    int number(String k,int fallback){return prefs.getInt(key(k),fallback);}
    boolean bool(String k,boolean fallback){return prefs.getBoolean(key(k),fallback);}
    String text(String k){return prefs.getString(key(k),"");}
    boolean adReturnMode(){return bool("adReturnMode",name().contains("星芽"));}
    void adReturnMode(boolean enabled){prefs.edit().putBoolean(key("adReturnMode"),enabled).apply();}
    int interval(){return number("interval",15);}
    int maxInterval(){return number("maxInterval",20);}
    int randomInterval(){int min=interval(),max=Math.max(min,maxInterval());return min+new Random().nextInt(max-min+1);}
    int waitSeconds(){return number("wait",15);}
    int configuredSessionMinutes(){return Math.max(5,prefs.getInt("sessionMinutes",30));}
    int sessionMinutes(){return prefs.getBoolean("timedStop",false)?configuredSessionMinutes():0;}
}
