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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends Activity implements Constants, ZoomSDKInitializeListener, MeetingServiceListener {

	private final static String TAG = "Zoom Demo APP";
	
	private EditText mEdtMeetingNo;
	private EditText mEdtMeetingPassword;
	
	private final static int STYPE = MeetingService.USER_TYPE_API_USER;
	private final static String DISPLAY_NAME = "Zoom Demo";

	private boolean mbPendingStartMeeting = false;

	private String meetingNumber = "";
	
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

		meetingNumber = mEdtMeetingNo.getText().toString().trim();
		
		if(meetingNumber.length() == 0 && vanityId.length() == 0) {
			Toast.makeText(this, "You need to enter a meeting number/ vanity id which you want to join.", Toast.LENGTH_LONG).show();
			return;
		}

		if(meetingNumber.length() != 0 && vanityId.length() !=0) {
			Toast.makeText(this, "Both meeting number and vanity id have value,  just set one of them", Toast.LENGTH_LONG).show();
			return;
		}

		writeMeetingNumberToFile();

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
			params.meetingNo = meetingNumber;
		}
		int ret = meetingService.joinMeetingWithParams(this, params);
		
		Log.i(TAG, "onClickBtnJoinMeeting, ret=" + ret);
	}
	
	public void onClickBtnStartMeeting(View view) {
		meetingNumber = mEdtMeetingNo.getText().toString().trim();

		if(meetingNumber.length() == 0) {
			Toast.makeText(this, "You need to enter a meeting number/ vanity  which you want to join.", Toast.LENGTH_LONG).show();
			return;
		}

		File f = new File(MEETINGNO_FILE);
		f.delete();

		ZoomSDK zoomSDK = ZoomSDK.getInstance();

		if(!zoomSDK.isInitialized()) {
			Toast.makeText(this, "ZoomSDK has not been initialized successfully", Toast.LENGTH_LONG).show();
			return;
		}
		
		final MeetingService meetingService = zoomSDK.getMeetingService();
		
		if(meetingService.getMeetingStatus() != MeetingStatus.MEETING_STATUS_IDLE) {
			long lMeetingNo = 0;
			try {
				lMeetingNo = Long.parseLong(meetingNumber);
			} catch (NumberFormatException e) {
				Toast.makeText(this, "Invalid meeting number: " + meetingNumber, Toast.LENGTH_LONG).show();
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
		params.meetingNo = meetingNumber;

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
			meetingNumber = "";
		}
	}

	public void writeMeetingNumberToFile() {
		try {
			File f = new File(MEETINGNO_FILE);
			if (f.exists()) {
				f.delete();
			}
			FileWriter fw = new FileWriter(f);
			fw.write(meetingNumber);
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void onClickBtnExit(View view) {
		finish();
	}
}
