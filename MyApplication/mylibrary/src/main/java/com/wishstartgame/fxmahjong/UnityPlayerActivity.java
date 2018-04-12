package com.wishstartgame.fxmahjong;

import com.unity3d.player.*;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import cn.magicwindow.MLink;
import cn.magicwindow.MWConfiguration;
import cn.magicwindow.MagicWindowSDK;
import cn.magicwindow.Session;
import cn.magicwindow.mlink.MLinkCallback;
import com.umeng.analytics.MobclickAgent;
import java.io.File;
import java.util.Map;


//import com.umeng.analytics.MobclickAgent.UMAnalyticsConfig;

public class UnityPlayerActivity extends Activity
{
    protected UnityPlayer mUnityPlayer;
    private String s_Url = "";
    private boolean isLogin = false;
    private boolean isLogined = false;
    @Override protected void onCreate (Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        getWindow().setFormat(PixelFormat.RGBX_8888); // <--- This makes xperia play happy

        mUnityPlayer = new UnityPlayer(this);
        setContentView(mUnityPlayer);
        mUnityPlayer.requestFocus();

        MobclickAgent.UMAnalyticsConfig pConfig = new MobclickAgent.UMAnalyticsConfig(this, "5a977917f29d9810bc000014", "android");
        MobclickAgent.startWithConfigure(pConfig);

        initMW();
        registerForMLinkCallback();
        Uri mLink = getIntent().getData();
        MLink.getInstance(this).deferredRouter();

        if (mLink != null) {
            MLink.getInstance(this).router(mLink);
        } else {
            MLink.getInstance(this).checkYYB();
        }
    }

    private void initMW() {
        MWConfiguration config = new MWConfiguration(this);
        config.setPageTrackWithFragment(true)
                .setSharePlatform(MWConfiguration.ORIGINAL);
        MagicWindowSDK.initSDK(config);
    }
    private  void registerForMLinkCallback() {
        MLink mLink = MLink.getInstance(this);
        mLink.registerDefault(new MLinkCallback() {
            public void execute(Map<String, String> paramMap, Uri uri, Context context) {
                if(uri != null)
                {
                    s_Url = uri.toString();
                    if(isLogin)
                    {
                        UnityPlayer.UnitySendMessage("GameCenter", "GameingOpen", s_Url);
                    }
                }
            }
        });
    }

    public void GetWechatOpenInfo()
    {
        String s_Trans = s_Url;
        UnityPlayer.UnitySendMessage("GameCenter", "WeChatShareOpen", s_Trans);
        s_Url = "";
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        isLogin = true;
        super.onNewIntent(intent);
        Uri mLink = intent.getData();
        if(intent != null && mLink != null)
        {
            UnityPlayer.UnitySendMessage("GameCenter", "GameingOpen", intent.getData().toString());
            s_Url = "";
        }
        else
        {
            MLink.getInstance(this).checkYYB();
        }
    }

    public void AccountSignIn(String s_UserID)
    {
        MobclickAgent.onProfileSignIn("WeiXin",s_UserID);
    }

    public void WifiPower()
    {
        int strength = 0;
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info.getBSSID() != null) {
            strength = WifiManager.calculateSignalLevel(info.getRssi(), 5);
            int speed = info.getLinkSpeed();
            String units = WifiInfo.LINK_SPEED_UNITS;
            String ssid = info.getSSID();
        }
        UnityPlayer.UnitySendMessage("GameCenter", "WifiPowerBack", String.valueOf(strength));
    }

    public void installApk(File apkFile) {
        if(apkFile != null)
        {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                Application sApp = com.wishstartgame.fxmahjong.AppUtil.sApp;
                Intent installApkIntent = new Intent();
                installApkIntent.setAction(Intent.ACTION_VIEW);
                installApkIntent.addCategory(Intent.CATEGORY_DEFAULT);
                installApkIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                installApkIntent.setDataAndType(FileProvider.getUriForFile(sApp, "com.wishstartgame.fxmahjong.fileprovider", apkFile), "application/vnd.android.package-archive");
                installApkIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (sApp.getPackageManager().queryIntentActivities(installApkIntent, 0).size() > 0) {
                    sApp.startActivity(installApkIntent);
                }
            } else {
                UnityPlayer.UnitySendMessage("GameCenter", "SmallThanAndroid_N_Install","false");
            }
        }
        else
        {

        }
    }

    // Quit Unity
    @Override protected void onDestroy ()
    {
        mUnityPlayer.quit();
        Session.onKillProcess();
        super.onDestroy();
    }

    // Pause Unity
    @Override protected void onPause()
    {
        super.onPause();
        mUnityPlayer.pause();
        Session.onPause(this);
        MobclickAgent.onPause(this);
    }

    // Resume Unity
    @Override protected void onResume()
    {
        super.onResume();
        mUnityPlayer.resume();
        Session.onResume(this);
        MobclickAgent.onResume(this);
        UnityPlayer.UnitySendMessage("GameCenter", "FocusBackReconnect", "");
    }

    // This ensures the layout will be correct.
    @Override public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        mUnityPlayer.configurationChanged(newConfig);
    }

    // Notify Unity of the focus change.
    @Override public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        mUnityPlayer.windowFocusChanged(hasFocus);
    }

    // For some reason the multiple keyevent type is not supported by the ndk.
    // Force event injection by overriding dispatchKeyEvent().
    @Override public boolean dispatchKeyEvent(KeyEvent event)
    {
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE)
            return mUnityPlayer.injectEvent(event);
        return super.dispatchKeyEvent(event);
    }

    // Pass any events not handled by (unfocused) views straight to UnityPlayer
    @Override public boolean onKeyUp(int keyCode, KeyEvent event)     { return mUnityPlayer.injectEvent(event); }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event)   { return mUnityPlayer.injectEvent(event); }
    @Override public boolean onTouchEvent(MotionEvent event)          { return mUnityPlayer.injectEvent(event); }
    /*API12*/ public boolean onGenericMotionEvent(MotionEvent event)  { return mUnityPlayer.injectEvent(event); }
}
