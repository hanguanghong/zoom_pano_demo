package us.zoom.sdkexample;

import us.zoom.sdk.MeetingActivity;

import android.content.AsyncQueryHandler;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

public class DemoMeetingActivity extends MeetingActivity {
    private final static String TAG = "Zoom Demo MeetingAct";
    private final static String PATH_TO_SHARING_IND = "/data/demo/zscreen";

    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onMeetingConnected() {
        Log.i(TAG, "onMeetingConnected");
        new Thread() {
            @Override
            public void run() {
                CheckIndTask t = new CheckIndTask();
                t.execute();
            }
        }.start();
    }

    @Override
    protected void onStartShare()
    {
        Log.i(TAG, "onStartShare");
        if (isSharingScreen()) {
            Log.i(TAG, "isSharingScreen");
            createSharingInd();
        }
    }

    @Override
    protected void onStopShare() {
        Log.i(TAG, "onStopShare");
        removeSharingInd();
    }

    private void createSharingInd() {
        Log.i(TAG, "createSharingInd");
        try {
            File f = new File(PATH_TO_SHARING_IND);
            f.createNewFile();
        } catch (Exception e) {
            Log.i(TAG, e.getStackTrace().toString());
        }
    }

    private void removeSharingInd() {
        Log.i(TAG, "removeSharingInd");
        try {
            File f = new File(PATH_TO_SHARING_IND);
            if (f.exists()) {
                f.delete();
            }
        } catch (Exception e) {
            Log.i(TAG, e.getStackTrace().toString());
        }
    }

    class CheckIndTask extends AsyncTask {
        @Override
        protected Object doInBackground(Object... params) {
            File f = new File(PATH_TO_SHARING_IND);
            do {
                if (f.exists() && !isSharingOut()) {
                    Log.i(TAG, "sharing ind exists and not sharing out");
                    //TODO: take Zoom to background
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
            return null;
        }
    }
}
