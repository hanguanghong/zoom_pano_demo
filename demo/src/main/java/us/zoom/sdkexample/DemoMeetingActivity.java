package us.zoom.sdkexample;

import us.zoom.sdk.MeetingActivity;

import android.app.Activity;
import android.app.ActivityManager;
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

    protected void onResume() {
        Log.i(TAG, "onResume " + this.hashCode());
        super.onResume();
    }

    protected void onStop() {
        Log.i(TAG, "onResume " + this.hashCode());
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
        if (isSharingScreen()) {
            Log.i(TAG, "isSharingScreen");
            createSharingInd();
        }
        if (isSharingOut()) {
            Log.i(TAG, "isSharingOut");
            this.isSharingOut = true;
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
            f.createNewFile();
            FileOutputStream fos = new FileOutputStream(f);
            DataOutputStream dos = new DataOutputStream(fos);
            dos.writeInt(this.hashCode());
            dos.close();
            fos.close();
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
                if (!isSharingOut && f.exists()) {
                    if (f.exists() && !isAtBack) {
                        try {
                            FileInputStream fis = new FileInputStream(f);
                            DataInputStream dis = new DataInputStream(fis);
                            int hc = dis.readInt();
                            dis.close();
                            fis.close();
                            Log.d(TAG, "hc=" + hc);
                            if (hc != this.hashCode()) {
                                Log.i(TAG, "sharing ind exists and not sharing out");

                                //TODO: take Zoom to background
                                moveTaskToBack(true);
                                //switchToHomeActivity();
                                isAtBack = true;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }
                    } else if (!f.exists() && isAtBack) {
                        ActivityManager am = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
                        List<ActivityManager.RunningTaskInfo> rt = am.getRunningTasks(Integer.MAX_VALUE);

                        for (int i = 0; i < rt.size(); i++)
                        {
                            // bring to front
                            //if (rt.get(i).baseActivity.toShortString().indexOf("yourproject") > -1) {
                            //    am.moveTaskToFront(rt.get(i).id, ActivityManager.MOVE_TASK_WITH_HOME);
                            //}
                        }
                        isAtBack = false;
                    }
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

    private void switchToHomeActivity() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        String packageName = "polycom.home";
        String className = "polycom.home.HomeActivity";
        intent.setClassName(packageName, className);
        startActivity(intent);
    }
}
