# WebTest
这篇文章主要讲解android 中h5与webview交互相关的内容。

# 1：android 调用js方法

## 1.1 loadUrl
首先设置webview与js交互，这里我们需要用到WebSettings这个类：
```
 WebSettings webSettings= binding.myWeb.getSettings();
 //设置与js交互
webSettings.setJavaScriptEnabled(true);
//设置可以打开弹框，适用于window.open
 webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
```
这里在项目本地放置了html文件，路径main/assets ：
```
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title>My_Hello</title>
    <script>
       function callJS(){
          alert("Android调用了JS的callJS方法");
       }
    </script>
</head>
</html>
```
接着，我们加载本地的html文件：
具体的方法是file://android_asset/+xxx.html;
```
webview.loadUrl("file:///android_asset/Test1.html");
```
至于调用callJS则需要用到webview的post方法：
```
myWeb.post(new Runnable() {
            @Override
            public void run() {
                 myWeb.loadUrl("javascript:callJS()");
            }
        });
```
最后我们设置webviewChromClient，重写onJsAlert方法，来响应alert弹框。

```
    class MyWebChromeClent extends WebChromeClient{

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

```
看下效果：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190719161003492.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzIzMDI1MzE5,size_16,color_FFFFFF,t_70)

## 1.2 evaluateJavascript
evaluateJavascript方法是android 4.4以后的新方法；
```
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
```
两个参数，第一个指定js方法，第二个回调js方法的返回值。
我们直接在loadurl后调用下该方法，并把返回值打印下：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190719171824144.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzIzMDI1MzE5,size_16,color_FFFFFF,t_70)
。。。返回值为null，什么情况。

这里需要我们注意，如果直接调用这个方法需要保证执行时间在onPageFinished之后，否则onReceiveValue中返回值一直是null。
所以，将调用方法放入onPageFinished

```
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
    }
```
由于loadurl每次调用都会刷新页面，所以我们在使用的时候结合两种方法一起使用，即19以下用loadUrl，以上使用evaluateJavascript（）；

## 2：js调用native

## 2.1:addJavascriptInterface
android 中内置的addJavascriptInterface进行对象映射，参数两个：
1：object 2：interfaceName
首先定义object，如下：
```
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
```
使用注解JavascriptInterface定义方法；
接着定义在html中调用：
```
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title>My_Hello</title>
    <script>
    function callAndroid(){
        console.log(window.Native.getToken());
        window.Native.showToast("这里是js传给你的值");
    }
    </script>
        <button  type="button" id="button1" onclick="callAndroid()" >获取token</button>
</head>
</html>
```
注意：html中的方法需要使用window.来开始，Native就是我们在JavascriptInterface定义的name。

## 2.2 shouldOverrideUrlLoading
shouldOverrideUrlLoading方法中我们可以进行url拦截，一般处理重定向问题也是在该方法中实现。
另外api21后参数为url的方法已经过时，新增的WebResourceRequest参数：

```
  @Override
    @SuppressWarnings("deprecation") // for invoking the old shouldOverrideUrlLoading.
    @RequiresApi(21)
    public boolean shouldOverrideUrlLoading(@NonNull WebView view,
            @NonNull WebResourceRequest request) {
        if (Build.VERSION.SDK_INT < 21) return false;
        return shouldOverrideUrlLoading(view, request.getUrl().toString());
    }
```



## 3：浏览器链接跳转本地activity
这里不再赘述addJavascriptInterface这中方法的实现，而是通过activity的隐式启动。
首先需要在manifest中定义activity的属性：

```
 <activity android:name=".SecondActivity">
            <intent-filter>
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <action android:name="android.intent.action.VIEW"/>
                <data
                    android:host="native"
                    android:scheme="webtest" />
            </intent-filter>
        </activity>
```
html文件中使用如下：

```
  <a href="webtest://native/param?id=123"> new Activity</a>
```
使用scheme和host，另外参数拼接。
接着由于是url需要我们在本地处理，所以需要重写shouldOverrideUrlLoading：

```
  class MyWebViewClient extends WebViewClient{

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
    }
```

最后是对应的activity中接收参数信息。

```
  Intent intent = getIntent();
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            String data = uri.toString();
            binding.tvParam.setText(data);
        }
```





