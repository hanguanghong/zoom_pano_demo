package us.zoom.sdkexample;

import us.zoom.sdk.JoinMeetingOptions;
import us.zoom.sdk.JoinMeetingParams;
import us.zoom.sdk.MeetingError;
import us.zoom.sdk.MeetingEvent;
import us.zoom.sdk.MeetingService;
import us.zoom.sdk.MeetingServiceListener;
import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.MeetingViewsOptions;
import us.zoom.sdk.StartMeetingOptions;
import us.zoom.sdk.StartMeetingParamsWithoutLogin;
import us.zoom.sdk.ZoomError;
import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomSDKInitializeListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends Activity implements Constants, ZoomSDKInitializeListener, MeetingServiceListener {

	private final static String TAG = "Zoom Demo APP";
	
	private EditText mEdtMeetingNo;
	private EditText mEdtMeetingPassword;
	
	private final static int STYPE = MeetingService.USER_TYPE_API_USER;
	private final static String DISPLAY_NAME = "Zoom Demo";

	private boolean mbPendingStartMeeting = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);

		mEdtMeetingNo = (EditText)findViewById(R.id.edtMeetingNo);
		mEdtMeetingPassword = (EditText)findViewById(R.id.edtMeetingPassword);
		
		if(savedInstanceState == null) {
			ZoomSDK sdk = ZoomSDK.getInstance();
			MeetingService meetingService1 = sdk.getMeetingService();
			sdk.initialize(this, APP_KEY, APP_SECRET, WEB_DOMAIN, this);
			MeetingService meetingService2 = sdk.getMeetingService();
		} else {
			ZoomSDK sdk = ZoomSDK.getInstance();
			MeetingService meetingService3 = sdk.getMeetingService();
			registerMeetingServiceListener();
		}
	}
	
	private void registerMeetingServiceListener() {
		ZoomSDK zoomSDK = ZoomSDK.getInstance();
		MeetingService meetingService = zoomSDK.getMeetingService();
		if(meetingService != null) {
			Log.i(TAG, "registerMeetingServiceListener getCurrentRtcMeetingID=" + meetingService.getCurrentMeetingUrl());
			meetingService.addListener(this);
		} else {
			Log.i(TAG, "registerMeetingServiceListener: noMeetingService");
		}
	}
	
	@Override
	public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
		Log.i(TAG, "onZoomSDKInitializeResult, errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);
		
		if(errorCode != ZoomError.ZOOM_ERROR_SUCCESS) {
			Toast.makeText(this, "Failed to initialize Zoom SDK. Error: " + errorCode + ", internalErrorCode=" + internalErrorCode, Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "Initialize Zoom SDK successfully.", Toast.LENGTH_LONG).show();
			
			registerMeetingServiceListener();
		}
	}
	
	@Override
	protected void onDestroy() {
		ZoomSDK zoomSDK = ZoomSDK.getInstance();
		
		if(zoomSDK.isInitialized()) {
			MeetingService meetingService = zoomSDK.getMeetingService();
			meetingService.removeListener(this);
		}
		
		super.onDestroy();
	}

	public void onClickBtnJoinMeeting(View view) {
		String vanityId = "";
		String meetingPassword = mEdtMeetingPassword.getText().toString().trim();

		String meetingNo = mEdtMeetingNo.getText().toString().trim();
		
		if(meetingNo.length() == 0 && vanityId.length() == 0) {
			Toast.makeText(this, "You need to enter a meeting number/ vanity id which you want to join.", Toast.LENGTH_LONG).show();
			return;
		}

		if(meetingNo.length() != 0 && vanityId.length() !=0) {
			Toast.makeText(this, "Both meeting number and vanity id have value,  just set one of them", Toast.LENGTH_LONG).show();
			return;
		}

		dialJamCall(meetingNo);

		ZoomSDK zoomSDK = ZoomSDK.getInstance();
		
		if(!zoomSDK.isInitialized()) {
			Toast.makeText(this, "ZoomSDK has not been initialized successfully", Toast.LENGTH_LONG).show();
			return;
		}
		
		MeetingService meetingService = zoomSDK.getMeetingService();
		
		JoinMeetingOptions opts = new JoinMeetingOptions();
//		opts.no_driving_mode = true;
//		opts.no_invite = true;
//		opts.no_meeting_end_message = true;
//		opts.no_titlebar = true;
//		opts.no_bottom_toolbar = true;
//		opts.no_dial_in_via_phone = true;
//		opts.no_dial_out_to_phone = true;
//		opts.no_disconnect_audio = true;
//		opts.no_share = true;
//		opts.invite_options = InviteOptions.INVITE_VIA_EMAIL + InviteOptions.INVITE_VIA_SMS;
//		opts.no_audio = true;
//		opts.no_video = true;
//		opts.meeting_views_options = MeetingViewsOptions.NO_BUTTON_SHARE;
//		opts.no_meeting_error_message = true;
//		opts.participant_id = "participant id";
		JoinMeetingParams params = new JoinMeetingParams();

		params.displayName = DISPLAY_NAME;
		params.password = meetingPassword;

		if(vanityId.length() != 0) {
			params.vanityID = vanityId;
		} else {
			params.meetingNo = meetingNo;
		}
		int ret = meetingService.joinMeetingWithParams(this, params);
		
		Log.i(TAG, "onClickBtnJoinMeeting, ret=" + ret);
	}
	
	public void onClickBtnStartMeeting(View view) {
		String meetingNo = mEdtMeetingNo.getText().toString().trim();

		if(meetingNo.length() == 0) {
			Toast.makeText(this, "You need to enter a meeting number/ vanity  which you want to join.", Toast.LENGTH_LONG).show();
			return;
		}

		if(meetingNo.length() != 0) {
			Toast.makeText(this, "Both meeting number and vanity  have value,  just set one of them", Toast.LENGTH_LONG).show();
			return;
		}

		dialJamCall(meetingNo);

		ZoomSDK zoomSDK = ZoomSDK.getInstance();

		if(!zoomSDK.isInitialized()) {
			Toast.makeText(this, "ZoomSDK has not been initialized successfully", Toast.LENGTH_LONG).show();
			return;
		}
		
		final MeetingService meetingService = zoomSDK.getMeetingService();
		
		if(meetingService.getMeetingStatus() != MeetingStatus.MEETING_STATUS_IDLE) {
			long lMeetingNo = 0;
			try {
				lMeetingNo = Long.parseLong(meetingNo);
			} catch (NumberFormatException e) {
				Toast.makeText(this, "Invalid meeting number: " + meetingNo, Toast.LENGTH_LONG).show();
				return;
			}
			
			if(meetingService.getCurrentRtcMeetingNumber() == lMeetingNo) {
				meetingService.returnToMeeting(this);
				return;
			}
			
			new AlertDialog.Builder(this)
				.setMessage("Do you want to leave current meeting and start another?")
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mbPendingStartMeeting = true;
						meetingService.leaveCurrentMeeting(false);
					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
					}
				})
				.show();
			return;
		}
		
		StartMeetingOptions opts = new StartMeetingOptions();
//		opts.no_driving_mode = true;
		opts.no_invite = true;
//		opts.no_meeting_end_message = true;
//		opts.no_titlebar = true;
//		opts.no_bottom_toolbar = true;
		opts.no_dial_in_via_phone = true;
		opts.no_dial_out_to_phone = true;
//		opts.no_disconnect_audio = true;
//		opts.no_share = true;
//		opts.invite_options = InviteOptions.INVITE_ENABLE_ALL;
		opts.no_audio = true;
		opts.no_video = true;
		opts.meeting_views_options = MeetingViewsOptions.NO_BUTTON_VIDEO;
//		opts.no_meeting_error_message = true;

        StartMeetingParamsWithoutLogin params = new StartMeetingParamsWithoutLogin();
		params.userId = USER_ID;
		params.zoomToken = ZOOM_TOKEN;
		params.userType = STYPE;;
		params.displayName = DISPLAY_NAME;
		params.zoomAccessToken = ZOOM_ACCESS_TOKEN;
		params.meetingNo = meetingNo;

		int ret = meetingService.startMeetingWithParams(this, params, opts);
		
		Log.i(TAG, "onClickBtnStartMeeting, ret=" + ret);
	}
	
	@Override
	public void onMeetingEvent(int meetingEvent, int errorCode,
			int internalErrorCode) {
		
		Log.i(TAG, "onMeetingEvent, meetingEvent=" + meetingEvent + ", errorCode=" + errorCode
				+ ", internalErrorCode=" + internalErrorCode);
		
		if(meetingEvent == MeetingEvent.MEETING_CONNECT_FAILED && errorCode == MeetingError.MEETING_ERROR_CLIENT_INCOMPATIBLE) {
			Toast.makeText(this, "Version of ZoomSDK is too low!", Toast.LENGTH_LONG).show();
		}
		
		if(meetingEvent == MeetingEvent.MEETING_DISCONNECTED) {
			hangupJamCall();
		}
	}

	public void dialJamCall(String meetingNo) {
		try {
			Runtime r = Runtime.getRuntime();
			Process p = r.exec("su");
			DataOutputStream os = new DataOutputStream(p.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			os.writeBytes("umount /data/demo\n");
			os.writeBytes("busybox mount -t nfs -o nolock,proto=tcp 10.220.225.29:/data2/demo /data/demo\n");
			os.writeBytes("cd /opt/polycom/bin; export LD_LIBRARY_PATH=./; . ./config-helper.sh\n");
			os.writeBytes("set_config comm.Callpreference.jamfactoryaddress 0 http://10.220.225.148:8080/");
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
		} catch (Exception e) {
			Log.i(TAG, e.getMessage() + e.getCause());
		}
	}

	public void onClickBtnExit(View view) {
		finish();
	}
}
