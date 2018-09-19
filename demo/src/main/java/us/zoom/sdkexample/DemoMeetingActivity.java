package us.zoom.sdkexample;

import ly.bit.nsq.NSQProducer;
import ly.bit.nsq.exceptions.NSQException;
import ly.bit.nsq.lookupd.BasicLookupd;
import ly.bit.nsq.syncresponse.SyncResponseHandler;
import ly.bit.nsq.syncresponse.SyncResponseReader;
import us.zoom.sdk.MeetingActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
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
import ly.bit.nsq.Message;

import static us.zoom.sdkexample.Constants.NSC_RECV_ADDRESS;
import static us.zoom.sdkexample.Constants.NSQ_SEND_ADDRESS;
import static us.zoom.sdkexample.Constants.NSQ_TOPIC;

public class DemoMeetingActivity extends MeetingActivity {
    private final static String TAG = "Zoom Demo MeetingAct";

    private NSQProducer producer = new NSQProducer(NSQ_SEND_ADDRESS, NSQ_TOPIC);
    private SyncResponseReader reader;

    private boolean isSharingOut;
    private boolean isStopping;
    private boolean isAtBack;

    private int hashCode;

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int hc = intent.getIntExtra("message", -1);
            Log.i(TAG, "onReceive, message=" + hc);
            if (hc == 0) {
                onSharingIndRemoved();
            } else if (hc != hashCode) {
                onSharingIndExist();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getConfParams();
        Log.i(TAG, "onCreate " + this.hashCode());
        super.onCreate(savedInstanceState);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                messageReceiver, new IntentFilter("nsq"));
        Log.i(TAG, LocalBroadcastManager.getInstance(this).toString());
        /* TODO: try to use service
        Intent i = new Intent(this, NSQService.class);
        i.putExtra("hashCode", this.hashCode);
        startService(i);
        */
        CreateNSQReaderTask t = new CreateNSQReaderTask();
        t.execute();
    }

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

    public void onDestroy() {
        Log.i(TAG, "onDestory");
        hangupJamCall();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        //stopService(new Intent(this, NSQService.class));
        super.onDestroy();
    }

    @Override
    protected void onStartShare()
    {
        Log.i(TAG, "onStartShare");
        if (isSharingScreen()) {
            Log.i(TAG, "isSharingScreen");
            if (isSharingOut()) {
                Log.i(TAG, "isSharingOut");
                this.isSharingOut = true;
                dialJamCall(MainActivity.meetingNumber);
                createSharingInd();
            }
        }
    }

    @Override
    protected void onStopShare() {
        Log.i(TAG, "onStopShare");
        if (isSharingOut) {
            this.isSharingOut = false;
            removeSharingInd();
            hangupJamCall();
        }
    }

    private void createSharingInd() {
        Log.i(TAG, "createSharingInd");
        if (producer != null) {
            SendNSQMessageTask t = new SendNSQMessageTask(hashCode);
            t.execute();
        }
    }

    private void removeSharingInd() {
        Log.i(TAG, "removeSharingInd");
        if (producer != null) {
            SendNSQMessageTask t = new SendNSQMessageTask(0);
            t.execute();
        }
    }

    private void onSharingIndExist() {
        Log.i(TAG, "onSharingIndExist");
        if (!isAtBack && !isSharingOut) {
            try {
                Log.i(TAG, "sharing ind exists and not sharing out");

                //TODO: take Zoom to background
                onBackPressed();
                //switchToHomeActivity();
                isAtBack = true;
                dialJamCall(MainActivity.meetingNumber);
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
            hangupJamCall();
            restore();
            isAtBack = false;
        }
    }

    class SendNSQMessageTask extends AsyncTask {

        private int intMessage;

        public SendNSQMessageTask(int i) {
            this.intMessage = i;
        }

        @Override
        protected Object doInBackground(Object... params) {
            try {
                String message = Integer.toString(intMessage);
                Log.i(TAG, "Sending: " + message);
                producer.put(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    class SharingIndHandler implements SyncResponseHandler {
        public boolean handleMessage(Message msg) throws NSQException {
            try {
                int hc = Integer.parseInt(new String(msg.getBody()));
                Log.i(TAG, "Receiving: " + hc);
                if (hc == 0) {
                    onSharingIndRemoved();
                } else if (hc != hashCode) {
                    onSharingIndExist();
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            return true;
        }
    }

    class CreateNSQReaderTask extends AsyncTask {
        @Override
        protected Object doInBackground(Object... params) {
            SyncResponseHandler sh = new SharingIndHandler();
            reader = new SyncResponseReader(NSQ_TOPIC, hashCode + "#ephemeral", sh);
            Log.i(TAG, "NSQReader created");
            if (reader != null) {
                Log.d(TAG, "reader.addLookupd");
                reader.addLookupd(new BasicLookupd(NSC_RECV_ADDRESS));
            }
            return null;
        }
    }

    @Override
    public void onBackPressed()
    {
        moveTaskToBack(true);
    }

    private void restore() {
        Log.i(TAG, "restore");
        Intent i = new Intent(DemoMeetingActivity.this, DemoMeetingActivity.class);
        i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        startActivity(i);
    }

    public void dialJamCall(String meetingNo) {
        try {
            Runtime r = Runtime.getRuntime();
            Process p = r.exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            os.writeBytes("cd /opt/polycom/bin; export LD_LIBRARY_PATH=./; . ./config-helper.sh\n");
            os.writeBytes("set_config feature.master.callservice.enabled 0 True\n");
            os.writeBytes("set_config feature.cod.enabled 0 True");
            os.writeBytes("set_config pm.layout.style 0 PANO\n");
            os.writeBytes("set_config comm.Callpreference.jamfactoryaddress 0 http://10.220.225.148:8080/\n");
            os.writeBytes("./pbdial 6144 " + meetingNo + " jam\n");
            os.writeBytes("exit\n");
            os.flush();
            os.close();
            p.waitFor();
            String line;
            StringBuilder sb = new StringBuilder(4096);
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            br.close();
            Log.d(TAG, sb.toString());
            Log.i(TAG, "dialed jam call");
        } catch (Exception e) {
            Log.i(TAG, e.getMessage() + e.getCause());
        }
    }

    public void hangupJamCall() {
        try {
            Runtime r = Runtime.getRuntime();
            Process p = r.exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            os.writeBytes("cd /opt/polycom/bin; export LD_LIBRARY_PATH=./; . ./config-helper.sh\n");
            os.writeBytes("./pbhangup\n");
            os.writeBytes("exit\n");
            os.flush();
            os.close();
            p.waitFor();
            String line;
            StringBuilder sb = new StringBuilder(4096);
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            br.close();
            Log.d(TAG, sb.toString());
            Log.i(TAG, "hangup jam call");

            Intent intent = new Intent("clean_session");
            sendBroadcast(intent);
        } catch (Exception e) {
            Log.i(TAG, e.getMessage() + e.getCause());
        }
    }

}
