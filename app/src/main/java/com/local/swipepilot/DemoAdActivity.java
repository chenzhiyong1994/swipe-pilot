package com.local.swipepilot;

import android.app.Activity;
import android.os.*;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;

/** Local practice task: exposes the same phrases without opening an actual game or ad. */
public class DemoAdActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private TextView reward;private int remaining=6;
    private final Runnable tick=new Runnable(){public void run(){remaining--;render();if(remaining>0)handler.postDelayed(this,1000);}};
    @Override public void onCreate(Bundle state){super.onCreate(state);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER);box.setPadding(40,40,40,40);box.setBackgroundColor(Color.rgb(249,239,216));setContentView(box);
        TextView title=new TextView(this);title.setText("广告返回练习\n这是独立的练习任务");title.setTextSize(26);title.setGravity(Gravity.CENTER);box.addView(title);
        reward=new TextView(this);reward.setTextSize(28);reward.setGravity(Gravity.CENTER);reward.setPadding(0,60,0,60);box.addView(reward,new LinearLayout.LayoutParams(-1,-2));
        Button stop=new Button(this);stop.setText("结束练习");stop.setTextSize(22);box.addView(stop);stop.setOnClickListener(v->{if(SwipeService.instance!=null)SwipeService.instance.stopSession();finishAndRemoveTask();});
        render();handler.postDelayed(tick,1000);
    }
    private void render(){reward.setText(remaining>0?"浏览"+remaining+"秒得奖励":"已获得奖励");}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
}
