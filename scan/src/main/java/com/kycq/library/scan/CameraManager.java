package com.kycq.library.scan;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.view.SurfaceHolder;

import java.io.IOException;

class CameraManager {
	private Context mContext;
	
	private CameraConfigManager mCameraConfigManager;
	private OpenCamera mOpenCamera;
	
	private boolean mIsInitialized;
	private boolean mIsPreviewing;
	
	private AutoFocusManager mAutoFocusManager;
	
	private boolean mIsFlashMode;
	
	CameraManager(Context context) {
		mContext = context;
		mCameraConfigManager = new CameraConfigManager();
	}
	
	boolean isOpen() {
		return mOpenCamera != null && mOpenCamera.getCamera() != null;
	}
	
	Point getCameraResolution() {
		return mCameraConfigManager.mCameraResolution;
	}
	
	Point getPreviewResolution() {
		return mCameraConfigManager.mPreviewResolution;
	}
	
	int getCameraRotationAngle() {
		return mCameraConfigManager.mCameraRotationAngle;
	}
	
	void setPreviewCallback(Camera.PreviewCallback previewCallback) {
		if (isOpen()) {
			mOpenCamera.getCamera().setOneShotPreviewCallback(previewCallback);
		}
	}
	
	void setFlashMode(boolean isFlashMode) {
		mIsFlashMode = isFlashMode;
		
		if (isOpen()) {
			Camera.Parameters parameters = mOpenCamera.getCamera().getParameters();
			if (isFlashMode) {
				parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			} else {
				parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			}
			mOpenCamera.getCamera().setParameters(parameters);
		}
	}
	
	boolean isFlashMode() {
		return mIsFlashMode;
	}
	
	void openDriver(SurfaceHolder surfaceHolder) throws IOException {
		if (isOpen()) {
			return;
		}
		OpenCamera theOpenCamera = mOpenCamera;
		if (theOpenCamera == null) {
			try {
				theOpenCamera = OpenCamera.open();
			} catch (Exception ignored) {
				ignored.printStackTrace();
			}
			if (theOpenCamera == null) {
				return;
			}
			mOpenCamera = theOpenCamera;
		}
		
		theOpenCamera.getCamera().setPreviewDisplay(surfaceHolder);
		Camera.Parameters parameters = theOpenCamera.getCamera().getParameters();
		if (mIsFlashMode) {
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
		} else {
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
		}
		theOpenCamera.getCamera().setParameters(parameters);
		
		if (!mIsInitialized) {
			mIsInitialized = true;
			mCameraConfigManager.initFromCameraParameters(mContext, theOpenCamera);
		}
		
		Camera theCamera = theOpenCamera.getCamera();
		Camera.Parameters cameraParameters = theCamera.getParameters();
		String parametersFlattened = cameraParameters == null ? null : cameraParameters.flatten();
		try {
			mCameraConfigManager.setDesiredCameraParameters(theOpenCamera, false);
		} catch (RuntimeException re) {
			re.printStackTrace();
			if (parametersFlattened != null) {
				cameraParameters = theCamera.getParameters();
				cameraParameters.unflatten(parametersFlattened);
				try {
					theCamera.setParameters(cameraParameters);
					mCameraConfigManager.setDesiredCameraParameters(theOpenCamera, true);
				} catch (RuntimeException ignored) {
					ignored.printStackTrace();
				}
			}
		}
		theCamera.setPreviewDisplay(surfaceHolder);
	}
	
	void closeDriver() {
		if (isOpen()) {
			mOpenCamera.getCamera().release();
			mOpenCamera = null;
		}
	}
	
	synchronized void startPreview() {
		OpenCamera theOpenCamera = mOpenCamera;
		if (theOpenCamera != null && !mIsPreviewing) {
			theOpenCamera.getCamera().startPreview();
			mIsPreviewing = true;
			mAutoFocusManager = new AutoFocusManager(theOpenCamera.getCamera(), true);
		}
	}
	
	synchronized void stopPreview() {
		if (mAutoFocusManager != null) {
			mAutoFocusManager.stop();
			mAutoFocusManager = null;
		}
		if (mOpenCamera != null && mIsPreviewing) {
			mOpenCamera.getCamera().stopPreview();
			mIsPreviewing = false;
		}
	}
}
