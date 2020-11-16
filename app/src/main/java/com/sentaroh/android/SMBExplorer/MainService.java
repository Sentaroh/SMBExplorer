package com.sentaroh.android.SMBExplorer;
/*
The MIT License (MIT)
Copyright (c) 2011 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;

import com.sentaroh.android.SMBExplorer.Log.LogUtil;
import com.sentaroh.android.Utilities3.SafManager3;
import com.sentaroh.android.Utilities3.SafStorage3;

import java.util.ArrayList;

import static com.sentaroh.android.SMBExplorer.Constants.SERVICE_HEART_BEAT;

public class MainService extends Service {
    private GlobalParameter mGp=null;

    private CommonUtilities mUtil=null;

    private Context mContext=null;
    private WifiManager mWifiMgr = null;
    private SleepReceiver mSleepReceiver=new SleepReceiver();
    private WifiReceiver mWifiReceiver=new WifiReceiver();
    private MediaReceiver mMediaReceiver=new MediaReceiver();

    private PowerManager.WakeLock mPartialWakelock=null;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext=getApplicationContext();
        mGp=GlobalWorkArea.getGlobalParameters(mContext);
        mUtil=new CommonUtilities(getApplicationContext(), "Service", mGp);

        mUtil.addDebugMsg(1,"I","onCreate entered");

        mWifiMgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        mGp.wifiIsActive = mWifiMgr.isWifiEnabled();
        if (mGp.wifiIsActive) {
            mGp.wifiSsid = mUtil.getConnectedWifiSsid();
        }
        mUtil.addDebugMsg(1, "I", "Wi-Fi Status, Active=" + mGp.wifiIsActive + ", SSID=" + mGp.wifiSsid);

        IntentFilter wifi_filter = new IntentFilter();
        wifi_filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifi_filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        wifi_filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        wifi_filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        wifi_filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        wifi_filter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        registerReceiver(mWifiReceiver, wifi_filter);

        IntentFilter int_filter = new IntentFilter();
        int_filter.addAction(Intent.ACTION_SCREEN_OFF);
        int_filter.addAction(Intent.ACTION_SCREEN_ON);
        int_filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mSleepReceiver, int_filter);

        IntentFilter media_filter = new IntentFilter();
        media_filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        media_filter.addDataScheme("file");
        media_filter.addAction(Intent.ACTION_MEDIA_EJECT);
        media_filter.addDataScheme("file");
        media_filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        media_filter.addDataScheme("file");
        media_filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        media_filter.addDataScheme("file");
        registerReceiver(mMediaReceiver, media_filter);


        mPartialWakelock=((PowerManager)getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
                                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                        , "ZipUtility-Partial");
        initNotification();
    }

    @SuppressLint("NewApi")
    private void setHeartBeat() {
        if (Build.VERSION.SDK_INT>=21) {
//			Thread.dumpStack();
            long time=System.currentTimeMillis()+1000*5;
//			Intent in = new Intent(mContext, SyncService.class);
            Intent in = new Intent(mContext, MainService.class);
            in.setAction(SERVICE_HEART_BEAT);
            PendingIntent pi = PendingIntent.getService(mContext, 0, in, PendingIntent.FLAG_UPDATE_CURRENT);
//			PendingIntent pi = PendingIntent.getService(mContext, 0, in, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager am = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
            if (Build.VERSION.SDK_INT>=23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
            else am.set(AlarmManager.RTC_WAKEUP, time, pi);
        }
    }

    private void cancelHeartBeat() {
//		Intent in = new Intent(mContext, SyncService.class);
        Intent in = new Intent(mContext, MainService.class);
        in.setAction(SERVICE_HEART_BEAT);
        PendingIntent pi = PendingIntent.getService(mContext, 0, in, PendingIntent.FLAG_CANCEL_CURRENT);
//		PendingIntent pi = PendingIntent.getService(mContext, 0, in, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        PowerManager.WakeLock wl=((PowerManager)getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
                                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                        , "ZipUtility-Service-1");
        wl.acquire();
        String action="";
        if (intent!=null) if (intent.getAction()!=null) action=intent.getAction();
        if (action.equals(Intent.ACTION_MEDIA_MOUNTED) ||
//                action.equals(Intent.ACTION_MEDIA_EJECT) ||
//                action.equals(Intent.ACTION_MEDIA_REMOVED) ||
                action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED) ||
                action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED) ||
                action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
            Handler hndl=new Handler();
            final String f_action=action;
            Thread th=new Thread() {
                @Override
                public void run() {
                    int prev_stor_cnt=mGp.safMgr.getLastStorageVolumeInfo().size();
                    if (prev_stor_cnt!=SafManager3.getStorageVolumeInfo(mContext).size()) {
                    }
                    int prev_ls=mGp.safMgr.getSafStorageList().size();
                    for(int i=0;i<50;i++) {
                        mGp.safMgr.refreshSafList();
                        ArrayList<SafStorage3> new_list=mGp.safMgr.getSafStorageList();
                        if (prev_ls!=new_list.size()) {
                            hndl.post(new Runnable() {
                                @Override
                                public void run() {
                                    notifyToMediaStatusChanged();
                                }
                            });
                            break;
                        } else {
                            SystemClock.sleep(300);
                        }
//                        mUtil.addDebugMsg(1,"I","i="+i+", cnt="+prev_ls+", new="+new_list.size());
                    }
                }
            };
            th.start();
        } else if (action.equals(SERVICE_HEART_BEAT)) {
//            mUtil.addDebugMsg(1,"I","onStartCommand entered, action="+action);
            setHeartBeat();
        } else {
            mUtil.addDebugMsg(2,"I","onStartCommand entered, action="+action);
        }
        wl.release();
//		if (isServiceToBeStopped()) stopSelf();
        return START_STICKY;
    };

    @Override
    public IBinder onBind(Intent intent) {
        mUtil.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName()+" entered,action="+intent.getAction());
        setActivityForeground();
//		if (arg0.getAction().equals("MessageConnection"))
        return mSvcClientStub;
//		else return svcInterface;
    };

    @Override
    public boolean onUnbind(Intent intent) {
        mUtil.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName()+" entered");
        return super.onUnbind(intent);
    };

//    @Override
//    public void onLowMemory() {
//        super.onLowMemory();
//        mUtil.addDebugMsg(1, "I", "onLowMemory entered");
//        // Application process is follow
//
//    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUtil.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName()+" entered");
        unregisterReceiver(mSleepReceiver);
        unregisterReceiver(mWifiReceiver);
        unregisterReceiver(mMediaReceiver);
        cancelHeartBeat();
        stopForeground(true);
        LogUtil.flushLog(mContext);
        if (mGp.settingExitClean) {
            Handler hndl=new Handler();
            hndl.postDelayed(new Runnable(){
                @Override
                public void run() {
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }, 100);
        } else {
            System.gc();
        }
    };


    final private ISvcClient.Stub mSvcClientStub = new ISvcClient.Stub() {
        @Override
        public void setCallBack(ISvcCallback callback)
                throws RemoteException {
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
            mGp.callbackStub=callback;
        }

        @Override
        public void removeCallBack(ISvcCallback callback)
                throws RemoteException {
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
            mGp.callbackStub=null;
            stopSelf();
        }

        @Override
        public void aidlStopService() throws RemoteException {
            stopSelf();
        }

        @Override
        public void aidlSetActivityInBackground() throws RemoteException {
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
            setActivityBackground();
        }

        @Override
        public void aidlSetActivityInForeground() throws RemoteException {
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
            setActivityForeground();
        }

        @Override
        public void aidlUpdateNotificationMessage(String msg_text) throws RemoteException {
//            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
            showNotification(msg_text);
        }

    };

    private void initNotification() {
        NotificationManager nm=(NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT>=26) {
            NotificationChannel channel = new NotificationChannel(
                    "SMBExplorer",
                    "SMBExplorer",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.enableLights(false);
            channel.setSound(null,null);
//            channel.setLightColor(Color.GREEN);
            channel.enableVibration(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            nm.deleteNotificationChannel("SMBExplorer");
            nm.createNotificationChannel(channel);
        }

        mNotificationBuilder=new Notification.Builder(mContext);
        mNotificationBuilder.setWhen(System.currentTimeMillis())
                .setContentTitle(mContext.getString(R.string.msgs_main_notification_title))
                .setContentText(mContext.getString(R.string.msgs_main_notification_message))
//                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.smbexplorer))
                .setSmallIcon(R.drawable.ic_48_smbexplorer);
        Intent activity_intent = new Intent(mContext, ActivityMain.class);
        PendingIntent activity_pi=PendingIntent.getActivity(mContext, 0, activity_intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationBuilder.setContentIntent(activity_pi);
        if (Build.VERSION.SDK_INT>=26) {
            mNotificationBuilder.setChannelId("SMBExplorer");
        }

    }

    private void showNotification(String msg_text) {
        if (mNotificationBuilder!=null) {
            mNotificationBuilder
//                    .setWhen(System.currentTimeMillis())
                    .setContentText(msg_text);
            if (mNotification!=null) {
                mNotification=mNotificationBuilder.build();
                NotificationManager nm=(NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(R.string.app_name, mNotification);
            }
        }
    }

    private void setActivityForeground() {
        mGp.activityIsBackground=false;
        cancelHeartBeat();
        if (mPartialWakelock.isHeld()) mPartialWakelock.release();
        stopForeground(true);
        mNotification=null;
    };

    private Notification.Builder mNotificationBuilder=null;
    private Notification mNotification=null;
    private void setActivityBackground() {
        mGp.activityIsBackground=true;
        if (!mPartialWakelock.isHeld()) mPartialWakelock.acquire();;
        setHeartBeat();

        mNotificationBuilder.setWhen(System.currentTimeMillis())
                .setContentText(mContext.getString(R.string.msgs_main_notification_message));

        mNotification=mNotificationBuilder.build();
        NotificationManager nm=(NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(R.string.app_name, mNotification);

        startForeground(R.string.app_name, mNotification);
    }

    final private class SleepReceiver  extends BroadcastReceiver {
        @SuppressLint({ "Wakelock", "NewApi"})
        @Override
        final public void onReceive(Context c, Intent in) {
            String action = in.getAction();
            if(action.equals(Intent.ACTION_SCREEN_ON)) {
            } else if(action.equals(Intent.ACTION_SCREEN_OFF)) {
            } else if(action.equals(Intent.ACTION_USER_PRESENT)) {
            }
        }
    }

    private void notifyToWifiStatusChanged() {
        if (mGp.callbackStub != null) {
            try {
                mGp.callbackStub.cbWifiStatusChanged();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    final private class MediaReceiver  extends BroadcastReceiver {
        @SuppressLint({ "Wakelock", "NewApi"})
        @Override
        final public void onReceive(Context c, Intent in) {
            final String action = in.getAction();
            mUtil.addDebugMsg(1,"I","Media Action="+action+", Path="+in.getDataString());
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED) ||
                action.equals(Intent.ACTION_MEDIA_EJECT) ||
                action.equals(Intent.ACTION_MEDIA_REMOVED) ||
                    action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                Thread th=new Thread() {
                    @Override
                    public void run() {
                        mGp.safMgr.refreshSafList();
                        notifyToMediaStatusChanged();
                    }
                };
                th.start();
            }
        }
    }

    private void notifyToMediaStatusChanged() {
        if (mGp.callbackStub != null) {
            try {
                mGp.callbackStub.cbMediaStatusChanged();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    final private class WifiReceiver  extends BroadcastReceiver {
        @SuppressLint({ "Wakelock", "NewApi"})
        @Override
        final public void onReceive(Context c, Intent in) {
            String tssid =null;
            try {
                tssid=mWifiMgr.getConnectionInfo().getSSID();
            } catch(Exception e){
                mUtil.addLogMsg("W", "WIFI receiver, getSSID() failed. msg="+e.getMessage());
            }
            String wssid = "";
            String ss = "";
            try {
                ss=mWifiMgr.getConnectionInfo().getSupplicantState().toString();
            } catch(Exception e){
                mUtil.addLogMsg("W", "WIFI receiver, getSupplicantState() failed. msg="+e.getMessage());
            }
            if (tssid == null || tssid.equals("<unknown ssid>")) wssid = "";
            else wssid = tssid.replaceAll("\"", "");
            if (wssid.equals("0x")) wssid = "";

            boolean new_wifi_enabled = mWifiMgr.isWifiEnabled();
            if (!new_wifi_enabled && mGp.wifiIsActive) {
                mUtil.addDebugMsg(1, "I", "WIFI receiver, WIFI Off");
                mGp.wifiSsid = "";
                mGp.wifiIsActive = false;
                notifyToWifiStatusChanged();
            } else {
                notifyToWifiStatusChanged();
                if (ss.equals("COMPLETED") || ss.equals("ASSOCIATING") || ss.equals("ASSOCIATED")) {
                    if (mGp.wifiSsid.equals("") && !wssid.equals("")) {
                        mUtil.addDebugMsg(1, "I", "WIFI receiver, Connected WIFI Access point ssid=" + wssid);
                        mGp.wifiSsid = wssid;
                        mGp.wifiIsActive = true;
//                        notifyToWifiStatusChanged();
                    }
                } else if (ss.equals("INACTIVE") ||
                        ss.equals("DISCONNECTED") ||
                        ss.equals("UNINITIALIZED") ||
                        ss.equals("INTERFACE_DISABLED") ||
                        ss.equals("SCANNING")) {
                    if (mGp.wifiIsActive) {
                        if (!mGp.wifiSsid.equals("")) {
                            mUtil.addDebugMsg(1, "I", "WIFI receiver, Disconnected WIFI Access point ssid=" + mGp.wifiSsid);
                            mGp.wifiSsid = "";
                            mGp.wifiIsActive = true;
//                            notifyToWifiStatusChanged();
                        }
                    } else {
                        if (new_wifi_enabled) {
                            mUtil.addDebugMsg(1, "I", "WIFI receiver, WIFI On");
                            mGp.wifiSsid = "";
                            mGp.wifiIsActive = true;
//                            notifyToWifiStatusChanged();
                        }
                    }
                }
            }
        }
    };
}
