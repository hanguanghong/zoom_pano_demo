package us.zoom.sdkexample;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import ly.bit.nsq.Message;
import ly.bit.nsq.exceptions.NSQException;
import ly.bit.nsq.lookupd.BasicLookupd;
import ly.bit.nsq.syncresponse.SyncResponseHandler;
import ly.bit.nsq.syncresponse.SyncResponseReader;

import static us.zoom.sdkexample.Constants.NSC_RECV_ADDRESS;
import static us.zoom.sdkexample.Constants.NSQ_TOPIC;

public class NSQService extends Service {

    private static String TAG = "Zoom Demo NSQService";

    private SyncResponseReader reader;
    private int hashCode;

    public NSQService() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        hashCode = intent.getIntExtra("hashCode", -1);
        Log.i(TAG, "onStartCommand, hashCode=" + hashCode);

        CreateNSQReaderTask t = new CreateNSQReaderTask();
        t.execute();

        Intent i = new Intent("nsq");
        i.putExtra("message", 5555);
        boolean result = LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.i(TAG, LocalBroadcastManager.getInstance(this).toString());
        Log.i(TAG, "sendBroadcast result=" + result);

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class SharingIndHandler implements SyncResponseHandler {
        public boolean handleMessage(Message msg) throws NSQException {
            try {
                int hc = Integer.parseInt(new String(msg.getBody()));
                Log.i(TAG, "Receiving: " + hc);
                Intent intent = new Intent("nsq");
                intent.putExtra("message", hc);
                boolean result = LocalBroadcastManager.getInstance(NSQService.this).sendBroadcast(intent);
                Log.i(TAG, "sendBroadcast result=" + result);
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
}

