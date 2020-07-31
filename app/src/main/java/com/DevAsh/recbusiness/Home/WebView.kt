package com.DevAsh.recbusiness.Home

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.DevAsh.recbusiness.Context.WebContext
import com.DevAsh.recbusiness.R
import kotlinx.android.synthetic.main.activity_web_view.*


class WebView : AppCompatActivity() {

    var page:String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        page = intent.getStringExtra("page")

        setContentView(R.layout.activity_web_view)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {

                loading.visibility= View.VISIBLE
                webView.loadUrl(request?.url.toString())
                return true
            }

           override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                loading.visibility= View.VISIBLE
                //SHOW LOADING IF IT ISNT ALREADY VISIBLE
            }


            override fun onPageFinished(view: WebView?, url: String?) {
                Handler().postDelayed({
                    loading.visibility= View.GONE
                },1000)
            }
        }
//      webView.loadUrl("https://www.rajalakshmi.org/")
        webView.loadUrl(WebContext.url+page)
    }

    override fun onBackPressed(){
        if(!webView.canGoBack()){
            super.onBackPressed()
        }else{
            webView.goBack()

        }
    }
}