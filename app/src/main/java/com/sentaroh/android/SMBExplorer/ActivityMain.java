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

import android.Manifest;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpProgressMonitor;
import com.sentaroh.android.SMBExplorer.Log.LogManagementFragment;
import com.sentaroh.android.SMBExplorer.FileManager.MountPointHistoryItem;
import com.sentaroh.android.Utilities3.AppUncaughtExceptionHandler;
import com.sentaroh.android.Utilities3.CallBackListener;
import com.sentaroh.android.Utilities3.ContextMenu.CustomContextMenu;
import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.MiscUtil;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.SafManager3;
import com.sentaroh.android.Utilities3.SystemInfo;
import com.sentaroh.android.Utilities3.ThemeUtil;
import com.sentaroh.android.Utilities3.Widget.CustomViewPager;
import com.sentaroh.android.Utilities3.Widget.CustomViewPagerAdapter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import static com.sentaroh.android.SMBExplorer.Constants.SMBEXPLORER_PROFILE_NAME;
import static com.sentaroh.android.SMBExplorer.Constants.SMBEXPLORER_TAB_LOCAL;
import static com.sentaroh.android.SMBExplorer.Constants.SMBEXPLORER_TAB_POS_LOCAL;
import static com.sentaroh.android.SMBExplorer.Constants.SMBEXPLORER_TAB_REMOTE;
import static com.sentaroh.android.Utilities3.SafFile3.SAF_FILE_PRIMARY_UUID;

public class ActivityMain extends AppCompatActivity {
	private final static String DEBUG_TAG = "SMBExplorer";

	private GlobalParameter mGp=null;
    private CommonUtilities mUtil=null;
	private boolean mIsApplicationTerminate = false;
	private int restartStatus=0;
	private Context mContext=null;
	private CustomContextMenu ccMenu = null;
	private ActivityMain mActivity=null;
	private ActionBar mActionBar=null;

    private CustomViewPager mMainViewPager=null;
    private CustomViewPagerAdapter mMainViewPagerAdapter=null;

    private Handler mUiHandler=null;

    private FileManager mFileMgr=null;

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered.");
        out.putString("save","data");
    }

    @Override
    protected void onRestoreInstanceState(Bundle in) {
        super.onRestoreInstanceState(in);
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered.");
//        restartStatus=2;
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
//        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
//        StrictMode.setVmPolicy(builder.build());
        super.onCreate(savedInstanceState);

		mContext=ActivityMain.this;
		mActivity=ActivityMain.this;
        makeCacheDirectory();

        mGp=GlobalWorkArea.getGlobalParameters(mContext);

        mUtil=mGp.mUtil=new CommonUtilities(mActivity, "Main", mGp);
		setContentView(R.layout.main);
        mUiHandler=new Handler();
		mActionBar = getSupportActionBar();
		mActionBar.setHomeButtonEnabled(false);
//		mGp.localBase=mGp.internalRootDirectory;

        mGp.smbConfigList = RemoteServerUtil.createRemoteServerConfigList(mContext, mGp,false, null);

        if (ccMenu ==null) ccMenu = new CustomContextMenu(getResources(),getSupportFragmentManager());
		mGp.commonDlg=new CommonDialog(mActivity, getSupportFragmentManager());
        mUtil.addDebugMsg(1, "I", "onCreate entered");

        MyUncaughtExceptionHandler myUncaughtExceptionHandler = new MyUncaughtExceptionHandler();
		myUncaughtExceptionHandler.init(mActivity, myUncaughtExceptionHandler);

//		String npe=null;
//		npe.length();

        mFileMgr=new FileManager(mActivity, mGp, mUtil, ccMenu);

        createTabAndView() ;
        mGp.mLocalView.setVisibility(LinearLayout.GONE);
        mGp.mRemoteView.setVisibility(LinearLayout.GONE);

		mIsApplicationTerminate = false;
		mContext.getExternalFilesDirs(null);
//        restartStatus=0;

//        Intent intmsg = new Intent(mContext, MainService.class);
//        startService(intmsg);

        ArrayList<String> sil= SystemInfo.listSystemInfo(mContext, mGp.safMgr);
        mUtil.addDebugMsg(1,"I","System Information begin");
        for(String item:sil) mUtil.addDebugMsg(1,"I","   "+item);
        mUtil.addDebugMsg(1,"I","System Information end");

        cleanupCacheFile();

//        Thread th=new Thread() {
//            @Override
//            public void run() {
//                try {
//                    long b_time=System.currentTimeMillis();
//                    JSch mJsch;
//                    Session mSession;
//                    ChannelSftp mChannel;
//                    Log.v("SMBExplorer", "started");
//                    mJsch = new JSch();
//                    mSession = mJsch.getSession("android", "192.168.200.40", 22);
//                    mSession.setConfig(new Properties());
//                    mSession.setTimeout(30*1000);
//                    mSession.setConfig("StrictHostKeyChecking", "no");
//                    mSession.setPassword("fh146746");
//
////                    mSession.setConfig("compression_level", "0");
//
//                    mSession.connect();
//                    mChannel = (ChannelSftp) mSession.openChannel("sftp");
//                    mChannel.connect();
//                    mChannel.cd("/data/share");
//
//                    File lf=new File("/sdcard/test.doc");
//                    lf.delete();
//                    FileOutputStream fos=new FileOutputStream(lf);
//                    BufferedOutputStream bos=new BufferedOutputStream(fos, 1024*1024*2);
//
//                    SftpProgressMonitor mon= new SftpProgressMonitor() {
//                        @Override
//                        public void init(int i, String s, String s1, long l) {
//                            Log.v("SMBExplorer", "init s="+s+", sl="+s1+", l="+l);
//                        }
//
//                        @Override
//                        public boolean count(long l) {
//                            Log.v("SMBExplorer", "count l="+l);
//                            return true;
//                        }
//
//                        @Override
//                        public void end() {
//                            Log.v("SMBExplorer", "end");
//                        }
//                    };
//                    mChannel.get("/data/share/aa.doc", bos, mon);
//                    bos.close();
//
////                    InputStream is=mChannel.get("/data/share/aa.doc", mon);
////
////                    BufferedInputStream bin = new BufferedInputStream(is, 1024*1024*4);
////
////                    byte[] buff=new byte[1024*1024*2];
////                    int rc=bin.read(buff,0,buff.length);
////                    while(rc>0) {
////                        rc=bin.read(buff,0,buff.length);
////                        Log.v("SMBExplorer", "rc="+rc);
////                    }
//                    Log.v("SMBExplorer", "ended elapsed="+(System.currentTimeMillis()-b_time));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        };
//        th.start();

    }

    private void listMediaStoreItem(Context c) {
        ContentResolver resolver = c.getContentResolver();
        Cursor cursor =null;
        try {
            cursor = resolver.query(MediaStore.Files.getContentUri("external") ,null , null, null, "_data");
            while( cursor.moveToNext() ){
                String file_path=cursor.getString(cursor.getColumnIndex( MediaStore.Images.Media.DATA));
                String display_name=cursor.getString(cursor.getColumnIndex( MediaStore.Images.Media.DISPLAY_NAME));
                long date_added=cursor.getLong(cursor.getColumnIndex( MediaStore.Images.Media.DATE_ADDED));
                long date_modified=cursor.getLong(cursor.getColumnIndex( MediaStore.Images.Media.DATE_MODIFIED));
                long media_size=cursor.getLong(cursor.getColumnIndex( MediaStore.Images.Media.SIZE));
                String media_title=cursor.getString(cursor.getColumnIndex( MediaStore.Images.Media.TITLE));
                String bu_name=cursor.getString(cursor.getColumnIndex( MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
                if (!file_path.startsWith("/storage/emulated")) mUtil.addDebugMsg(1, "I", "MeidaStore fp="+file_path);
            }

        } finally {
            if (cursor!=null) cursor.close();
        }
    }


    private class MyUncaughtExceptionHandler extends AppUncaughtExceptionHandler {
        @Override
        public void appUniqueProcess(Throwable ex, String strace) {
            mUtil.addLogMsg("E", strace);
            mUtil.flushLog();
        }
    }

    @Override
	protected void onStart() {
		super.onStart();
		mUtil.addDebugMsg(1, "I", "onStart entered");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		mUtil.addDebugMsg(1, "I", "onRestart entered");
	}

    @Override
	protected void onResume() {
		super.onResume();
		mUtil.addDebugMsg(1, "I","onResume entered"+ " restartStatus="+restartStatus);
//        byte[] oo=new byte[Integer.MAX_VALUE];
        if (restartStatus==1) {
            removeTaskData();
            setActivityInForeground();
            if (mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
                if (!isUiEnabled()) {
                    mGp.localFileListView.setVisibility(ListView.INVISIBLE);
                } else {
                    if (isLegacyExternalStoragePermissionGranted()) {
                        mFileMgr.refreshFileListView();
                    }
                }
            }
        } else {
            NotifyEvent ntfy=new NotifyEvent(mContext);
            ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                @Override
                public void positiveResponse(Context context, Object[] objects) {
                    setCallbackListener();
                    final LinearLayout main_view=(LinearLayout)findViewById(R.id.main_screen_view);
                    if (isLegacyExternalStoragePermissionGranted()) {
                        if (!isTaskDataExists()) {
                            mFileMgr.setMainListener();
                            refreshOptionMenu();
                            main_view.setVisibility(LinearLayout.VISIBLE);
                        } else {
                            if (isTaskDataExists()) {
                                mUiHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        restoreTaskData();
                                        removeTaskData();
                                        mUiHandler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                main_view.setVisibility(LinearLayout.VISIBLE);
                                            }
                                        },100);
                                    }
                                });
                            } else {
                                mFileMgr.setMainListener();
                            }
                            refreshOptionMenu();
                        }
                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mGp.mLocalView.setVisibility(LinearLayout.VISIBLE);
                                mGp.mRemoteView.setVisibility(LinearLayout.VISIBLE);
                            }
                        });
                        restartStatus=1;
                    } else {
                        checkRequiredPermissions();
                    }
                }
                @Override
                public void negativeResponse(Context context, Object[] objects) {
                }
            });
            openService(ntfy);
        }
	}

    private void openService(NotifyEvent p_ntfy) {
        mUtil.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName()+" entered, svcClient="+mGp.svcClient);
        if (mGp.svcClient!=null) {
            p_ntfy.notifyToListener(true, null);
            return;
        }
        mGp.svcConnection = new ServiceConnection(){
            public void onServiceConnected(ComponentName arg0, IBinder service) {
                mUtil.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName()+" entered");
                mGp.svcClient =ISvcClient.Stub.asInterface(service);
                p_ntfy.notifyToListener(true, null);
            }
            public void onServiceDisconnected(ComponentName name) {
                mGp.svcConnection = null;
                mUtil.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName()+" entered");
            }
        };

        Intent intmsg = new Intent(mContext, MainService.class);
        intmsg.setAction("Bind");
        bindService(intmsg, mGp.svcConnection, BIND_AUTO_CREATE);
    }

    final private void setCallbackListener() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        try {
            mGp.svcClient.setCallBack(mSvcCallbackStub);
        } catch (RemoteException e) {
            e.printStackTrace();
            mUtil.addDebugMsg(1, "E", "setCallbackListener error :" + e.toString());
        }
    }

    final private void unsetCallbackListener() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        if (mGp.svcClient != null) {
            try {
                mGp.svcClient.removeCallBack(mSvcCallbackStub);
            } catch (RemoteException e) {
                e.printStackTrace();
                mUtil.addDebugMsg(1, "E", "unsetCallbackListener error :" + e.toString());
            }
        }
    }

    private ISvcCallback mSvcCallbackStub = new ISvcCallback.Stub() {
        @Override
        public void cbWifiStatusChanged() throws RemoteException {
            mUtil.addDebugMsg(2, "I","cbWifiStatusChanged entered");
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    FileManager.setRemoteTabEnabled(mGp);
                }
            });
        }
        @Override
        public void cbMediaStatusChanged() throws RemoteException {
            mUtil.addDebugMsg(1, "I","cbMediaStatusChanged entered");
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mFileMgr.updateLocalDirSpinner();
                }
            });
        }
    };

    @Override
	protected void onPause() {
		super.onPause();
		mUtil.addDebugMsg(1, "I","onPause entered, enableKill="+mIsApplicationTerminate+
				", getChangingConfigurations="+String.format("0x%08x", getChangingConfigurations()));
        saveTaskData();
	}

	@Override
	protected void onStop() {
		super.onStop();
		mUtil.addDebugMsg(1, "I","onStop entered, enableKill="+mIsApplicationTerminate);
		if (!isUiEnabled()) setActivityInBackground();
//        saveTaskData();
    }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mUtil.addDebugMsg(1, "I","onDestroy entered, enableKill="+mIsApplicationTerminate+
				", getChangingConfigurations="+String.format("0x%08x", getChangingConfigurations()));
        if (!isUiEnabled()) stopService();
        closeService();
        unsetCallbackListener();
        if (mIsApplicationTerminate) removeTaskData();
        cleanupCacheFile();
	}

    private void closeService() {
        mUtil.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName()+" entered, conn="+mGp.svcConnection);
        if (mGp.svcConnection !=null) {
            unbindService(mGp.svcConnection);
        }
    };

    private void setActivityInBackground() {
        try {
            mGp.svcClient.aidlSetActivityInBackground();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void setActivityInForeground() {
        try {
            mGp.svcClient.aidlSetActivityInForeground();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void stopService() {
        try {
            mGp.svcClient.aidlStopService();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
	public void onConfigurationChanged(final Configuration newConfig) {
	    // Ignore orientation change to keep activity from restarting
	    super.onConfigurationChanged(newConfig);
	    mUtil.addDebugMsg(1,"I","onConfigurationChanged Entered, "+"orientation="+newConfig.orientation);

        mFileMgr.setSpinnerSelectionEnabled(false);

        ViewSaveArea vsa=new ViewSaveArea();
	    saveViewStatus(vsa);
		setContentView(R.layout.main);
		createTabAndView() ;

		restoreViewStatus(vsa);

        mFileMgr.setPasteItemList();
        mGp.tabHost.setCurrentTabByTag(mGp.currentTabName);
        mFileMgr.setLocalDirBtnListener();
        mFileMgr.setRemoteDirBtnListener();
        mGp.localFileListDirSpinner.setSelection(vsa.local_spinner_pos, false);
        mGp.remoteFileListDirSpinner.setSelection(vsa.remote_spinner_pos, false);
        mFileMgr.setLocalFilelistItemClickListener();
        mFileMgr.setLocalFilelistLongClickListener();
        mFileMgr.setRemoteFilelistItemClickListener();
        mFileMgr.setRemoteFilelistLongClickListener();
        mFileMgr.setLocalContextButtonListener();
        mFileMgr.setEmptyFolderView();
		
		refreshOptionMenu();
        final LinearLayout main_view=(LinearLayout)findViewById(R.id.main_screen_view);
        main_view.setVisibility(LinearLayout.VISIBLE);

		Handler hndl=new Handler();
		hndl.postDelayed(new Runnable(){
		    @Override
            public void run() {
                mFileMgr.setSpinnerSelectionEnabled(true);
            }
        },100);
    }
	
	private void saveViewStatus(ViewSaveArea vsa) {
		if (mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
			if (mGp.localProgressView.getVisibility()!=LinearLayout.GONE) {
				if (mGp.localProgressView !=null) vsa.progressVisible=mGp.localProgressView.getVisibility();
				if (mGp.progressCancelBtn!=null) vsa.progressCancelBtnText=mGp.progressCancelBtn.getText().toString();
				if (mGp.progressMsgView!=null) vsa.progressMsgText=mGp.progressMsgView.getText().toString();
			}
			if (mGp.localDialogView.getVisibility()!=LinearLayout.GONE) {
				if (mGp.localDialogView !=null) vsa.dialogVisible=mGp.localDialogView.getVisibility();
				if (mGp.dialogMsgView!=null) vsa.dialogMsgText=mGp.dialogMsgView.getText().toString();
			}
		} else if (mGp.currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
			if (mGp.remoteProgressView.getVisibility()!=LinearLayout.GONE) {
				if (mGp.remoteProgressView !=null) vsa.progressVisible=mGp.remoteProgressView.getVisibility();
				if (mGp.progressCancelBtn!=null) vsa.progressCancelBtnText=mGp.progressCancelBtn.getText().toString();
				if (mGp.progressMsgView!=null) vsa.progressMsgText=mGp.progressMsgView.getText().toString();
			}
			if (mGp.remoteDialogView.getVisibility()!=LinearLayout.GONE) {
				if (mGp.remoteDialogView !=null) vsa.dialogVisible=mGp.remoteDialogView.getVisibility();
				if (mGp.dialogMsgView!=null) vsa.dialogMsgText=mGp.dialogMsgView.getText().toString();
			}
		}
		vsa.lclPos=mGp.localFileListView.getFirstVisiblePosition();
		if (mGp.localFileListView.getChildAt(0)!=null) vsa.lclPosTop=mGp.localFileListView.getChildAt(0).getTop();
		vsa.remPos=mGp.remoteFileListView.getFirstVisiblePosition();
		if (mGp.remoteFileListView.getChildAt(0)!=null) vsa.remPosTop=mGp.remoteFileListView.getChildAt(0).getTop();

		if (mGp.localFileListAdapter!=null) vsa.local_file_list=mGp.localFileListAdapter.getDataList();
        if (mGp.remoteFileListAdapter!=null) vsa.remote_file_list=mGp.remoteFileListAdapter.getDataList();

        vsa.local_spinner_pos=mGp.localFileListDirSpinner.getSelectedItemPosition();
        vsa.remote_spinner_pos=mGp.remoteFileListDirSpinner.getSelectedItemPosition();

        vsa.local_file_path=mGp.localFileListPath.getText().toString();
        vsa.remote_file_path=mGp.remoteFileListPath.getText().toString();
	}
	
	private void restoreViewStatus(ViewSaveArea vsa) {
		if (mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
			if (vsa.progressVisible!=LinearLayout.GONE) {
                mFileMgr.showLocalProgressView();
			}
			mGp.progressMsgView=mGp.localProgressMsg;
			mGp.progressCancelBtn=mGp.localProgressCancel;
			mGp.progressCancelBtn.setEnabled(true);
			mGp.progressCancelBtn.setOnClickListener(mGp.progressOnClickListener);
			mGp.progressCancelBtn.setText(vsa.progressCancelBtnText);
			mGp.progressMsgView.setText(vsa.progressMsgText);
			if (vsa.dialogVisible!=LinearLayout.GONE) {
                mFileMgr.showLocalDialogView();
                mFileMgr.showDialogMsg(mGp.dialogMsgCat,vsa.dialogMsgText,"");
			}
		} else if (mGp.currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
			if (vsa.progressVisible!=LinearLayout.GONE) {
                mFileMgr.showRemoteProgressView();
				mGp.progressMsgView=mGp.remoteProgressMsg;
				mGp.progressCancelBtn=mGp.remoteProgressCancel;
				mGp.progressCancelBtn.setEnabled(true);
				mGp.progressCancelBtn.setOnClickListener(mGp.progressOnClickListener);
				mGp.progressCancelBtn.setText(vsa.progressCancelBtnText);
				mGp.progressMsgView.setText(vsa.progressMsgText);
			}
			if (vsa.dialogVisible!=LinearLayout.GONE) {
                mFileMgr.showRemoteDialogView();
                mFileMgr.showDialogMsg(mGp.dialogMsgCat,vsa.dialogMsgText,"");
			}
		}
		if (mGp.progressCancelBtn!=null) mGp.progressCancelBtn.setOnClickListener(mGp.progressOnClickListener);

        mGp.localFileListAdapter.setDataList(vsa.local_file_list);
        mGp.localFileListView.setAdapter(mGp.localFileListAdapter);
        mGp.localFileListView.setSelectionFromTop(vsa.lclPos, vsa.lclPosTop);

        mGp.remoteFileListAdapter.setDataList(vsa.remote_file_list);
        mGp.remoteFileListView.setAdapter(mGp.remoteFileListAdapter);
        mGp.remoteFileListView.setSelectionFromTop(vsa.remPos, vsa.remPosTop);

        mGp.localFileListPath.setText(vsa.local_file_path);
        mGp.remoteFileListPath.setText(vsa.remote_file_path);

        mFileMgr.setLocalContextButtonListener();
        mFileMgr.setLocalContextButtonStatus();

        mFileMgr.setRemoteContextButtonListener();
        mFileMgr.setRemoteContextButtonStatus();
    }
	
	private void refreshOptionMenu() {
		if (Build.VERSION.SDK_INT>=11)
			mActivity.invalidateOptionsMenu();
	}

	private void createTabAndView() {
		mGp.themeColorList = ThemeUtil.getThemeColorList(mActivity);
//        getWindow().setNavigationBarColor(Color.RED);

        mGp.tabHost =(TabHost)findViewById(android.R.id.tabhost);
        mGp.tabHost.setup();
        mGp.tabWidget = (TabWidget) findViewById(android.R.id.tabs);
        mGp.tabWidget.setStripEnabled(false);
        mGp.tabWidget.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);

        LinearLayout main_view=(LinearLayout)findViewById(R.id.main_screen_view);
        main_view.setVisibility(LinearLayout.INVISIBLE);
//        main_view.setBackgroundColor(Color.BLACK);//mGp.themeColorList.window_background_color_content);

        CustomTabContentView tabLocal = new CustomTabContentView(mActivity, SMBEXPLORER_TAB_LOCAL);
        mGp.tabHost.addTab(mGp.tabHost.newTabSpec(SMBEXPLORER_TAB_LOCAL).setIndicator(tabLocal).setContent(android.R.id.tabcontent));

        CustomTabContentView tabRemote = new CustomTabContentView(mActivity, SMBEXPLORER_TAB_REMOTE);
        mGp.tabHost.addTab(mGp.tabHost.newTabSpec(SMBEXPLORER_TAB_REMOTE).setIndicator(tabRemote).setContent(android.R.id.tabcontent));

        mGp.tabHost.setOnTabChangedListener(new OnTabChange());

        LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mGp.mLocalView=(LinearLayout)vi.inflate(R.layout.main_local_tab, null);
        mGp.mRemoteView=(LinearLayout)vi.inflate(R.layout.main_remote_tab, null);

        mFileMgr.createView();

        mMainViewPager=(CustomViewPager)findViewById(R.id.main_screen_pager);
        mMainViewPagerAdapter=new CustomViewPagerAdapter(this, new View[]{mGp.mLocalView, mGp.mRemoteView});

//        mMainViewPager.setBackgroundColor(mGp.themeColorList.window_background_color_content);
        mMainViewPager.setAdapter(mMainViewPagerAdapter);
        mMainViewPager.setOnPageChangeListener(new MainPageChangeListener());
        if (restartStatus==0) {
            mGp.tabHost.setCurrentTabByTag(SMBEXPLORER_TAB_LOCAL);
            mMainViewPager.setCurrentItem(SMBEXPLORER_TAB_POS_LOCAL);
        }

        mGp.mainPasteListClearBtn=(Button)findViewById(R.id.explorer_filelist_paste_clear);
        mGp.mainPasteListClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFileMgr.clearPasteItemList();
            }
        });
	}

	class OnTabChange implements TabHost.OnTabChangeListener {
		@Override
		public void onTabChanged(String tabId){
			mUtil.addDebugMsg(1, "I","onTabchanged entered. tab="+tabId);
            mGp.currentTabName=tabId;
            mMainViewPager.setCurrentItem(mGp.tabHost.getCurrentTab());
//			if (mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) refreshFileListView();
            mFileMgr.setFileListPathName(mGp.localFileListPath,"",mGp.localDirectory);
            mFileMgr.setFileListPathName(mGp.remoteFileListPath,mGp.remoteMountpoint,mGp.remoteDirectory);
            mFileMgr.setPasteButtonEnabled();
		};
	}

    private class MainPageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageSelected(int position) {
            mUtil.addDebugMsg(1,"I","onPageSelected entered, pos="+position);
            mGp.tabWidget.setCurrentTab(position);
            mGp.tabHost.setCurrentTab(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
//	    	mUtil.addDebugMsg(1,"I","onPageScrollStateChanged entered, state="+state);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//	    	mUtil.addDebugMsg(1, "I","onPageScrolled entered, pos="+position);
        }
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
//		mUtil.addDebugMsg(1, "I","onCreateOptionsMenu entered");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_top, menu);
		return true;
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//    	mUtil.addDebugMsg(1, "I","onPrepareOptionsMenu entered");
    	super.onPrepareOptionsMenu(menu);
    	if (CommonUtilities.isAndroidVersion30orUp()) {
    	    menu.findItem(R.id.menu_top_show_all_file_access_permission).setVisible(true);
            menu.findItem(R.id.menu_top_request_storage_permission).setVisible(false);
        } else {
    	    menu.findItem(R.id.menu_top_show_all_file_access_permission).setVisible(false);
            menu.findItem(R.id.menu_top_request_storage_permission).setVisible(true);
        }
    	if (isUiEnabled()) {
    		menu.findItem(R.id.menu_top_export).setEnabled(true);
    		menu.findItem(R.id.menu_top_import).setEnabled(true);
    		menu.findItem(R.id.menu_top_settings).setEnabled(true);
            menu.findItem(R.id.menu_top_edit_smb_server).setEnabled(true);
            menu.findItem(R.id.menu_top_refresh).setEnabled(true);
    	} else {
    		menu.findItem(R.id.menu_top_export).setEnabled(false);
    		menu.findItem(R.id.menu_top_import).setEnabled(false);
    		menu.findItem(R.id.menu_top_settings).setEnabled(false);
            menu.findItem(R.id.menu_top_edit_smb_server).setEnabled(false);
            menu.findItem(R.id.menu_top_refresh).setEnabled(false);
    	}
        return true;
    }

    private boolean isLegacyExternalStoragePermissionGranted() {
        if (CommonUtilities.isAndroidVersion30orUp()) {
            boolean granted= isAllFileAccessPermissionGranted();
            return granted;
        } else {
            if (Build.VERSION.SDK_INT<23) return true;
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) return true;
            else return false;
        }
    }

//    private final int REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1;
    private boolean mLegacyStoragePermissionInprocess=false;
    private void checkRequiredPermissions() {
        if (mLegacyStoragePermissionInprocess) return;
        if (CommonUtilities.isAndroidVersion30orUp()) {
            requestAllFileAccessPermission();
            return;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            mUtil.addDebugMsg(1, "I", "Prermission WriteExternalStorage=" + checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) +
                    ", WakeLock=" + checkSelfPermission(Manifest.permission.WAKE_LOCK)
            );
            if (!isLegacyExternalStoragePermissionGranted()) {
                mLegacyStoragePermissionInprocess=true;
                mGp.commonDlg.showCommonDialog(mContext, false, "W",
                    mContext.getString(R.string.msgs_main_permission_external_storage_title),
                    mContext.getString(R.string.msgs_main_permission_external_storage_request_msg), new CallBackListener(){
                    @Override
                    public void onCallBack(Context c, boolean positive, Object[] o) {
                        if (positive) {
                            ActivityResultLauncher<String> request_permission =mActivity.registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                                if (isGranted) {
                                    mLegacyStoragePermissionInprocess=false;
                                    mFileMgr.setMainListener();
                                } else {
                                    mGp.commonDlg.showCommonDialog(mActivity, false, "W",
                                        mContext.getString(R.string.msgs_main_permission_external_storage_title),
                                        mContext.getString(R.string.msgs_main_permission_external_storage_denied_msg),
                                        new CallBackListener(){
                                        @Override
                                        public void onCallBack(Context c, boolean positive, Object[] objects) {
                                            if (positive) finish();
                                        }
                                    });
                                }
                            });
                            request_permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        } else {
                            mGp.commonDlg.showCommonDialog(mContext, false, "W",
                                mContext.getString(R.string.msgs_main_permission_external_storage_title),
                                mContext.getString(R.string.msgs_main_permission_external_storage_denied_msg), new CallBackListener(){
                                @Override
                                public void onCallBack(Context c, boolean positive, Object[] o) {
                                    if (positive) finish();
                                }
                            });
                        }
                    }
                });
            }
        }
    }

    private void requestAllFileAccessPermission() {
        mLegacyStoragePermissionInprocess=true;
        NotifyEvent ntfy_all_file_access=new NotifyEvent(mContext);
        ntfy_all_file_access.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
//                ActivityResultLauncher<String> request_permission =mActivity.registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
//                    if (isGranted) {
//                        mLegacyStoragePermissionInprocess=false;
//                        mFileMgr.setMainListener();
//                    } else {
//                        mGp.commonDlg.showCommonDialog(mActivity, false, "W",
//                                mContext.getString(R.string.msgs_main_permission_all_file_access_title),
//                                mContext.getString(R.string.msgs_main_permission_all_file_access_denied_msg),
//                            new CallBackListener(){
//                            @Override
//                            public void onCallBack(Context c, boolean positive, Object[] objects) {
//                                if (positive) finish();
//                            }
//                        });
//                    }
//                });
//                request_permission.launch(Manifest.permission.MANAGE_EXTERNAL_STORAGE);

                ActivityResultLauncher<Intent> activity_laucher =
                        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (isAllFileAccessPermissionGranted()) {
                            mLegacyStoragePermissionInprocess=false;
                            mFileMgr.setMainListener();
                        } else {
                            mGp.commonDlg.showCommonDialog(mActivity, false, "W",
                                    mContext.getString(R.string.msgs_main_permission_all_file_access_title),
                                    mContext.getString(R.string.msgs_main_permission_all_file_access_denied_msg),
                                    new CallBackListener(){
                                        @Override
                                        public void onCallBack(Context c, boolean positive, Object[] objects) {
                                            if (positive) finish();
                                        }
                                    });
                        }
                    }
                });
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activity_laucher.launch(intent);

            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {
                NotifyEvent ntfy_denied=new NotifyEvent(mContext);
                ntfy_denied.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        finish();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                mGp.commonDlg.showCommonDialog(false, "W",
                        mContext.getString(R.string.msgs_main_permission_all_file_access_title),
                        mContext.getString(R.string.msgs_main_permission_all_file_access_denied_msg), ntfy_denied);
            }
        });
        showAllFileAccessGrantedMessage(mContext.getString(R.string.msgs_main_permission_all_file_access_title),
                mContext.getString(R.string.msgs_main_permission_all_file_access_request_msg), ntfy_all_file_access);

    }

    private void showAllFileAccessGrantedMessage(final  String title_text, final String msg_text, final NotifyEvent p_ntfy) {
        final Dialog dialog=new Dialog(mActivity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.dialog_with_image_view_dlg);

        TextView dlg_title=(TextView)dialog.findViewById(R.id.dialog_with_image_view_dlg_title);
        TextView dlg_msg=(TextView)dialog.findViewById(R.id.dialog_with_image_view_dlg_msg);
        ImageView dlg_image=(ImageView)dialog.findViewById(R.id.dialog_with_image_view_dlg_image);

        Button dlg_ok=(Button)dialog.findViewById(R.id.dialog_with_image_view_dlg_btn_ok);
        Button dlg_cancel=(Button)dialog.findViewById(R.id.dialog_with_image_view_dlg_btn_cancel);

        dlg_title.setText(title_text);
        dlg_msg.setText(msg_text);
        dlg_image.setImageResource(R.drawable.all_file_access_desc_en);

        dlg_ok.setText(mContext.getString(R.string.msgs_main_permission_all_file_access_button_text));

        dlg_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                p_ntfy.notifyToListener(true, null);
                dialog.dismiss();
            }
        });
        dlg_cancel.setVisibility(Button.GONE);
        dlg_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                p_ntfy.notifyToListener(false, null);
                dialog.dismiss();;
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                dlg_cancel.performClick();
            }
        });

        dialog.show();
    }

    private boolean isAllFileAccessPermissionGranted() {
        return Environment.isExternalStorageManager();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mUtil.addDebugMsg(1, "I","onActivityResult entered, requestCode="+requestCode+", resultCode="+resultCode+", data="+data);
//        try {
//            mContext.getContentResolver().takePersistableUriPermission(
//                    data.getData(),
//                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
//            List<UriPermission> permissions = mContext.getContentResolver().getPersistedUriPermissions();
//            for(UriPermission item:permissions) mUtil.addDebugMsg(1, "I", "uri="+item.toString());
//
//        } catch(Exception e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions,grantResults);
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mUtil.addDebugMsg(1, "I","onOptionsItemSelected entered");
		switch (item.getItemId()) {
			case R.id.menu_top_export:
                RemoteServerUtil.exportRemoteServerConfigListDlg(this, mGp, mGp.internalRootDirectory, SMBEXPLORER_PROFILE_NAME);
				return true;
			case R.id.menu_top_import:
                RemoteServerUtil.importRemoteServerConfigDlg(this, mGp, mGp.internalRootDirectory, SMBEXPLORER_PROFILE_NAME);
				return true;
            case R.id.menu_top_request_storage_permission:
                requestStoragePermissions();
                return true;
            case R.id.menu_top_show_all_file_access_permission:
                try {
                    Intent in=new Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION");//Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(in);
                } catch(Exception e) {
                    e.printStackTrace();
                    mGp.commonDlg.showCommonDialog(false, "E", "All File Access", "Error occured when invoke All File Access permission, error="+e.getMessage(), null);
                }
                return true;
			case R.id.menu_top_settings:
				invokeSettingsActivity();
				return true;
			case R.id.menu_top_quit:
				confirmTerminateApplication();
				return true;
            case R.id.menu_top_edit_smb_server:
                RemoteServerListEditor sm=new RemoteServerListEditor(mActivity, mGp);
                return true;
            case R.id.menu_top_refresh:
//                sendMagicPacket("08:bd:43:f6:48:2a", "255.255.255.255");
                mFileMgr.refreshFileListView();
                return true;
            case R.id.menu_top_log_management:
                invokeLogManagement();
                return true;

		}
		return false;
	}

    private void invokeLogManagement() {
        mUtil.flushLog();
        LogManagementFragment lfm= LogManagementFragment.newInstance(false, getString(R.string.msgs_log_management_title));
        lfm.showDialog(mContext, getSupportFragmentManager(), lfm, null);
    };

    private void sendMagicPacket(final String target_mac, final String if_network) {
//                sendMagicPacket("08:bd:43:f6:48:2a", "192.168.200.128");
        Thread th=new Thread(){
            @Override
            public void run() {
                // Total 102byte = 6byte 0xffffffffffff + (6 byte target dev mac address)を16回繰り返す
                byte[] magicPacket=new byte[102];
                try {
                    int j=if_network.lastIndexOf(".");
                    String if_ba=if_network.substring(0,if_network.lastIndexOf("."))+".255";
                    InetAddress broadcastIpAddress = InetAddress.getByName(if_ba);//.getByAddress(new byte[]{-1,-1,-1,-1});

                    byte[] targetMacAddress=new byte[6];
                    String[] m_array=target_mac.split(":");
                    for(int i=0;i<6;i++) targetMacAddress[i]=Integer.decode("0x"+m_array[i]).byteValue();

                    Arrays.fill(magicPacket, 0, 6, (byte)0xff);
                    for (int i=0;i<16;i++) System.arraycopy(targetMacAddress,0, magicPacket,(i*6)+6, 6);

                    DatagramPacket packet = new DatagramPacket(magicPacket, magicPacket.length, broadcastIpAddress, 9);
                    DatagramSocket socket = new DatagramSocket();
                    socket.send(packet);
                    socket.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (!mFileMgr.processBackKey()) switchToHome();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
		}
	}

	private void confirmTerminateApplication() {
		NotifyEvent ne=new NotifyEvent(mContext);
		ne.setListener(new NotifyEvent.NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				terminateApplication();
			}
			@Override
			public void negativeResponse(Context c,Object[] o) {}
		});
		ne.notifyToListener(true, null);
//		mGp.commonDlg.showCommonDialog(true,"W",getString(R.string.msgs_terminate_confirm),"",ne);
		return;
	}

	private void switchToHome() {
		Intent in=new Intent();
		in.setAction(Intent.ACTION_MAIN);
		in.addCategory(Intent.CATEGORY_HOME);
		in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(in);
	}
	
	private void terminateApplication() {
        mIsApplicationTerminate = true; // exit cleanly
//		moveTaskToBack(true);
        finish();
    }

	private void applySettingParms() {
		mGp.loadSettingsParm(mContext);
		refreshOptionMenu();
	}
	
	private boolean mUiEnabled=true;

	public boolean isUiEnabled() {
		return mUiEnabled;
	}

	public void setUiEnabled(boolean enabled) {
		if ((enabled && mUiEnabled) || (!enabled && !mUiEnabled)) return;
		mUiEnabled=enabled;

		mGp.tabWidget.setEnabled(enabled);
        mGp.localFileListDirSpinner.setEnabled(enabled);
        mGp.remoteFileListDirSpinner.setEnabled(enabled);
        mGp.remoteFileListView.setEnabled(enabled);
        mGp.localFileListView.setEnabled(enabled);
		if (enabled) {
            mGp.remoteFileListView.setVisibility(ListView.VISIBLE);
            mGp.localFileListView.setVisibility(ListView.VISIBLE);
            mFileMgr.setPasteButtonEnabled();
		} else {
            mGp.remoteFileListView.setVisibility(ListView.INVISIBLE);
            mGp.localFileListView.setVisibility(ListView.INVISIBLE);
		}
        mGp.localFileListUpBtn.setClickable(enabled);
        mGp.localFileListTopBtn.setClickable(enabled);
        mGp.remoteFileListUpBtn.setClickable(enabled);
        mGp.remoteFileListTopBtn.setClickable(enabled);

		refreshOptionMenu();
	}

	private void invokeSettingsActivity() {
        ActivityResultLauncher<Intent> activity_laucher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        mUtil.addDebugMsg(1, "I", "Return from Setting activity.");
                        applySettingParms();
                    }
                });
        Intent intent = new Intent(mContext, ActivitySetting.class);
        activity_laucher.launch(intent);
	}

//	private final int REQUEST_CODE_STORAGE_ACCESS =40;
//    private final int REQUEST_CODE_ALL_FILE_ACCESS =80;

	public void requestStoragePermissions() {
        StoragePermission ss=new StoragePermission(mActivity, mGp);
        ss.showDialog();
    }

	public void requestStoragePermissionsByUuid(String uuid) {
        Intent intent = null;
        StorageManager sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        ArrayList<SafManager3.StorageVolumeInfo>vol_list=SafManager3.getStorageVolumeInfo(mContext);
        for(SafManager3.StorageVolumeInfo svi:vol_list) {
            if (svi.uuid.equals(uuid)) {
                if (Build.VERSION.SDK_INT>=24) {
                    if (Build.VERSION.SDK_INT>=29) {
                        if (!svi.uuid.equals(SAF_FILE_PRIMARY_UUID)) {
                            intent=svi.volume.createOpenDocumentTreeIntent();
                            break;
                        }
                    } else {
                        if (!svi.uuid.equals(SAF_FILE_PRIMARY_UUID)) {
                            intent=svi.volume.createAccessIntent(null);
                            break;
                        }
                    }
                } else {
                    if (!svi.uuid.equals(SAF_FILE_PRIMARY_UUID)) {
                        intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        break;
                    }
                }
            }
        }
        if (intent!=null) {
            ActivityResultLauncher<Intent> activity_laucher =
                    registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Intent data=result.getData();
                    mUtil.addDebugMsg(1,"I","Storage picker action="+data.getAction()+", path="+data.getData().getPath());
                    if (mGp.safMgr.isRootTreeUri(data.getData())) {
                        mGp.safMgr.addUuid(data.getData());
                        mGp.safMgr.refreshSafList();
                        mFileMgr.updateLocalDirSpinner();
                    } else {
                        NotifyEvent ntfy=new NotifyEvent(mContext);
                        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                            @Override
                            public void positiveResponse(Context context, Object[] objects) {
                                requestStoragePermissions();
                            }
                            @Override
                            public void negativeResponse(Context context, Object[] objects) {}
                        });
                        mGp.commonDlg.showCommonDialog(true, "W", "ルートディレクトリーが選択されていません、選択しなおしますか?", data.getData().getPath(), ntfy);
                    }
                }
            });
            activity_laucher.launch(intent);
        }
    }

    public class CustomTabContentView extends FrameLayout {
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        public CustomTabContentView(Context context) {
            super(context);
        }
        public CustomTabContentView(Context context, String title) {
            this(context);
            View childview1 = inflater.inflate(R.layout.tab_widget1, null);
            TextView tv1 = (TextView) childview1.findViewById(R.id.tab_widget1_textview);
            tv1.setText(title);
            addView(childview1);
        }
    }

    private String getRemovableStoragePaths(Context context, boolean debug) {
        String mpi="";
        ArrayList<String> paths = new ArrayList<String>();
        try {
            StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            Method getVolumeList = sm.getClass().getDeclaredMethod("getVolumeList");
            Object[] volumeList = (Object[]) getVolumeList.invoke(sm);
            for (Object volume : volumeList) {
                Method getPath = volume.getClass().getDeclaredMethod("getPath");
//	            Method isRemovable = volume.getClass().getDeclaredMethod("isRemovable");
                Method getUuid = volume.getClass().getDeclaredMethod("getUuid");
                Method toString = volume.getClass().getDeclaredMethod("toString");
                Method getId = volume.getClass().getDeclaredMethod("getId");
                String path = (String) getPath.invoke(volume);
//	            boolean removable = (Boolean)isRemovable.invoke(volume);
                Method getLabel = volume.getClass().getDeclaredMethod("getUserLabel");
                String label=(String) getLabel.invoke(volume)+"\n";
                String aa=(String) toString.invoke(volume)+"\n";
                mpi+="getId="+((String) getId.invoke(volume))+"\n";
                mpi+=(String) toString.invoke(volume)+"\n";
//	            if ((String)getUuid.invoke(volume)!=null) {
//	            	paths.add(path);
//					if (debug) {
////						Log.v(APPLICATION_TAG, "RemovableStorages Uuid="+(String)getUuid.invoke(volume)+", removable="+removable+", path="+path);
//						mUtil.addLogMsg("I", (String)toString.invoke(volume));
//					}
//	            }
            }
        } catch (ClassCastException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return mpi;
    }

    private static class ViewSaveArea implements Externalizable {
        private static final long serialVersionUID = 1L;
        public int progressVisible= LinearLayout.GONE;
        public String progressCancelBtnText="";
        public String progressMsgText="";
        public int profPos,profPosTop=0,lclPos,lclPosTop=0,remPos=0,remPosTop=0;

        public int local_spinner_pos=0;
        public int remote_spinner_pos=0;

        public ArrayList<FileListItem>local_file_list=null;
        public String local_file_path="";
        public ArrayList<FileListItem>remote_file_list=null;
        public String remote_file_path="";

        public int dialogVisible=LinearLayout.GONE;
        public String dialogMsgText="";

        public ViewSaveArea() {};

        @Override
        public void writeExternal(ObjectOutput objectOutput) throws IOException {
            objectOutput.writeLong(serialVersionUID);
            objectOutput.writeInt(progressVisible);
            objectOutput.writeInt(profPos);
            objectOutput.writeInt(profPosTop);
            objectOutput.writeInt(lclPos);
            objectOutput.writeInt(lclPosTop);
            objectOutput.writeInt(remPos);
            objectOutput.writeInt(remPosTop);
            objectOutput.writeInt(local_spinner_pos);
            objectOutput.writeInt(remote_spinner_pos);
            objectOutput.writeInt(dialogVisible);
            objectOutput.writeUTF(progressCancelBtnText);
            objectOutput.writeUTF(progressMsgText);
            objectOutput.writeUTF(local_file_path);
            objectOutput.writeUTF(remote_file_path);
            objectOutput.writeUTF(dialogMsgText);
            objectOutput.writeObject(local_file_list);
            objectOutput.writeObject(remote_file_list);
        }

        @Override
        public void readExternal(ObjectInput objectInput) throws ClassNotFoundException, IOException {
            long sv=objectInput.readLong();
            progressVisible=objectInput.readInt();
            profPos=objectInput.readInt();
            profPosTop=objectInput.readInt();
            lclPos=objectInput.readInt();
            lclPosTop=objectInput.readInt();
            remPos=objectInput.readInt();
            remPosTop=objectInput.readInt();
            local_spinner_pos=objectInput.readInt();
            remote_spinner_pos=objectInput.readInt();
            dialogVisible=objectInput.readInt();
            progressCancelBtnText=objectInput.readUTF();
            progressMsgText=objectInput.readUTF();
            local_file_path=objectInput.readUTF();
            remote_file_path=objectInput.readUTF();
            dialogMsgText=objectInput.readUTF();
            local_file_list= (ArrayList<FileListItem>) objectInput.readObject();
            remote_file_list= (ArrayList<FileListItem>) objectInput.readObject();
        }
    }

    private void saveTaskData() {
	    ViewSaveArea vsa=new ViewSaveArea();
	    saveViewStatus(vsa);

        File task_data_file=getTaskDataFile();
        try {
            OutputStream os=new FileOutputStream(task_data_file);
            ObjectOutputStream oos=new ObjectOutputStream(os);

            oos.writeObject(vsa);
            oos.writeUTF(mGp.currentTabName);
            oos.writeUTF(mGp.localDirectory);

            oos.writeUTF(mGp.remoteMountpoint);
            mUtil.addDebugMsg(1, "I", "saveTaskData remoteMountPoint="+mGp.remoteMountpoint);
            oos.writeUTF(mGp.remoteDirectory);
            oos.writeObject(mGp.currentRemoteServerConfig);

            oos.writeObject(mGp.mountPointHistoryList);
            oos.writeObject(mGp.currentLocalStorage);

            oos.writeObject(mGp.pasteInfo);

            oos.flush();
            oos.close();

            mUtil.addDebugMsg(1, "I", "Task data was saved");
        } catch (IOException e) {
            e.printStackTrace();
            mUtil.addDebugMsg(1, "I", "Task data was save error\n"+ MiscUtil.getStackTraceString(e));
        }
    }

    private File getTaskDataFile() {
        File[] fl=getExternalFilesDirs(null);
        File task_data_file=new File(fl[0].getPath()+"/task_data");
        return task_data_file;
    }

    private void removeTaskData() {
	    File task_data_file=getTaskDataFile();
        if (task_data_file.exists()) {
            task_data_file.delete();
            mUtil.addDebugMsg(1, "I", "Task data was removed");
        }
    }

    private boolean isTaskDataExists() {
        File task_data_file=getTaskDataFile();
	    return task_data_file.exists();
    }

    private void restoreTaskData() {
        File task_data_file=getTaskDataFile();
        try {
            InputStream is=new FileInputStream(task_data_file);
            ObjectInputStream ois=new ObjectInputStream(is);

            ViewSaveArea vsa= (ViewSaveArea) ois.readObject();
            mGp.currentTabName=ois.readUTF();
            mGp.localDirectory=ois.readUTF();

            mGp.remoteMountpoint=ois.readUTF();
            mUtil.addDebugMsg(1, "I", "restoreTaskData new remoteMountPoint="+mGp.remoteMountpoint);
            mGp.remoteDirectory=ois.readUTF();
            mGp.currentRemoteServerConfig = (RemoteServerConfig) ois.readObject();

            mGp.mountPointHistoryList= (ArrayList<MountPointHistoryItem>) ois.readObject();
            mGp.currentLocalStorage= (LocalStorage) ois.readObject();
            if (mGp.currentLocalStorage!=null && mGp.currentLocalStorage.storage_root_path!=null) {
                if (!mGp.currentLocalStorage.storage_root_path.equals(""))
                    mGp.currentLocalStorage.storage_root=new SafFile3(mContext, mGp.currentLocalStorage.storage_root_path);
            }

            mGp.pasteInfo= (GlobalParameter.PasteInfo) ois.readObject();
            if (!mGp.pasteInfo.pasteToSafRootPath.equals(""))
                mGp.pasteInfo.pasteToSafRoot=new SafFile3(mContext, mGp.pasteInfo.pasteToSafRootPath);
            if (!mGp.pasteInfo.pasteFromSafRootPath.equals(""))
                mGp.pasteInfo.pasteFromSafRoot=new SafFile3(mContext, mGp.pasteInfo.pasteFromSafRootPath);

            ois.close();

            restoreViewStatus(vsa);

            mFileMgr.setPasteItemList();
            mGp.tabHost.setCurrentTabByTag(mGp.currentTabName);
            mFileMgr.setSpinnerSelectionEnabled(false);
            mFileMgr.setLocalDirBtnListener();
            mFileMgr.setRemoteDirBtnListener();
            mGp.localFileListDirSpinner.setSelection(vsa.local_spinner_pos, false);
            mGp.remoteFileListDirSpinner.setSelection(vsa.remote_spinner_pos, false);
            mFileMgr.setLocalFilelistItemClickListener();
            mFileMgr.setLocalFilelistLongClickListener();
            mFileMgr.setRemoteFilelistItemClickListener();
            mFileMgr.setRemoteFilelistLongClickListener();
            mFileMgr.setLocalContextButtonListener();
            mFileMgr.setRemoteContextButtonListener();
            mFileMgr.setEmptyFolderView();

            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mFileMgr.setSpinnerSelectionEnabled(true);
                }
            });
            mUtil.addDebugMsg(1, "I", "Task data was restored");
        } catch (Exception e) {
            mUtil.addDebugMsg(1, "I", "Task data was restore error\n"+ MiscUtil.getStackTraceString(e));
            e.printStackTrace();
        }
    }

    private void makeCacheDirectory() {
        String packageName = mContext.getPackageName();
        File[] stg_array=mContext.getExternalFilesDirs(null);
        for(File item:stg_array) {
            if (item!=null) {
                File cd=new File(item.getParentFile()+"/cache");
                if (!cd.exists()) {
                    boolean rc=cd.mkdirs();
//                mUtil.addDebugMsg(1, "I", "makeChachDirectory directory create result="+rc);
                }
            }
        }
    }

    private void cleanupCacheFile() {
        File[] fl=mContext.getExternalCacheDirs();
        if (fl!=null && fl.length>0) {
            for(File cf:fl) {
                if (cf!=null) {
                    File[] child_list=cf.listFiles();
                    if (child_list!=null) {
                        for(File ch_item:child_list) {
                            if (ch_item!=null) {
                                if (!deleteCacheFile(ch_item)) break;
                            }
                        }
                    }
                }
            }
        } else {
            fl=mContext.getExternalCacheDirs();
        }
    }

    private boolean deleteCacheFile(File del_item) {
        boolean result=true;
        if (del_item.isDirectory()) {
            File[] child_list=del_item.listFiles();
            for(File child_item:child_list) {
                if (child_item!=null) {
                    if (!deleteCacheFile(child_item)) {
                        result=false;
                        break;
                    }
                }
            }
            if (result) result=del_item.delete();
        } else {
            result=del_item.delete();
        }
        return result;
    }

}

