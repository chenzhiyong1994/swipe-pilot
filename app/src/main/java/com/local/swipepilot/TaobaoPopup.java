package com.local.swipepilot;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.*;

/** Uses semantic popup evidence plus a labelled/id'd close control above its heading. */
final class TaobaoPopup implements AutoCloseable {
    final List<String> lines=new ArrayList<>();
    final List<AccessibilityNodeInfo> candidates=new ArrayList<>();
    final Rect screen;
    int headingTop=Integer.MAX_VALUE;
    TaobaoPopup(AccessibilityNodeInfo root,Rect screen){this.screen=screen;scan(root,new int[]{0});}
    private void scan(AccessibilityNodeInfo node,int[] count){
        if(++count[0]>700)return;
        if(node.isVisibleToUser()){
            String text=node.getText()==null?"":node.getText().toString();
            String desc=node.getContentDescription()==null?"":node.getContentDescription().toString();
            lines.add(text);lines.add(desc);
            Rect b=new Rect();node.getBoundsInScreen(b);
            if((text.contains("睡前小福利")||text.contains("看视频赚钱")||desc.contains("睡前小福利"))&&b.height()<screen.height()/4&&b.height()>0)headingTop=Math.min(headingTop,b.top);
            String id=node.getViewIdResourceName();
            String label=(text.isEmpty()?desc:text).trim().toLowerCase(Locale.ROOT);
            boolean closeLabel=label.matches("(?:[x×✕✖]|关闭|关闭弹窗|关闭弹框|关闭按钮|关闭弹窗按钮|close|close dialog)");
            boolean closeId=id!=null&&id.toLowerCase(Locale.ROOT).matches(".*(?:^|[/_:])(?:[a-z0-9_]*close[a-z0-9_]*|dismiss)(?:$)");
            if(node.isEnabled()&&(closeLabel||closeId)&&b.centerX()>screen.left+screen.width()*0.72&&b.centerY()>screen.top+screen.height()*0.08&&b.centerY()<screen.top+screen.height()*0.78&&b.width()>0&&b.width()<screen.width()*0.2&&b.height()>0&&b.height()<screen.height()*0.12)
                candidates.add(AccessibilityNodeInfo.obtain(node));
        }
        for(int i=0;i<node.getChildCount()&&count[0]<700;i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null){scan(child,count);child.recycle();}}
    }
    boolean visible(){return PopupPolicy.matches(lines);}
    AccessibilityNodeInfo closeControl(){
        if(!visible()||headingTop==Integer.MAX_VALUE)return null;
        AccessibilityNodeInfo chosen=null;Rect chosenBounds=new Rect();
        for(AccessibilityNodeInfo node:candidates){
            Rect bounds=new Rect();node.getBoundsInScreen(bounds);if(bounds.centerY()>=headingTop)continue;
            if(chosen!=null){if(Math.abs(bounds.centerX()-chosenBounds.centerX())>10||Math.abs(bounds.centerY()-chosenBounds.centerY())>10)return null;}
            chosen=node;chosenBounds=bounds;
        }
        return chosen;
    }
    @Override public void close(){for(AccessibilityNodeInfo node:candidates)node.recycle();}
}
