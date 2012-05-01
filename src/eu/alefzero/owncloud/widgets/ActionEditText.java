package eu.alefzero.owncloud.widgets;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import eu.alefzero.owncloud.R;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class ActionEditText extends EditText {
  private String s;
  private String optionOneString;
  private int optionOneColor; 
  private String optionTwoString;
  private int optionTwoColor;
  
  private String badgeClickCallback;
  private Rect btn_rect;
  
  public ActionEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
    getAttrs(attrs);
    s = optionOneString;
  }
  
  public ActionEditText(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    getAttrs(attrs);
    s = optionOneString;
  }
  
  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    Rect r = new Rect();
    
    Paint p = getPaint();
    Rect text_bounds = new Rect();
    
    p.getTextBounds(s, 0, s.length(), text_bounds);
    
    getDrawingRect(r);
    r.top += 10;
    r.bottom -= 10;
    r.left = (int)(getWidth() - text_bounds.width() - 18);
    r.right = getWidth() - 10;
    btn_rect = r;
    
    if (s.equals(optionOneString))
      p.setColor(optionOneColor);
    else
      p.setColor(optionTwoColor);
    canvas.drawRect(r, p);
    p.setColor(Color.GRAY);
    
    canvas.drawText(s, r.left + 3, r.bottom - (text_bounds.height()/2), p);
    
    invalidate();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int touchX = (int) event.getX();
    int touchY = (int) event.getY();
    boolean r = super.onTouchEvent(event);
    if (event.getAction() == MotionEvent.ACTION_UP) {
      if (btn_rect.contains(touchX, touchY)) {
        if (s.equals(optionTwoString)) s = optionOneString;
        else s = optionTwoString;
        if (badgeClickCallback != null) {
          Class[] paramtypes = new Class[2];
          paramtypes[0] = android.view.View.class;
          paramtypes[1] = String.class;
          Method method;
          try {

            method = getContext().getClass().getMethod(badgeClickCallback, paramtypes);
            method.invoke(getContext(), this, s);

          } catch (NoSuchMethodException e) {
            e.printStackTrace();
          } catch (IllegalArgumentException e) {
            e.printStackTrace();
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          } catch (InvocationTargetException e) {
            e.printStackTrace();
          }
          
          invalidate();
        }
      }
    }
    return r;
  }

  private void getAttrs(AttributeSet attr) {
    TypedArray a = getContext().obtainStyledAttributes(attr, R.styleable.ActionEditText);
    optionOneString = a.getString(R.styleable.ActionEditText_optionOneString);
    optionTwoString = a.getString(R.styleable.ActionEditText_optionTwoString);
    optionOneColor = a.getColor(R.styleable.ActionEditText_optionOneColor, 0x00ff00);
    optionTwoColor = a.getColor(R.styleable.ActionEditText_optionTwoColor, 0xff0000);
    badgeClickCallback = a.getString(R.styleable.ActionEditText_onBadgeClick);
  }
  
}
