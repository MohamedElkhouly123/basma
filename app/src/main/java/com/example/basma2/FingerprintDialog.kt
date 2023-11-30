package com.example.basma.ui.theme

import android.app.Service
import android.content.DialogInterface
import android.os.Bundle
import android.os.Vibrator
import android.util.Base64
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import com.example.basma2.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import java.security.SignatureException

/**
 * 类名：FingerprintDialog
 * 作者：Yun.Lei
 * 功能：
 * 创建日期：2017-11-13 15:05
 * 修改人：
 * 修改时间：
 * 修改备注：
 */
class FingerprintDialog : BottomSheetDialogFragment() {
    private lateinit var mVibrator: Vibrator
    private lateinit var fingerImageView: ImageView
    private lateinit var hintTv: TextView
    private lateinit var btnCancel: TextView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mVibrator = requireActivity()!!.getSystemService(Service.VIBRATOR_SERVICE) as Vibrator
        return inflater.inflate(R.layout.dialog_fingerprint, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fingerImageView = view.findViewById(R.id.fingerprint_image)
        hintTv = view.findViewById(R.id.hint_tv)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnCancel.setOnClickListener { dismiss() }
        FingerprintHelper.authenticate(object : FingerprintManagerCompat.AuthenticationCallback() {

            //在识别指纹成功时调用。
            override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
                startVibrate()
                val gson = Gson()
                val json = gson.toJson(result!!.cryptoObject)
                val resultJsonStr = json.toString()
                Log.d("MY_APP_TAG", "Biometric features are currently unavailable. "+resultJsonStr)

                // إذا كان لديك كائن Signature في CryptoObject
                if (result.cryptoObject != null &&
                    result.cryptoObject!!.signature != null) {
                    try {
                        val signature = result.cryptoObject!!.signature
                        // تحديث التوقيع باستخدام رسالة مولدة من قبل الخادم
//                        signature.update(mToBeSignedMessage.toByteArray())
                        // حصول على التوقيع كسلسلة Base64
                        val signatureString = Base64.encodeToString(signature!!.sign(), Base64.URL_SAFE)
                        Log.d("MY_APP_TAG", "Biometric features are currently unavailable. "+signatureString)

//                        // حفظ التوقيع في SharedPreferences
//                        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
//                        val editor = sharedPreferences.edit()
//                        editor.putString("signature", signatureString)
//                        editor.apply()
                    } catch (e: SignatureException) {
                        throw RuntimeException()
                    }
                } else {
                    // Error
                    Toast.makeText(context, "Something wrong", Toast.LENGTH_SHORT).show()
                }
                hintTv.text = "onAuthenticationSucceeded"
                fingerImageView.setImageResource(R.drawable.fingerprint_svgrepo_com)
                hintTv.setTextColor(context!!.resources.getColor(R.color.black))
                hintTv.postDelayed({ dismiss() }, 1000)
            }

            //当指纹有效但未被识别时调用。
            override fun onAuthenticationFailed() {
                hintTv.text = "onAuthenticationFailed"
                startVibrate()
            }

            //当遇到不可恢复的错误并且操作完成时调用。
            override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
                hintTv.text = errString
                if (errMsgId != 5) {   //取消不震动
                    startVibrate()
                }
            }

            //在认证期间遇到可恢复的错误时调用。
            override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
                hintTv.text = helpString
                startVibrate()
            }
        })
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        FingerprintHelper.cancel()
    }

    fun startVibrate() {
        mVibrator.vibrate(500)
    }
}