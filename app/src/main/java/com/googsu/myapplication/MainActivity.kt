package com.googsu.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URISyntaxException
import android.content.Intent.getIntent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails


class MainActivity : AppCompatActivity() {
    private var HOME_URL = "https://www.googsu.com/"
    private lateinit var referrerClient: InstallReferrerClient

    private var doubleBackToExitPressedOnce = false
    private lateinit var webView: WebView
    private lateinit var webViewLayout: FrameLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)
        webViewLayout = findViewById(R.id.webview_frame);

        // 웹뷰 네비게이션
        // 뒤로가기 버튼
        //goBackButton.setOnClickListener {            webView.goBack() // 뒤로 가기        }

        // 앞으로 가기 버튼
        //goForwardButton.setOnClickListener {            webView.goForward()        }

        // 홈 버튼
        //goHomeButton.setOnClickListener {            webView.loadUrl(HOME_URL)        }

        // 리프레시 레이아웃
        //refreshLayout.setOnRefreshListener {            webView.reload() // 새로 고침        }

        webView.settings.run {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
        }

        webView.webChromeClient = object: WebChromeClient() {

            // 페이지 로딩 정도 0~100
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)

                progressBar.progress = newProgress
            }

            /// ---------- 팝업 열기 ----------
            /// - 카카오 JavaScript SDK의 로그인 기능은 popup을 이용합니다.
            /// - window.open() 호출 시 별도 팝업 webview가 생성되어야 합니다.
            ///
            override fun onCreateWindow(
                    view: WebView,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message
            ): Boolean {

                // 웹뷰 만들기
                var childWebView = WebView(view.context)
                Log.d("TAG", "웹뷰 만들기")
                // 부모 웹뷰와 동일하게 웹뷰 설정
                childWebView.run {
                    settings.run {
                        javaScriptEnabled = true
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(true)
                    }
                    layoutParams = view.layoutParams
                    webViewClient = view.webViewClient
                    webChromeClient = view.webChromeClient
                }

                // 화면에 추가하기
                webViewLayout.addView(childWebView)
                // TODO: 화면 추가 이외에 onBackPressed() 와 같이
                //       사용자의 내비게이션 액션 처리를 위해
                //       별도 웹뷰 관리를 권장함
                //   ex) childWebViewList.add(childWebView)

                // 웹뷰 간 연동
                val transport = resultMsg.obj as WebView.WebViewTransport
                transport.webView = childWebView
                resultMsg.sendToTarget()

                return true
            }

            /// ---------- 팝업 닫기 ----------
            /// - window.close()가 호출되면 앞에서 생성한 팝업 webview를 닫아야 합니다.
            ///
            override fun onCloseWindow(window: WebView) {
                super.onCloseWindow(window)

                // 화면에서 제거하기
                webViewLayout.removeView(window)
                // TODO: 화면 제거 이외에 onBackPressed() 와 같이
                //       사용자의 내비게이션 액션 처리를 위해
                //       별도 웹뷰 array 관리를 권장함
                //   ex) childWebViewList.remove(childWebView)
            }

        }

        webView.webViewClient = object: WebViewClient() {

            // 페이지가 다 로딩 되었을때
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                //refreshLayout.isRefreshing = false

                progressBar.setVisibility(View.INVISIBLE)

                //goBackButton.isEnabled = webView.canGoBack()
                //goForwardButton.isEnabled = webView.canGoForward()
                //addressBar.setText(url)
            }

            // 시작 시
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                progressBar.setVisibility(View.VISIBLE)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                Log.d("TAG url", request.url.toString())
                Log.d("TAG scheme", request.url.scheme.toString())
                if (request.url.scheme == "https") {
                    //webView.loadUrl(request.url.toString())
                }

                if (request.url.scheme == "intent") {
                    try {
                        Log.d("TAG scheme", intent.getPackage().toString())
                        val intent = Intent.parseUri(request.url.toString(), Intent.URI_INTENT_SCHEME)
                        // 실행 가능한 앱이 있으면 앱 실행
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(intent)
                            Log.d("TAG", "ACTIVITY: ${intent.`package`}")
                            return true
                        }

                        // Fallback URL이 있으면 현재 웹뷰에 로딩
                        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                        if (fallbackUrl != null) {
                            view.loadUrl(fallbackUrl)
                            Log.d("TAG FALLBACK", "FALLBACK: $fallbackUrl")
                            return true
                        }

                        Log.e("TAG", "Could not parse anythings")

                    } catch (e: URISyntaxException) {
                        Log.e("TAG", "Invalid intent request", e)
                    }
                }

                // 나머지 서비스 로직 구현


                Log.d("TAG", "return false")
                return false
            }
        }


        if (intent != null) {
            val uri: Uri? = intent.data
            if (uri != null) {
                Log.d("MainAtv-receivedata", uri.toString())
                uri.getQueryParameter("url")?.let {
                    Log.d("MainAtv-receivedata", it)
                    Toast.makeText(baseContext, it, Toast.LENGTH_SHORT).show()
                    webView.loadUrl(it)
                }

            }
            else {


                //referrerClient
                var referrerUrl=""
                referrerClient = InstallReferrerClient.newBuilder(this).build()
                referrerClient.startConnection(object : InstallReferrerStateListener {

                    override fun onInstallReferrerSetupFinished(responseCode: Int) {
                        when (responseCode) {
                            InstallReferrerClient.InstallReferrerResponse.OK -> {
                                // Connection established.
                                val response: ReferrerDetails = referrerClient.installReferrer
                                referrerUrl = response.installReferrer
                                val referrerClickTime: Long = response.referrerClickTimestampSeconds
                                val appInstallTime: Long = response.installBeginTimestampSeconds
                                val instantExperienceLaunched: Boolean = response.googlePlayInstantParam

                                val uri: Uri? = Uri.parse(referrerUrl)
                                if (uri != null) {
                                    Log.d("onInstallReferrerSetupFinished", uri.toString())
                                    uri.getQueryParameter("url")?.let {
                                        Log.d("onInstallReferrerSetupFinished", it)
                                        Toast.makeText(baseContext, it, Toast.LENGTH_SHORT).show()
                                        webView.loadUrl(it)
                                    }

                                }

                            }
                            InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                                // API not available on the current Play Store app.
                            }
                            InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                                // Connection couldn't be established.
                            }
                        }
                    }

                    override fun onInstallReferrerServiceDisconnected() {
                        // Try to restart the connection on the next request to
                        // Google Play by calling the startConnection() method.
                    }
                })

                if (referrerUrl == "") {
                    Log.d("none referrerUrl", HOME_URL)
                    webView.loadUrl(HOME_URL)
                }
            }
        }
        else {
            webView.loadUrl("https://www.googsu.com/kakao_talk_message.html")
        }

        //안됨 webView.loadUrl("intent://inappbrowser?url=https%3A%2F%2Faccounts.kakao.com%2Fqr_check_in#Intent;scheme=kakaotalk;package=com.kakao.talk;end")
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            val msg = token.toString()
            Log.d("FCM", msg)
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        })
    }

    //뒤로가기(back) 버튼 리스너 재정의
    override fun onBackPressed() {
        if(webView.canGoBack()){
            webView.goBack()
        }
        else{
            super.onBackPressed()
        }

        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            doubleBackToExitPressedOnce = false
        }, 2000)
    }

}