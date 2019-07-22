package com.zh.webtest;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.zh.webtest.databinding.ActivityMainBinding;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= DataBindingUtil. setContentView(this,R.layout.activity_main);
        binding.setPresenter(this);

        WebSettings webSettings= binding.myWeb.getSettings();
        //设置与js交互
        webSettings.setJavaScriptEnabled(true);
        //设置可以打开弹框，适用于window.open
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        binding.myWeb.addJavascriptInterface(new MyJavascriptInterface(),"Native");
        binding.myWeb.setWebChromeClient(new MyWebChromeClent());
        binding.myWeb.setWebViewClient(new MyWebViewClient());
        binding.myWeb.loadUrl("file:///android_asset/Test1.html");

    }

    class  MyJavascriptInterface extends Object{

        @JavascriptInterface
        public String getToken(){
            return  "这里用户登录token";
        }
        @JavascriptInterface
        public void showToast(String x){
            Toast.makeText(MainActivity.this,x,Toast.LENGTH_SHORT).show();
        }
    }



    public void MyEvaluateJavascript(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            binding.myWeb.evaluateJavascript("callJS2()", new ValueCallback<String>() {
                @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                @Override
                public void onReceiveValue(String value) {

                    Toast.makeText(getApplicationContext(), value, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    class MyWebViewClient extends WebViewClient{
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                 MyEvaluateJavascript();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            try {
                if (url.contains("webtest:")){
                    Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
            }catch (Exception e){
            }
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return super.shouldOverrideUrlLoading(view, request);
        }
    }

    class MyWebChromeClent extends WebChromeClient{

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
        }

        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
            return super.onJsPrompt(view, url, message, defaultValue, result);
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(message);
            builder.setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            }).show();
            return true;
        }
    }

    //android 调用js代码
    public void onNativeCallJs(){
        binding.myWeb.post(new Runnable() {
            @Override
            public void run() {
                binding.myWeb.loadUrl("javascript:callJS()");
            }
        });
    }
}
