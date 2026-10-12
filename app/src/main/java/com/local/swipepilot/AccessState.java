package com.local.swipepilot;
import android.content.*;
import android.provider.Settings;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityManager;

final class AccessState {
    enum State { OFF, CONNECTING, READY }
    static State read(Context context){
        if(SwipeService.instance!=null)return State.READY;
        ComponentName wanted=new ComponentName(context,SwipeService.class);
        String enabled=Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if(enabled!=null)for(String component:enabled.split(":"))if(wanted.equals(ComponentName.unflattenFromString(component)))return State.CONNECTING;
        AccessibilityManager manager=(AccessibilityManager)context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        for(AccessibilityServiceInfo service:manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK))
            if(wanted.equals(ComponentName.unflattenFromString(service.getId())))return State.CONNECTING;
        return State.OFF;
    }
}
