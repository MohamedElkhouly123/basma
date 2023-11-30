package com.example.basma2

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import com.example.basma.ui.theme.FingerprintDialog
import com.example.basma.ui.theme.FingerprintHelper
import com.example.basma2.databinding.ActivityMainBinding
import com.google.gson.Gson
import java.security.KeyStore
import java.security.SignatureException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE: Int=23
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var mDialog: FingerprintDialog
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val REQUEST_CODE_PERMISSION = 1
    private var fingerprintManager: FingerprintManagerCompat? = null
    private var cancellationSignal: CancellationSignal? = null
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
        mDialog = FingerprintDialog()
        FingerprintHelper.init(this)
        binding.fab.setOnClickListener { view ->
            if (FingerprintHelper.isHardwareDetected()) {
                if (FingerprintHelper.hasEnrolledFingerprints()) {
                    mDialog.show(supportFragmentManager, "dialog")
                } else {
                    Toast.makeText(applicationContext, "您未录制任何指纹", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(applicationContext, "您的手机不支持指纹识别", Toast.LENGTH_SHORT).show()
            }
        }
        if (ActivityCompat.checkSelfPermission(
                MainActivity@ this,
                android.Manifest.permission.USE_FINGERPRINT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.USE_FINGERPRINT),
                REQUEST_CODE_PERMISSION
            )
        } else {

        }
        val biometricManager = BiometricManager.from(this)
//        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
//            BiometricManager.BIOMETRIC_SUCCESS ->
//                Log.d("MY_APP_TAG", "App can authenticate using biometrics.")
//            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
//                Log.e("MY_APP_TAG", "No biometric features available on this device.")
//            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
//                Log.e("MY_APP_TAG", "Biometric features are currently unavailable.")
//            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
//                // Prompts the user to create credentials that your app accepts.
//                val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
//                    putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
//                        BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
//                }
//                startActivityForResult(enrollIntent, REQUEST_CODE)
//            }
//        }
        if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
            // التحقق من أن الإذن ممنوح لاستخدام بصمة الإصبع
            if (ActivityCompat.checkSelfPermission(
                    MainActivity@ this,
                    android.Manifest.permission.USE_BIOMETRIC
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.USE_BIOMETRIC),
                    REQUEST_CODE_PERMISSION
                )
            } else {
                // بدء عملية التوثيق باستخدام بصمة الإصبع
//                promptInfo = BiometricPrompt.PromptInfo.Builder()
//                    .setTitle("My App's Authentication")
//                    .setSubtitle("Please login to get access")
//                    .setDescription("My App is using Android biometric authentication")
//                    .setDeviceCredentialAllowed(true)
//                    .build()
                getPromptInfo()
                instanceOfBiometricPrompt().authenticate(promptInfo)
                showBiometricPrompt()
            }
        } else {
            // جهازك لا يدعم بصمة الإصبع أو لم يتم تكوينه بشكل صحيح
            Toast.makeText(this, "بصمة الإصبع غير مدعومة على جهازك", Toast.LENGTH_SHORT).show()
        }

    }
    private fun instanceOfBiometricPrompt(): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(this)

        val callback = object: BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                showMessage("$errorCode :: $errString")
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                showMessage("Authentication failed for an unknown reason")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                showMessage("Authentication was successful")


                val gson = Gson()
                val json = gson.toJson(result.cryptoObject)
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
                    Toast.makeText(applicationContext, "Something wrong"+result.authenticationType!!, Toast.LENGTH_SHORT).show()
                }
            }
        }

        val biometricPrompt = BiometricPrompt(this, executor, callback)
        return biometricPrompt
    }

    private fun showMessage(s: String) {
        Toast.makeText(MainActivity@this,s, Toast.LENGTH_SHORT).show()
    }

    private fun getPromptInfo(): BiometricPrompt.PromptInfo {
         promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("My App's Authentication")
            .setSubtitle("Please login to get access")
            .setDescription("My App is using Android biometric authentication")
            .setDeviceCredentialAllowed(true)
//             .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        return promptInfo
    }

    private fun generateSecretKey(keyGenParameterSpec: KeyGenParameterSpec) {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")

        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null)
        return keyStore.getKey("KEY_NAME", null) as SecretKey
    }

    private fun getCipher(): Cipher {
        return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_CBC + "/"
                + KeyProperties.ENCRYPTION_PADDING_PKCS7)
    }


    private fun showBiometricPrompt() {
//        biometricPrompt = BiometricPrompt.Builder(MainActivity@ this)
//            .setTitle("تسجيل الدخول باستخدام بصمة الإصبع")
//            .setNegativeButton(
//                "إلغاء",
//                this.mainExecutor,
//                { _, _ -> /* تم النقر على زر "إلغاء" */ }
//            )
//            .build()
//        getAuthenticationCallback()
// إنشاء مثيل من BiometricPrompt
//        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
//            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
//                super.onAuthenticationError(errorCode, errString)
//                // هنا يمكنك التعامل مع حالات الخطأ المختلفة
//                Toast.makeText(applicationContext, "خطأ في التوثيق: $errString", Toast.LENGTH_SHORT).show()
//            }
//
//            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
//                super.onAuthenticationSucceeded(result)
//                // هنا يمكنك تنفيذ الإجراءات المطلوبة بعد التوثيق الناجح
//                Toast.makeText(applicationContext, "تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show()
//            }
//
//            override fun onAuthenticationFailed() {
//                super.onAuthenticationFailed()
//                // هنا يمكنك التعامل مع حالة فشل التوثيق
//                Toast.makeText(applicationContext, "فشل في تسجيل الدخول", Toast.LENGTH_SHORT).show()
//            }
//        })




//        biometricPrompt.authenticate(
//            BiometricPrompt.Builder(this)
//                .setDescription("قم بتوجيه بصمتك لتسجيل الدخول")
//                .setNegativeButton("إلغاء",
//                    this.mainExecutor,
//                    { _, _ -> /* تم النقر على زر "إلغاء" */ })
//                .build()
//        ) { result ->
//            // تنفيذ الإجراءات المطلوبة بناءً على نتيجة التوثيق
//            if (result.authenticationResult != null) {
//                // تم التوثيق بنجاح، يمكنك تنفيذ الإجراءات المطلوبة لتسجيل الدخول
//                Toast.makeText(this, "تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show()
//            } else {
//                // فشل في التوثيق بصمة الإصبع، يمكنك التعامل مع هذا الحالة وفقًا لمتطلبات تطبيقك
//                Toast.makeText(this, "فشل في تسجيل الدخول", Toast.LENGTH_SHORT).show()
//            }
//        }

    }

    override fun onDestroy() {
        FingerprintHelper.cancel()
        super.onDestroy()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}