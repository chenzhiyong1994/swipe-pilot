package com.local.swipepilot;

import android.app.*;
import android.os.*;
import android.provider.Settings;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    static final int INK=Color.rgb(25,45,42), GREEN=Color.rgb(22,125,114), MUTED=Color.rgb(87,104,97), LIGHT=Color.rgb(233,242,239);
    private Config config;
    private LinearLayout content, grid, customChoice;
    private Button startButton, timeButton;
    private ImageButton accessButton;
    private TextView sessionStatus;
    private String selectedPkg="", selectedName="";
    private boolean fromPicker;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){public void run(){updateStatus();handler.postDelayed(this,1000);}};
    private final String[] names={"淘宝","抖音","支付宝","红果短剧"};
    private final String[] packages={"com.taobao.taobao","com.ss.android.ugc.aweme","com.eg.android.AlipayGphone","com.phoenix.read"};

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);config=new Config(this);
        if(saved!=null){selectedPkg=saved.getString("selectedPkg","");selectedName=saved.getString("selectedName","");fromPicker=saved.getBoolean("fromPicker");}
        resolvePinnedApps();
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(245,247,245));
        content=column();content.setFocusableInTouchMode(true);content.setPadding(dp(18),dp(12),dp(18),dp(24));scroll.addView(content);setContentView(scroll);
        LinearLayout header=row();content.addView(header);
        TextView title=text("轻滑助手",30,INK,true);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        accessButton=iconButton(R.drawable.ic_access,"无障碍授权状态",v->showAccess());header.addView(accessButton,new LinearLayout.LayoutParams(dp(48),dp(48)));
        label(content,"选一个应用，点启动就能用",18,MUTED,false);
        LinearLayout card=card();LinearLayout cardHeader=row();card.addView(cardHeader);
        cardHeader.addView(text("选择应用",23,INK,true),new LinearLayout.LayoutParams(0,-2,1));
        cardHeader.addView(iconButton(R.drawable.ic_apps,"选择其他应用",v->selectApp()),new LinearLayout.LayoutParams(dp(48),dp(48)));
        startButton=button(card,"保存并启动",v->runTarget(),true);
        grid=column();card.addView(grid);customChoice=column();card.addView(customChoice);
        timeButton=button(content,"",v->showSettings(),false);
        label(content,"切换应用自动暂停，回来继续\n离开 2 分钟或锁屏 30 秒自动停止",18,MUTED,false);
        sessionStatus=label(content,"",18,MUTED,false);
        Button stop=button(content,"停止使用",v->{if(SwipeService.instance!=null)SwipeService.instance.stopSession();updateStatus();},false);
        stop.setTextColor(INK);
        renderSelection();content.requestFocus();
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("selectedPkg",selectedPkg);out.putString("selectedName",selectedName);out.putBoolean("fromPicker",fromPicker);}
    @Override public void onResume(){super.onResume();renderSelection();handler.removeCallbacks(refresh);handler.post(refresh);}
    @Override public void onPause(){super.onPause();handler.removeCallbacks(refresh);}
    private void updateStatus(){
        AccessState.State state=AccessState.read(this);
        String label=state==AccessState.State.READY?"无障碍已开启，可以使用":state==AccessState.State.CONNECTING?"无障碍已授权，服务等待连接":"无障碍尚未授权，点此开启";
        accessButton.setContentDescription(label);accessButton.setTooltipText(label);
        accessButton.setColorFilter(state==AccessState.State.READY?GREEN:state==AccessState.State.CONNECTING?Color.rgb(187,119,23):MUTED);
        String status=SwipeService.instance==null?config.prefs.getString("lastStop",""):SwipeService.instance.status();
        sessionStatus.setText(status);sessionStatus.setVisibility(status.isEmpty()?View.GONE:View.VISIBLE);
    }
    private void showAccess(){
        AccessState.State state=AccessState.read(this);
        String title=state==AccessState.State.READY?"已开启，可以使用":state==AccessState.State.CONNECTING?"已授权，等待服务连接":"开启使用权限";
        String message=state==AccessState.State.READY?"权限已开启。选择应用后，点击保存并启动即可。":state==AccessState.State.CONNECTING?
            "系统已记录你的授权，助手服务暂未连接。请稍等；若一直不能启动，可在系统设置中关闭再开启「轻滑助手」。也请允许它在后台运行。":
            "首次使用需在系统设置中开启「轻滑助手」。它只在本机读取页面文字、识别广告并执行上滑，不上传页面内容。";
        showReadableDialog(new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("打开系统设置",(d,w)->{
            Intent i=new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);startActivity(i);
        }).setNegativeButton("知道了",null).create());
    }
    private void resolvePinnedApps(){
        for(ResolveInfo info:getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)){
            String label=info.loadLabel(getPackageManager()).toString();
            if(label.contains("红果")&&label.contains("短剧"))packages[3]=info.activityInfo.packageName;
        }
    }
    private boolean installed(String pkg){return getPackageManager().getLaunchIntentForPackage(pkg)!=null;}
    private Drawable appIcon(String pkg){try{return getPackageManager().getApplicationIcon(pkg);}catch(PackageManager.NameNotFoundException e){return null;}}
    private void renderSelection(){
        grid.removeAllViews();customChoice.removeAllViews();
        for(int r=0;r<2;r++){
            LinearLayout row=row();LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=dp(12);grid.addView(row,rp);
            for(int c=0;c<2;c++){
                final int n=r*2+c;boolean active=!fromPicker&&selectedPkg.equals(packages[n]);boolean available=installed(packages[n]);
                FrameLayout tile=new FrameLayout(this);tile.setBackground(tileBackground(active));tile.setSelected(active);
                tile.setContentDescription(names[n]+(active?"，已选中":available?"，点此选择":"，未安装"));tile.setFocusable(true);
                LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-1,1);if(c==0)tp.rightMargin=dp(6);else tp.leftMargin=dp(6);row.addView(tile,tp);
                LinearLayout center=column();center.setGravity(Gravity.CENTER);center.setPadding(dp(8),dp(14),dp(8),dp(10));tile.addView(center,new FrameLayout.LayoutParams(-1,-2));
                Drawable icon=appIcon(packages[n]);
                if(icon!=null){ImageView image=new ImageView(this);image.setImageDrawable(icon);image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);center.addView(image,new LinearLayout.LayoutParams(dp(54),dp(54)));}
                else{TextView fallback=text(names[n].substring(0,1),28,Color.WHITE,true);fallback.setGravity(Gravity.CENTER);fallback.setBackground(bg(Color.rgb(135,148,142),dp(12)));center.addView(fallback,new LinearLayout.LayoutParams(dp(54),dp(54)));}
                TextView name=text(names[n],21,INK,true);name.setGravity(Gravity.CENTER);center.addView(name);
                if(!available){TextView missing=text("未安装",16,MUTED,false);missing.setGravity(Gravity.CENTER);center.addView(missing);}
                if(active){ImageView tick=new ImageView(this);tick.setImageResource(R.drawable.ic_check);tick.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);FrameLayout.LayoutParams mark=new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.TOP|Gravity.RIGHT);mark.topMargin=dp(6);mark.rightMargin=dp(6);tile.addView(tick,mark);}
                center.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                tile.setOnClickListener(v->{if(!available){toast("手机还没有安装"+names[n]);return;}chooseTarget(packages[n],names[n],false);});
            }
        }
        if(fromPicker&&!selectedPkg.isEmpty()){
            Button selected=button(customChoice,"✓  "+selectedName,v->selectApp(),true);selected.setSelected(true);selected.setContentDescription(selectedName+"，已选中，点此更换");
        }
        boolean canStart=!selectedPkg.isEmpty()&&installed(selectedPkg);
        startButton.setEnabled(canStart);startButton.setBackground(bg(canStart?GREEN:Color.rgb(216,222,218),dp(14)));startButton.setTextColor(canStart?Color.WHITE:Color.rgb(123,135,127));
        timeButton.setText((config.adReturnMode()?"广告返回 · 不自动翻页":"翻页 "+config.interval()+"–"+config.maxInterval()+" 秒 · 广告 "+config.waitSeconds()+" 秒")+"\n"+(config.sessionMinutes()>0?"用 "+config.sessionMinutes()+" 分钟后停止":"不限使用时长")+"  ›");
    }
    private void chooseTarget(String pkg,String name,boolean picker){
        if(!installed(pkg)){toast("这个应用暂时无法打开，请选择其他应用");return;}
        if(SwipeService.instance!=null)SwipeService.instance.stopSession();
        selectedPkg=pkg;selectedName=name;fromPicker=picker;config.target(pkg,name);renderSelection();
    }
    private void runTarget(){
        if(selectedPkg.isEmpty()||!installed(selectedPkg))return;
        config.target(selectedPkg,selectedName);
        if(AccessState.read(this)!=AccessState.State.READY){showAccess();return;}
        SwipeService.instance.startSession(selectedPkg,false);SwipeService.instance.returnToTarget();
    }
    private void selectApp(){
        List<ResolveInfo> apps=getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);
        apps.sort((a,b)->a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));
        ArrayList<String> labels=new ArrayList<>(),packages=new ArrayList<>();
        for(ResolveInfo info:apps){String p=info.activityInfo.packageName;if(p.equals(getPackageName())||packages.contains(p))continue;packages.add(p);labels.add(info.loadLabel(getPackageManager()).toString());}
        LinearLayout box=column();box.setPadding(dp(16),0,dp(16),0);
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(22);search.setMinHeight(dp(60));search.setHint("搜索应用名称");box.addView(search);
        ListView list=new ListView(this);box.addView(list,new LinearLayout.LayoutParams(-1,Math.min(dp(380),getResources().getDisplayMetrics().heightPixels/2)));
        ArrayList<Integer> visible=new ArrayList<>();ArrayList<String> rows=new ArrayList<>();
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,rows){
            @Override public View getView(int position,View convertView,ViewGroup parent){TextView row=(TextView)super.getView(position,convertView,parent);row.setTextSize(22);row.setTextColor(INK);row.setSingleLine(false);row.setMinHeight(dp(64));row.setPadding(dp(12),dp(12),dp(12),dp(12));return row;}
        };list.setAdapter(adapter);
        Runnable filter=()->{visible.clear();rows.clear();String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);for(int n=0;n<labels.size();n++)if(labels.get(n).toLowerCase(Locale.ROOT).contains(q)){visible.add(n);rows.add(labels.get(n));}adapter.notifyDataSetChanged();};
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){filter.run();}public void afterTextChanged(android.text.Editable e){}});filter.run();
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("选择其他应用").setView(box).setNegativeButton("取消",null).create();
        list.setOnItemClickListener((p,v,pos,id)->{int n=visible.get(pos);chooseTarget(packages.get(n),labels.get(n),true);dialog.dismiss();});showReadableDialog(dialog);
    }
    private void showSettings(){
        if(selectedPkg.isEmpty()){toast("先点选要使用的应用，再调整时间");return;}
        ScrollView scroll=new ScrollView(this);LinearLayout box=column();box.setPadding(dp(20),dp(4),dp(20),dp(12));box.setFocusableInTouchMode(true);scroll.addView(box);
        label(box,"使用模式",20,INK,true);
        RadioGroup modes=new RadioGroup(this);box.addView(modes);
        RadioButton adMode=new RadioButton(this);adMode.setId(View.generateViewId());adMode.setText("广告返回，不自动翻页");adMode.setTextSize(20);adMode.setMinHeight(dp(56));modes.addView(adMode);
        RadioButton swipeMode=new RadioButton(this);swipeMode.setId(View.generateViewId());swipeMode.setText("按时间自动翻页");swipeMode.setTextSize(20);swipeMode.setMinHeight(dp(56));modes.addView(swipeMode);
        modes.check(config.adReturnMode()?adMode.getId():swipeMode.getId());
        TextView adHelp=label(box,"看到广告预告后等待奖励完成，再从最近任务回到原应用。",18,MUTED,false);
        LinearLayout pacing=column();box.addView(pacing);
        EditText min=field(pacing,"每次翻页最短等几秒",config.interval()),max=field(pacing,"每次翻页最长等几秒",config.maxInterval()),ad=field(pacing,"点广告等待时等几秒",config.waitSeconds());
        pacing.setVisibility(adMode.isChecked()?View.GONE:View.VISIBLE);adHelp.setVisibility(adMode.isChecked()?View.VISIBLE:View.GONE);
        modes.setOnCheckedChangeListener((group,id)->{pacing.setVisibility(adMode.isChecked()?View.GONE:View.VISIBLE);adHelp.setVisibility(adMode.isChecked()?View.VISIBLE:View.GONE);});
        Switch timedStop=new Switch(this);timedStop.setText("定时停止（可选）");timedStop.setTextSize(20);timedStop.setMinHeight(dp(56));timedStop.setChecked(config.sessionMinutes()>0);box.addView(timedStop);
        LinearLayout timerFields=column();box.addView(timerFields);EditText minutes=field(timerFields,"使用多少分钟后停止",config.configuredSessionMinutes());timerFields.setVisibility(timedStop.isChecked()?View.VISIBLE:View.GONE);
        timedStop.setOnCheckedChangeListener((b,checked)->timerFields.setVisibility(checked?View.VISIBLE:View.GONE));
        label(box,"离开应用 2 分钟、锁屏 30 秒，也会自动停止。",18,MUTED,false);
        LinearLayout advanced=column();advanced.setVisibility(View.GONE);
        addMoreSettingsToggle(box,advanced);box.addView(advanced);
        Switch detect=new Switch(this);detect.setText("自动识别广告");detect.setTextSize(20);detect.setMinHeight(dp(56));detect.setChecked(config.bool("detect",true));advanced.addView(detect);
        label(advanced,"额外倒计时标识（逗号分隔）",18,MUTED,false);label(advanced,"仅识别标识与秒数在同一项的倒计时；普通关键词不触发等待。",18,MUTED,false);EditText keywords=new EditText(this);keywords.setTextSize(22);keywords.setText(config.text("keywords"));advanced.addView(keywords);
        EditText x=field(advanced,"上滑水平位置 %（10–90）",config.number("x",65)),start=field(advanced,"上滑起点 %（20–90）",config.number("start",78)),end=field(advanced,"上滑终点 %（10–80）",config.number("end",25)),duration=field(advanced,"上滑时长，毫秒（150–1500）",config.number("duration",450));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("调整使用方式").setView(scroll).setPositiveButton("保存",null).setNegativeButton("取消",null).create();
        button(advanced,"打开练习页面",v->{dialog.dismiss();if(SwipeService.instance!=null)SwipeService.instance.stopSession();startActivity(new Intent(this,DemoActivity.class));},false);
        showReadableDialog(dialog);box.requestFocus();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{
            int i=integer(min,3,3600),m=integer(max,3,3600),w=integer(ad,5,600),limit=timedStop.isChecked()?integer(minutes,5,120):config.configuredSessionMinutes(),xx=integer(x,10,90),s=integer(start,20,90),e=integer(end,10,80),d=integer(duration,150,1500);
            if(m<i){max.setError("最长时间不能小于最短时间");return;}if(s-e<15){start.setError("起点至少比终点高 15");return;}
            config.prefs.edit().putBoolean(config.key("adReturnMode"),adMode.isChecked()).putInt(config.key("interval"),i).putInt(config.key("maxInterval"),m).putInt(config.key("wait"),w).putBoolean("timedStop",timedStop.isChecked()).putInt("sessionMinutes",limit).putInt(config.key("x"),xx).putInt(config.key("start"),s).putInt(config.key("end"),e).putInt(config.key("duration"),d).putBoolean(config.key("detect"),detect.isChecked()).putString(config.key("keywords"),keywords.getText().toString().trim()).apply();
            if(SwipeService.instance!=null&&SwipeService.instance.isRunning())SwipeService.instance.changeMode(adMode.isChecked());
            renderSelection();dialog.dismiss();
        }catch(IllegalArgumentException ignored){}});
    }
    private int integer(EditText field,int min,int max){try{int n=Integer.parseInt(field.getText().toString().trim());if(n>=min&&n<=max)return n;}catch(NumberFormatException ignored){}field.setError("请输入 "+min+"–"+max);field.requestFocus();throw new IllegalArgumentException();}
    private EditText field(LinearLayout parent,String title,int value){label(parent,title,18,MUTED,false);EditText field=new EditText(this);field.setTextSize(24);field.setSingleLine(true);field.setMinHeight(dp(56));field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);field.setText(""+value);parent.addView(field);return field;}
    private ImageButton iconButton(int icon,String description,View.OnClickListener listener){ImageButton b=new ImageButton(this);b.setImageResource(icon);b.setColorFilter(GREEN);b.setPadding(dp(12),dp(12),dp(12),dp(12));b.setBackground(bg(LIGHT,dp(24)));b.setContentDescription(description);b.setTooltipText(description);b.setOnClickListener(listener);return b;}
    private void addMoreSettingsToggle(LinearLayout parent,View advanced){
        LinearLayout toggle=row();toggle.setBaselineAligned(false);toggle.setGravity(Gravity.CENTER);toggle.setMinimumHeight(dp(60));toggle.setPadding(dp(12),dp(12),dp(12),dp(12));toggle.setBackground(bg(LIGHT,dp(14)));toggle.setFocusable(true);toggle.setContentDescription("更多设置，已收起");
        TextView title=new TextView(this);title.setText("更多设置");title.setTextSize(21);title.setTextColor(GREEN);title.setIncludeFontPadding(false);title.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);toggle.addView(title,new LinearLayout.LayoutParams(-2,-2));
        ImageView arrow=new ImageView(this);arrow.setImageResource(R.drawable.ic_chevron_down);arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);LinearLayout.LayoutParams icon=new LinearLayout.LayoutParams(dp(24),dp(24));icon.leftMargin=dp(10);toggle.addView(arrow,icon);
        toggle.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName(Button.class.getName());}});
        toggle.setOnClickListener(v->{boolean expand=advanced.getVisibility()!=View.VISIBLE;advanced.setVisibility(expand?View.VISIBLE:View.GONE);arrow.setRotation(expand?180:0);toggle.setContentDescription(expand?"更多设置，已展开":"更多设置，已收起");});
        LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.topMargin=dp(10);parent.addView(toggle,layout);
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout card(){LinearLayout c=column();c.setPadding(dp(14),dp(10),dp(14),dp(14));c.setBackground(bg(Color.WHITE,dp(18)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(12);content.addView(c,p);return c;}
    private GradientDrawable tileBackground(boolean selected){GradientDrawable d=bg(selected?LIGHT:Color.rgb(248,250,248),dp(14));d.setStroke(dp(selected?2:1),selected?GREEN:Color.rgb(227,233,229));return d;}
    private TextView text(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(5),0,dp(5));v.setLineSpacing(dp(3),1);if(bold)v.setTypeface(null,Typeface.BOLD);return v;}
    private TextView label(LinearLayout p,String s,int size,int color,boolean bold){TextView v=text(s,size,color,bold);p.addView(v);return v;}
    private Button button(LinearLayout p,String title,View.OnClickListener click,boolean primary){Button b=new Button(this);b.setText(title);b.setTextSize(21);b.setAllCaps(false);b.setMinHeight(dp(60));b.setPadding(dp(10),dp(10),dp(10),dp(10));b.setTextColor(primary?Color.WHITE:GREEN);b.setBackground(bg(primary?GREEN:LIGHT,dp(14)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(10);p.addView(b,lp);b.setOnClickListener(click);return b;}
    static void showReadableDialog(AlertDialog dialog){dialog.show();enlargeDialog(dialog.getWindow().getDecorView());}
    static void enlargeDialog(View view){if(view instanceof TextView){TextView t=(TextView)view;float density=t.getResources().getDisplayMetrics().scaledDensity;t.setTextSize(Math.max(t.getTextSize()/density,20));if(t instanceof Button)t.setMinimumHeight(Math.round(56*t.getResources().getDisplayMetrics().density));}if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)enlargeDialog(g.getChildAt(i));}}
    static GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
}
