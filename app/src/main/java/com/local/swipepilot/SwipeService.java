package com.local.swipepilot;

import android.accessibilityservice.*;
import android.app.KeyguardManager;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.*;
import android.widget.*;
import java.util.*;

public class SwipeService extends AccessibilityService {
    public static SwipeService instance;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final SwipePolicy policy=new SwipePolicy();
    private final PopupPolicy popup=new PopupPolicy();
    private boolean popupBlocked;
    private final SessionPolicy session=new SessionPolicy();
    private final AdReturnPolicy adReturn=new AdReturnPolicy();
    private final Set<String> launchers=new HashSet<>();
    private boolean adMode;
    private int returnStage,recentScroll;
    private long returnStarted,navNext,cardClickedAt;
    private String targetLabel="",returnFailure="";
    private Config config;
    private boolean running,paused,demo,inFlight,wasTarget,wasBlocked,collapsed=true,dockRight,receiverRegistered,dragging;
    private String target="",state="",pauseReason="已暂停，点继续恢复";
    private int gestures,generation;
    private WindowManager wm;
    private LinearLayout overlay;
    private TextView overlayStatus;
    private Button pauseButton,bubble,adModeButton,swipeModeButton,waitButton,endWaitButton;
    private WindowManager.LayoutParams overlayParams;
    private final Runnable tick=new Runnable(){public void run(){try{step();}catch(Exception e){ServiceDiagnostics.stepFailed(e);paused=true;pauseReason="读取页面遇到问题，点继续重试";state=pauseReason;render();}finally{if(running)handler.postDelayed(this,500);}}};
    private final BroadcastReceiver screenReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(running&&Intent.ACTION_SCREEN_OFF.equals(i.getAction())){
        popup.reset();popupBlocked=false;adReturn.cancel();returnStage=0;String reason=session.update(SystemClock.elapsedRealtime(),false,false);wasTarget=false;if(reason!=null)finishSession(reason);else{state="已锁屏，暂停翻页";render();}
    }}};

    @Override protected void onServiceConnected(){
        super.onServiceConnected();instance=this;config=new Config(this);wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        ServiceDiagnostics.event("service_connected");
        for(android.content.pm.ResolveInfo info:getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0))launchers.add(info.activityInfo.packageName);
        if(!receiverRegistered){IntentFilter f=new IntentFilter(Intent.ACTION_SCREEN_OFF);if(Build.VERSION.SDK_INT>=33)registerReceiver(screenReceiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(screenReceiver,f);receiverRegistered=true;}
    }
    public String status(){return state;}
    public boolean isRunning(){return running;}
    public void startSession(String pkg,boolean demoMode){
        ServiceDiagnostics.event("session_start");
        stopSession();target=pkg;demo=demoMode;targetLabel=demo?"轻滑助手":config.name();adMode=!demo&&config.adReturnMode();adReturn.reset();returnStage=0;returnFailure="";running=true;paused=false;gestures=0;collapsed=true;dockRight=false;
        long now=SystemClock.elapsedRealtime();policy.reset(now,interval());session.start(now,config.sessionMinutes());state="等待目标应用";config.prefs.edit().putString("lastStop","").apply();
        showOverlay();handler.post(tick);Toast.makeText(this,"已启动，点屏幕侧边的小圆钮可展开",Toast.LENGTH_LONG).show();
    }
    public void stopSession(){if(running)finishSession("本次使用已停止");}
    private void finishSession(String reason){
        ServiceDiagnostics.event("session_stop");
        popup.reset();popupBlocked=false;adReturn.reset();returnStage=0;returnFailure="";boolean wasRunning=running;running=false;paused=false;inFlight=false;wasTarget=false;wasBlocked=false;dragging=false;generation++;
        handler.removeCallbacks(tick);state=reason;
        if(overlay!=null){try{wm.removeView(overlay);}catch(Exception ignored){}overlay=null;}
        if(config!=null&&wasRunning)config.prefs.edit().putString("lastStop",reason).apply();
        if(wasRunning&&!reason.equals("本次使用已停止"))Toast.makeText(this,reason,Toast.LENGTH_LONG).show();
    }
    private int interval(){return demo?3:config.randomInterval();}
    private boolean unlocked(){return ((PowerManager)getSystemService(POWER_SERVICE)).isInteractive()&&!((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked();}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(!running)return;
        if(event.getEventType()==AccessibilityEvent.TYPE_TOUCH_INTERACTION_START&&!inFlight)policy.defer(SystemClock.elapsedRealtime(),interval());
    }
    @Override public void onInterrupt(){ServiceDiagnostics.event("service_interrupted");if(running){adReturn.cancel();returnStage=0;paused=true;pauseReason="系统中断，点继续恢复";state=pauseReason;render();}}
    @Override public boolean onUnbind(Intent intent){ServiceDiagnostics.event("service_unbound");stopSession();instance=null;return super.onUnbind(intent);}
    @Override public void onDestroy(){ServiceDiagnostics.event("service_destroyed");handler.removeCallbacksAndMessages(null);stopSession();instance=null;if(receiverRegistered)unregisterReceiver(screenReceiver);super.onDestroy();}

    private AccessibilityNodeInfo foreground(){
        List<AccessibilityWindowInfo> windows=getWindows();
        for(AccessibilityWindowInfo w:windows){
            if(w.getType()==AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY||w.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD)continue;
            if(w.isActive()||w.isFocused()){AccessibilityNodeInfo root=w.getRoot();if(root!=null)return root;}
        }
        // A floating control may take focus. Use the top application window beneath it.
        for(AccessibilityWindowInfo w:windows)if(w.getType()==AccessibilityWindowInfo.TYPE_APPLICATION){AccessibilityNodeInfo root=w.getRoot();if(root!=null)return root;}
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root!=null){AccessibilityWindowInfo w=root.getWindow();if(w!=null){boolean excluded=w.getType()==AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY||w.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD;w.recycle();if(excluded){root.recycle();return null;}}}
        return root;
    }
    private void step(){
        if(!running)return;long now=SystemClock.elapsedRealtime();boolean unlocked=unlocked();
        AccessibilityNodeInfo root=unlocked?foreground():null;
        try{
            boolean inTarget=root!=null&&target.equals(String.valueOf(root.getPackageName()));
            String reason=session.update(now,inTarget,unlocked);
            if(reason!=null){finishSession(reason);return;}
            if(!inTarget||!unlocked||paused){popup.reset();popupBlocked=false;}
            if(!unlocked){adReturn.cancel();returnStage=0;wasTarget=false;state="已锁屏，暂停运行";render();return;}
            if(inTarget&&!paused&&(target.equals("com.taobao.taobao")||demo)&&handlePopup(now,root)){render();return;}
            if(adMode){stepAdReturn(now,root,inTarget);render();return;}
            if(!inTarget){wasTarget=false;state="暂时离开，回来继续";render();return;}
            if(paused){state=pauseReason;render();return;}
            if(dragging){policy.defer(now,interval());state="正在移动按钮，暂停翻页";render();return;}
            if(!wasTarget){policy.defer(now,interval());wasTarget=true;}
            for(AccessibilityWindowInfo w:getWindows())if(w.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD){state="正在输入，暂停翻页";wasTarget=false;render();return;}
            ArrayList<String> lines=new ArrayList<>();collect(root,lines,new int[]{0});
            // Practice controls mention ads; only the simulated video status is evidence in this fixture.
            if(demo){ArrayList<String> statusLines=new ArrayList<>();for(String line:lines)if(line.startsWith("练习页面状态："))statusLines.add(line.substring("练习页面状态：".length()));lines=statusLines;}
            if(demo||config.bool("detect",true))policy.observe(now,AdDetector.detect(lines,demo?"":config.text("keywords")));else policy.clearAutomatic();
            String hold=policy.hold(now);
            if(hold!=null){wasBlocked=true;state=hold;render();return;}
            if(wasBlocked){policy.defer(now,interval());wasBlocked=false;}
            if(inFlight){state="正在上滑";render();return;}
            if(policy.ready(now)){
                Rect bounds=new Rect();root.getBoundsInScreen(bounds);
                if(bounds.width()<100||bounds.height()<200){state="等待页面显示";render();return;}
                swipe(bounds);
            }else state=((policy.nextSwipe-now+999)/1000)+" 秒后翻页"+(session.remainingMinutes(now)>=0?" · 还可用 "+session.remainingMinutes(now)+" 分钟":"");
            render();
        }finally{if(root!=null)root.recycle();}
    }
    private boolean handlePopup(long now,AccessibilityNodeInfo root){
        android.util.DisplayMetrics metrics=getResources().getDisplayMetrics();
        try(TaobaoPopup sheet=new TaobaoPopup(root,new Rect(0,0,metrics.widthPixels,metrics.heightPixels))){
            boolean visible=sheet.visible();boolean blocked=popup.observe(now,visible);
            if(!blocked){
                if(popupBlocked){popupBlocked=false;policy.clearAutomatic();policy.defer(now,interval());wasBlocked=false;}
                return false;
            }
            popupBlocked=true;wasTarget=true;
            if(!visible){state="弹窗已消失，正在确认";return true;}
            long remaining=popup.remaining(now);
            if(remaining>0){state="发现淘宝弹窗，"+remaining+" 秒后关闭";return true;}
            if(inFlight||dragging){state="等待关闭弹窗";return true;}
            for(AccessibilityWindowInfo w:getWindows())if(w.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD){state="正在输入，暂停关闭弹窗";return true;}
            if(popup.exhausted()){state="弹窗未关闭，请手动点 ×";return true;}
            if(!popup.shouldClose(now)){state="正在确认弹窗已关闭";return true;}
            AccessibilityNodeInfo close=sheet.closeControl();
            if(close==null){state="未识别关闭按钮，请手动点 ×";return true;}
            Rect bounds=new Rect();close.getBoundsInScreen(bounds);
            if(overlay!=null){int[] pos=new int[2];overlay.getLocationOnScreen(pos);Rect panel=new Rect(pos[0],pos[1],pos[0]+overlay.getWidth(),pos[1]+overlay.getHeight());
                if(panel.contains(bounds.centerX(),bounds.centerY())){if(!collapsed)setCollapsed(true);else state="请移开侧边按钮，让出弹窗 ×";return true;}}
            popup.attempted(now);state="正在关闭淘宝弹窗";
            if(!close.performAction(AccessibilityNodeInfo.ACTION_CLICK))tapPopupClose(bounds);
            return true;
        }
    }
    private void tapPopupClose(Rect bounds){
        // The coordinate comes from the freshly identified close control, never the supplied photo.
        Path path=new Path();path.moveTo(bounds.centerX(),bounds.centerY());inFlight=true;final int attempt=generation;
        boolean sent=dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,80)).build(),new GestureResultCallback(){
            @Override public void onCompleted(GestureDescription g){if(running&&generation==attempt)inFlight=false;}
            @Override public void onCancelled(GestureDescription g){if(running&&generation==attempt)inFlight=false;}
        },handler);if(!sent)inFlight=false;
    }
    private boolean systemSurface(String pkg){return launchers.contains(pkg)||pkg.equals("android")||pkg.equals("com.android.systemui")||pkg.equals("com.android.settings")||(!demo&&pkg.equals(getPackageName()));}
    private void stepAdReturn(long now,AccessibilityNodeInfo root,boolean inTarget){
        wasTarget=inTarget;
        if(paused){state=pauseReason;return;}
        if(dragging){state="正在移动按钮，暂停运行";return;}
        if(root==null){state="等待页面显示";return;}
        String pkg=String.valueOf(root.getPackageName());
        if(returnStage!=0){navigateRecentTask(now,root,inTarget,pkg);return;}
        for(AccessibilityWindowInfo w:getWindows())if(w.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD){adReturn.cancel();state="正在输入，暂停运行";return;}
        ArrayList<String> lines=new ArrayList<>();collect(root,lines,new int[]{0});
        boolean preview=AdReturnPolicy.contains(lines,"即将播放广告");
        boolean reward=AdReturnPolicy.contains(lines,"已获得奖励");
        boolean adPage=AdReturnPolicy.contains(lines,"浏览")&&AdReturnPolicy.contains(lines,"得奖励");
        if(adReturn.observe(now,inTarget,systemSurface(pkg),preview,reward,adPage)){
            returnFailure="";returnStage=1;recentScroll=0;returnStarted=now;navNext=now+600;
            if(!collapsed)setCollapsed(true);
            state="奖励已完成，正在返回";
            if(!performGlobalAction(GLOBAL_ACTION_RECENTS))failReturn();
            return;
        }
        if(inTarget){returnFailure="";state=adReturn.status();}
        else state=adReturn.pending()?adReturn.status():returnFailure.isEmpty()?"暂时离开，回来继续":returnFailure;
    }
    private void navigateRecentTask(long now,AccessibilityNodeInfo root,boolean inTarget,String pkg){
        state="奖励已完成，正在返回";
        if(now-returnStarted>8000){failReturn();return;}
        if(now<navNext)return;
        if(returnStage==2){
            if(inTarget&&!systemSurface(pkg)&&now-cardClickedAt>=1000){returnStage=0;adReturn.cancel();state="已回到视频，等待下次广告";}
            return;
        }
        if(!launchers.contains(pkg)&&!pkg.equals("com.android.systemui"))return;
        if(RecentTasks.clickTarget(root,targetLabel,dp(60),dp(120))){returnStage=2;cardClickedAt=now;navNext=now+1000;return;}
        // Try neighbouring cards in both directions. Never close or clear tasks.
        if(recentScroll<5){boolean forward=recentScroll<2;recentScroll++;RecentTasks.scroll(root,forward,new int[]{0});navNext=now+700;}
        else failReturn();
    }
    private void failReturn(){returnStage=0;adReturn.cancel();returnFailure="未找到原应用，请手动切回";state=returnFailure;}
    public void changeMode(boolean useAdReturn){
        if(adMode==useAdReturn)return;
        adMode=useAdReturn;if(!demo)config.adReturnMode(adMode);
        popup.reset();popupBlocked=false;adReturn.reset();returnStage=0;returnFailure="";wasBlocked=false;
        policy.reset(SystemClock.elapsedRealtime(),interval());
        state=paused?pauseReason:adMode?"广告返回模式，不自动翻页":"计时翻页模式，重新计时";
        render();
    }
    private void collect(AccessibilityNodeInfo n,List<String> text,int[] count){
        if(++count[0]>500)return;
        if(!demo&&getPackageName().contentEquals(n.getPackageName()==null?"":n.getPackageName()))return;
        if(n.isVisibleToUser()){if(n.getText()!=null)text.add(n.getText().toString());if(n.getContentDescription()!=null)text.add(n.getContentDescription().toString());}
        for(int i=0;i<n.getChildCount()&&count[0]<500;i++){AccessibilityNodeInfo c=n.getChild(i);if(c!=null){collect(c,text,count);c.recycle();}}
    }
    private void swipe(Rect bounds){
        float x=bounds.left+bounds.width()*(demo?65:config.number("x",65))/100f;
        float y1=bounds.top+bounds.height()*(demo?78:config.number("start",78))/100f;
        float y2=bounds.top+bounds.height()*(demo?25:config.number("end",25))/100f;
        if(overlay!=null){int[] xy=new int[2];overlay.getLocationOnScreen(xy);Rect panel=new Rect(xy[0],xy[1],xy[0]+overlay.getWidth(),xy[1]+overlay.getHeight());if(x>=panel.left&&x<=panel.right&&y1>=panel.top&&y2<=panel.bottom){
            if(!collapsed){setCollapsed(true);policy.defer(SystemClock.elapsedRealtime(),interval());state="已收起浮窗，继续翻页";return;}
            paused=true;pauseReason="请把侧边小圆钮拖到另一边，再点继续";state=pauseReason;return;
        }}
        Path path=new Path();path.moveTo(x,y1);path.lineTo(x,y2);inFlight=true;final int attempt=generation;
        boolean sent=dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,demo?450:config.number("duration",450))).build(),new GestureResultCallback(){
            @Override public void onCompleted(GestureDescription gesture){if(!running||attempt!=generation)return;inFlight=false;gestures++;long now=SystemClock.elapsedRealtime();policy.gestureCompleted(now);policy.defer(now,interval());state="上滑完成";}
            @Override public void onCancelled(GestureDescription gesture){if(!running||attempt!=generation)return;inFlight=false;policy.defer(SystemClock.elapsedRealtime(),interval());state="手势被中断，稍后再试";}
        },handler);
        if(!sent){inFlight=false;paused=true;pauseReason="系统未接受手势，请点继续重试";state=pauseReason;}
    }
    public void returnToTarget(){
        if(!running||!unlocked())return;
        adReturn.cancel();returnStage=0;returnFailure="";
        Intent i=demo?new Intent(this,DemoActivity.class):getPackageManager().getLaunchIntentForPackage(target);
        if(i==null){paused=true;pauseReason="应用无法打开，请重新选择";state=pauseReason;render();return;}
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try{startActivity(i);policy.defer(SystemClock.elapsedRealtime(),interval());}catch(Exception e){paused=true;pauseReason="请手动切回目标应用";state=pauseReason;render();}
    }
    private void dismissAd(){
        if(adMode){adReturn.cancel();returnStage=0;state="已取消本次广告识别，等待下次广告";render();return;}
        policy.dismissAd(SystemClock.elapsedRealtime(),interval());wasBlocked=false;paused=false;state="已结束等待，恢复翻页";render();
        Toast.makeText(this,"已结束广告等待，按正常间隔翻页",Toast.LENGTH_SHORT).show();
    }
    private void showOverlay(){
        overlay=new LinearLayout(this);overlay.setOrientation(LinearLayout.VERTICAL);
        overlayParams=new WindowManager.LayoutParams(dp(56),WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
        overlayParams.gravity=Gravity.TOP|Gravity.LEFT;overlayParams.x=0;overlayParams.y=dp(150);
        buildOverlay();wm.addView(overlay,overlayParams);render();
    }
    private void buildOverlay(){
        overlay.removeAllViews();overlayStatus=null;pauseButton=null;bubble=null;adModeButton=null;swipeModeButton=null;waitButton=null;endWaitButton=null;
        overlay.setPadding(collapsed?0:dp(8),collapsed?0:dp(6),collapsed?0:dp(8),collapsed?0:dp(8));
        overlay.setBackground(MainActivity.bg(Color.rgb(25,45,42),dp(collapsed?28:14)));
        if(collapsed){
            bubble=new Button(this);bubble.setAllCaps(false);bubble.setTextSize(20);bubble.setTextColor(Color.WHITE);bubble.setPadding(0,0,0,0);bubble.setMinWidth(0);bubble.setMinimumWidth(0);bubble.setBackground(MainActivity.bg(MainActivity.GREEN,dp(28)));bubble.setContentDescription("轻滑助手，点一下展开，拖动可移动");overlay.addView(bubble,new LinearLayout.LayoutParams(dp(56),dp(56)));bubble.setOnClickListener(v->setCollapsed(false));attachDrag(bubble,true);
        }else{
            LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);overlay.addView(head);
            TextView grip=new TextView(this);grip.setText("轻滑助手");grip.setTextSize(18);grip.setTextColor(Color.WHITE);grip.setMinHeight(dp(48));grip.setGravity(Gravity.CENTER_VERTICAL);grip.setContentDescription("拖动悬浮窗");head.addView(grip,new LinearLayout.LayoutParams(0,-2,1));attachDrag(grip,false);
            Button hide=smallButton(head,"收起",v->setCollapsed(true));hide.setTextSize(16);hide.setLayoutParams(new LinearLayout.LayoutParams(dp(52),dp(48)));
            overlayStatus=new TextView(this);overlayStatus.setTextColor(Color.WHITE);overlayStatus.setTextSize(18);overlayStatus.setMinHeight(dp(52));overlay.addView(overlayStatus);
            LinearLayout modeRow=new LinearLayout(this);overlay.addView(modeRow);
            adModeButton=smallButton(modeRow,"广告返回",v->changeMode(true));swipeModeButton=smallButton(modeRow,"计时翻页",v->changeMode(false));
            adModeButton.setTextSize(16);swipeModeButton.setTextSize(16);
            LinearLayout row=new LinearLayout(this);overlay.addView(row);
            pauseButton=smallButton(row,"暂停",v->{paused=!paused;adReturn.cancel();returnStage=0;pauseReason="已暂停，点继续恢复";state=paused?pauseReason:adMode?"等待下次广告":"继续翻页";policy.defer(SystemClock.elapsedRealtime(),interval());render();});
            smallButton(row,"回主应用",v->returnToTarget());
            row=new LinearLayout(this);overlay.addView(row);
            waitButton=smallButton(row,"广告等待",v->{policy.waitManually(SystemClock.elapsedRealtime(),config.waitSeconds());wasBlocked=true;state="等待 "+config.waitSeconds()+" 秒";render();});
            endWaitButton=smallButton(row,"结束等待",v->dismissAd());
            row=new LinearLayout(this);overlay.addView(row);
            smallButton(row,"最近任务",v->{adReturn.cancel();returnStage=0;performGlobalAction(GLOBAL_ACTION_RECENTS);});smallButton(row,"停止",v->stopSession());
        }
    }
    private void setCollapsed(boolean value){
        if(overlay==null)return;
        int screenWidth=getResources().getDisplayMetrics().widthPixels;
        if(value)dockRight=overlayParams.x+overlay.getWidth()/2>screenWidth/2;
        collapsed=value;overlayParams.width=dp(collapsed?56:196);overlayParams.x=dockRight?screenWidth-overlayParams.width:0;
        buildOverlay();overlayParams.y=Math.max(0,Math.min(overlayParams.y,getResources().getDisplayMetrics().heightPixels-dp(collapsed?100:430)));
        wm.updateViewLayout(overlay,overlayParams);render();
    }
    private void attachDrag(View view,boolean click){
        view.setOnTouchListener(new View.OnTouchListener(){float sx,sy;int px,py;boolean moved;
            @Override public boolean onTouch(View v,MotionEvent event){
                if(event.getAction()==MotionEvent.ACTION_DOWN){dragging=true;policy.defer(SystemClock.elapsedRealtime(),interval());sx=event.getRawX();sy=event.getRawY();px=overlayParams.x;py=overlayParams.y;moved=false;return true;}
                if(event.getAction()==MotionEvent.ACTION_MOVE){float dx=event.getRawX()-sx,dy=event.getRawY()-sy;if(Math.abs(dx)+Math.abs(dy)>dp(6))moved=true;
                    if(moved){android.util.DisplayMetrics m=getResources().getDisplayMetrics();overlayParams.x=Math.max(0,Math.min(m.widthPixels-overlay.getWidth(),px+(int)dx));overlayParams.y=Math.max(0,Math.min(m.heightPixels-overlay.getHeight()-dp(40),py+(int)dy));wm.updateViewLayout(overlay,overlayParams);}return true;}
                if(event.getAction()==MotionEvent.ACTION_UP){dragging=false;policy.defer(SystemClock.elapsedRealtime(),interval());if(!moved&&click)v.performClick();else if(collapsed){dockRight=overlayParams.x+overlay.getWidth()/2>getResources().getDisplayMetrics().widthPixels/2;overlayParams.x=dockRight?getResources().getDisplayMetrics().widthPixels-overlay.getWidth():0;wm.updateViewLayout(overlay,overlayParams);}return true;}
                if(event.getAction()==MotionEvent.ACTION_CANCEL){dragging=false;policy.defer(SystemClock.elapsedRealtime(),interval());return true;}
                return true;
            }
        });
    }
    private Button smallButton(LinearLayout row,String text,View.OnClickListener listener){Button b=new Button(this);b.setText(text);b.setTextSize(18);b.setAllCaps(false);b.setPadding(dp(3),dp(8),dp(3),dp(8));b.setMinHeight(dp(52));b.setMinWidth(0);b.setMinimumWidth(0);b.setTextColor(Color.WHITE);b.setBackground(MainActivity.bg(Color.rgb(45,78,70),dp(7)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(2),dp(3),dp(2),0);row.addView(b,p);b.setOnClickListener(listener);return b;}
    private void render(){
        if(overlay==null)return;
        if(overlayStatus!=null)overlayStatus.setText(state);
        if(pauseButton!=null)pauseButton.setText(paused?"继续":"暂停");
        if(adModeButton!=null){
            renderModeButton(adModeButton,"广告返回",adMode);renderModeButton(swipeModeButton,"计时翻页",!adMode);
            waitButton.setVisibility(adMode?View.GONE:View.VISIBLE);
            endWaitButton.setText(adMode?"取消本次识别":"结束等待");
            endWaitButton.setContentDescription(adMode?"取消本次广告返回识别":"结束广告等待并恢复翻页");
        }
        if(bubble!=null){bubble.setText(paused?"Ⅱ":popupBlocked?"窗":adMode?(returnStage!=0?"返":adReturn.pending()?"等":!wasTarget?"回":"看"):!wasTarget?"回":policy.hold(SystemClock.elapsedRealtime())!=null?"等":"滑");bubble.setContentDescription("轻滑助手，"+(adMode?"广告返回模式：":"计时翻页模式：")+state+"。点一下展开，拖动可移动");}
    }
    private void renderModeButton(Button button,String label,boolean selected){
        String text=(selected?"✓ ":"")+label;
        if(!text.contentEquals(button.getText()))button.setText(text);
        if(button.isSelected()!=selected||button.getTag()==null){button.setSelected(selected);button.setTag(Boolean.TRUE);button.setBackground(MainActivity.bg(selected?MainActivity.GREEN:Color.rgb(45,78,70),dp(7)));}
        button.setContentDescription(label+(selected?"，已选中":"，点此切换"));
    }
    @Override public void onConfigurationChanged(android.content.res.Configuration c){super.onConfigurationChanged(c);if(overlay!=null)setCollapsed(collapsed);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
