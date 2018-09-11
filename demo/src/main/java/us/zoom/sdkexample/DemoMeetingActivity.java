package us.zoom.sdkexample;

import us.zoom.sdk.MeetingActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.AsyncQueryHandler;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.FileObserver;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

public class DemoMeetingActivity extends MeetingActivity {
    private final static String TAG = "Zoom Demo MeetingAct";
    private final static String PATH_TO_SHARING_IND = "/data/demo/zoom_share";

    private boolean isSharingOut;
    private boolean isStopping;
    private boolean isAtBack;

    private int hashCode;

    protected void onResume() {
        Log.i(TAG, "onResume " + this.hashCode());
        super.onResume();
        this.hashCode = this.hashCode();
    }

    protected void onStop() {
        Log.i(TAG, "onStop " + this.hashCode());
        isStopping = true;
        super.onStop();
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
        if (isSharingOut()) {
            Log.i(TAG, "isSharingOut");
            this.isSharingOut = true;
        }
        if (isSharingScreen()) {
            Log.i(TAG, "isSharingScreen");
            createSharingInd();
        }
    }

    @Override
    protected void onStopShare() {
        Log.i(TAG, "onStopShare");
        this.isSharingOut = false;
        removeSharingInd();
    }

    private void createSharingInd() {
        Log.i(TAG, "createSharingInd");
        try {
            File f = new File(PATH_TO_SHARING_IND);
            if (!f.exists()) {
                f.createNewFile();
                //f.setReadable(true, false);
            }
            FileOutputStream fos = new FileOutputStream(f);
            DataOutputStream dos = new DataOutputStream(fos);
            dos.writeInt(this.hashCode());
            dos.flush();
            dos.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
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

    private void onSharingIndExist() {
        Log.i(TAG, "onSharingIndExist");
        if (!isAtBack && !isSharingOut) {
            try {
                File f = new File(PATH_TO_SHARING_IND);
                FileInputStream fis = new FileInputStream(f);
                DataInputStream dis = new DataInputStream(fis);
                int rint = dis.readInt();
                dis.close();
                fis.close();
                Log.d(TAG, "rint=" + rint + ", this.hashCode=" + hashCode);
                if (rint != hashCode) {
                    Log.i(TAG, "sharing ind exists and not sharing out");

                    //TODO: take Zoom to background
                    onBackPressed();
                    //switchToHomeActivity();
                    isAtBack = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void onSharingIndRemoved() {
        Log.i(TAG, "onSharingIndRemoved");
        if (isAtBack) {
                                    /*
                        ActivityManager am = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
                        List<ActivityManager.RunningTaskInfo> rt = am.getRunningTasks(Integer.MAX_VALUE);

                        for (int i = 0; i < rt.size(); i++)
                        {
                            Log.d(TAG, "rt.get(i).baseActivity.toShortString()=" + rt.get(i).baseActivity.toShortString());
                            // bring Zoom to front
                            if (rt.get(i).baseActivity.toShortString().indexOf("us.zoom.sdkexample") > -1) {
                                Log.d(TAG,"moveTaskToFront");
                                am.moveTaskToFront(rt.get(i).id, ActivityManager.MOVE_TASK_WITH_HOME);
                            }
                        }
                        */
            //restore();
            isAtBack = false;
        }
    }

    class CheckIndTask extends AsyncTask {
        @Override
        protected Object doInBackground(Object... params) {
            File f = new File(PATH_TO_SHARING_IND);
            do {
                Log.d(TAG, "CheckIndTask isSharingOut=" + isSharingOut + ";f.exists()=" + f.exists());
                String[] pl = f.getParentFile().list();
                for (String s : pl) {
                    Log.d(TAG, s);
                }
                if (f.exists()) {
                    onSharingIndExist();
                } else if (isAtBack) {
                    onSharingIndRemoved();
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            } while (!isStopping);
            return null;
        }
    }

    /*
    @Override
    public void onBackPressed()
    {
        moveTaskToBack(true);
    }*/

    private void restore() {
        Intent intent = new Intent(DemoMeetingActivity.this, DemoMeetingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
}
