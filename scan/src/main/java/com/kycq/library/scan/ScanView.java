package com.kycq.library.scan;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

public final class ScanView extends FrameLayout implements SurfaceHolder.Callback, Camera.PreviewCallback {
	private CameraManager mCameraManager;
	private SurfaceView mPreviewView;
	private FinderView mFinderView;
	
	private DecodeTask mDecodeTask;
	private MultiFormatReader mMultiFormatReader;
	
	OnScanListener mOnScanListener;
	
	public ScanView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		if (isInEditMode()) {
			return;
		}
		
		mCameraManager = new CameraManager(context);
		
		mPreviewView = new SurfaceView(context);
		addView(mPreviewView);
		mFinderView = new FinderView(context);
		addView(mFinderView);
		
		mMultiFormatReader = new MultiFormatReader();
		Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
		Collection<BarcodeFormat> decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
		decodeFormats.addAll(DecodeFormat.PRODUCT_FORMATS);
		decodeFormats.addAll(DecodeFormat.INDUSTRIAL_FORMATS);
		decodeFormats.addAll(DecodeFormat.QR_CODE_FORMATS);
		decodeFormats.addAll(DecodeFormat.DATA_MATRIX_FORMATS);
		decodeFormats.addAll(DecodeFormat.AZTEC_FORMATS);
		decodeFormats.addAll(DecodeFormat.PDF417_FORMATS);
		hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
		//if (characterSet != null) {
		//	hints.put(DecodeHintType.CHARACTER_SET, characterSet);
		//}
		mMultiFormatReader.setHints(hints);
		
		mPreviewView.getHolder().addCallback(this);
	}
	
	public void setOnScanListener(OnScanListener onScanListener) {
		mOnScanListener = onScanListener;
	}
	
	public void startScan() {
		mCameraManager.startPreview();
	}
	
	public void stopScan() {
		mCameraManager.stopPreview();
	}
	
	public void setFlashMode(boolean flashMode) {
		mCameraManager.setFlashMode(flashMode);
	}
	
	public boolean isFlashMode() {
		return mCameraManager.isFlashMode();
	}
	
	public void decodeFile(File file) {
		DecodeInfo decodeInfo = new DecodeInfo();
		decodeInfo.file = file;
		
		if (mDecodeTask != null) {
			mDecodeTask.cancel(true);
		}
		
		mDecodeTask = new DecodeTask(this, mMultiFormatReader);
		mDecodeTask.setNotifyFailure(true);
		mDecodeTask.execute(decodeInfo);
	}
	
	void requestPreview() {
		if (mDecodeTask != null) {
			mDecodeTask.cancel(true);
			mDecodeTask = null;
		}
		mCameraManager.setPreviewCallback(this);
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (mDecodeTask != null && mDecodeTask.getStatus() == AsyncTask.Status.RUNNING) {
			return;
		}
		
		DecodeInfo decodeInfo = new DecodeInfo();
		decodeInfo.data = data;
		decodeInfo.dataWidth = mCameraManager.getCameraResolution().x;
		decodeInfo.dataHeight = mCameraManager.getCameraResolution().y;
		decodeInfo.rotationAngle = mCameraManager.getCameraRotationAngle();
		Rect finderRect = mFinderView.getFinderRect();
		if (finderRect != null) {
			Rect decodeRect = new Rect();
			int widthOffset = (mPreviewView.getMeasuredWidth() - getMeasuredWidth()) / 2;
			int heightOffset = (mPreviewView.getMeasuredHeight() - getMeasuredHeight()) / 2;
			
			decodeRect.left = finderRect.left + widthOffset;
			decodeRect.top = finderRect.top + heightOffset;
			decodeRect.right = finderRect.right + widthOffset;
			decodeRect.bottom = finderRect.bottom + heightOffset;
			decodeInfo.decodeRect = decodeRect;
		}
		mDecodeTask = new DecodeTask(this, mMultiFormatReader);
		mDecodeTask.execute(decodeInfo);
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder surfaceHolder) {
		try {
			mCameraManager.openDriver(surfaceHolder);
		} catch (IOException e) {
			mCameraManager.closeDriver();
		}
		
		try {
			mCameraManager.startPreview();
		} catch (Exception e) {
			mCameraManager.closeDriver();
		}
		
		mCameraManager.setPreviewCallback(this);
		
		if (mCameraManager.isOpen()) {
			int measuredWidth = mPreviewView.getMeasuredWidth();
			int measuredHeight = mPreviewView.getMeasuredHeight();
			if (measuredWidth == 0 || measuredHeight == 0) {
				return;
			}
			
			Point previewResolution = mCameraManager.getPreviewResolution();
			if (measuredWidth != previewResolution.x || measuredHeight != previewResolution.y) {
				requestLayout();
			}
		}
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
		if (surfaceHolder.getSurface() == null) {
			return;
		}
		
		if (mCameraManager.getPreviewResolution() == null) {
			return;
		}
		
		mCameraManager.stopPreview();
		mCameraManager.startPreview();
		
		mCameraManager.setPreviewCallback(this);
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
		mCameraManager.stopPreview();
		mCameraManager.closeDriver();
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		Point previewResolution = mCameraManager.getPreviewResolution();
		if (previewResolution == null) {
			return;
		}
		
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		
		int previewWidth = previewResolution.x;
		int previewHeight = previewResolution.y;
		
		double ratio = (double) previewWidth / (double) previewHeight;
		
		int widthOffset = previewWidth - widthSize;
		int heightOffset = previewHeight - heightSize;
		
		int absWidthOffset = Math.abs(widthOffset);
		int absHeightOffset = Math.abs(heightOffset);
		
		int measuredWidth;
		int measuredHeight;
		
		if (widthOffset < 0 && heightOffset < 0) {
			// 预览分辨率宽高比控件分辨率宽高小
			if (absWidthOffset > absHeightOffset) {
				measuredWidth = widthSize;
				measuredHeight = (int) (widthSize / ratio);
			} else {
				measuredWidth = (int) (heightSize * ratio);
				measuredHeight = heightSize;
			}
		} else if (widthOffset < 0 && heightOffset > 0) {
			// 预览分辨率宽度比控件分辨率宽度大，预览分辨率高度比控件分辨率高度小
			measuredWidth = widthSize;
			measuredHeight = (int) (widthSize / ratio);
		} else if (widthOffset > 0 && heightOffset < 0) {
			// 预览分辨率宽度比控件分辨率宽度小，预览分辨率高度比控件分辨率高度大
			measuredWidth = (int) (heightSize * ratio);
			measuredHeight = heightSize;
		} else {
			measuredWidth = previewWidth;
			measuredHeight = previewHeight;
		}
		
		mPreviewView.measure(
				MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
		);
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		
		int width = right - left;
		int height = bottom - top;
		
		int measuredWidth = mPreviewView.getMeasuredWidth();
		int measureHeight = mPreviewView.getMeasuredHeight();
		int widthOffset = (width - measuredWidth) / 2;
		int heightOffset = (height - measureHeight) / 2;
		mPreviewView.layout(
				widthOffset,
				heightOffset,
				widthOffset + measuredWidth,
				heightOffset + measureHeight
		);
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		
		if (mDecodeTask == null) {
			return;
		}
		mDecodeTask.cancel(true);
		mDecodeTask = null;
	}
}
