package com.foobnix.pdf;

import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.wrapper.AppState;

import android.content.Context;
import android.os.Vibrator;

public class CommandProccesor {

    public static String CMD_IMAGE_OFF = "@img_off";
    public static String CMD_IMAGE_ON = "@img_on";

    public static boolean isRunCommand(Context c, String cmd) {
        if (TxtUtils.isEmpty(cmd)) {
            return false;
        }
        cmd = cmd.trim();

        if (CMD_IMAGE_ON.equals(cmd)) {
            vibrate(c);
            AppState.get().isBrowseImages = true;
            return true;
        }

        if (CMD_IMAGE_OFF.equals(cmd)) {
            vibrate(c);
            AppState.get().isBrowseImages = true;
            return true;
        }



        return false;

    }

    private static void vibrate(Context c) {
        if (c == null) {
            return;
        }
        Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(500);
    }

}
