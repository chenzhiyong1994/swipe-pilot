package com.local.swipepilot;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

/** Uses the task's accessible label and clickable card, never fixed screen coordinates. */
final class RecentTasks {
    static boolean clickTarget(AccessibilityNodeInfo root,String name,int minWidth,int minHeight){
        AccessibilityNodeInfo label=findLabel(root,name,new int[]{0});if(label==null)return false;
        AccessibilityNodeInfo node=label;
        try{
            for(int depth=0;node!=null&&depth<8;depth++){
                Rect b=new Rect();node.getBoundsInScreen(b);
                if(node.isVisibleToUser()&&node.isClickable()&&b.width()>=minWidth&&b.height()>=minHeight)
                    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                AccessibilityNodeInfo parent=node.getParent();node.recycle();node=parent;
            }
            return false;
        }finally{if(node!=null)node.recycle();}
    }
    private static AccessibilityNodeInfo findLabel(AccessibilityNodeInfo node,String name,int[] count){
        if(++count[0]>400)return null;
        if(node.isVisibleToUser()&&(matches(node.getText(),name)||matches(node.getContentDescription(),name)))return AccessibilityNodeInfo.obtain(node);
        for(int n=0;n<node.getChildCount()&&count[0]<400;n++){
            AccessibilityNodeInfo child=node.getChild(n);if(child==null)continue;
            AccessibilityNodeInfo found=findLabel(child,name,count);child.recycle();if(found!=null)return found;
        }
        return null;
    }
    private static boolean matches(CharSequence value,String name){
        if(value==null||name.isEmpty())return false;String s=value.toString().trim();
        return s.equals(name)||s.startsWith(name+"，")||s.startsWith(name+",")||s.startsWith(name+"\n");
    }
    static boolean scroll(AccessibilityNodeInfo node,boolean forward,int[] count){
        if(++count[0]>300)return false;
        int action=forward?AccessibilityNodeInfo.ACTION_SCROLL_FORWARD:AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
        if(node.isVisibleToUser()&&node.isScrollable()&&node.performAction(action))return true;
        for(int n=0;n<node.getChildCount()&&count[0]<300;n++){
            AccessibilityNodeInfo child=node.getChild(n);if(child==null)continue;
            boolean done=scroll(child,forward,count);child.recycle();if(done)return true;
        }
        return false;
    }
}
