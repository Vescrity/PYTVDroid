package org.pytorch.demo.torchvideo;
import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;
import android.os.VibrationEffect;
public class BaseUtils {
    public static void vibrate(Context context, int msec) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            VibrationEffect effect = VibrationEffect.createOneShot(msec, VibrationEffect.DEFAULT_AMPLITUDE);
            vibrator.vibrate(effect);
            new Handler().postDelayed(vibrator::cancel, msec);
        }
    }
}
