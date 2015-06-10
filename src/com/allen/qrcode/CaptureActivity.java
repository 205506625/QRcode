package com.allen.qrcode;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.allen.myqrcode.R;
import com.allen.qrcode.db.DatabaseUtil;
import com.allen.qrcode.zxing.camera.CameraManager;
import com.allen.qrcode.zxing.control.AmbientLightManager;
import com.allen.qrcode.zxing.control.BeepManager;
import com.allen.qrcode.zxing.decode.*;
import com.allen.qrcode.zxing.view.ViewfinderView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;

public final class CaptureActivity extends Activity implements
		SurfaceHolder.Callback {
	private Button btn_back;
	private TextView button_lamp;
	private LinearLayout btn_lamp;
	private LinearLayout btn_history;
	private ImageView imageView;
	private boolean isTorchOn = false;
	private CameraManager cameraManager;
	private CaptureActivityHandler handler;
	private Result savedResultToShow;
	private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private Collection<BarcodeFormat> decodeFormats;
	private Map<DecodeHintType, ?> decodeHints;
	private String characterSet;
	private InactivityTimer inactivityTimer;

	private BeepManager beepManager;
	private AmbientLightManager ambientLightManager;
	private boolean vibrate;// ��
	private DatabaseUtil dbUtil;

	private Button setBtn;

	public ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}

	public CameraManager getCameraManager() {
		return cameraManager;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.capture);
		setBtn = (Button) findViewById(R.id.btn_set);
		setBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent intent = new Intent();
				intent.setClass(CaptureActivity.this, SetingActivity.class);
				startActivity(intent);
			}
		});
		btn_lamp = (LinearLayout) findViewById(R.id.linearlayout_lamp);
		btn_history = (LinearLayout) findViewById(R.id.linearlayout_history);
		button_lamp = (TextView) findViewById(R.id.btn_lamp);
		imageView = (ImageView) findViewById(R.id.lamp);
		btn_lamp.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				playVibrate();
				// ���ص�
				if (isTorchOn) {
					isTorchOn = false;
					button_lamp.setText("����");
					// imageView.setImageResource(R.drawable.lamp_off);
					cameraManager.setTorch(false);
				} else {
					isTorchOn = true;
					button_lamp.setText("�ص�");
					// imageView.setImageResource(R.drawable.lamp_on);
					cameraManager.setTorch(true);
				}
			}
		});
		btn_history.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				playVibrate();
				Intent intent = new Intent();
				intent.setClass(CaptureActivity.this, HistoryActivity.class);
				startActivity(intent);
			}
		});

		hasSurface = false;
		inactivityTimer = new InactivityTimer(this);
		beepManager = new BeepManager(this);
		ambientLightManager = new AmbientLightManager(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();

		cameraManager = new CameraManager(getApplication());

		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		viewfinderView.setCameraManager(cameraManager);

		handler = null;
		resetStatusView();

		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			initCamera(surfaceHolder);
		} else {
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		beepManager.updatePrefs();
		ambientLightManager.start(cameraManager);

		inactivityTimer.onResume();
		vibrate = true;
		decodeFormats = null;
		characterSet = null;

	}

	private void playVibrate() {

		Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		vibrator.vibrate(VIBRATE_DURATION);

	}

	@Override
	protected void onPause() {
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		inactivityTimer.onPause();
		ambientLightManager.stop();
		cameraManager.closeDriver();
		if (!hasSurface) {
			SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
			SurfaceHolder surfaceHolder = surfaceView.getHolder();
			surfaceHolder.removeCallback(this);
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		inactivityTimer.shutdown();
		viewfinderView.recycleLineDrawable();
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		switch (keyCode) {
		case KeyEvent.KEYCODE_CAMERA:// ���������
			return true;
		case KeyEvent.KEYCODE_BACK:
			AnimationUtil.finishActivityAnimation(CaptureActivity.this);
			;

			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
		if (handler == null) {
			savedResultToShow = result;
		} else {
			if (result != null) {
				savedResultToShow = result;
			}
			if (savedResultToShow != null) {
				Message message = Message.obtain(handler,
						R.id.decode_succeeded, savedResultToShow);
				handler.sendMessage(message);
			}
			savedResultToShow = null;
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	/** ������� */
	public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
		inactivityTimer.onActivity();
		beepManager.playBeepSoundAndVibrate();

		String msg = rawResult.getText();
		if (msg == null || "".equals(msg)) {
			msg = "�޷�ʶ��";
		}
		playBeepSoundAndVibrate();// ɨ�������ʾ
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");// �������ڸ�ʽ
		String time = simpleDateFormat.format(new Date());
		dbUtil = new DatabaseUtil(this);
		dbUtil.open();
		dbUtil.createLocation(msg, time);
		dbUtil.close();
		Intent intent = new Intent(CaptureActivity.this, ShowActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("msg", msg);
		intent.putExtras(bundle);
		startActivity(intent);
	}

	/**
	 * ɨ�������ʾ
	 */
	private static final long VIBRATE_DURATION = 50;

	private void playBeepSoundAndVibrate() {

		if (vibrate) {
			Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_DURATION);
		}
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		if (surfaceHolder == null) {
			return;
		}
		if (cameraManager.isOpen()) {
			return;
		}
		try {
			cameraManager.openDriver(surfaceHolder);
			if (handler == null) {
				handler = new CaptureActivityHandler(this, decodeFormats,
						decodeHints, characterSet, cameraManager);
			}
			decodeOrStoreSavedBitmap(null, null);
		} catch (IOException ioe) {
			displayFrameworkBugMessageAndExit();
		} catch (RuntimeException e) {
			displayFrameworkBugMessageAndExit();
		}
	}

	private void displayFrameworkBugMessageAndExit() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("����");
		builder.setMessage("��Ǹ������������⣬��������Ҫ�����豸");
		builder.setPositiveButton("ȷ��", new FinishListener(this));
		builder.setOnCancelListener(new FinishListener(this));
		builder.show();
	}

	public void restartPreviewAfterDelay(long delayMS) {
		if (handler != null) {
			handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
		}
		resetStatusView();
	}

	private void resetStatusView() {
		viewfinderView.setVisibility(View.VISIBLE);
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}

}
