package com.example.basma.ui.theme

import android.content.Context
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.core.os.CancellationSignal

object FingerprintHelper {

    private var fingerprintManager: FingerprintManagerCompat?=null
    private var mCancellationSignal: CancellationSignal? = null
    fun init(ctx: Context) {
        fingerprintManager = FingerprintManagerCompat.from(ctx)
    }

    /**
     * 确定是否至少有一个指纹登记过
     *
     * @return 如果至少有一个指纹登记，则为true，否则为false
     */
    fun hasEnrolledFingerprints(): Boolean {
        return fingerprintManager!!.hasEnrolledFingerprints()
    }

    /**
     * 确定指纹硬件是否存在并且功能正常。
     *
     * @return 如果硬件存在且功能正确，则为true，否则为false。
     */
    fun isHardwareDetected(): Boolean {
        return fingerprintManager!!.isHardwareDetected
    }

    /**
     * 开始进行指纹识别
     * @param callback 指纹识别回调函数
     */
    fun authenticate(callback: FingerprintManagerCompat.AuthenticationCallback) {
        mCancellationSignal = CancellationSignal()
        fingerprintManager!!.authenticate(null, 0, mCancellationSignal, callback, null)
    }

    /**
     * 提供取消操作能力
     */
    fun cancel(){
        mCancellationSignal?.cancel()
    }
}