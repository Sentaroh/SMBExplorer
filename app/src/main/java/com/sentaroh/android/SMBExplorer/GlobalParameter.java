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

import android.content.Context;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import com.sentaroh.android.SMBExplorer.Log.LogUtil;
import com.sentaroh.android.SMBExplorer.FileManager.MountPointHistoryItem;
import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.LogUtil.CommonLogParameters;
import com.sentaroh.android.Utilities3.LogUtil.CommonLogParametersFactory;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.SafManager3;
import com.sentaroh.android.Utilities3.ThemeColorList;
import com.sentaroh.android.Utilities3.Widget.NonWordwrapTextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.LoggerWriter;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;

import static com.sentaroh.android.SMBExplorer.Constants.SMBEXPLORER_TAB_LOCAL;

public class GlobalParameter {
	public Context context =null;
	public String internalRootDirectory="";
    public String internalAppSpecificDirectory="";
    public CommonDialog commonDlg=null;
    public CommonUtilities mUtil=null;

    public SafManager3 safMgr =null;

    public String remoteMountpoint = "";//, localBase = "";
    public String remoteDirectory = "", localDirectory = "";

    public LocalStorage currentLocalStorage=null;

    public boolean wifiIsActive=false;
    public String wifiSsid="";

    public ArrayList<LocalStorage> localStorageList =new ArrayList<LocalStorage>();
//    public String smbUser=null, smbPass=null;
    public SmbServerConfig currentSmbServerConfig =null;
    public ThemeColorList themeColorList;

    public ArrayList<SmbServerConfig> smbConfigList =null;

    public ISvcClient svcClient = null;
    public ServiceConnection svcConnection =null;

    public TabHost tabHost =null;
    public TabWidget tabWidget =null;

    public LinearLayout mLocalView=null, mRemoteView=null;

    public ArrayList<MountPointHistoryItem> mountPointHistoryList =new ArrayList<MountPointHistoryItem>();

    public FileListAdapter localFileListAdapter=null;
    public FileListAdapter remoteFileListAdapter=null;
    public ListView localFileListView=null;
    public ListView remoteFileListView=null;
    public String currentTabName=SMBEXPLORER_TAB_LOCAL;
    public Spinner localFileListDirSpinner=null;
    public Spinner remoteFileListDirSpinner=null;

    public Button mainPasteListClearBtn=null;

    public ImageButton localContextBtnCreate =null;
    public LinearLayout localContextBtnCreateView =null;
    public ImageButton localContextBtnCopy =null;
    public LinearLayout localContextBtnCopyView =null;
    public ImageButton localContextBtnCut =null;
    public LinearLayout localContextBtnCutView =null;
    public ImageButton localContextBtnPaste =null;
    public LinearLayout localContextBtnPasteView =null;
    public ImageButton localContextBtnRename =null;
    public LinearLayout localContextBtnRenameView =null;
    public ImageButton localContextBtnDelete =null;
    public LinearLayout localContextBtnDeleteView =null;
    public ImageButton localContextBtnSelectAll =null;
    public LinearLayout localContextBtnSelectAllView =null;
    public ImageButton localContextBtnUnselectAll =null;
    public LinearLayout localContextBtnUnselectAllView =null;

    public ImageButton remoteContextBtnCreate =null;
    public LinearLayout remoteContextBtnCreateView =null;
    public ImageButton remoteContextBtnCopy =null;
    public LinearLayout remoteContextBtnCopyView =null;
    public ImageButton remoteContextBtnCut =null;
    public LinearLayout remoteContextBtnCutView =null;
    public ImageButton remoteContextBtnPaste =null;
    public LinearLayout remoteContextBtnPasteView =null;
    public ImageButton remoteContextBtnRename =null;
    public LinearLayout remoteContextBtnRenameView =null;
    public ImageButton remoteContextBtnDelete =null;
    public LinearLayout remoteContextBtnDeleteView =null;
    public ImageButton remoteContextBtnSelectAll =null;
    public LinearLayout remoteContextBtnSelectAllView =null;
    public ImageButton remoteContextBtnUnselectAll =null;
    public LinearLayout remoteContextBtnUnselectAllView =null;

    public NonWordwrapTextView localFileListPath=null;
    public TextView localFileListEmptyView=null;
    public Button localFileListUpBtn=null, localFileListTopBtn=null;

    public int dialogBackgroundColor=0xff111111;

    public NonWordwrapTextView remoteFileListPath=null;
    public TextView remoteFileListEmptyView=null;
    public Button remoteFileListUpBtn=null;
    public Button remoteFileListTopBtn=null;

    public ArrayList<FileIoLinkParm> fileioLinkParm=new ArrayList<FileIoLinkParm>();

    public TextView localProgressMsg =null;
    public Button localProgressCancel =null;
    public TextView remoteProgressMsg =null;
    public Button remoteProgressCancel =null;
    public View.OnClickListener progressOnClickListener =null;
    public LinearLayout localProgressView =null;
    public LinearLayout remoteProgressView =null;
    public LinearLayout localDialogView =null;
    public LinearLayout remoteDialogView =null;

    public View.OnClickListener dialogOnClickListener =null;
    public TextView localDialogMsg =null;
    public Button localDialogCloseBtn =null;
    public TextView remoteDialogMsg =null;
    public Button remoteDialogCloseBtn =null;
    public String dialogMsgCat ="";

    public boolean fileIoWifiLockRequired=false;
	public boolean fileIoWakeLockRequired=true;
	
	public TextView progressMsgView=null;
	public Button progressCancelBtn=null;
	public TextView dialogMsgView=null;
	public Button dialogCloseBtn=null;

//	Settings parameter
    public boolean settingExitClean=true;
    public boolean settingUseLightTheme=false;

    public boolean settingsVideoPlaybackKeepAspectRatio=true;//Vide player

    public ISvcCallback callbackStub=null;

    public boolean activityIsBackground=false;

    static public class PasteInfo implements Externalizable {
        public ArrayList<FileListItem> pasteFromList=new ArrayList<FileListItem>();
        public String pasteFromUrl="", pasteItemList="", pasteFromBase="";
        public String pasteFromDomain="", pasteFromUser="", pasteFromPass="", pasteFromSmbLevel="";
        public SafFile3 pasteFromSafRoot=null;
        public String pasteFromSafRootPath="";
        public SafFile3 pasteToSafRoot=null;
        public String pasteToSafRootPath="";
        public boolean pasteFromSmbIpc=false, pasteFromSmbSMB2Nego=false;
        public boolean isPasteCopy=false,isPasteEnabled=false, isPasteFromLocal=false;

        public PasteInfo() {};

        @Override
        public void writeExternal(ObjectOutput objectOutput) throws IOException {
            objectOutput.writeUTF(pasteFromPass);
            objectOutput.writeUTF(pasteFromBase);
            objectOutput.writeUTF(pasteFromDomain);
            if (pasteFromSafRoot!=null) pasteFromSafRootPath=pasteFromSafRoot.getPath();
            objectOutput.writeUTF(pasteFromSafRootPath);
            objectOutput.writeUTF(pasteFromSmbLevel);
            objectOutput.writeUTF(pasteFromUrl);
            objectOutput.writeUTF(pasteFromUser);

            objectOutput.writeUTF(pasteItemList);

            objectOutput.writeBoolean(pasteFromSmbIpc);
            objectOutput.writeBoolean(pasteFromSmbSMB2Nego);

            if (pasteToSafRoot!=null) pasteToSafRootPath=pasteToSafRoot.getPath();
            objectOutput.writeUTF(pasteToSafRootPath);

            objectOutput.writeObject(pasteFromList);

            objectOutput.writeBoolean(isPasteCopy);
            objectOutput.writeBoolean(isPasteEnabled);
            objectOutput.writeBoolean(isPasteFromLocal);
        }

        @Override
        public void readExternal(ObjectInput objectInput) throws ClassNotFoundException, IOException {
            pasteFromPass=objectInput.readUTF();
            pasteFromBase=objectInput.readUTF();
            pasteFromDomain=objectInput.readUTF();
            pasteFromSafRootPath=objectInput.readUTF();
            pasteFromSmbLevel=objectInput.readUTF();
            pasteFromUrl=objectInput.readUTF();
            pasteFromUser=objectInput.readUTF();

            pasteItemList=objectInput.readUTF();

            pasteFromSmbIpc=objectInput.readBoolean();
            pasteFromSmbSMB2Nego=objectInput.readBoolean();

            pasteToSafRootPath=objectInput.readUTF();

            pasteFromList= (ArrayList<FileListItem>) objectInput.readObject();

            isPasteCopy=objectInput.readBoolean();
            isPasteEnabled=objectInput.readBoolean();
            isPasteFromLocal=objectInput.readBoolean();
        }
    }
    public PasteInfo pasteInfo=new PasteInfo();

    public GlobalParameter() {};

    private static Logger slf4jLog = LoggerFactory.getLogger(GlobalParameter.class);

    public String AppSpecificDirectory="/Android/data/com.sentaroh.android.SMBExplorer/files";

	public void  initGlobalParameter(Context c) {
        context =c;
        safMgr =new SafManager3(c);

//        internalRootDirectory= Environment.getExternalStorageDirectory().toString();
//        internalAppSpecificDirectory=internalRootDirectory+AppSpecificDirectory;
        internalAppSpecificDirectory=c.getExternalFilesDirs(null)[0].toString();
        internalRootDirectory= internalAppSpecificDirectory.substring(0, internalAppSpecificDirectory.indexOf("/Android/data/"));

        CommonLogParameters clog= CommonLogParametersFactory.getLogParms(c);

        final LogUtil jcifs_ng_lu = new LogUtil(c, "SLF4J");
        JcifsNgLogWriter jcifs_ng_lw=new JcifsNgLogWriter(jcifs_ng_lu);
        slf4jLog.setWriter(jcifs_ng_lw);

        loadSettingsParm(c);


	};
	
	public void loadSettingsParm(Context c) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

    }

    public void setUsbMediaPath(String path) {

    }

    class JcifsNgLogWriter extends LoggerWriter {
        private LogUtil mLu =null;
        public JcifsNgLogWriter(LogUtil lu) {
            mLu =lu;
        }
        @Override
        public void write(String msg) {
            mLu.addDebugMsg(1,"I", msg);
        }
    }

}


