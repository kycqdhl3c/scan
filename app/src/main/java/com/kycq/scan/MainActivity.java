package com.kycq.scan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewStub;
import android.widget.Toast;

import com.kycq.library.scan.OnScanListener;
import com.kycq.library.scan.ScanView;

public class MainActivity extends AppCompatActivity {
	private ScanView scanView;
	private ViewStub viewStub;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		this.viewStub = (ViewStub) findViewById(R.id.viewStubScan);
		
		ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
	}
	
	private void initScan() {
		this.viewStub.setVisibility(View.VISIBLE);
		this.scanView = (ScanView) findViewById(R.id.scanView);
		this.scanView.setOnScanListener(new OnScanListener() {
			@Override
			public void scanSuccess(String result) {
				Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void decodeFailure() {
				Toast.makeText(MainActivity.this, "解码失败", Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (this.scanView != null) {
			this.scanView.startScan();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (this.scanView != null) {
			this.scanView.stopScan();
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			initScan();
			scanView.startScan();
			return;
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}
}
