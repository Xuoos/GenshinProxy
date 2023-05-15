package Xuoos.GenshinImpact.Proxy;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ToastCustom {
    private Toast toast;

    private static ToastCustom toastCustom;

    public static ToastCustom createToastConfig() {
        if (toastCustom == null) {
            toastCustom = new ToastCustom();
        }
        return toastCustom;
    }

    public void show(Activity activity, String msg, int duration, int backgroundColor, int textColor) {
        TextView toastView = new TextView(activity);
        toastView.setText(msg);
        toastView.setTextColor(textColor);
        toastView.setGravity(Gravity.CENTER);
        toastView.setBackgroundColor(backgroundColor);
        toastView.setPadding(25, 30, 25, 30);
        float radius = 20f;
        ShapeDrawable shapeDrawable = new ShapeDrawable(new RoundRectShape(new float[]{radius, radius, radius, radius, radius, radius, radius, radius}, null, null));
        shapeDrawable.getPaint().setColor(backgroundColor);
        toastView.setBackground(shapeDrawable);

        toast = new Toast(activity);
        toast.setGravity(Gravity.BOTTOM, 0, 80);
        if (duration == 1) {
          toast.setDuration(Toast.LENGTH_LONG);
        } else {
          toast.setDuration(Toast.LENGTH_SHORT);
        }
        toast.setView(toastView);

        View view = toast.getView();
        if (view != null) {
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int width = view.getMeasuredWidth();
            int height = view.getMeasuredHeight();
            view.setPadding(0, 0, 0, 0);
            view.setMinimumWidth(width);
            view.setMinimumHeight(height);
        }

        toast.show();
    }

    public void show_ServerStatus(Activity activity, String msg, int duration, int backgroundColor, int textColor) {
        TextView toastView = new TextView(activity);
        toastView.setText(msg);
        toastView.setTextColor(textColor);
        toastView.setGravity(Gravity.CENTER);
        toastView.setBackgroundColor(backgroundColor);
        toastView.setPadding(25, 30, 25, 30);
        float radius = 20f;
        ShapeDrawable shapeDrawable = new ShapeDrawable(new RoundRectShape(new float[]{radius, radius, radius, radius, radius, radius, radius, radius}, null, null));
        shapeDrawable.getPaint().setColor(backgroundColor);
        toastView.setBackground(shapeDrawable);

        toast = new Toast(activity);
        toast.setGravity(Gravity.TOP, 0, 0);
        if (duration == 1) {
          toast.setDuration(Toast.LENGTH_LONG);
        } else {
          toast.setDuration(Toast.LENGTH_SHORT);
        }
        toast.setView(toastView);

        View view = toast.getView();
        if (view != null) {
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int width = view.getMeasuredWidth();
            int height = view.getMeasuredHeight();
            view.setPadding(0, 0, 0, 0);
            view.setMinimumWidth(width);
            view.setMinimumHeight(height);
        }

        toast.show();
    }
}
