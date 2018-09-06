package us.zoom.sdkexample;

import us.zoom.sdk.MeetingActivity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.util.Log;

public class DemoMeetingActivity extends MeetingActivity {
    private final static String TAG = "Zoom Demo MeetingAct";

    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onMeetingConnected() {
        Log.i(TAG, "onMeetingConnected");
    }

    @Override
    protected void onStartShare() {
        Log.i(TAG, "onStartShare");
    }

    @Override
    protected void onStopShare() {
        Log.i(TAG, "onStopShare");
    }

}
