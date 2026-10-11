package com.sejun.autoclicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 앱 안의 사용 설명서. 앱에 함께 들어 있는 `assets/manual.html`을 그대로 보여 준다(인터넷 없이 열린다).
 * 문서 안의 차례 링크(#자리)만 따라가고, 바깥 주소는 열지 않는다. 스크립트는 쓰지 않는다.
 */
class ManualActivity : Activity() {
    companion object {
        const val PAGE = "file:///android_asset/manual.html"
        /** 설명서 안에서 움직이는 주소인지(바깥 주소는 막는다). */
        fun isInside(url: String?): Boolean = url != null && url.substringBefore('#') == PAGE
        fun open(context: Context) = context.startActivity(Intent(context, ManualActivity::class.java))
    }

    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        T.use(this)
        val dp = resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()
        val bg = getColor(R.color.background)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg); fitsSystemWindows = true }
        // 위 막대: 제목과 닫기
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(16), 0, px(8), 0)
        }
        bar.addView(TextView(this).apply {
            text = T.a.manual; textSize = 18f; setTypeface(typeface, Typeface.BOLD)
            setTextColor(getColor(R.color.text_primary))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(TextView(this).apply {
            text = T.s.close; textSize = 15f; setTypeface(typeface, Typeface.BOLD); gravity = Gravity.CENTER
            setTextColor(getColor(R.color.primary))
            setPadding(px(14), 0, px(14), 0)
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(48)))
        root.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(52)))

        web = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = false
            settings.allowContentAccess = false
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = !isInside(request.url?.toString())
            }
            loadUrl(PAGE)
        }
        root.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    /** 차례 링크로 옮겨 다닌 뒤의 뒤로 가기는 먼저 설명서 안에서 되돌아가고, 더 갈 곳이 없으면 닫는다. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }
}
