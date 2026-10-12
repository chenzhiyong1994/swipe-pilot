package com.local.swipepilot;

import android.app.*;
import android.content.*;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

public class DemoActivity extends Activity {
    boolean adPreview,adDemoDeparted;
    int page=1, remaining; float downY; TextView title, timer, count; LinearLayout root;
    final Handler handler=new Handler(Looper.getMainLooper());
    final Runnable countdown=new Runnable(){public void run(){if(remaining>0){remaining--;render();handler.postDelayed(this,1000);}}};
    @Override public void onCreate(Bundle state){super.onCreate(state);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(40,40,40,40);root.setBackgroundColor(Color.rgb(233,242,239));setContentView(root);
        title=text("",32);timer=text("",20);count=text("",16);
        text("上滑切换 · 每第三页模拟等待",14);
        button("开始演示自动上滑（3 秒）",v->{if(SwipeService.instance==null){MainActivity.showReadableDialog(new AlertDialog.Builder(this).setMessage("请返回首页，先开启轻滑助手无障碍服务。").setPositiveButton("好",null).create());}else SwipeService.instance.startSession(getPackageName(),true);});
        button("练习等待与弹窗",v->MainActivity.showReadableDialog(new AlertDialog.Builder(this).setTitle("选择练习")
            .setItems(new String[]{"广告等待 8 秒","淘宝弹窗：打卡","淘宝弹窗：提醒","淘宝弹窗：已完成"},(dialog,which)->{
                if(which==0){remaining=8;handler.removeCallbacks(countdown);render();handler.postDelayed(countdown,1000);}
                else showPopupPractice(which);
            }).setNegativeButton("取消",null).create()));
        button("练习广告返回（不翻页）",v->{
            if(SwipeService.instance==null){MainActivity.showReadableDialog(new AlertDialog.Builder(this).setMessage("请先开启无障碍服务，再开始练习。").setPositiveButton("好",null).create());return;}
            handler.removeCallbacksAndMessages(null);remaining=0;adPreview=true;
            SwipeService.instance.startSession(getPackageName(),true);SwipeService.instance.changeMode(true);render();
            handler.postDelayed(()->{adDemoDeparted=true;startActivity(new Intent(this,DemoAdActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK));},2500);
        });
        button("停止并返回设置",v->{if(SwipeService.instance!=null)SwipeService.instance.stopSession();finish();});render();
    }
    void showPopupPractice(int variant){
        if(SwipeService.instance==null){Toast.makeText(this,"请先开启无障碍服务",Toast.LENGTH_LONG).show();return;}
        handler.removeCallbacksAndMessages(null);remaining=0;adPreview=false;render();
        SwipeService.instance.startSession(getPackageName(),true);
        Dialog dialog=new Dialog(this);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(24,12,24,24);panel.setBackgroundColor(Color.rgb(226,242,253));
        Button close=new Button(this);close.setText("×");close.setTextSize(28);close.setContentDescription("关闭弹窗");
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(56),dp(56));cp.gravity=Gravity.RIGHT;panel.addView(close,cp);close.setOnClickListener(v->dialog.dismiss());
        TextView card=new TextView(this);card.setText(new String[]{"","打卡即可得金豆","开始赚钱即可获得1088金豆","已攒 3440 记得到点来领奖励哦"}[variant]);card.setTextSize(24);card.setTextColor(Color.WHITE);card.setGravity(Gravity.CENTER);card.setBackgroundColor(Color.rgb(25,71,121));panel.addView(card,new LinearLayout.LayoutParams(-1,0,1));
        TextView heading=new TextView(this);heading.setText("睡前小福利 看视频赚钱");heading.setTextSize(26);heading.setTextColor(Color.BLACK);heading.setGravity(Gravity.CENTER);heading.setPadding(0,dp(18),0,dp(18));panel.addView(heading);
        Button action=new Button(this);action.setText(new String[]{"","打卡赚钱","提醒我19点后打卡赚钱","提醒我明天6点领3440金豆"}[variant]);action.setTextSize(22);action.setMinHeight(dp(64));panel.addView(action);action.setOnClickListener(v->Toast.makeText(this,"这是练习按钮，无需点击",Toast.LENGTH_SHORT).show());
        dialog.setContentView(panel);dialog.setCanceledOnTouchOutside(false);dialog.show();
        Window window=dialog.getWindow();window.setGravity(Gravity.BOTTOM);window.setLayout(-1,Math.min(dp(440),getResources().getDisplayMetrics().heightPixels*3/4));
    }
    int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    TextView text(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(Math.max(size,22));v.setTextColor(Color.rgb(25,45,42));v.setGravity(Gravity.CENTER);v.setPadding(0,18,0,18);root.addView(v,new LinearLayout.LayoutParams(-1,-2));return v;}
    void button(String s,View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setTextSize(22);b.setMinHeight(Math.round(64*getResources().getDisplayMetrics().density));root.addView(b,new LinearLayout.LayoutParams(-1,-2));b.setOnClickListener(l);}
    void render(){title.setText("演示视频 "+page);timer.setText(adPreview?"即将播放广告":remaining>0?"广告 · "+remaining+" 秒后可翻页":"普通视频，可以上滑");timer.setContentDescription("练习页面状态："+timer.getText());count.setText("已成功翻页 "+(page-1)+" 次");}
    @Override public boolean dispatchTouchEvent(MotionEvent event){if(event.getAction()==MotionEvent.ACTION_DOWN)downY=event.getY();if(event.getAction()==MotionEvent.ACTION_UP&&downY-event.getY()>150){if(remaining==0){page++;if(page%3==0){remaining=8;handler.removeCallbacks(countdown);handler.postDelayed(countdown,1000);}render();}return true;}return super.dispatchTouchEvent(event);}
    @Override public void onResume(){super.onResume();if(adDemoDeparted){adDemoDeparted=false;adPreview=false;render();}}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);if(SwipeService.instance!=null)SwipeService.instance.stopSession();super.onDestroy();}
}
