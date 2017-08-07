package com.kycq.library.scan;

public interface OnScanListener {
	/**
	 * 扫描成功
	 *
	 * @param result 结果信息
	 */
	void scanSuccess(String result);
	
	/**
	 * 解码失败
	 */
	void decodeFailure();
}
