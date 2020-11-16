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

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import com.sentaroh.android.Utilities3.ContextMenu.CustomContextMenu;
import com.sentaroh.android.Utilities3.ContextMenu.CustomContextMenuItem;
import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.SafStorage3;
import com.sentaroh.android.Utilities3.ThreadCtrl;
import com.sentaroh.android.Utilities3.Widget.CustomSpinnerAdapter;
import com.sentaroh.android.Utilities3.Widget.NonWordwrapTextView;
import com.sentaroh.jcifs.JcifsException;
import com.sentaroh.jcifs.JcifsFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

import static com.sentaroh.android.SMBExplorer.Constants.FILEIO_PARM_COPY_LOCAL_TO_LOCAL;
import static com.sentaroh.android.SMBExplorer.Constants.FILEIO_PARM_COPY_LOCAL_TO_REMOTE;
import static com.sentaroh.android.SMBExplorer.Constants.FILEIO_PARM_COPY_REMOTE_TO_LOCAL;
import static com.sentaroh.android.SMBExplorer.Constants.FILEIO_PARM_COPY_REMOTE_TO_REMOTE;
import static com.sentaroh.android.SMBExplorer.Constants.FILEIO_PARM_DOWLOAD_REMOTE_FILE;
import static com.sentaroh.android.SMBExplorer.Constants.FILEIO_PARM_LOCAL_CREATE;
import static com.sentaroh.android.SMBExplorer.Constants.FILEIO_PARM_LOCAL_DELETE;
import static com.sentaroh.android.SMBExplorer.Constants.FILEIO_PARM_LOCAL_RENAME;
import static com.sentaroh.android.SMBExplorer.Constants.FILEIO_PARM_MOVE_LOCAL_TO_LOCAL;
import static com.sentaroh.android.SMBExplorer.Constants.FILEIO_PARM_MOVE_LOCAL_TO_REMOTE;
import static com.sentaroh.android.SMBExplorer.Constants.FILEIO_PARM_MOVE_REMOTE_TO_LOCAL;
import static com.sentaroh.android.SMBExplorer.Constants.FILEIO_PARM_MOVE_REMOTE_TO_REMOTE;
import static com.sentaroh.android.SMBExplorer.Constants.FILEIO_PARM_REMOTE_CREATE;
import static com.sentaroh.android.SMBExplorer.Constants.FILEIO_PARM_REMOTE_DELETE;
import static com.sentaroh.android.SMBExplorer.Constants.FILEIO_PARM_REMOTE_RENAME;
import static com.sentaroh.android.SMBExplorer.Constants.SMBEXPLORER_TAB_LOCAL;
import static com.sentaroh.android.SMBExplorer.Constants.SMBEXPLORER_TAB_POS_LOCAL;
import static com.sentaroh.android.SMBExplorer.Constants.SMBEXPLORER_TAB_POS_REMOTE;
import static com.sentaroh.android.SMBExplorer.Constants.SMBEXPLORER_TAB_REMOTE;

public class FileManager {
    private static Logger log= LoggerFactory.getLogger(FileManager.class);
    private GlobalParameter mGp;
    private Context mContext;
    private ActivityMain mActivity;
    private CommonUtilities mUtil=null;
    private CustomContextMenu ccMenu = null;
    private boolean mSpinnerSelectionEnabled =true;
    private Handler mUiHandler=null;

    public FileManager(ActivityMain a, GlobalParameter gp, CommonUtilities mu, CustomContextMenu cc) {
        mActivity=a;
        mGp=gp;
        mContext=gp.context;
        mUtil=mu;
        ccMenu=cc;
        mUiHandler=new Handler();
    }

    public void setSpinnerSelectionEnabled(boolean enabled) {
        mSpinnerSelectionEnabled =enabled;
    }

    public boolean isSpinnerSelectionEnabled() {
        return mSpinnerSelectionEnabled;
    }

    public void setMainListener() {
        setLocalDirBtnListener();
        setRemoteDirBtnListener();

        setLocalFilelistItemClickListener();
        setLocalFilelistLongClickListener();
        setRemoteFilelistItemClickListener();
        setRemoteFilelistLongClickListener();

        setLocalContextButtonListener();
        setRemoteContextButtonListener();
    }

    public void createView() {
        LinearLayout ll_local_tab=(LinearLayout)mGp.mLocalView.findViewById(R.id.explorer_filelist_local_tab);
        mGp.localContextBtnCreate =(ImageButton)ll_local_tab.findViewById(R.id.context_button_clear);
        mGp.localContextBtnCreateView =(LinearLayout)ll_local_tab.findViewById(R.id.context_button_clear_view);
        mGp.localContextBtnCopy =(ImageButton)ll_local_tab.findViewById(R.id.context_button_copy);
        mGp.localContextBtnCopyView =(LinearLayout)ll_local_tab.findViewById(R.id.context_button_copy_view);
        mGp.localContextBtnCut =(ImageButton)ll_local_tab.findViewById(R.id.context_button_cut);
        mGp.localContextBtnCutView =(LinearLayout)ll_local_tab.findViewById(R.id.context_button_cut_view);
        mGp.localContextBtnPaste =(ImageButton)ll_local_tab.findViewById(R.id.context_button_paste);
        mGp.localContextBtnPasteView =(LinearLayout)ll_local_tab.findViewById(R.id.context_button_paste_view);
        mGp.localContextBtnRename =(ImageButton)ll_local_tab.findViewById(R.id.context_button_rename);
        mGp.localContextBtnRenameView =(LinearLayout)ll_local_tab.findViewById(R.id.context_button_rename_view);
        mGp.localContextBtnDelete =(ImageButton)ll_local_tab.findViewById(R.id.context_button_delete);
        mGp.localContextBtnDeleteView =(LinearLayout)ll_local_tab.findViewById(R.id.context_button_delete_view);
        mGp.localContextBtnSelectAll =(ImageButton)ll_local_tab.findViewById(R.id.context_button_select_all);
        mGp.localContextBtnSelectAllView =(LinearLayout)ll_local_tab.findViewById(R.id.context_button_select_all_view);
        mGp.localContextBtnUnselectAll =(ImageButton)ll_local_tab.findViewById(R.id.context_button_unselect_all);
        mGp.localContextBtnUnselectAllView =(LinearLayout)ll_local_tab.findViewById(R.id.context_button_unselect_all_view);

        LinearLayout ll_remote_tab=(LinearLayout)mGp.mRemoteView.findViewById(R.id.explorer_filelist_remote_tab);
        mGp.remoteContextBtnCreate =(ImageButton)ll_remote_tab.findViewById(R.id.context_button_clear);
        mGp.remoteContextBtnCreateView =(LinearLayout)ll_remote_tab.findViewById(R.id.context_button_clear_view);
        mGp.remoteContextBtnCopy =(ImageButton)ll_remote_tab.findViewById(R.id.context_button_copy);
        mGp.remoteContextBtnCopyView =(LinearLayout)ll_remote_tab.findViewById(R.id.context_button_copy_view);
        mGp.remoteContextBtnCut =(ImageButton)ll_remote_tab.findViewById(R.id.context_button_cut);
        mGp.remoteContextBtnCutView =(LinearLayout)ll_remote_tab.findViewById(R.id.context_button_cut_view);
        mGp.remoteContextBtnPaste =(ImageButton)ll_remote_tab.findViewById(R.id.context_button_paste);
        mGp.remoteContextBtnPasteView =(LinearLayout)ll_remote_tab.findViewById(R.id.context_button_paste_view);
        mGp.remoteContextBtnRename =(ImageButton)ll_remote_tab.findViewById(R.id.context_button_rename);
        mGp.remoteContextBtnRenameView =(LinearLayout)ll_remote_tab.findViewById(R.id.context_button_rename_view);
        mGp.remoteContextBtnDelete =(ImageButton)ll_remote_tab.findViewById(R.id.context_button_delete);
        mGp.remoteContextBtnDeleteView =(LinearLayout)ll_remote_tab.findViewById(R.id.context_button_delete_view);
        mGp.remoteContextBtnSelectAll =(ImageButton)ll_remote_tab.findViewById(R.id.context_button_select_all);
        mGp.remoteContextBtnSelectAllView =(LinearLayout)ll_remote_tab.findViewById(R.id.context_button_select_all_view);
        mGp.remoteContextBtnUnselectAll =(ImageButton)ll_remote_tab.findViewById(R.id.context_button_unselect_all);
        mGp.remoteContextBtnUnselectAllView =(LinearLayout)ll_remote_tab.findViewById(R.id.context_button_unselect_all_view);

        mGp.localFileListDirSpinner=(Spinner)mGp.mLocalView.findViewById(R.id.explorer_filelist_local_tab_dir);
        mGp.remoteFileListDirSpinner=(Spinner)mGp.mRemoteView.findViewById(R.id.explorer_filelist_remote_tab_dir);

        mGp.localFileListView=(ListView)mGp.mLocalView.findViewById(R.id.explorer_filelist_local_tab_listview);
        mGp.remoteFileListView=(ListView)mGp.mRemoteView.findViewById(R.id.explorer_filelist_remote_tab_listview);
        mGp.localFileListEmptyView=(TextView)mGp.mLocalView.findViewById(R.id.explorer_filelist_local_empty_view);
        mGp.remoteFileListEmptyView=(TextView)mGp.mRemoteView.findViewById(R.id.explorer_filelist_remote_empty_view);
        mGp.localFileListPath=(NonWordwrapTextView)mGp.mLocalView.findViewById(R.id.explorer_filelist_local_filepath);
        mGp.localFileListUpBtn=(Button)mGp.mLocalView.findViewById(R.id.explorer_filelist_local_up_btn);
        mGp.localFileListTopBtn=(Button)mGp.mLocalView.findViewById(R.id.explorer_filelist_local_top_btn);

        mGp.remoteFileListPath=(NonWordwrapTextView)mGp.mRemoteView.findViewById(R.id.explorer_filelist_remote_filepath);
        mGp.remoteFileListUpBtn=(Button)mGp.mRemoteView.findViewById(R.id.explorer_filelist_remote_up_btn);
        mGp.remoteFileListTopBtn=(Button)mGp.mRemoteView.findViewById(R.id.explorer_filelist_remote_top_btn);
        mGp.remoteFileListUpBtn.setEnabled(false);
        mGp.remoteFileListTopBtn.setEnabled(false);

        mGp.localProgressMsg =(TextView)mGp.mLocalView.findViewById(R.id.explorer_filelist_local_progress_msg);
        mGp.localProgressCancel =(Button)mGp.mLocalView.findViewById(R.id.explorer_filelist_local_progress_cancel);
        mGp.remoteProgressMsg =(TextView)mGp.mRemoteView.findViewById(R.id.explorer_filelist_remote_progress_msg);
        mGp.remoteProgressCancel =(Button)mGp.mRemoteView.findViewById(R.id.explorer_filelist_remote_progress_cancel);
        mGp.progressMsgView=null;
        mGp.progressCancelBtn=null;
        mGp.dialogMsgView=null;

        mGp.localProgressView =(LinearLayout)mGp.mLocalView.findViewById(R.id.explorer_filelist_local_progress);
        mGp.remoteProgressView =(LinearLayout)mGp.mRemoteView.findViewById(R.id.explorer_filelist_remote_progress);
        mGp.localDialogView =(LinearLayout)mGp.mLocalView.findViewById(R.id.explorer_filelist_local_dialog);
        mGp.remoteDialogView =(LinearLayout)mGp.mRemoteView.findViewById(R.id.explorer_filelist_remote_dialog);

        mGp.localDialogMsg =(TextView)mGp.mLocalView.findViewById(R.id.explorer_filelist_local_dialog_msg);
        mGp.localDialogCloseBtn =(Button)mGp.mLocalView.findViewById(R.id.explorer_filelist_local_dialog_close);
        mGp.remoteDialogMsg =(TextView)mGp.mRemoteView.findViewById(R.id.explorer_filelist_remote_dialog_msg);
        mGp.remoteDialogCloseBtn =(Button)mGp.mRemoteView.findViewById(R.id.explorer_filelist_remote_dialog_close);

        mGp.dialogBackgroundColor=mGp.themeColorList.text_background_color;

        mGp.localFileListAdapter=new FileListAdapter(mActivity);
        mGp.localFileListView.setAdapter(mGp.localFileListAdapter);

        mGp.remoteFileListAdapter=new FileListAdapter(mActivity);
        mGp.remoteFileListView.setAdapter(mGp.remoteFileListAdapter);

        mGp.localFileListEmptyView.setVisibility(TextView.GONE);
        mGp.localFileListView.setVisibility(ListView.VISIBLE);

        setPasteButtonEnabled();
    }

    public void refreshFileListView() {
        if (mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
            if (mGp.currentLocalStorage==null) return;
//            mGp.safMgr.buildSafFileList();
            NotifyEvent ntfy=new NotifyEvent(mContext);
            ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                @Override
                public void positiveResponse(Context context, Object[] objects) {
                    ArrayList<FileListItem> tfl=(ArrayList<FileListItem>)objects[0];
                    mGp.localFileListAdapter.setDataList(tfl);
                    mGp.localFileListAdapter.notifyDataSetChanged();
                    mGp.tabHost.getTabWidget().getChildTabViewAt(SMBEXPLORER_TAB_POS_LOCAL).setEnabled(true);
                    setEmptyFolderView();
                }
                @Override
                public void negativeResponse(Context context, Object[] objects) {}
            });

            int fv=0;
            int top=0;
            if (mGp.localFileListView.getChildAt(0)!=null) {
                fv=mGp.localFileListView.getFirstVisiblePosition();
                top=mGp.localFileListView.getChildAt(0).getTop();
            }
            if (!updateLocalDirSpinner()) {
//                String t_dir=mGp.localDirectory;//buildFullPath(mGp.localBase,mGp.localDirectory);
//                loadLocalFilelist("",mGp.localDirectory, null);
//                mGp.localFileListView.setSelectionFromTop(fv, top);
//                setEmptyFolderView();
            }
            if (mGp.currentLocalStorage.storage_saf_file) createSafApiFileList(mGp.currentLocalStorage.storage_root, false, mGp.localDirectory, ntfy);
            else createFileApiFileList(false, mGp.localDirectory, ntfy);
        } else if (mGp.currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
            if (!mGp.remoteMountpoint.equals("")) {
                loadRemoteFilelist(mGp.remoteMountpoint, mGp.remoteDirectory, null);
            }
        }
    }

    private MountPointHistoryItem getMountPointHistoryItem(String mp) {
//        Thread.dumpStack();
        for(MountPointHistoryItem item:mGp.mountPointHistoryList) {
            if (item.mp_name.equals(mp)) {
                mUtil.addDebugMsg(1,"I","getMountPointHistoryItem mp="+mp+", result=found");
                return item;
            }
        }
        mUtil.addDebugMsg(1,"I","getMountPointHistoryItem mp="+mp+", result=Not found");
        return null;
    }

    private void removeDirectoryHistoryItem(String mp, String dir) {
        MountPointHistoryItem mphi=getMountPointHistoryItem(mp);
        if (mphi!=null) {
            boolean removed=false;
            for(DirectoryHistoryItem item:mphi.directory_history) {
                if (item.directory_name.equals(dir)) {
                    mphi.directory_history.remove(item);
                    mUtil.addDebugMsg(1,"I","removeDirectoryHistoryItem removed mp="+mp+", dir="+dir);
                    removed=true;
                    break;
                }
            }
            if (!removed) mUtil.addDebugMsg(1,"I","removeDirectoryHistoryItem DIRECTORY not found mp="+mp+", dir="+dir);
        } else {
            mUtil.addDebugMsg(1,"I","removeDirectoryHistoryItem MP not found mp="+mp+", dir="+dir);
        }
    }

    private void updateDirectoryHistoryItem(String mp, String dir, ListView lv, FileListAdapter fa) {
        int pos_fv=lv.getFirstVisiblePosition();
        int pos_top=0;
        if (lv.getChildAt(0)!=null) pos_top=lv.getChildAt(0).getTop();

        MountPointHistoryItem mphi=getMountPointHistoryItem(mp);
        if (mphi!=null) {
            for(DirectoryHistoryItem item:mphi.directory_history) {
                if (item.directory_name.equals(dir)) {
                    item.pos_fv =pos_fv;
                    item.pos_top =pos_top;
                    item.file_list=fa.getDataList();
                    mUtil.addDebugMsg(1,"I","updateDirectoryHistoryItem updated mp="+mp+", dir="+dir);
                    break;
                }
            }
        } else {
            mUtil.addDebugMsg(1,"I","updateDirectoryHistoryItem MP not found mp="+mp+", dir="+dir);
        }
    }

    private DirectoryHistoryItem getDirectoryHistoryItem(String mp, String dir) {
        MountPointHistoryItem mphi=getMountPointHistoryItem(mp);
        if (mphi!=null) {
            for(DirectoryHistoryItem item:mphi.directory_history) {
                if (item.directory_name.equals(dir)) {
                    mUtil.addDebugMsg(1,"I","getDirectoryHistoryItem found mp="+mp+", dir="+dir+", result="+item.directory_name);
                    return item;
                }
            }
        }
        mUtil.addDebugMsg(1,"I","getDirectoryHistoryItem not found mp="+mp+", dir="+dir);
        return null;
    }

    private void addDirectoryHistoryItem(String mp, String dir, ArrayList<FileListItem>fl) {
        MountPointHistoryItem mphi=getMountPointHistoryItem(mp);
        if (mphi!=null) {
            DirectoryHistoryItem dhi=getDirectoryHistoryItem(mp, dir);
            if (dhi==null) {
                DirectoryHistoryItem n_dhi=new DirectoryHistoryItem();
                n_dhi.directory_name=dir;
                n_dhi.file_list=fl;
                mphi.directory_history.add(n_dhi);
                mUtil.addDebugMsg(1,"I","addDirectoryHistoryItem added mp="+mp+", dir="+dir);
            } else {
                mUtil.addDebugMsg(1,"I","addDirectoryHistoryItem updated mp="+mp+", dir="+dir);
                dhi.file_list=fl;
            }
        } else {
            mUtil.addDebugMsg(1,"I","addDirectoryHistoryItem mp not found mp="+mp+", dir="+dir);
        }

    }

    private void addMountPointHistoryItem(String mp, String dir, ArrayList<FileListItem>fl) {
        MountPointHistoryItem mp_hist=getMountPointHistoryItem(mp);
        if (mp_hist==null) {
            DirectoryHistoryItem dhi=new DirectoryHistoryItem();
            dhi.directory_name=dir;
            dhi.file_list=fl;
            MountPointHistoryItem nmp=new MountPointHistoryItem();
            nmp.mp_name=mp;
            nmp.directory_history=new ArrayList<DirectoryHistoryItem>();
            nmp.directory_history.add(dhi);
            mGp.mountPointHistoryList.add(nmp);
            mUtil.addDebugMsg(1,"I","addMountPointHistoryItem Added mp="+mp+", dir="+dir);
        } else {
            mUtil.addDebugMsg(1,"I","addMountPointHistoryItem Already registered mp="+mp+", dir="+dir);
            DirectoryHistoryItem dhi=getDirectoryHistoryItem(mp,dir);
            if (dhi!=null) {
                dhi.file_list=fl;
            }
        }
    }

    public void loadLocalFilelist(final String path, NotifyEvent p_ntfy) {
        if (mGp.safMgr.getSafStorageList().size()>0) {
            NotifyEvent ntfy=new NotifyEvent(mContext);
            ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                @Override
                public void positiveResponse(Context context, Object[] objects) {
                    ArrayList<FileListItem> tfl=(ArrayList<FileListItem>)objects[0];
                    if (tfl==null) return;
                    addMountPointHistoryItem(mGp.currentLocalStorage.storage_id, mGp.localDirectory, tfl);

                    mGp.localFileListAdapter.setShowLastModified(true);
                    mGp.localFileListAdapter.setDataList(tfl);
                    mGp.localFileListAdapter.notifyDataSetChanged();

                    if (mGp.localDirectory.equals(mGp.currentLocalStorage.storage_root.getPath())) {
                        mGp.localFileListTopBtn.setEnabled(false);
                        mGp.localFileListUpBtn.setEnabled(false);
                    } else {
                        mGp.localFileListTopBtn.setEnabled(true);
                        mGp.localFileListUpBtn.setEnabled(true);
                    }

                    mGp.tabHost.getTabWidget().getChildTabViewAt(SMBEXPLORER_TAB_POS_LOCAL).setEnabled(true);

                    setFileListPathName(mGp.localFileListPath,"",mGp.localDirectory);
                    setLocalContextButtonStatus();

                    if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
                }

                @Override
                public void negativeResponse(Context context, Object[] objects) {}
            });
            String t_dir="";
            if (mGp.currentLocalStorage.storage_saf_file) createSafApiFileList(mGp.currentLocalStorage.storage_root, false, path, ntfy);
            else createFileApiFileList(false, path, ntfy);
        }
    }

    private void loadRemoteFilelist(final String url, final String dir, final DirectoryHistoryItem dhli) {
        final String t_dir=buildFullPath(mGp.remoteMountpoint,mGp.remoteDirectory);
        NotifyEvent ne=new NotifyEvent(mContext);
        ne.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c,Object[] o) {
                ArrayList<FileListItem> tfl=(ArrayList<FileListItem>)o[0];
                addDirectoryHistoryItem(url, dir, tfl);
                mGp.remoteFileListAdapter.setDataList(tfl);
                mGp.remoteFileListAdapter.notifyDataSetChanged();
                setFileListPathName(mGp.remoteFileListPath,mGp.remoteMountpoint,mGp.remoteDirectory);
                setRemoteContextButtonStatus();
                setEmptyFolderView();
                if (dhli!=null) mGp.remoteFileListView.setSelectionFromTop(dhli.pos_fv, dhli.pos_top);

                if (dir.equals("")) {
                    mGp.remoteFileListTopBtn.setEnabled(false);
                    mGp.remoteFileListUpBtn.setEnabled(false);
                } else {
                    mGp.remoteFileListTopBtn.setEnabled(true);
                    mGp.remoteFileListUpBtn.setEnabled(true);
                }
            }

            @Override
            public void negativeResponse(Context c,Object[] o) {
                addDirectoryHistoryItem(url, dir, new ArrayList<FileListItem>());
                mGp.remoteFileListAdapter.setDataList(new ArrayList<FileListItem>());
                mGp.remoteFileListAdapter.notifyDataSetChanged();
                setFileListPathName(mGp.remoteFileListPath,mGp.remoteMountpoint,mGp.remoteDirectory);
                setEmptyFolderView();
            }
        });
        if (dir.equals("")) createRemoteFileList(RetrieveFileList.OPCD_FILE_LIST, url+"/",ne);
        else createRemoteFileList(RetrieveFileList.OPCD_FILE_LIST, url+"/"+dir+"/",ne);
    }

    private LocalStorage getLocalStorageItem(ArrayList<LocalStorage>lsl, String stg_name) {
        LocalStorage ls_item=null;
        for(LocalStorage item:lsl) {
            if (item.storage_name.equals(stg_name)) {
                ls_item=item;
                break;
            }
        }
        return ls_item;
    }

    public boolean updateLocalDirSpinner() {
//        Thread.dumpStack();
        int sel_no=mGp.localFileListDirSpinner.getSelectedItemPosition();

        CustomSpinnerAdapter adapter = (CustomSpinnerAdapter) mGp.localFileListDirSpinner.getAdapter();
        ArrayList<LocalStorage>lsl=createLocalProfileEntry();
        boolean changed=false;

        for(LocalStorage item:lsl) {
            LocalStorage prev_item=getLocalStorageItem(mGp.localStorageList, item.storage_name);
            if (prev_item!=null) {
                item.setLastUseDirectory(prev_item.getLastUseDirectory());
                item.storage_pos_fv=prev_item.storage_pos_fv;
                item.storage_pos_top=prev_item.storage_pos_top;
            }
        }
        mGp.localStorageList.clear();
        mGp.localStorageList.addAll(lsl);

        adapter.clear();
        for (int i = 0; i<mGp.localStorageList.size(); i++) {
            adapter.add(mGp.localStorageList.get(i).storage_name);
        }
        if (adapter.getCount()>sel_no) mGp.localFileListDirSpinner.setSelection(sel_no);
        else mGp.localFileListDirSpinner.setSelection(0);

        return changed;
    }

    public void setLocalDirBtnListener() {
//        Thread.dumpStack();
        int sel_no=mGp.localFileListDirSpinner.getSelectedItemPosition();
        CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(mActivity, android.R.layout.simple_spinner_item);
//        adapter.setTextColor(Color.BLACK);
        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        mGp.localFileListDirSpinner.setPrompt("Select Storage");
        mGp.localFileListDirSpinner.setAdapter(adapter);

        mGp.localStorageList =createLocalProfileEntry();
        for (int i = 0; i<mGp.localStorageList.size(); i++) {
            adapter.add(mGp.localStorageList.get(i).storage_name);
        }
        if (adapter.getCount()>sel_no) mGp.localFileListDirSpinner.setSelection(sel_no);
        else mGp.localFileListDirSpinner.setSelection(0);

//        if (mGp.localStorageList.size()==0) {
//            if (Build.VERSION.SDK_INT>=SCOPED_STORAGE_SDK) {
//                mActivity.requestStoragePermissions();
//            }
//        }

        mGp.localFileListDirSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isSpinnerSelectionEnabled()) return;
                Spinner spinner = (Spinner) parent;
                String stg_name=(String) spinner.getSelectedItem();
                if (mGp.currentLocalStorage !=null) {
                    mGp.currentLocalStorage.setLastUseDirectory(mGp.localDirectory);
                    int pos_fv=mGp.localFileListView.getFirstVisiblePosition();
                    int pos_top=0;
                    if (mGp.localFileListView.getChildAt(0)!=null) pos_top=mGp.localFileListView.getChildAt(0).getTop();
                    mGp.currentLocalStorage.storage_pos_fv=pos_fv;
                    mGp.currentLocalStorage.storage_pos_top=pos_top;
                    log.info("set fv="+mGp.currentLocalStorage.storage_pos_fv+", top="+mGp.currentLocalStorage.storage_pos_top);
                    boolean found=false;
                    for(LocalStorage item:mGp.localStorageList) {
                        if (item.storage_name.equals(mGp.currentLocalStorage.storage_name)) {
                            item.storage_pos_fv=mGp.currentLocalStorage.storage_pos_fv;
                            item.storage_pos_top=mGp.currentLocalStorage.storage_pos_top;
                            item.setLastUseDirectory(mGp.localDirectory);
                            found=true;
                        }
                    }
                    if (!found) {
                        LocalStorage item=new LocalStorage();
                        item.storage_name=mGp.currentLocalStorage.storage_name;
                        item.storage_pos_fv=mGp.currentLocalStorage.storage_pos_fv;
                        item.storage_pos_top=mGp.currentLocalStorage.storage_pos_top;
                        item.setLastUseDirectory(mGp.localDirectory);
                        mGp.localStorageList.add(item);
                    }
                }
                boolean found=false;
                for(LocalStorage item:mGp.localStorageList) {
                    if (item.storage_name.equals(stg_name)) {
                        mGp.currentLocalStorage =item;
                        mGp.currentLocalStorage.storage_pos_fv=item.storage_pos_fv;
                        mGp.currentLocalStorage.storage_pos_top=item.storage_pos_top;
                        mGp.localDirectory =item.getLastUseDirectory().equals("")?item.storage_root.getPath():item.getLastUseDirectory();
                        log.info("new fv="+mGp.currentLocalStorage.storage_pos_fv+", top="+mGp.currentLocalStorage.storage_pos_top);
                        found=true;
//                        if (!item.storage_saf_file) mGp.localBase=mGp.internalRootDirectory;
//                        else mGp.localBase=item.storage_root.getName();
                    }
                }
//                mGp.localDirectory =mGp.currentLocalStorage.storage_root.getPath();
                NotifyEvent ntfy=new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        for (int j = 0; j < mGp.localFileListView.getChildCount(); j++)
                            mGp.localFileListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
                        setEmptyFolderView();
                        if (mGp.currentLocalStorage.storage_pos_fv==-1) mGp.localFileListView.setSelection(0);
                        else mGp.localFileListView.setSelectionFromTop(mGp.currentLocalStorage.storage_pos_fv, mGp.currentLocalStorage.storage_pos_top);
//                        log.info("restore fv="+mGp.currentLocalStorage.storage_pos_fv+", top="+mGp.currentLocalStorage.storage_pos_top);
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                String t_dir="";
                loadLocalFilelist(mGp.localDirectory, ntfy);
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        mGp.localFileListUpBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View arg0) {
                processLocalUpButton();
            }
        });

        mGp.localFileListTopBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View arg0) {
                processLocalTopButton();
            }
        });

        setLocalContextButtonStatus();
        NotifyEvent cb=new NotifyEvent(mContext);
        cb.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {setLocalContextButtonStatus();}
            @Override
            public void negativeResponse(Context context, Object[] objects) {setLocalContextButtonStatus();}
        });
        mGp.localFileListAdapter.setCbCheckListener(cb);
    }

    public void setLocalContextButtonStatus() {
        int sel_cnt=mGp.localFileListAdapter.getCheckedItemCount();
        if (sel_cnt==1) {
            mGp.localContextBtnCreateView.setVisibility(LinearLayout.INVISIBLE);
            mGp.localContextBtnCopyView.setVisibility(LinearLayout.VISIBLE);
            mGp.localContextBtnCutView.setVisibility(LinearLayout.VISIBLE);
            mGp.localContextBtnPasteView.setVisibility(LinearLayout.INVISIBLE);
            mGp.localContextBtnRenameView.setVisibility(LinearLayout.VISIBLE);
            mGp.localContextBtnDeleteView.setVisibility(LinearLayout.VISIBLE);
            mGp.localContextBtnSelectAllView.setVisibility(LinearLayout.VISIBLE);
            mGp.localContextBtnUnselectAllView.setVisibility(LinearLayout.VISIBLE);
        } else if (sel_cnt>1) {
            mGp.localContextBtnCreateView.setVisibility(LinearLayout.INVISIBLE);
            mGp.localContextBtnCopyView.setVisibility(LinearLayout.VISIBLE);
            mGp.localContextBtnCutView.setVisibility(LinearLayout.VISIBLE);
            mGp.localContextBtnPasteView.setVisibility(LinearLayout.INVISIBLE);
            mGp.localContextBtnRenameView.setVisibility(LinearLayout.INVISIBLE);
            mGp.localContextBtnDeleteView.setVisibility(LinearLayout.VISIBLE);
            mGp.localContextBtnSelectAllView.setVisibility(LinearLayout.VISIBLE);
            mGp.localContextBtnUnselectAllView.setVisibility(LinearLayout.VISIBLE);
        } else {
            mGp.localContextBtnCreateView.setVisibility(LinearLayout.VISIBLE);
            mGp.localContextBtnCopyView.setVisibility(LinearLayout.INVISIBLE);
            mGp.localContextBtnCutView.setVisibility(LinearLayout.INVISIBLE);
//            if (isPasteEnabled) localContextBtnPasteView.setVisibility(LinearLayout.VISIBLE);
//            else localContextBtnPasteView.setVisibility(LinearLayout.INVISIBLE);
            setPasteButtonEnabled();
            mGp.localContextBtnRenameView.setVisibility(LinearLayout.INVISIBLE);
            mGp.localContextBtnDeleteView.setVisibility(LinearLayout.INVISIBLE);
            mGp.localContextBtnSelectAllView.setVisibility(LinearLayout.VISIBLE);
            mGp.localContextBtnUnselectAllView.setVisibility(LinearLayout.VISIBLE);
        }
    }

    public void setRemoteContextButtonStatus() {
        int sel_cnt=mGp.remoteFileListAdapter.getCheckedItemCount();
        if (sel_cnt==1) {
            mGp.remoteContextBtnCreateView.setVisibility(LinearLayout.INVISIBLE);
            mGp.remoteContextBtnCopyView.setVisibility(LinearLayout.VISIBLE);
            mGp.remoteContextBtnCutView.setVisibility(LinearLayout.VISIBLE);
            mGp.remoteContextBtnPasteView.setVisibility(LinearLayout.INVISIBLE);
            mGp.remoteContextBtnRenameView.setVisibility(LinearLayout.VISIBLE);
            mGp.remoteContextBtnDeleteView.setVisibility(LinearLayout.VISIBLE);
            mGp.remoteContextBtnSelectAllView.setVisibility(LinearLayout.VISIBLE);
            mGp.remoteContextBtnUnselectAllView.setVisibility(LinearLayout.VISIBLE);
        } else if (sel_cnt>1) {
            mGp.remoteContextBtnCreateView.setVisibility(LinearLayout.INVISIBLE);
            mGp.remoteContextBtnCopyView.setVisibility(LinearLayout.VISIBLE);
            mGp.remoteContextBtnCutView.setVisibility(LinearLayout.VISIBLE);
            mGp.remoteContextBtnPasteView.setVisibility(LinearLayout.INVISIBLE);
            mGp.remoteContextBtnRenameView.setVisibility(LinearLayout.INVISIBLE);
            mGp.remoteContextBtnDeleteView.setVisibility(LinearLayout.VISIBLE);
            mGp.remoteContextBtnSelectAllView.setVisibility(LinearLayout.VISIBLE);
            mGp.remoteContextBtnUnselectAllView.setVisibility(LinearLayout.VISIBLE);
        } else {
            mGp.remoteContextBtnCreateView.setVisibility(LinearLayout.VISIBLE);
            mGp.remoteContextBtnCopyView.setVisibility(LinearLayout.INVISIBLE);
            mGp.remoteContextBtnCutView.setVisibility(LinearLayout.INVISIBLE);
//            if (isPasteEnabled) remoteContextBtnPasteView.setVisibility(LinearLayout.VISIBLE);
//            else remoteContextBtnPasteView.setVisibility(LinearLayout.INVISIBLE);
            setPasteButtonEnabled();
            mGp.remoteContextBtnRenameView.setVisibility(LinearLayout.INVISIBLE);
            mGp.remoteContextBtnDeleteView.setVisibility(LinearLayout.INVISIBLE);
            mGp.remoteContextBtnSelectAllView.setVisibility(LinearLayout.VISIBLE);
            mGp.remoteContextBtnUnselectAllView.setVisibility(LinearLayout.VISIBLE);
        }
    }

    private String getParentDirectory(String c_dir) {
        String result="";

        if (c_dir.lastIndexOf("/")>0) {
            result=c_dir.substring(0,c_dir.lastIndexOf("/"));
        }
        return result;
    }

    public boolean processBackKey() {
        boolean result=false;
        if (mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
            if (mGp.localFileListAdapter.isItemSelected()) {
                mGp.localFileListAdapter.setAllItemChecked(false);
                result=true;
            } else {
//                if (mGp.currentLocalStorage!=null && mGp.localFileListUpBtn.isEnabled()) {
//                    processLocalUpButton();
//                    result=true;
//                }
            }
        } else if (mGp.currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
            if (mGp.remoteFileListAdapter.isItemSelected()) {
                mGp.remoteFileListAdapter.setAllItemChecked(false);
                result=true;
            } else {
//                if (mGp.remoteFileListUpBtn.isEnabled()) {
//                    processRemoteUpButton();
//                    result=true;
//                }
            }
        }
        return result;
    }

    private void processLocalUpButton() {
        final MountPointHistoryItem mphi=getMountPointHistoryItem(mGp.currentLocalStorage.storage_id);
        mphi.directory_history.remove(mphi.directory_history.size()-1);
        final DirectoryHistoryItem n_dhi=mphi.directory_history.get(mphi.directory_history.size()-1);

        if (mphi.directory_history.size()==1) {
            mGp.localFileListUpBtn.setEnabled(false);
            mGp.localFileListTopBtn.setEnabled(false);
        }
        NotifyEvent ntfy=new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                ArrayList<FileListItem> tfl=(ArrayList<FileListItem>)objects[0];
                if (tfl==null) return;
                mGp.localDirectory =n_dhi.directory_name;
                mGp.localFileListAdapter.setDataList(tfl);
                mGp.localFileListAdapter.notifyDataSetChanged();
//                setFileListPathName(mGp.localFileListPath,mGp.localBase,mGp.localDirectory);
                setFileListPathName(mGp.localFileListPath,"",mGp.localDirectory);
                setEmptyFolderView();
                mGp.localFileListView.setSelectionFromTop(n_dhi.pos_fv, n_dhi.pos_top);
                mUiHandler.post(new Runnable(){
                    @Override
                    public void run() {
                        mGp.localFileListView.setSelectionFromTop(n_dhi.pos_fv, n_dhi.pos_top);
//                        log.info("restored fv="+n_dhi.pos_fv+", top="+n_dhi.pos_top);
                    }
                });
//                mGp.localFileListView.setSelectionFromTop(n_dhi.pos_fv, n_dhi.pos_top);
//                log.info("restored fv="+n_dhi.pos_fv+", top="+n_dhi.pos_top);
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        if (mGp.currentLocalStorage.storage_saf_file) createSafApiFileList(mGp.currentLocalStorage.storage_root, false, n_dhi.directory_name, ntfy);
        else createFileApiFileList(false, n_dhi.directory_name, ntfy);
    }

    private void processLocalTopButton() {
        MountPointHistoryItem mphi=getMountPointHistoryItem(mGp.currentLocalStorage.storage_id);
        DirectoryHistoryItem n_dhi=mphi.directory_history.get(0);
        mphi.directory_history.clear();
        mphi.directory_history.add(n_dhi);

        NotifyEvent ntfy=new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                ArrayList<FileListItem> tfl=(ArrayList<FileListItem>)objects[0];
                if (tfl==null) return;
                mGp.localDirectory =n_dhi.directory_name;
                mGp.localFileListAdapter.setDataList(tfl);
                mGp.localFileListAdapter.notifyDataSetChanged();
                setFileListPathName(mGp.localFileListPath,"",mGp.localDirectory);
                mUiHandler.post(new Runnable(){
                    @Override
                    public void run() {
                        mGp.localFileListView.setSelectionFromTop(n_dhi.pos_fv, n_dhi.pos_top);
                    }
                });
                setEmptyFolderView();
                for (int j = 0; j < mGp.localFileListView.getChildCount(); j++)
                    mGp.localFileListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
                mGp.localFileListTopBtn.setEnabled(false);
                mGp.localFileListUpBtn.setEnabled(false);
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        if (mGp.currentLocalStorage.storage_saf_file) createSafApiFileList(mGp.currentLocalStorage.storage_root, false, n_dhi.directory_name, ntfy);
        else createFileApiFileList(false, n_dhi.directory_name, ntfy);
    }

    public void setEmptyFolderView() {
        if (mGp.localFileListAdapter!=null) {
            if (mGp.localFileListAdapter.getCount()>0) {
                mGp.localFileListEmptyView.setVisibility(TextView.GONE);
                mGp.localFileListView.setVisibility(ListView.VISIBLE);
            } else {
                mGp.localFileListEmptyView.setVisibility(TextView.VISIBLE);
                mGp.localFileListView.setVisibility(ListView.GONE);
            }
        } else {
            mGp.localFileListEmptyView.setVisibility(TextView.VISIBLE);
            mGp.localFileListView.setVisibility(ListView.GONE);
        }
        if (mGp.remoteFileListAdapter!=null) {
            LinearLayout ll_context=(LinearLayout)mGp.mRemoteView.findViewById(R.id.context_view_file);
            if (mGp.remoteFileListAdapter.getCount()>0 || !mGp.remoteMountpoint.equals("")) {
                ll_context.setVisibility(LinearLayout.VISIBLE);
                mGp.remoteFileListEmptyView.setVisibility(TextView.GONE);
                mGp.remoteFileListView.setVisibility(ListView.VISIBLE);
            } else {
                ll_context.setVisibility(LinearLayout.GONE);
                mGp.remoteFileListEmptyView.setVisibility(TextView.VISIBLE);
                mGp.remoteFileListView.setVisibility(ListView.GONE);
            }
        } else {
            mGp.remoteFileListEmptyView.setVisibility(TextView.VISIBLE);
            mGp.remoteFileListView.setVisibility(ListView.GONE);
            LinearLayout ll_context=(LinearLayout)mGp.mRemoteView.findViewById(R.id.context_view_file);
            ll_context.setVisibility(LinearLayout.GONE);
        }
    }

    static public void setRemoteTabEnabled(GlobalParameter mGp) {
        String ia=CommonUtilities.getIfIpAddress("wlan0");
        String et=CommonUtilities.getIfIpAddress("eth0");
        if (ia.equals("")) ia=et;
        if (ia.startsWith("192.168.") || ia.startsWith("10.") || ia.startsWith("172.16") ) {
            mGp.remoteFileListDirSpinner.setEnabled(true);
            mGp.remoteFileListView.setEnabled(true);
            if (mGp.remoteDirectory.equals("")) {
                mGp.remoteFileListTopBtn.setEnabled(false);
                mGp.remoteFileListUpBtn.setEnabled(false);
            } else {
                mGp.remoteFileListTopBtn.setEnabled(true);
                mGp.remoteFileListUpBtn.setEnabled(true);
            }
            mGp.remoteFileListAdapter.setAdapterEnabled(true);
            mGp.remoteFileListAdapter.notifyDataSetChanged();
        } else {
            mGp.remoteFileListDirSpinner.setEnabled(false);
            mGp.remoteFileListView.setEnabled(false);
            mGp.remoteFileListTopBtn.setEnabled(false);
            mGp.remoteFileListUpBtn.setEnabled(false);
            mGp.remoteFileListAdapter.setAdapterEnabled(false);
            mGp.remoteFileListAdapter.notifyDataSetChanged();
        }
    }

    public void setRemoteDirBtnListener() {
        final CustomSpinnerAdapter spAdapter = new CustomSpinnerAdapter(mActivity, android.R.layout.simple_spinner_item);
        spAdapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        mGp.remoteFileListDirSpinner.setPrompt("リモートの選択");
        mGp.remoteFileListDirSpinner.setAdapter(spAdapter);
        if (mGp.remoteMountpoint.equals("")) spAdapter.add("--- Not selected ---");
        int a_no=0;
        for (int i = 0; i<mGp.smbConfigList.size(); i++) {
            spAdapter.add(mGp.smbConfigList.get(i).getName());
            String surl=buildRemoteBase(mGp.smbConfigList.get(i));
            if (surl.equals(mGp.remoteMountpoint))
                mGp.remoteFileListDirSpinner.setSelection(a_no);
            a_no++;
        }
        setRemoteTabEnabled(mGp);
        mGp.remoteFileListDirSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isSpinnerSelectionEnabled()) return;
                Spinner spinner = (Spinner) parent;
                if (((String)spinner.getSelectedItem()).startsWith("---")) return;

                String sel_item=(String)spinner.getSelectedItem();
                if (spAdapter.getItem(0).startsWith("---")) {
                    spAdapter.remove(spAdapter.getItem(0));
                    spinner.setSelection(position-1);
                }
                if (sel_item.startsWith("---")) return;

                SmbServerConfig pli=null;
                for (int i = 0; i<mGp.smbConfigList.size(); i++) {
                    if (mGp.smbConfigList.get(i).getName().equals(sel_item)) {
                        pli=mGp.smbConfigList.get(i);
                        break;
                    }
                }

                String turl=buildRemoteBase(pli);
                MountPointHistoryItem mphi=getMountPointHistoryItem(turl);
                if (mphi!=null) {
                    updateDirectoryHistoryItem(mGp.remoteMountpoint, mGp.remoteDirectory, mGp.remoteFileListView, mGp.remoteFileListAdapter);
                    ArrayList<DirectoryHistoryItem> dhl=mphi.directory_history;
                    DirectoryHistoryItem dhi=dhl.get(dhl.size()-1);
                    mGp.currentSmbServerConfig =pli;
                    mGp.remoteMountpoint =mphi.mp_name;
                    mGp.remoteDirectory =dhi.directory_name;

                    loadRemoteFilelist(mGp.remoteMountpoint, mGp.remoteDirectory, dhi);
//                    mGp.remoteFileListView.setSelection(0);
                    for (int j = 0; j < mGp.remoteFileListView.getChildCount(); j++)
                        mGp.remoteFileListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
                } else {
                    updateDirectoryHistoryItem(mGp.remoteMountpoint, mGp.remoteDirectory, mGp.remoteFileListView, mGp.remoteFileListAdapter);
                    mGp.tabHost.getTabWidget().getChildTabViewAt(SMBEXPLORER_TAB_POS_REMOTE).setEnabled(true);
                    mGp.currentSmbServerConfig =pli;
                    mGp.remoteMountpoint = turl;
                    mGp.remoteDirectory ="";
                    addMountPointHistoryItem(mGp.remoteMountpoint, mGp.remoteDirectory, new ArrayList<FileListItem>());

                    loadRemoteFilelist(mGp.remoteMountpoint, mGp.remoteDirectory, null);
                    mGp.remoteFileListView.setSelection(0);
                    for (int j = 0; j < mGp.remoteFileListView.getChildCount(); j++)
                        mGp.remoteFileListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
                }

            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        mGp.remoteFileListUpBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View arg0) {
                processRemoteUpButton();
            }
        });

        mGp.remoteFileListTopBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View arg0) {
                processRemoteTopButton();
            }
        });

        setRemoteContextButtonStatus();
        NotifyEvent cb=new NotifyEvent(mContext);
        cb.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                setRemoteContextButtonStatus();
            }
            @Override
            public void negativeResponse(Context context, Object[] objects) {
                setRemoteContextButtonStatus();
            }
        });
        mGp.remoteFileListAdapter.setCbCheckListener(cb);

    }

    private void processRemoteUpButton() {
        MountPointHistoryItem mphi=getMountPointHistoryItem(mGp.remoteMountpoint);
        mphi.directory_history.remove(mphi.directory_history.size()-1);
        DirectoryHistoryItem n_dhi=mphi.directory_history.get(mphi.directory_history.size()-1);

        if (mphi.directory_history.size()==1) {
            mGp.remoteFileListUpBtn.setEnabled(false);
            mGp.remoteFileListTopBtn.setEnabled(false);
        }
        NotifyEvent ntfy=new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                ArrayList<FileListItem> tfl=(ArrayList<FileListItem>)objects[0];
                if (tfl==null) return;
                mGp.remoteDirectory =n_dhi.directory_name;
                mGp.remoteFileListAdapter.setDataList(tfl);
                mGp.remoteFileListAdapter.notifyDataSetChanged();
                setFileListPathName(mGp.remoteFileListPath,mGp.remoteMountpoint,mGp.remoteDirectory);
                mGp.remoteFileListView.setSelectionFromTop(n_dhi.pos_fv, n_dhi.pos_top);
                setEmptyFolderView();
                for (int j = 0; j < mGp.remoteFileListView.getChildCount(); j++)
                    mGp.remoteFileListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        createRemoteFileList(RetrieveFileList.OPCD_FILE_LIST, mGp.remoteMountpoint +"/"+n_dhi.directory_name, ntfy);
    }

    private void processRemoteTopButton() {
        MountPointHistoryItem mphi=getMountPointHistoryItem(mGp.remoteMountpoint);
        DirectoryHistoryItem n_dhi=mphi.directory_history.get(0);
        mphi.directory_history.clear();
        mphi.directory_history.add(n_dhi);

        mGp.remoteFileListUpBtn.setEnabled(false);
        mGp.remoteFileListTopBtn.setEnabled(false);

        NotifyEvent ntfy=new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                ArrayList<FileListItem> tfl=(ArrayList<FileListItem>)objects[0];
                if (tfl==null) return;
                mGp.remoteMountpoint =n_dhi.directory_name;
                mGp.remoteDirectory ="";
                mGp.remoteFileListAdapter.setDataList(tfl);
                mGp.remoteFileListAdapter.notifyDataSetChanged();
                setFileListPathName(mGp.remoteFileListPath,mGp.remoteMountpoint,mGp.remoteDirectory);
                mGp.remoteFileListView.setSelectionFromTop(n_dhi.pos_fv, n_dhi.pos_top);
                setEmptyFolderView();
                for (int j = 0; j < mGp.remoteFileListView.getChildCount(); j++)
                    mGp.remoteFileListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        createRemoteFileList(RetrieveFileList.OPCD_FILE_LIST, mGp.remoteMountpoint +"/"+n_dhi.directory_name, ntfy);
    }

    private String buildRemoteBase(SmbServerConfig pli) {
        String url="", sep="";
        if (!pli.getSmbPort().equals("")) sep=":";
        url = "smb://"+pli.getSmbHost()+sep+pli.getSmbPort()+"/"+pli.getSmbShare() ;
        return url;
    }

    public void setLocalFilelistItemClickListener() {
        if (mGp.localFileListView==null) return;
        mGp.localFileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                for (int j = 0; j < parent.getChildCount(); j++)
                    parent.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
                FileListItem item = mGp.localFileListAdapter.getItem(position);
                if (!mActivity.isUiEnabled()) return;
                if (mGp.localFileListAdapter.isItemSelected()) {
                    item.setChecked(!item.isChecked());
                    mGp.localFileListAdapter.notifyDataSetChanged();
                } else {
                    mActivity.setUiEnabled(false);
                    mUtil.addDebugMsg(1,"I","Local filelist item clicked :" + item.getName()+", dir="+mGp.localDirectory);
                    if (item.isDirectory()) {
                        updateDirectoryHistoryItem(mGp.currentLocalStorage.storage_id, item.getPath(), mGp.localFileListView, mGp.localFileListAdapter);
                        NotifyEvent ntfy=new NotifyEvent(mContext);
                        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                            @Override
                            public void positiveResponse(Context context, Object[] objects) {
                                ArrayList<FileListItem> tfl=(ArrayList<FileListItem>)objects[0];
                                if (tfl==null) return;
                                String t_dir=item.getPath()+"/"+item.getName();
                                mGp.localDirectory =t_dir;//.replace(mGp.localBase,"");
                                mGp.localFileListAdapter.setDataList(tfl);
                                mGp.localFileListAdapter.notifyDataSetChanged();
                                for (int j = 0; j < mGp.localFileListView.getChildCount(); j++)
                                    mGp.localFileListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
                                setFileListPathName(mGp.localFileListPath,"",mGp.localDirectory);
                                setEmptyFolderView();
                                mGp.localFileListView.setSelection(0);
                                mGp.localFileListTopBtn.setEnabled(true);
                                mGp.localFileListUpBtn.setEnabled(true);

                                addDirectoryHistoryItem(mGp.currentLocalStorage.storage_id, mGp.localDirectory, tfl);

                                mActivity.setUiEnabled(true);
                            }

                            @Override
                            public void negativeResponse(Context context, Object[] objects) {}
                        });
                        if (mGp.currentLocalStorage.storage_saf_file) {
                            if (mGp.localDirectory.equals("")) createSafApiFileList(mGp.currentLocalStorage.storage_root, false, item.getPath()+"/"+item.getName(), ntfy);
                            else createSafApiFileList(mGp.currentLocalStorage.storage_root, false, item.getPath()+"/"+item.getName(), ntfy);
                        } else {
                            if (mGp.localDirectory.equals("")) createFileApiFileList(false, item.getPath()+"/"+item.getName(), ntfy);
                            else createFileApiFileList(false, item.getPath()+"/"+item.getName(), ntfy);
                        }
                    } else {
                        if (isFileListItemSelected(mGp.localFileListAdapter)) {
                            item.setChecked(!item.isChecked());
                            mGp.localFileListAdapter.notifyDataSetChanged();
                            mActivity.setUiEnabled(true);
                        } else {
                            mActivity.setUiEnabled(true);
                            String fn=item.getName().toLowerCase();
                            if (fn.endsWith(".md") || fn.endsWith(".markdown")) startLocalFileViewerIntent(item, "text/plain");
                            else startLocalFileViewerIntent(item, null);
                        }
                    }
                }
            }
        });
    }

    public void setFileListPathName(NonWordwrapTextView btn, String base, String dir) {
        if (dir.startsWith("/")) btn.setText(dir);
        else btn.setText("/"+dir);
        btn.invalidate();
        btn.requestLayout();
        setPasteButtonEnabled();
    }

    public void setLocalFilelistLongClickListener() {
        if (mGp.localFileListView==null) return;
        mGp.localFileListView
                .setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        if (!mActivity.isUiEnabled()) return true;
                        createFilelistContextMenu(arg1, arg2,mGp.localFileListAdapter);
                        return true;
                    }
                });
    }

    public void setRemoteContextButtonListener() {
        mGp.remoteContextBtnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCopyFrom(mGp.remoteFileListAdapter);
            }
        });
        mGp.remoteContextBtnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGp.remoteDirectory.length()==0) createItem(mGp.remoteFileListAdapter,"C", mGp.remoteMountpoint);
                else createItem(mGp.remoteFileListAdapter,"C", mGp.remoteMountpoint +"/"+mGp.remoteDirectory);
            }
        });
        mGp.remoteContextBtnCut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCutFrom(mGp.remoteFileListAdapter);
            }
        });
        mGp.remoteContextBtnPaste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String to_dir="";
                if (mGp.remoteDirectory.equals("")) to_dir=mGp.remoteMountpoint;
                else to_dir=mGp.remoteMountpoint +"/"+mGp.remoteDirectory;
                pasteItem(mGp.remoteFileListAdapter, to_dir, mGp.remoteMountpoint);
            }
        });
        mGp.remoteContextBtnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteItem(mGp.remoteFileListAdapter);
            }
        });
        mGp.remoteContextBtnRename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                renameItem(mGp.remoteFileListAdapter);
                mGp.remoteFileListAdapter.setAllItemChecked(false);
            }
        });
        mGp.remoteContextBtnUnselectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGp.remoteFileListAdapter.setAllItemChecked(false);
            }
        });
        mGp.remoteContextBtnSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGp.remoteFileListAdapter.setAllItemChecked(true);
            }
        });
    }

    public void setLocalContextButtonListener() {
        mGp.localContextBtnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCopyFrom(mGp.localFileListAdapter);
            }
        });
        mGp.localContextBtnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGp.localDirectory.length()==0) createItem(mGp.localFileListAdapter,"C", "");
                else createItem(mGp.localFileListAdapter,"C", mGp.localDirectory);
            }
        });
        mGp.localContextBtnCut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCutFrom(mGp.localFileListAdapter);
            }
        });
        mGp.localContextBtnPaste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String to_dir="";
//                if (mGp.localDirectory.equals("")) to_dir=mGp.localBase;
//                else to_dir=mGp.localBase+"/"+mGp.localDirectory;
                to_dir=mGp.localDirectory;
                pasteItem(mGp.localFileListAdapter, to_dir, "");
            }
        });
        mGp.localContextBtnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteItem(mGp.localFileListAdapter);
            }
        });
        mGp.localContextBtnRename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                renameItem(mGp.localFileListAdapter);
                mGp.localFileListAdapter.setAllItemChecked(false);
            }
        });
        mGp.localContextBtnUnselectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGp.localFileListAdapter.setAllItemChecked(false);
            }
        });
        mGp.localContextBtnSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGp.localFileListAdapter.setAllItemChecked(true);
            }
        });
    }

    public void setRemoteFilelistItemClickListener() {
        if (mGp.remoteFileListView==null) return;
        mGp.remoteFileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    final int position, long id) {
                for (int j = 0; j < parent.getChildCount(); j++)
                    parent.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
                if (!mActivity.isUiEnabled()) return;
                final FileListItem item = mGp.remoteFileListAdapter.getItem(position);
                mUtil.addDebugMsg(1,"I","Remote filelist item clicked :" + item.getName());
                if (mGp.remoteFileListAdapter.isItemSelected()) {
                    item.setChecked(!item.isChecked());
                    mGp.remoteFileListAdapter.notifyDataSetChanged();
                } else {
                    mActivity.setUiEnabled(false);
                    if (item.isDirectory()) {
                        NotifyEvent ne=new NotifyEvent(mContext);
                        ne.setListener(new NotifyEvent.NotifyEventListener() {
                            @Override
                            public void positiveResponse(Context c,Object[] o) {
                                String t_dir=item.getPath()+"/"+item.getName();

                                updateDirectoryHistoryItem(mGp.remoteMountpoint, mGp.remoteDirectory, mGp.remoteFileListView, mGp.remoteFileListAdapter);

                                mGp.remoteDirectory =t_dir.replace(mGp.remoteMountpoint +"/", "");

                                addDirectoryHistoryItem(mGp.remoteMountpoint, mGp.remoteDirectory, mGp.remoteFileListAdapter.getDataList());

                                mGp.remoteFileListAdapter.setDataList((ArrayList<FileListItem>)o[0]);
                                mGp.remoteFileListAdapter.notifyDataSetChanged();
                                for (int j = 0; j < mGp.remoteFileListView.getChildCount(); j++)
                                    mGp.remoteFileListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
//								setFilelistCurrDir(mGp.remoteFileListDirSpinner,mGp.remoteMountpoint, mGp.remoteDirectory);
                                setFileListPathName(mGp.remoteFileListPath,mGp.remoteMountpoint,mGp.remoteDirectory);
                                setEmptyFolderView();
                                mGp.remoteFileListView.setSelection(0);

                                mActivity.setUiEnabled(true);

                                mGp.remoteFileListTopBtn.setEnabled(true);
                                mGp.remoteFileListUpBtn.setEnabled(true);
                            }
                            @Override
                            public void negativeResponse(Context c,Object[] o) {
                                mActivity.setUiEnabled(true);
                            }
                        });
                        String t_dir=item.getPath()+"/"+item.getName();
                        createRemoteFileList(RetrieveFileList.OPCD_FILE_LIST, item.getPath()+"/"+item.getName(),ne);
                    } else {
                        mActivity.setUiEnabled(true);
                        if (isFileListItemSelected(mGp.remoteFileListAdapter)) {
                            item.setChecked(!item.isChecked());
                            mGp.remoteFileListAdapter.notifyDataSetChanged();
                        } else {
//				            view.setBackgroundColor(Color.DKGRAY);
                            startRemoteFileViewerIntent(mGp.remoteFileListAdapter, item);
                            //mGp.commonDlg.showCommonDialog(false,false,"E","","Remote file was not viewd.",null);
                        }
                    }
                }
            }
        });
    };

    private boolean isFileListItemSelected(FileListAdapter tfa) {
        boolean result=false;
        for (int i=0;i<tfa.getCount();i++) {
            if (tfa.getItem(i).isChecked()) {
                result=true;
                break;
            }
        }
        return result;
    }

    public void setRemoteFilelistLongClickListener() {
        if (mGp.remoteFileListView==null) return;
        mGp.remoteFileListView
                .setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        if (!mActivity.isUiEnabled()) return true;
                        createFilelistContextMenu(arg1, arg2, mGp.remoteFileListAdapter);
                        return true;
                    }
                });
    };

    private void createFilelistContextMenu(View view, int idx, final FileListAdapter fla) {
        mGp.fileioLinkParm.clear();
        final FileListItem item=fla.getItem(idx);
        ccMenu.addMenuItem("Property("+item.getName()+")", R.drawable.menu_properties).setOnClickListener(new CustomContextMenuItem.CustomContextMenuOnClickListener() {
            @Override
            public void onClick(CharSequence menuTitle) {
                showProperty(fla,"C", item.getName(), item.isDirectory(),idx);
//                setAllFilelistItemUnChecked(fla);
            }
        });
        if (mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL) && item.isDirectory() && !item.getName().startsWith(".") && item.getPath().indexOf("/.")<0) {
            ccMenu.addMenuItem("Scan media file", R.drawable.context_button_media_file_scan).setOnClickListener(new CustomContextMenuItem.CustomContextMenuOnClickListener() {
                @Override
                public void onClick(CharSequence menuTitle) {
                    scanLocalMediaFile(item);
//                    setAllFilelistItemUnChecked(fla);
                }
            });
        }
        if (mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL) && !item.isDirectory()) {
            ccMenu.addMenuItem("Open with Text file("+item.getName()+")").setOnClickListener(new CustomContextMenuItem.CustomContextMenuOnClickListener() {
                @Override
                public void onClick(CharSequence menuTitle) {
                    startLocalFileViewerIntent(item, "text/plain");
//                    setAllFilelistItemUnChecked(fla);
                }
            });
            ccMenu.addMenuItem("Open with Zip file("+item.getName()+")").setOnClickListener(new CustomContextMenuItem.CustomContextMenuOnClickListener() {
                @Override
                public void onClick(CharSequence menuTitle) {
                    startLocalFileViewerIntent(item, "application/zip");
//                    setAllFilelistItemUnChecked(fla);
                }
            });
            ccMenu.addMenuItem("Share("+item.getName()+")", R.drawable.context_button_share_dark).setOnClickListener(new CustomContextMenuItem.CustomContextMenuOnClickListener() {
                @Override
                public void onClick(CharSequence menuTitle) {
                    String fid="", mt=null;
                    if (item.getName().lastIndexOf(".") > 0) {
                        fid = item.getName().substring(item.getName().lastIndexOf(".") + 1, item.getName().length());
                        fid=fid.toLowerCase();
                    }
                    mt= MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);

                    Intent intent = new Intent(Intent.ACTION_SEND);
                    File lf=new File(item.getPath()+"/"+item.getName());
                    Uri uri =null;
                    if (Build.VERSION.SDK_INT>=26)  uri= FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", lf);
                    else uri=Uri.parse("file://"+lf.getPath());
//                    uri=Uri.parse("file://"+lf.getPath());
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    if (mt!=null) intent.setType(mt);
                    else intent.setType("*/*");

                    try {
                        mActivity.startActivity(intent);
                    } catch(Exception e) {
                        mGp.commonDlg.showCommonDialog(false, "E", "Share error", "startActivity() failed at shareItem() for send item. message="+e.getMessage(), null);
                    }

                }
            });
        }
        ccMenu.addMenuItem("Copy("+item.getName()+")", R.drawable.context_button_copy).setOnClickListener(new CustomContextMenuItem.CustomContextMenuOnClickListener() {
            @Override
            public void onClick(CharSequence menuTitle) {
                FileListAdapter tfla=new FileListAdapter(mActivity);
                ArrayList<FileListItem>dl=new ArrayList<FileListItem>();
                FileListItem n_item=item.clone();
                n_item.setChecked(true);
                dl.add(n_item);
                tfla.setDataList(dl);

                setCopyFrom(tfla);
            }
        });
        ccMenu.addMenuItem("Cut("+item.getName()+")", R.drawable.context_button_cut).setOnClickListener(new CustomContextMenuItem.CustomContextMenuOnClickListener() {
            @Override
            public void onClick(CharSequence menuTitle) {
                FileListAdapter tfla=new FileListAdapter(mActivity);
                ArrayList<FileListItem>dl=new ArrayList<FileListItem>();
                FileListItem n_item=item.clone();
                n_item.setChecked(true);
                dl.add(n_item);
                tfla.setDataList(dl);

                setCutFrom(tfla);
            }
        });
        ccMenu.addMenuItem("Rename("+item.getName()+")", R.drawable.context_button_rename).setOnClickListener(new CustomContextMenuItem.CustomContextMenuOnClickListener() {
            @Override
            public void onClick(CharSequence menuTitle) {
                FileListAdapter tfla=new FileListAdapter(mActivity);
                ArrayList<FileListItem>dl=new ArrayList<FileListItem>();
                FileListItem n_item=item.clone();
                n_item.setChecked(true);
                dl.add(n_item);
                tfla.setDataList(dl);

                renameItem(tfla);
            }
        });
        ccMenu.addMenuItem("Delete("+item.getName()+")", R.drawable.context_button_trash).setOnClickListener(new CustomContextMenuItem.CustomContextMenuOnClickListener() {
            @Override
            public void onClick(CharSequence menuTitle) {
                FileListAdapter tfla=new FileListAdapter(mActivity);
                ArrayList<FileListItem>dl=new ArrayList<FileListItem>();
                FileListItem n_item=item.clone();
                n_item.setChecked(true);
                dl.add(n_item);
                tfla.setDataList(dl);

                deleteItem(tfla);
            }
        });
//        ccMenu.addMenuItem("Select").setOnClickListener(new CustomContextMenuItem.CustomContextMenuOnClickListener() {
//            @Override
//            public void onClick(CharSequence menuTitle) {
//                item.setChecked(true);
//                fla.notifyDataSetChanged();
//            }
//        });
        ccMenu.createMenu();
    };

//    private void setFilelistItemToChecked(FileListAdapter fla, int pos, boolean p) {
//        fla.getItem(pos).setChecked(p);
//    }

//    private void invokeTextFileBrowser(FileListAdapter fla, final String item_optyp, final String item_name,
//                                       final boolean item_isdir, final int item_num) {
//        FileListItem item=fla.getItem(item_num);
//        try {
//            Intent intent;
//            intent = new Intent();
//            intent.setDataAndType( Uri.parse("file://"+item.getPath()+"/"+item.getName()), "text/plain");
//            mContext.startActivity(intent);
//        } catch(ActivityNotFoundException e) {
//            showDialogMsg("E", "Can not find the text file viewer.", " File name="+item.getName());
//        }
//    }

    private void sendMsgToProgDlg(Handler hndl, final String log_msg) {
        hndl.post(new Runnable() {// UI thread
            @Override
            public void run() {
                mGp.progressMsgView.setText(log_msg);
//					Log.v("","pop="+log_msg);
            }
        });
    }

    private int mScanDeleteCount=0, mScanUpdateCount=0, mScanAddCount=0;
    private void scanLocalMediaFile(FileListItem scan_dir) {
        mUtil.addDebugMsg(1,"I","Scan started");
        final Handler hndl=new Handler();
        mGp.progressMsgView=mGp.localProgressMsg;
        mGp.progressCancelBtn=mGp.localProgressCancel;
        showLocalProgressView();
        Thread th=new Thread(){
            @Override
            public void run() {
                sendMsgToProgDlg(hndl, "Start media file scan");
                mScanDeleteCount=mScanUpdateCount=mScanAddCount=0;
                ArrayList<File>fl=new ArrayList<File>();
                File lf=new File(scan_dir.getPath()+"/"+scan_dir.getName());
                getAllMediaFileInDirectory(fl, lf, true);

                ArrayList<FileListItem> ml=new ArrayList<FileListItem>();
                for(File item:fl ) {
                    FileListItem entry=null;
                    entry=new FileListItem(item.getName(), false, item.length(), item.lastModified(), false, item.canRead(), item.canWrite(),
                            item.isHidden(),item.getParent(), 0);
                    ml.add(entry);
                }
                Collections.sort(ml, new Comparator<FileListItem>(){
                    @Override
                    public int compare(FileListItem l, FileListItem r) {
                        return ((l.getPath()+"/"+l.getName())).compareToIgnoreCase((r.getPath()+"/"+r.getName()));
                    }
                });
                for(FileListItem entry:ml) mUtil.addDebugMsg(2,"I","Media file from FileSystem="+(entry.getPath()+"/"+entry.getName())+", size="+entry.getLength()+", lastModified="+entry.getLastModified());

                scanMediaStoreFileList(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, lf.getPath(), ml);
                sendMsgToProgDlg(hndl, "Image file scan ended");
                scanMediaStoreFileList(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, lf.getPath(), ml);
                sendMsgToProgDlg(hndl, "Audio file scan ended");
                scanMediaStoreFileList(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, lf.getPath(), ml);
                sendMsgToProgDlg(hndl, "Video file scan ended");

                for(FileListItem item:ml) {
                    if (!item.isChecked()) {
                        MediaScannerConnection.scanFile(mGp.context, new String[]{item.getPath()+"/"+item.getName()}, null, null);
                        mUtil.addDebugMsg(2,"I","Scan for add initiated. fp="+item.getPath()+"/"+item.getName());
                        mScanAddCount++;
                    }
                }
                hndl.post(new Runnable(){
                    @Override
                    public void run() {
                        hideLocalProgressView();
                        showDialogMsg("I","Scan media file was ended","Directory="+lf.getPath()+"\n"+"Scan for Add="+mScanAddCount+"\n"+"Scan for Delete="+mScanDeleteCount+"\n"+
                                "Scan for Update="+mScanUpdateCount);
                    }
                });
                mUtil.addDebugMsg(1,"I","Scan ended");
            }
        };
        th.start();
    }

    private void scanMediaStoreFileList(Uri ms_uri, String scan_dir, ArrayList<FileListItem>ml) {
        String[] msQueryProj=new String[] {
                MediaStore.MediaColumns.DATA,MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.DATE_MODIFIED,MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE};
        //MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        Cursor ci = mGp.context.getContentResolver().query(ms_uri ,msQueryProj ,null,null);//"_data=?" ,new String[]{scan_dir} ,"_data");

        if (ci!=null ) {
            while( ci.moveToNext() ){
                boolean media_file_different=false, m_s_d=false, m_m_d=false, f_n_e=false;
                String file_path=ci.getString(ci.getColumnIndex( MediaStore.Images.Media.DATA));
                if (file_path.startsWith(scan_dir)) {
                    String display_name=ci.getString(ci.getColumnIndex( MediaStore.Images.Media.DISPLAY_NAME));
                    long date_added=ci.getLong(ci.getColumnIndex( MediaStore.Images.Media.DATE_ADDED));
                    long date_modified=ci.getLong(ci.getColumnIndex( MediaStore.Images.Media.DATE_MODIFIED));
                    long media_size=ci.getLong(ci.getColumnIndex( MediaStore.Images.Media.SIZE));
                    File mf=new File(file_path);
                    FileListItem key=new FileListItem(mf.getName(), true, 0, date_modified, false, true,true,
                            false,mf.getParent(), 0);
                    mUtil.addDebugMsg(2,"I","Media info frm MediaStore="+file_path+", size="+media_size+", lastModified="+date_modified);
                    int idx=Collections.binarySearch(ml, key, new Comparator<FileListItem>(){
                        @Override
                        public int compare(FileListItem l, FileListItem r) {
                            return ((l.getPath()+"/"+l.getName())).compareToIgnoreCase((r.getPath()+"/"+r.getName()));
                        }
                    });
                    if (idx>=0) {
                        FileListItem file_info=ml.get(idx);
                        if (!file_info.isChecked()) {
                            file_info.setChecked(true);
                            if ((file_info.getLastModified()/1000)!=date_modified || file_info.getLength()!=media_size) {//Update required
                                MediaScannerConnection.scanFile(mGp.context, new String[]{file_path}, null, null);
                                mUtil.addDebugMsg(2,"I","Scan for update initiated. fp="+file_path);
                                mScanUpdateCount++;
                            }
                        }
                    } else {//Delete media file
                        MediaScannerConnection.scanFile(mGp.context, new String[]{file_path}, null, null);
                        mUtil.addDebugMsg(2,"I","Scan for delete initiated. fp="+file_path);
                        mScanDeleteCount++;
                    }
                }
            }
            ci.close();
        }
    }

    public void startLocalFileViewerIntent(FileListItem item, String mime_type) {
        String mt = null, fid = null;
        mUtil.addDebugMsg(1,"I","Start Intent: name=" + item.getName());
        if (item.getName().lastIndexOf(".") > 0) {
            fid = item.getName().substring(item.getName().lastIndexOf(".") + 1, item.getName().length());
            fid=fid.toLowerCase();
        }
        if (mime_type==null) {
            mt= MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);
            if (mt==null && fid!=null && fid.equals("log")) mt="text/plain";
            else if (mt==null && fid!=null && (fid.equals("m3u") || fid.equals("m3u8"))) mt="application/vnd.apple.mpegurl";
        } else {
            mt=mime_type;
        }
        if (mt != null) {
//            if (mt.startsWith("text")) mt="text/plain";
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                SafFile3 sf = new SafFile3(mContext, item.getPath() + "/" + item.getName());
                Uri uri=null;
//                if (Build.VERSION.SDK_INT>=29) {
//                    if (sf.isSafFile()) {
//                        uri=sf.getUri();
//                    } else {
//                        uri= FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID+".provider", new File(item.getPath()+"/"+item.getName()));
//                    }
//                } else {
//                    uri= FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID+".provider", new File(item.getPath()+"/"+item.getName()));
//                }
                if (sf.isSafFile()) {
                    uri=sf.getUri();
                } else {
                    if (Build.VERSION.SDK_INT>=24) {
                        uri= FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID+".provider", new File(item.getPath()+"/"+item.getName()));
                    } else {
                        uri= Uri.fromFile(new File(item.getPath()+"/"+item.getName()));
                    }
                }
                intent.setDataAndType(uri, mt);
                mActivity.startActivity(intent);
            } catch(ActivityNotFoundException e) {
                showDialogMsg("E", "File viewer can not be found.", "File name="+item.getName()+", MimeType="+mt);
            }
        } else {
            showDialogMsg("E", "MIME type can not be found.", "File name="+item.getName());
        }
    }

    private void startRemoteFileViewerIntent(FileListAdapter fla, final FileListItem item) {
        mUtil.addDebugMsg(1,"I","Start Intent: name=" + item.getName());
        String fid = null;
        if (item.getName().lastIndexOf(".") > 0) {
            fid = item.getName().substring(item.getName().lastIndexOf(".") + 1, item.getName().length()).toLowerCase();
        }
        String mt=MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);
        if (mt==null && (fid.equals("log"))) mt="text/plain";
        else if (mt==null && (fid.equals("m3u") || fid.equals("m3u8"))) mt="application/vnd.apple.mpegurl";

        if (mt != null) {
            final String mime_type=mt;
            NotifyEvent ntfy=new NotifyEvent(mContext);
            ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                @Override
                public void positiveResponse(Context c,Object[] o) {
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                Uri uri=null;
                                SafFile3 dl=new SafFile3(mContext, mGp.internalRootDirectory+"/Download/"+item.getName());
                                if (Build.VERSION.SDK_INT>=24) {
                                    uri= FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID+".provider",
                                            new File(mGp.internalRootDirectory+"/Download/"+item.getName()));
                                } else {
                                    uri=Uri.parse("file://"+mGp.internalRootDirectory+"/Download/"+item.getName());
                                }
                                intent.setDataAndType(uri, mime_type);
                                mActivity.startActivity(intent);
                            } catch(Exception e) {
                                showDialogMsg("E", "File viewer can not be found.", ", error="+e.getMessage()+", File name="+item.getName()+", MimeType="+mime_type);
                            }
                        }
                    });
                }
                @Override
                public void negativeResponse(Context c,Object[] o) {
                }
            });
            downloadRemoteFile(fla, item, mGp.remoteMountpoint, ntfy );
        } else {
            showDialogMsg("E", "MIME type can not be found.", "File name="+item.getName());
        }
    }

    private void downloadRemoteFile(FileListAdapter fla, FileListItem item, String url, NotifyEvent p_ntfy) {
        SmbServerConfig smb_item=mGp.smbConfigList.get(mGp.remoteFileListDirSpinner.getSelectedItemPosition());
        mGp.fileioLinkParm.clear();
        FileIoLinkParm fio=new FileIoLinkParm();
        fio.setFromDirectory(item.getPath());
        fio.setFromName(item.getName());
        fio.setFromSmbLevel(smb_item.getSmbLevel());
        fio.setToDirectory(mGp.internalRootDirectory+"/Download");
        fio.setToName(item.getName());
        fio.setFromUser(mGp.currentSmbServerConfig.getSmbUser());
        fio.setFromPass(mGp.currentSmbServerConfig.getSmbPass());
        fio.setFromSmbOptionIpcSignEnforce(mGp.currentSmbServerConfig.isSmbOptionIpcSigningEnforced());
        fio.setFromSmbOptionUseSMB2Negotiation(mGp.currentSmbServerConfig.isSmbOptionUseSMB2Negotiation());
//        SafFile3 rt=mGp.safMgr.getRootSafFile("1CF8-3412");

        mGp.fileioLinkParm.add(fio);
        startFileioTask(fla,FILEIO_PARM_DOWLOAD_REMOTE_FILE,mGp.fileioLinkParm, item.getName(),p_ntfy, mGp.internalRootDirectory);
    }

    private ThreadCtrl mTcFileIoTask=null;

    private void startFileioTask(FileListAdapter fla, final int op_cd,final ArrayList<FileIoLinkParm> alp,String item_name,
                                 final NotifyEvent p_ntfy, final String lmp) {
        fla.setAllItemChecked(false);

        String dst="";
        String dt = null;
        String nitem=item_name;
        mGp.fileIoWifiLockRequired=false;
        switch (op_cd) {
            case FILEIO_PARM_REMOTE_CREATE:
                mGp.fileIoWifiLockRequired=true;
            case FILEIO_PARM_LOCAL_CREATE:
                dt="Create";
                dst=item_name+" was created.";
                nitem="";
                break;
            case FILEIO_PARM_REMOTE_RENAME:
                mGp.fileIoWifiLockRequired=true;
            case FILEIO_PARM_LOCAL_RENAME:
                dt="Rename";
                dst=item_name+" was renamed.";
                nitem="";
                break;
            case FILEIO_PARM_REMOTE_DELETE:
                mGp.fileIoWifiLockRequired=true;
            case FILEIO_PARM_LOCAL_DELETE:
                dt="Delete";
                dst="Following dirs/files were deleted.";
                break;
            case FILEIO_PARM_COPY_REMOTE_TO_LOCAL:
            case FILEIO_PARM_COPY_REMOTE_TO_REMOTE:
            case FILEIO_PARM_COPY_LOCAL_TO_REMOTE:
                mGp.fileIoWifiLockRequired=true;
            case FILEIO_PARM_COPY_LOCAL_TO_LOCAL:
                dt="Copy";
                dst="Following dirs/files were copied.";
                break;
            case FILEIO_PARM_MOVE_REMOTE_TO_REMOTE:
            case FILEIO_PARM_MOVE_LOCAL_TO_REMOTE:
            case FILEIO_PARM_MOVE_REMOTE_TO_LOCAL:
                mGp.fileIoWifiLockRequired=true;
            case FILEIO_PARM_MOVE_LOCAL_TO_LOCAL:
                dt="Move";
                dst="Following dirs/files were moved.";
                break;
            case FILEIO_PARM_DOWLOAD_REMOTE_FILE:
                mGp.fileIoWifiLockRequired=true;
                dt="Download";
                dst="";
            default:
                break;
        }

        mTcFileIoTask=new ThreadCtrl();
        mTcFileIoTask.setEnabled();

        if (mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
            mGp.progressMsgView=mGp.localProgressMsg;
            mGp.progressCancelBtn=mGp.localProgressCancel;
            showLocalProgressView();
        } else if (mGp.currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
            mGp.progressMsgView=mGp.remoteProgressMsg;
            mGp.progressCancelBtn=mGp.remoteProgressCancel;
            showRemoteProgressView();
        }
        mGp.progressMsgView.setText("Preparing "+dt);

        mGp.progressCancelBtn.setEnabled(true);
        mGp.progressCancelBtn.setText("Cancel");
        mGp.progressOnClickListener =new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mTcFileIoTask.setDisabled();
                mGp.progressCancelBtn.setEnabled(false);
                mGp.progressCancelBtn.setText("Cancelling");
            }
        };
        mGp.progressCancelBtn.setOnClickListener(mGp.progressOnClickListener);

        NotifyEvent ntfy=new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c,Object[] o) {
                String elapsed_time_msg=o[0]!=null?(String)o[0]:"";
                hideRemoteProgressView();
                hideLocalProgressView();
                String end_msg="File I/O task was ended without error.";
                if (!mTcFileIoTask.isThreadResultSuccess()) {
                    if (p_ntfy!=null) p_ntfy.notifyToListener(false, null);
                    if (mTcFileIoTask.isThreadResultCancelled()) {
                        end_msg="File I/O task was cancelled.";
                        showDialogMsg("W",end_msg,"");
                        mUtil.addLogMsg("W",end_msg);
                    } else {
                        end_msg="File I/O task was failed."+"\n"+mTcFileIoTask.getThreadMessage();
                        showDialogMsg("E",end_msg,"");
                        mUtil.addLogMsg("E",end_msg);
                    }
//                    if (!mGp.activityIsBackground)
                    refreshFileListView();
                } else {
//                    if (!elapsed_time_msg.equals("")) showDialogMsg("I",elapsed_time_msg,"");
                    if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
                    else {
//					    if (!mGp.activityIsBackground)
                        refreshFileListView();
                    }
                }
                if (mGp.activityIsBackground) {
                    try {
                        mGp.svcClient.aidlUpdateNotificationMessage(end_msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                alp.clear();
            }

            @Override
            public void negativeResponse(Context c,Object[] o) {
                hideRemoteProgressView();
                hideLocalProgressView();
            }
        });

        FileIO th = new FileIO(mGp, op_cd, alp, mTcFileIoTask, ntfy, mContext, lmp);
        mTcFileIoTask.initThreadCtrl();
		th.setName("FileIo");
        th.start();
    }

    public void showDialogMsg(String cat, String st, String mt) {
        mActivity.setUiEnabled(false);
        createDialogCloseBtnListener();
        String msg="";
        if (mt.equals("")) msg=st;
        else msg=st+"\n"+mt;
        if (mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
            showLocalDialogView();
            mGp.dialogMsgView=mGp.localDialogMsg;
            mGp.dialogCloseBtn=mGp.localDialogCloseBtn;
        } else if (mGp.currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
            showRemoteDialogView();
            mGp.dialogMsgView=mGp.remoteDialogMsg;
            mGp.dialogCloseBtn=mGp.remoteDialogCloseBtn;
        }
        if (cat.equals("E")) mGp.dialogMsgView.setTextColor(Color.RED);
        else if (cat.equals("W")) mGp.dialogMsgView.setTextColor(Color.YELLOW);
        else mGp.dialogMsgView.setTextColor(Color.WHITE);
        mGp.dialogMsgView.setText(msg);
        mGp.dialogCloseBtn.setOnClickListener(mGp.dialogOnClickListener);
        mGp.dialogMsgCat =cat;
    }

    private void createDialogCloseBtnListener() {
        mGp.dialogOnClickListener =new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                hideLocalDialogView();
                hideRemoteDialogView();
                mActivity.setUiEnabled(true);
            }
        };
    }

    private void createItem(final FileListAdapter fla, final String item_optyp, final String base_dir) {
        mUtil.addDebugMsg(1,"I","createItem entered.");

        // カスタムダイアログの生成
        final Dialog dialog = new Dialog(mActivity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.file_rename_create_dlg);
        final EditText newName = (EditText) dialog.findViewById(R.id.file_rename_create_dlg_newname);
        final Button btnOk = (Button) dialog.findViewById(R.id.file_rename_create_dlg_ok_btn);
        final Button btnCancel = (Button) dialog.findViewById(R.id.file_rename_create_dlg_cancel_btn);

        CommonDialog.setDlgBoxSizeCompact(dialog);

        ((TextView)dialog.findViewById(R.id.file_rename_create_dlg_title)).setText("Create directory");
        ((TextView)dialog.findViewById(R.id.file_rename_create_dlg_subtitle)).setText("Enter new name");

        // newName.setText(item_name);

        btnOk.setEnabled(false);
        // btnCancel.setEnabled(false);
        newName.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() < 1) btnOk.setEnabled(false);
                else btnOk.setEnabled(true);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before,int count) {}
        });

        // OKボタンの指定
        btnOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                if (!checkDuplicateDir(fla,newName.getText().toString())) {
                    mGp.commonDlg.showCommonDialog(false,"E","Create","Duplicate directory name specified",null);
                } else {
                    int cmd=0;
                    if (mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
//                        mGp.fileioLinkParm=buildFileioLinkParm(mGp.fileioLinkParm, base_dir,"",newName.getText().toString(),"", mGp.smbUser,mGp.smbPass,true);
                        FileIoLinkParm fio=new FileIoLinkParm();
                        fio.setToDirectory(base_dir);
                        fio.setToName(newName.getText().toString());
                        fio.setToSafRoot(mGp.currentLocalStorage.storage_root);
                        mGp.fileioLinkParm.add(fio);
                        cmd=FILEIO_PARM_LOCAL_CREATE;
                    } else {
                        cmd=FILEIO_PARM_REMOTE_CREATE;
//                        mGp.fileioLinkParm=buildFileioLinkParm(mGp.fileioLinkParm, base_dir,"",newName.getText().toString(),"", mGp.smbUser,mGp.smbPass,true);
                        FileIoLinkParm fio=new FileIoLinkParm();
                        fio.setToDirectory(base_dir);
                        fio.setToName(newName.getText().toString()+"/");
                        fio.setToSmbLevel(mGp.currentSmbServerConfig.getSmbLevel());
                        fio.setToUser(mGp.currentSmbServerConfig.getSmbUser());
                        fio.setToPass(mGp.currentSmbServerConfig.getSmbPass());
                        fio.setToSmbOptionIpcSignEnforce(mGp.currentSmbServerConfig.isSmbOptionIpcSigningEnforced());
                        fio.setToSmbOptionUseSMB2Negotiation(mGp.currentSmbServerConfig.isSmbOptionUseSMB2Negotiation());

                        mGp.fileioLinkParm.add(fio);
                    }
                    mUtil.addDebugMsg(1,"I","createItem FILEIO task invoked.");
                    startFileioTask(fla,cmd,mGp.fileioLinkParm, newName.getText().toString(),null,null);
                }
            }
        });
        // CANCELボタンの指定
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                mUtil.addDebugMsg(1,"W","createItem cancelled.");
            }
        });
        // Cancelリスナーの指定
        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btnCancel.performClick();
            }
        });
        dialog.show();
    }

    private boolean checkDuplicateDir(FileListAdapter fla,String ndir) {
        for (int i = 0; i < fla.getCount(); i++) {
            if (ndir.equals(fla.getItem(i).getName()))
                return false; // duplicate dir
        }
        return true;
    }

    private void showProperty(FileListAdapter fla, final String item_optyp, final String item_name, final boolean item_isdir, final int item_num) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        FileListItem item = fla.getItem(item_num);

        String info;

        info = "Path="+item.getPath()+"\n";
        info = info+
                "Name="+item.getName()+"\n"+
                "Directory : "+item.isDirectory()+"\n"+
                "Hidden : "+item.isHidden()+"\n"+
                "canRead :"+item.canRead()+"\n"+
                "canWrite :"+item.canWrite()+"\n"+
                "Length : "+item.getLength()+"\n"+
                "Last modified : "+df.format(item.getLastModified())+"\n"+
                "Last modified(ms):"+item.getLastModified();
        mGp.commonDlg.showCommonDialog(false,"I","Property",info,null);

    }

    private void renameItem(final FileListAdapter fla) {
        mUtil.addDebugMsg(1,"I","renameItem entered.");
        ArrayList<FileListItem>fl=fla.getDataList();
        FileListItem t_from_item=null;
        for(FileListItem item:fl) {
            if (item.isChecked()) {
                t_from_item=item;
                break;
            }
        }
        if (t_from_item==null) return;
        FileListItem from_item=t_from_item;
        String item_name=from_item.getName();

        // カスタムダイアログの生成
        final Dialog dialog = new Dialog(mActivity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.file_rename_create_dlg);
        final EditText newName = (EditText) dialog.findViewById(R.id.file_rename_create_dlg_newname);
        final Button btnOk = (Button) dialog.findViewById(R.id.file_rename_create_dlg_ok_btn);
        final Button btnCancel = (Button) dialog.findViewById(R.id.file_rename_create_dlg_cancel_btn);

        CommonDialog.setDlgBoxSizeCompact(dialog);

        ((TextView) dialog.findViewById(R.id.file_rename_create_dlg_title)).setText("Rename");
        ((TextView) dialog.findViewById(R.id.file_rename_create_dlg_subtitle)).setText("Enter new name");

        newName.setText(item_name);

        btnOk.setEnabled(false);
        newName.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() < 1 || item_name.equals(s.toString())) btnOk.setEnabled(false);
                else btnOk.setEnabled(true);
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        // OKボタンの指定
        btnOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
//				setFixedOrientation(false);
                if (item_name.equals(newName.getText().toString())) {
                    mGp.commonDlg.showCommonDialog(false,"E","Rename", "Duplicate file name specified",null);
                } else {
                    int cmd=0;
                    if (mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
                        FileIoLinkParm fio=new FileIoLinkParm();
                        fio.setFromDirectory(from_item.getPath());
                        fio.setFromName(from_item.getName());
                        fio.setToDirectory(from_item.getPath());
                        fio.setToName(newName.getText().toString());
                        fio.setFromSafRoot(mGp.currentLocalStorage.storage_root);
                        fio.setToSafRoot(mGp.currentLocalStorage.storage_root);
                        mGp.fileioLinkParm.add(fio);
                        cmd=FILEIO_PARM_LOCAL_RENAME;
                    } else {
                        cmd=FILEIO_PARM_REMOTE_RENAME;
                        FileIoLinkParm fio=new FileIoLinkParm();
                        fio.setFromDirectory(from_item.getPath());
                        if (from_item.isDirectory()) fio.setFromName(from_item.getName()+"/");
                        else fio.setFromName(from_item.getName());
                        fio.setFromUser(mGp.currentSmbServerConfig.getSmbUser());
                        fio.setFromPass(mGp.currentSmbServerConfig.getSmbPass());

                        fio.setToDirectory(from_item.getPath());
                        if (from_item.isDirectory()) fio.setToName(newName.getText().toString()+"/");
                        else fio.setToName(newName.getText().toString());
                        fio.setToUser(mGp.currentSmbServerConfig.getSmbUser());
                        fio.setToPass(mGp.currentSmbServerConfig.getSmbPass());
                        mGp.fileioLinkParm.add(fio);
                    }
                    mUtil.addDebugMsg(1,"I","renameItem FILEIO task invoked.");
                    startFileioTask(fla,cmd,mGp.fileioLinkParm,item_name,null, null);
                }
            }
        });
        // CANCELボタンの指定
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                mUtil.addDebugMsg(1,"W","renameItem cancelled.");
            }
        });
        // Cancelリスナーの指定
        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btnCancel.performClick();
            }
        });
        dialog.show();
    }

    private void deleteItem(final FileListAdapter fla) {
        mUtil.addDebugMsg(1,"I","deleteItem entered.");
        String di ="";
        for (int i=0;i<fla.getCount();i++) {
            FileListItem item = fla.getItem(i);
            if (item.isChecked()) di=di+item.getName()+"\n";
        }

        final String item_name=di;
        NotifyEvent ne=new NotifyEvent(mContext);
        ne.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c,Object[] t) {
                mUtil.addDebugMsg(1,"I","deleteItem prepare begins.");
                for (int i=fla.getCount()-1;i>=0;i--) {
                    FileListItem item = fla.getItem(i);
                    if (item.isChecked()) {
                        if (mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
//                            buildFileioLinkParm(mGp.fileioLinkParm,item.getPath(), "",item.getName(),"","","",true);
                            FileIoLinkParm fio=new FileIoLinkParm();
                            fio.setToDirectory(item.getPath());
                            fio.setToName(item.getName());
                            fio.setToSafRoot(mGp.currentLocalStorage.storage_root);
                            mGp.fileioLinkParm.add(fio);
                        } else {
//                            buildFileioLinkParm(mGp.fileioLinkParm,item.getPath(),
//                                    "",item.getName(),"",mGp.smbUser,mGp.smbPass,true);
                            FileIoLinkParm fio=new FileIoLinkParm();
                            fio.setToDirectory(item.getPath());
                            if (item.isDirectory()) fio.setToName(item.getName()+"/");
                            else fio.setToName(item.getName());
                            fio.setToSmbLevel(mGp.currentSmbServerConfig.getSmbLevel());
                            fio.setToUser(mGp.currentSmbServerConfig.getSmbUser());
                            fio.setToPass(mGp.currentSmbServerConfig.getSmbPass());
                            fio.setToSmbOptionIpcSignEnforce(mGp.currentSmbServerConfig.isSmbOptionIpcSigningEnforced());
                            fio.setToSmbOptionUseSMB2Negotiation(mGp.currentSmbServerConfig.isSmbOptionUseSMB2Negotiation());

                            mGp.fileioLinkParm.add(fio);
                        }
                    }
                }
                fla.setAllItemChecked(false);
                mUtil.addDebugMsg(1,"I","deleteItem invokw FILEIO task.");
                if (mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL))
                    startFileioTask(fla,FILEIO_PARM_LOCAL_DELETE,mGp.fileioLinkParm,item_name,null,null);
                else startFileioTask(fla,FILEIO_PARM_REMOTE_DELETE,mGp.fileioLinkParm,item_name,null,null);
            }
            @Override
            public void negativeResponse(Context c,Object[] o) {
                fla.setAllItemChecked(false);
                mUtil.addDebugMsg(1,"W","deleteItem canceled");
            }
        });
        mGp.commonDlg.showCommonDialog(true,"W",mContext.getString(R.string.msgs_delete_file_dirs_confirm),item_name,ne);
    }

    private void setCopyFrom(FileListAdapter fla) {
        mGp.pasteInfo.pasteItemList="";
        if (mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
            mGp.pasteInfo.pasteFromUrl="/"+mGp.localDirectory;
            mGp.pasteInfo.isPasteFromLocal=true;
            mGp.pasteInfo.pasteFromSafRoot=mGp.currentLocalStorage.storage_root;
//            pasteFromBase=mGp.localBase;
        } else {
            mGp.pasteInfo.pasteFromUrl=mGp.remoteMountpoint +"/"+mGp.remoteDirectory;;
            mGp.pasteInfo.isPasteFromLocal=false;
            mGp.pasteInfo.pasteFromBase=mGp.remoteMountpoint;
            mGp.pasteInfo.pasteFromUser=mGp.currentSmbServerConfig.getSmbUser();
            mGp.pasteInfo.pasteFromPass=mGp.currentSmbServerConfig.getSmbPass();
            mGp.pasteInfo.pasteFromSmbLevel=mGp.currentSmbServerConfig.getSmbLevel();
            mGp.pasteInfo.pasteFromSmbIpc=mGp.currentSmbServerConfig.isSmbOptionIpcSigningEnforced();
            mGp.pasteInfo.pasteFromSmbSMB2Nego=mGp.currentSmbServerConfig.isSmbOptionUseSMB2Negotiation();
        }
        //Get selected item names
        mGp.pasteInfo.isPasteCopy=true;
        mGp.pasteInfo.isPasteEnabled=true;
        FileListItem fl_item;
        mGp.pasteInfo.pasteFromList.clear();
        String sep="";
        for (int i = 0; i < fla.getCount(); i++) {
            fl_item = fla.getItem(i);
            if (fl_item.isChecked()) {
                mGp.pasteInfo.pasteItemList=mGp.pasteInfo.pasteItemList+sep+fl_item.getName();
                sep=",";
                mGp.pasteInfo.pasteFromList.add(fl_item);
            }
        }
        fla.setAllItemChecked(false);
        setPasteItemList();
        setLocalContextButtonStatus();
        setRemoteContextButtonStatus();
        mUtil.addDebugMsg(1,"I","setCopyFrom fromUrl="+mGp.pasteInfo.pasteFromUrl+ ", num_of_list="+mGp.pasteInfo.pasteFromList.size());
    }

    private void setCutFrom(FileListAdapter fla) {
        mGp.pasteInfo.pasteItemList="";
        if (mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
            mGp.pasteInfo.pasteFromUrl="/"+mGp.localDirectory;
            mGp.pasteInfo.isPasteFromLocal=true;
            mGp.pasteInfo.pasteFromSafRoot=mGp.currentLocalStorage.storage_root;
//            pasteFromBase=mGp.localBase;
        } else {
            mGp.pasteInfo.pasteFromUrl=mGp.remoteMountpoint +"/"+mGp.remoteDirectory;
            mGp.pasteInfo.isPasteFromLocal=false;
            mGp.pasteInfo.pasteFromBase=mGp.remoteMountpoint;
            mGp.pasteInfo.pasteFromUser=mGp.currentSmbServerConfig.getSmbUser();
            mGp.pasteInfo.pasteFromPass=mGp.currentSmbServerConfig.getSmbPass();
            mGp.pasteInfo.pasteFromSmbLevel=mGp.currentSmbServerConfig.getSmbLevel();
        }
        //Get selected item names
        mGp.pasteInfo.isPasteCopy=false;
        mGp.pasteInfo.isPasteEnabled=true;
        FileListItem fl_item;
        mGp.pasteInfo.pasteFromList.clear();
        String sep="";
        for (int i = 0; i < fla.getCount(); i++) {
            fl_item = fla.getItem(i);
            if (fl_item.isChecked()) {
                mGp.pasteInfo.pasteItemList=mGp.pasteInfo.pasteItemList+sep+fl_item.getName();
                sep=",";
                mGp.pasteInfo.pasteFromList.add(fl_item);
            }
        }
        fla.setAllItemChecked(false);
        setPasteItemList();
        setLocalContextButtonStatus();
        setRemoteContextButtonStatus();
        mUtil.addDebugMsg(1,"I","setCutFrom fromUrl="+mGp.pasteInfo.pasteFromUrl+ ", num_of_list="+mGp.pasteInfo.pasteFromList.size());
    }

    public void setPasteItemList() {
        LinearLayout lc=(LinearLayout)mActivity.findViewById(R.id.explorer_filelist_copy_paste);
        TextView pp=(TextView)mActivity.findViewById(R.id.explorer_filelist_paste_list);
        ImageView cc=(ImageView)mActivity.findViewById(R.id.explorer_filelist_paste_copycut);
        if (mGp.pasteInfo.isPasteEnabled) {
            if (mGp.pasteInfo.isPasteCopy) cc.setImageDrawable(mContext.getDrawable(R.drawable.context_button_copy));
            else cc.setImageDrawable(mContext.getDrawable(R.drawable.context_button_cut));
            pp.setText(mGp.pasteInfo.pasteItemList);
            lc.setVisibility(LinearLayout.VISIBLE);
        }
        setPasteButtonEnabled();
    }

    public void setPasteButtonEnabled() {
        boolean vp=false;
        if (mGp.pasteInfo.isPasteEnabled) {
            if (mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
                vp=isValidPasteDestination(mGp.currentLocalStorage.storage_root.getPath(),mGp.localDirectory);
                if (vp) mGp.localContextBtnPasteView.setVisibility(LinearLayout.VISIBLE);
                else mGp.localContextBtnPasteView.setVisibility(LinearLayout.INVISIBLE);
            } else {
                vp=isValidPasteDestination(mGp.remoteMountpoint,mGp.remoteDirectory);
                if (vp) mGp.remoteContextBtnPasteView.setVisibility(LinearLayout.VISIBLE);
                else mGp.remoteContextBtnPasteView.setVisibility(LinearLayout.INVISIBLE);
            }
        } else {
            mGp.localContextBtnPasteView.setVisibility(LinearLayout.INVISIBLE);
            mGp.remoteContextBtnPasteView.setVisibility(LinearLayout.INVISIBLE);
        }
    }

    public void clearPasteItemList() {
        LinearLayout lc=(LinearLayout)mActivity.findViewById(R.id.explorer_filelist_copy_paste);
        mGp.pasteInfo.isPasteEnabled=false;
        mGp.pasteInfo.pasteItemList="";
        TextView pp=(TextView)mActivity.findViewById(R.id.explorer_filelist_paste_list);
//		TextView cc=(TextView)findViewById(R.id.explorer_filelist_paste_copycut);
        pp.setText(mGp.pasteInfo.pasteItemList);
        lc.setVisibility(LinearLayout.GONE);
        setLocalContextButtonStatus();
        setRemoteContextButtonStatus();
        setPasteButtonEnabled();
    }

    private boolean isValidPasteDestination(String base, String dir) {
        boolean result=false;
        mUtil.addDebugMsg(1,"I", "isValidPasteDestination base="+base+", Dir="+dir);
//		Log.v("","base="+base+", dir="+dir);
//		Thread.currentThread().dumpStack();
        if (mGp.pasteInfo.isPasteEnabled) {
            String to_dir=dir;
//            if (dir.equals("")) to_dir=base;
//            else {
//                if (base.equals("/")) {
//                    if (dir.startsWith("/")) to_dir=dir;
//                    else to_dir=base+dir;
//                } else {
//                    if (dir.startsWith("/")) to_dir=base+dir;
//                    else to_dir=base+"/"+dir;
//                }
//            }
            if (mGp.pasteInfo.pasteFromSafRoot!=null && mGp.pasteInfo.pasteFromSafRoot.isSafFile()) {
//                String from_dir = "/" + pasteFromSafRoot.getName() + pasteFromList.get(0).getPath();
//                String from_path = "/" + pasteFromSafRoot.getName() + pasteFromList.get(0).getPath() + "/" + pasteFromList.get(0).getName();
                String from_dir = mGp.pasteInfo.pasteFromList.get(0).getPath();
                String from_path =mGp.pasteInfo.pasteFromList.get(0).getPath() + "/" + mGp.pasteInfo.pasteFromList.get(0).getName();
                mUtil.addDebugMsg(1, "I", "isValidPasteDestination(Saf) from_dir=" + from_dir + ", from_path=" + from_path + ", to_dir=" + to_dir);
                if (!to_dir.equals(from_dir)) {
                    if (!from_path.equals(to_dir)) {
                        if (!to_dir.startsWith(from_path)) {
                            result = true;
                        }
                    }
                }
            } else if (mGp.pasteInfo.pasteFromSafRoot!=null && !mGp.pasteInfo.pasteFromSafRoot.isSafFile()) {
//                String from_dir = "/" + pasteFromSafRoot.getName() + pasteFromList.get(0).getPath();
//                String from_path = "/" + pasteFromSafRoot.getName() + pasteFromList.get(0).getPath() + "/" + pasteFromList.get(0).getName();
                String from_dir = mGp.pasteInfo.pasteFromList.get(0).getPath();
                String from_path =mGp.pasteInfo.pasteFromList.get(0).getPath() + "/" + mGp.pasteInfo.pasteFromList.get(0).getName();
                mUtil.addDebugMsg(1, "I", "isValidPasteDestination(Local) from_dir=" + from_dir + ", from_path=" + from_path + ", to_dir=" + to_dir);
                if (!to_dir.equals(from_dir)) {
                    if (!from_path.equals(to_dir)) {
                        if (!to_dir.startsWith(from_path)) {
                            result = true;
                        }
                    }
                }
            } else {
                String from_dir=mGp.pasteInfo.pasteFromList.get(0).getPath();
                String from_path=mGp.pasteInfo.pasteFromList.get(0).getPath()+"/"+mGp.pasteInfo.pasteFromList.get(0).getName();
                mUtil.addDebugMsg(1,"I", "isValidPasteDestination(Remote) from_dir="+from_dir+", from_path="+from_path+", to_dir="+to_dir);
                if (!to_dir.equals(from_dir)) {
                    if (!from_path.equals(to_dir)) {
                        if (!to_dir.startsWith(from_path)) {
                            result=true;
                        }
                    }
                }
            }
        }
        return result;
    }

    private void pasteItem(final FileListAdapter fla, final String to_dir, final String lmp) {
        //Get selected item names
        FileListItem fl_item;
        if (mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
            String fl_name="",fl_exists="";
            boolean fl_conf_req=false;
            for (int i = 0; i < mGp.pasteInfo.pasteFromList.size(); i++) {
                fl_item = mGp.pasteInfo.pasteFromList.get(i);
                fl_name=fl_name+fl_item.getName()+"\n";
                File lf=new File(to_dir+"/"+fl_item.getName());
                if (lf.exists()) {
                    fl_conf_req=true;
                    fl_exists=fl_exists+fl_item.getName()+"\n";
                }
            }
//			Log.v("","t="+to_dir);
            pasteCreateIoParm(fla,to_dir,fl_name,fl_exists,fl_conf_req, lmp);
        } else {
            final ArrayList<String> d_list=new ArrayList<String>();
            for (int i = 0; i < mGp.pasteInfo.pasteFromList.size(); i++) {
//                mUtil.addDebugMsg(1,"I","to_dir="+to_dir+", name="+pasteFromList.get(i).getName());
                d_list.add(to_dir+"/"+mGp.pasteInfo.pasteFromList.get(i).getName());
            }
            NotifyEvent ntfy=new NotifyEvent(mContext);
            // set commonDialog response
            ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                @Override
                public void positiveResponse(Context c,Object[] o) {
                    String fl_name="",fl_exists="";
                    boolean fl_conf_req=false;
                    for (int i=0;i<d_list.size();i++)
                        if (!d_list.get(i).equals(""))
                            fl_exists=fl_exists+d_list.get(i)+"\n";
                    if (!fl_exists.equals("")) fl_conf_req=true;
                    for (int i = 0; i < mGp.pasteInfo.pasteFromList.size(); i++)
                        fl_name=fl_name+mGp.pasteInfo.pasteFromList.get(i).getName()+"\n";
                    pasteCreateIoParm(fla,to_dir,fl_name,fl_exists,fl_conf_req, lmp);
                }
                @Override
                public void negativeResponse(Context c,Object[] o) {	}
            });
            checkRemoteFileExists(mGp.remoteMountpoint, d_list, mGp.currentSmbServerConfig, ntfy);
        }
    }

    private void pasteCreateIoParm(FileListAdapter fla, String to_dir, String fl_name, String fl_exists,boolean fl_conf_req, final String lmp) {
        FileListItem fi ;
        for (int i=0;i<mGp.pasteInfo.pasteFromList.size();i++) {
            fi=mGp.pasteInfo.pasteFromList.get(i);
            FileIoLinkParm fio=new FileIoLinkParm();
            fio.setFromDirectory(fi.getPath());
            fio.setToDirectory(to_dir);

            if (fi.getPath().startsWith("smb://")) {
                fio.setFromBaseDirectory(fi.getBaseUrl());
                fio.setFromUser(mGp.pasteInfo.pasteFromUser);
                fio.setFromPass(mGp.pasteInfo.pasteFromPass);
                fio.setFromSmbLevel(mGp.pasteInfo.pasteFromSmbLevel);
                fio.setFromSmbOptionIpcSignEnforce(mGp.pasteInfo.pasteFromSmbIpc);
                fio.setFromSmbOptionUseSMB2Negotiation(mGp.pasteInfo.pasteFromSmbSMB2Nego);
                if (fi.isDirectory()) fio.setFromName(fi.getName()+"/");
                else fio.setFromName(fi.getName());
            } else {
                fio.setFromBaseDirectory(fi.getBaseUrl());
                fio.setFromSafRoot(mGp.pasteInfo.pasteFromSafRoot);
                fio.setFromName(fi.getName());
            }

            if (to_dir.startsWith("smb://")) {
                fio.setToBaseDirectory(mGp.remoteMountpoint);
                fio.setToUser(mGp.currentSmbServerConfig.getSmbUser());
                fio.setToPass(mGp.currentSmbServerConfig.getSmbPass());
                fio.setToSmbLevel(mGp.currentSmbServerConfig.getSmbLevel());
                fio.setToSmbOptionIpcSignEnforce(mGp.currentSmbServerConfig.isSmbOptionIpcSigningEnforced());
                fio.setToSmbOptionUseSMB2Negotiation(mGp.currentSmbServerConfig.isSmbOptionUseSMB2Negotiation());
                if (fi.isDirectory()) fio.setToName(fi.getName()+"/");
                else fio.setToName(fi.getName());
            } else {
                fio.setToSafRoot(mGp.currentLocalStorage.storage_root);
//                fio.setToBaseDirectory(mGp.localBase);
                if (fi.getPath().startsWith("smb://")) {
                    fio.setToName(fi.getName()+"/");
                } else {
                    fio.setToName(fi.getName());
                }
            }

            mGp.fileioLinkParm.add(fio);
        }
        // copy process
        if (mGp.pasteInfo.isPasteCopy) {
            if (mGp.pasteInfo.isPasteFromLocal && mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
                // Local to Local copy localCurrDir->curr_dir
                copyConfirm(fla,FILEIO_PARM_COPY_LOCAL_TO_LOCAL,mGp.fileioLinkParm,
                        fl_name,fl_conf_req, fl_exists, lmp);
            } else if (mGp.pasteInfo.isPasteFromLocal && mGp.currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
                // Local to Remote copy localCurrDir->remoteUrl
                copyConfirm(fla,FILEIO_PARM_COPY_LOCAL_TO_REMOTE,mGp.fileioLinkParm,
                        fl_name,fl_conf_req, fl_exists, lmp);

            } else if (!mGp.pasteInfo.isPasteFromLocal && mGp.currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
                // Remote to Remote copy localCurrDir->remoteUrl
                copyConfirm(fla,FILEIO_PARM_COPY_REMOTE_TO_REMOTE,mGp.fileioLinkParm,
                        fl_name,fl_conf_req, fl_exists, lmp);

            } else {
                // Remote to Local copy localCurrDir->remoteUrl
                copyConfirm(fla,FILEIO_PARM_COPY_REMOTE_TO_LOCAL,mGp.fileioLinkParm,
                        fl_name,fl_conf_req, fl_exists, lmp);
            }
        } else {
            // process move
            clearPasteItemList();
            if (mGp.pasteInfo.isPasteFromLocal && mGp.currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
                // Local to Local
                moveConfirm(fla,FILEIO_PARM_MOVE_LOCAL_TO_LOCAL,mGp.fileioLinkParm,
                        fl_name,fl_conf_req, fl_exists, lmp);
            } else if (mGp.pasteInfo.isPasteFromLocal && mGp.currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
                // Local to Remote
                moveConfirm(fla,FILEIO_PARM_MOVE_LOCAL_TO_REMOTE,mGp.fileioLinkParm,
                        fl_name,fl_conf_req, fl_exists, lmp);

            } else if (!mGp.pasteInfo.isPasteFromLocal && mGp.currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
                // Remote to Remote
                moveConfirm(fla,FILEIO_PARM_MOVE_REMOTE_TO_REMOTE,mGp.fileioLinkParm,
                        fl_name,fl_conf_req, fl_exists, lmp);

            } else {
                // Remote to Local
                moveConfirm(fla,FILEIO_PARM_MOVE_REMOTE_TO_LOCAL,mGp.fileioLinkParm,
                        fl_name,fl_conf_req, fl_exists, lmp);
            }
        }

    }

    private void copyConfirm(final FileListAdapter fla, final int cmd_cd, ArrayList<FileIoLinkParm> alp,
                             final String selected_name, boolean conf_req, String conf_msg, final String lmp) {

        if (conf_req) {
            NotifyEvent ne=new NotifyEvent(mContext);
            // set commonDialog response
            ne.setListener(new NotifyEvent.NotifyEventListener() {
                @Override
                public void positiveResponse(Context c,Object[] o) {
                    mUtil.addDebugMsg(1,"I","copyConfirm File I/O task invoked.");
                    startFileioTask(fla,cmd_cd,mGp.fileioLinkParm,selected_name,null, lmp);
                }
                @Override
                public void negativeResponse(Context c,Object[] o) {
                    mUtil.addLogMsg("W","Ccopy override confirmation cancelled.");
                }
            });
            mGp.commonDlg.showCommonDialog(true,"W","Copy following dirs/files are overrides?",conf_msg,ne);

        } else {
            NotifyEvent ne=new NotifyEvent(mContext);
            // set commonDialog response
            ne.setListener(new NotifyEvent.NotifyEventListener() {
                @Override
                public void positiveResponse(Context c,Object[] o) {
                    mUtil.addDebugMsg(1,"I","copyConfirm FILE I/O task invoked.");
                    startFileioTask(fla,cmd_cd,mGp.fileioLinkParm,selected_name,null, lmp);
                }
                @Override
                public void negativeResponse(Context c,Object[] o) {
                    mUtil.addLogMsg("W","Copy cancelled."+"\n"+selected_name);
                }
            });
            mGp.commonDlg.showCommonDialog(true,"I","Following dirs/files are copy?",selected_name,ne);
        }
        return;
    }

    private void moveConfirm(final FileListAdapter fla,
                             final int cmd_cd, ArrayList<FileIoLinkParm> alp,
                             final String selected_name, boolean conf_req, String conf_msg, final String lmp) {

        if (conf_req) {
            NotifyEvent ne=new NotifyEvent(mContext);
            // set commonDialog response
            ne.setListener(new NotifyEvent.NotifyEventListener() {
                @Override
                public void positiveResponse(Context c,Object[] o) {
                    mUtil.addDebugMsg(1,"I","moveConfirm File I/O task invoked.");
                    startFileioTask(fla,cmd_cd,mGp.fileioLinkParm,selected_name,null, lmp);
                }
                @Override
                public void negativeResponse(Context c,Object[] o) {
                    mUtil.addLogMsg("W","Move override confirmation cancelled.");
                }
            });
            mGp.commonDlg.showCommonDialog(true,"W","Move following dirs/files are overrides?", conf_msg,ne);

        } else {
            NotifyEvent ne=new NotifyEvent(mContext);
            // set commonDialog response
            ne.setListener(new NotifyEvent.NotifyEventListener() {
                @Override
                public void positiveResponse(Context c,Object[] o) {
                    mUtil.addDebugMsg(1,"I","moveConfirm FILE I/O task invoked.");
                    startFileioTask(fla,cmd_cd,mGp.fileioLinkParm,selected_name,null, lmp);
                }
                @Override
                public void negativeResponse(Context c,Object[] o) {
                    mUtil.addLogMsg("W","Move cancelled."+"\n"+selected_name);
                }
            });
            mGp.commonDlg.showCommonDialog(true,"I","Following dirs/files are move?",selected_name,ne);
        }
        return;
    }

    static private boolean hasContainedNomediaFile(File lf) {
        boolean result=false;
        File nomedia=new File(lf.getAbsolutePath()+"/.nomedia");
        result=nomedia.exists();
        return result;
    }

    final static public void getAllMediaFileInDirectory(ArrayList<File>fl, File lf, boolean process_sub_directories) {
        if (lf.exists()) {
            if ((!lf.isHidden() && !hasContainedNomediaFile(lf))) {
                if (lf.isDirectory()) {
                    File[] cfl=lf.listFiles();
                    if (cfl!=null && cfl.length>0) {
                        for(File cf:cfl) {//Process file
                            if (!cf.isDirectory()) {
                                if (isMediaFile(cf)) fl.add(cf);
                            }
                        }
                        for(File cf:cfl) {
                            if (cf.isDirectory()) {
                                if (!hasContainedNomediaFile(cf)) {
                                    if (!cf.getName().equals(".thumbnails")) {
                                        if (process_sub_directories)
                                            getAllMediaFileInDirectory(fl, cf, process_sub_directories);
                                    }
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    fl.add(lf);
                }
            }
        }
    }

    static private boolean isMediaFile(File lf) {
        final String[] FILE_TYPE_AUDIO=
                new String[]{".aac",".aif", ".aifc", ".aiff", ".kar", ".m3u", ".m4a", ".mid", ".midi", ".mp2",
                        ".mp3", ".mpga", ".ra", ".ram", ".wav"};
        final String[] FILE_TYPE_IMAGE=
                new String[]{".bmp", ".cgm", ".djv", ".djvu", ".gif", ".ico", ".ief", ".jpe", ".jpeg", ".jpg", ".pbm",
                        ".pgm", ".png", ".pnm", ".ppm", ".ras", ".rgb", ".svg", ".tif", ".tiff", ".wbmp", ".xbm",
                        ".xpm", ".xwd"};
        final String[] FILE_TYPE_VIDEO=
                new String[]{".avi", ".m4u", ".mov", ".mp4", ".movie", ".mpe", ".mpeg", ".mpg", ".mxu", ".qt", ".wmv"};

        boolean result=false;
        for(String ft:FILE_TYPE_AUDIO) {
            if (lf.getName().toLowerCase().endsWith(ft)) {
                result=true;
                break;
            }
        }
        if (!result) {
            for(String ft:FILE_TYPE_IMAGE) {
                if (lf.getName().toLowerCase().endsWith(ft)) {
                    result=true;
                    break;
                }
            }
        }
        if (!result) {
            for(String ft:FILE_TYPE_VIDEO) {
                if (lf.getName().toLowerCase().endsWith(ft)) {
                    result=true;
                    break;
                }
            }
        }
        return result;
    }

    final public long getSafApiAllFileSizeInDirectory(SafFile3 sd, boolean process_sub_directories, ContentProviderClient cpc) {
        long dir_size=0l;
        if (sd.exists(cpc)) {
            if (sd.isDirectory(cpc)) {
//                long b_time=System.currentTimeMillis();
                SafFile3[] cfl=sd.listFiles(cpc);
//                mUtil.addDebugMsg(1,"I","list children name="+sd.getName()+", elapsed="+(System.currentTimeMillis()-b_time));
                for(SafFile3 cf:cfl) {
                    if (cf.isDirectory(cpc)) {
                        if (process_sub_directories)
                            dir_size+=getSafApiAllFileSizeInDirectory(cf, process_sub_directories, cpc);
                    } else {
                        dir_size+=cf.length(cpc);
                    }
                }
            } else {
                dir_size+=sd.length(cpc);
            }
        }
        return dir_size;
    }

    final public long getSafApiAllFileSizeInDirectory(SafFile3 sd, boolean process_sub_directories) {
        long dir_size=0l;
        if (sd.exists()) {
            if (sd.isDirectory()) {
//                long b_time=System.currentTimeMillis();
                SafFile3[] cfl=sd.listFiles();
//                mUtil.addDebugMsg(1,"I","list children name="+sd.getName()+", elapsed="+(System.currentTimeMillis()-b_time));
                for(SafFile3 cf:cfl) {
                    if (cf.isDirectory()) {
                        if (process_sub_directories)
                            dir_size+= getSafApiAllFileSizeInDirectory(cf, process_sub_directories);
                    } else {
                        dir_size+=cf.length();
                    }
                }
            } else {
                dir_size+=sd.length();
            }
        }
        return dir_size;
    }

    final public long getFileApiAllFileSizeInDirectory(File sd, boolean process_sub_directories) {
        long dir_size=0l;
        if (sd.exists()) {
            if (sd.isDirectory()) {
//                long b_time=System.currentTimeMillis();
                File[] cfl=sd.listFiles();
//                mUtil.addDebugMsg(1,"I","list children name="+sd.getName()+", elapsed="+(System.currentTimeMillis()-b_time));
                if (cfl!=null) {
                    for(File cf:cfl) {
                        if (cf.isDirectory()) {
                            if (process_sub_directories)
                                dir_size+= getFileApiAllFileSizeInDirectory(cf, process_sub_directories);
                        } else {
                            dir_size+=cf.length();
                        }
                    }
                }
            } else {
                dir_size+=sd.length();
            }
        }
        return dir_size;
    }

    private String getRootFilePath(String fp) {
        if (fp.startsWith("/storage/emulated/0")) return "/storage/emulated/0";
        else {
            String[] fp_parts=fp.startsWith("/")?fp.substring(1).split("/"):fp.split("/");
            String rt_fp="/"+fp_parts[0]+"/"+fp_parts[1];
            return rt_fp;
        }
    }

    private void createFileApiFileList(boolean dir_only, String url, NotifyEvent p_ntfy) {
        mUtil.addDebugMsg(1,"I","createFileApiFileList create file list started, dir=" + url+", rp="+getRootFilePath(url));
//        Thread.dumpStack();
        final Dialog pd=CommonDialog.showProgressSpinIndicator(mActivity);
        pd.show();
        Thread th=new Thread(){
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                SimpleDateFormat sdf_ut = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                sdf_ut.setTimeZone(TimeZone.getTimeZone("UTC"));
                ArrayList<FileListItem> dir = new ArrayList<FileListItem>();
                ArrayList<FileListItem> fls = new ArrayList<FileListItem>();
                String ignore_directory=getRootFilePath(url)+"/Android/";
                File sf = new File(url);
                File[] file_list = sf.listFiles();
                if (file_list!=null) {
                    try {
                        for (File ff : file_list) {
                            FileListItem tfi=null;
                            if (ff.canRead()) {
                                if (ff.isDirectory()) {
                                    String[] tfl=ff.list();
                                    int sdc=0;
                                    if (tfl!=null) sdc=tfl.length;
                                    int ll=0;
                                    tfi=createFileApiFilelistItem("", ff, sdc, ll, true, url);
                                    dir.add(tfi);
//                                    if (!CommonUtilities.isAndroidVersion30orUp() ||
//                                        (CommonUtilities.isAndroidVersion30orUp() && !(ff.getPath()+"/").toLowerCase().startsWith(ignore_directory.toLowerCase()))) {
//                                        dir.add(tfi);
//                                    }
                                } else {
                                    tfi=createFileApiFilelistItem("", ff, 0, 0, false, url);
                                    fls.add(tfi);
                                    long et=ff.lastModified();
                                }
                                if (mUtil.getLogLevel()>=3) {
                                    long et=tfi.getLastModified();
                                    String ut=sdf_ut.format(et);
                                    if (tfi.isDirectory()) {
                                        mUtil.addDebugMsg(3,"I","Directory=" + tfi.getName()+", "+
                                                "length=" + tfi.getLength()+", "+
                                                "Last mod Local=" + sdf.format(tfi.getLastModified())+", "+
                                                "Last mod UTC=" + ut+", "+
                                                "Last mod EPOC=" + et+", "+
                                                "path=" + tfi.getPath()+", ");
                                    } else {
                                        mUtil.addDebugMsg(3,"I","File=" + tfi.getName()+", "+
                                                "length=" + tfi.getLength()+", "+
                                                "Last mod Local=" + sdf.format(tfi.getLastModified())+", "+
                                                "Last mod UTC=" + ut+", "+
                                                "Last mod EPOC=" + et+", "+
                                                "path=" + tfi.getPath()+", ");
                                    }
                                }
                            } else {
                                tfi=createFileApiFilelistItem("", ff, 0, 0, false, url);
                                if (tfi.isDirectory()) dir.add(tfi);
                                else fls.add(tfi);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        mUtil.addLogMsg("E",e.toString());
                        showDialogMsg("E",mContext.getString(R.string.msgs_local_file_list_create_error), e.getMessage());
                        dir=null;
                    }
                }

                if (dir!=null) {
                    FileListAdapter.sort(dir);
                    if (!dir_only) {
                        FileListAdapter.sort(fls);
                        dir.addAll(fls);
                    }
                }
                mUtil.addDebugMsg(1,"I","createFileApiFileList file list create ended");
                final ArrayList<FileListItem> dir_final = dir;
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        p_ntfy.notifyToListener(true, new Object[]{dir_final});
                        pd.dismiss();
                    }
                });
            }
        };
        th.start();
    }

    private void createSafApiFileList(final SafFile3 rf, boolean dir_only, final String url, final NotifyEvent p_ntfy) {
//        Thread.dumpStack();
        mUtil.addDebugMsg(1,"I","createSafApiFileList file list create started. dir=" + url);

        File lf=new File(url);
        if (lf.canRead()) {
            createFileApiFileList(dir_only, url, p_ntfy);
            return;
        }

        final ThreadCtrl tc = new ThreadCtrl();
        tc.setEnabled();
        tc.setThreadResultSuccess();

        final Dialog dialog= CommonDialog.showProgressSpinIndicator(mActivity);
        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                tc.setDisabled();//disableAsyncTask();
                mUtil.addDebugMsg(1, "W", "createSafApiFileList cancelled.");
            }
        });
        dialog.show();

        final Handler hndl=new Handler();
        Thread th=new Thread(){
            @Override
            public void run(){
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                SimpleDateFormat sdf_ut = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                sdf_ut.setTimeZone(TimeZone.getTimeZone("UTC"));
//                long b_time=System.currentTimeMillis();
                ArrayList<FileListItem> dir = new ArrayList<FileListItem>();
                ArrayList<FileListItem> fls = new ArrayList<FileListItem>();
                SafFile3 sf=new SafFile3(mContext, url);
                ContentProviderClient cpc=sf.getContentProviderClient();
//                mUtil.addDebugMsg(1,"I","list1 elapsed="+(System.currentTimeMillis()-b_time));
                if (sf!=null) {
                    SafFile3[] file_list = sf.listFiles(cpc);
                    if (file_list!=null) {
                        try {
                            for (SafFile3 ff : file_list) {
                                long b_time_s=System.currentTimeMillis();
                                FileListItem tfi=null;
                                if (ff.isDirectory(cpc)) {
                                    int sdc=0;
                                    int ll=0;
                                    sdc=ff.getCount(cpc);
                                    tfi= createSafApiFilelistItem("", ff, sdc, ll, true, url, cpc);
                                    dir.add(tfi);
                                } else {
                                    tfi= createSafApiFilelistItem("", ff, 0, 0, false, url, cpc);
                                    fls.add(tfi);
                                }
                                long et=tfi.getLastModified();
                                String ut=sdf_ut.format(et);
                                if (tfi.isDirectory()) {
                                    mUtil.addDebugMsg(3,"I","Directory=" + tfi.getName()+", "+
                                            "length=" + tfi.getLength()+", "+
                                            "Last mod Local=" + sdf.format(tfi.getLastModified())+", "+
                                            "Last mod UTC=" + ut+", "+
                                            "Last mod EPOC=" + et+", "+
                                            "path=" + tfi.getPath()+", ");
                                } else {
                                    mUtil.addDebugMsg(3,"I","File=" + tfi.getName()+", "+
                                            "length=" + tfi.getLength()+", "+
                                            "Last mod Local=" + sdf.format(tfi.getLastModified())+", "+
                                            "Last mod UTC=" + ut+", "+
                                            "Last mod EPOC=" + et+", "+
                                            "path=" + tfi.getPath()+", ");
                                }
//                                mUtil.addDebugMsg(1,"I","fp="+ff.getPath()+", elapse="+(System.currentTimeMillis()-b_time_s));
                            }
//                            mUtil.addDebugMsg(1,"I","list2 elapsed="+(System.currentTimeMillis()-b_time));
                        } catch (Exception e) {
                            e.printStackTrace();
                            mUtil.addDebugMsg(1,"E",e.toString());
                            showDialogMsg("E",mContext.getString(R.string.msgs_local_file_list_create_error), e.getMessage());
                            dir=null;
                        }
                    }
                    cpc.release();
                    if (dir!=null) {
                        FileListAdapter.sort(dir);
                        if (!dir_only) {
                            FileListAdapter.sort(fls);
                            dir.addAll(fls);
                        }
                    }
                    mUtil.addDebugMsg(1,"I","createSafApiFileList file list create ended");
                    final Object[] result=new Object[]{dir};
                    hndl.post(new Runnable(){
                        @Override
                        public void run() {
                            p_ntfy.notifyToListener(true, result);
                            dialog.dismiss();
                        }
                    });

                }
            }
        };
        th.start();
    }

    private void createRemoteFileList(String opcd, final String url, final NotifyEvent parent_event) {
        mUtil.addDebugMsg(1,"I","Create remote filelist remote url:"+url);
//        Thread.dumpStack();
        final NotifyEvent n_event=new NotifyEvent(mContext);
        n_event.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c,Object[] o) {
                String itemname = "";
                @SuppressWarnings("unchecked")
                ArrayList<FileListItem> sf_item=(ArrayList<FileListItem>)o[0];

                ArrayList<FileListItem> dir = new ArrayList<FileListItem>();
                List<FileListItem> fls = new ArrayList<FileListItem>();

                for (int i = 0; i < sf_item.size(); i++) {
                    itemname = sf_item.get(i).getName();
                    if (itemname.equals("IPC$")) {
                        // ignore IPC$
                    } else {
                        if (sf_item.get(i).canRead()) {
                            if (sf_item.get(i).isDirectory()) {
                                dir.add(createNewFilelistItem(mGp.remoteMountpoint, sf_item.get(i)));
                            } else {
                                fls.add(createNewFilelistItem(mGp.remoteMountpoint, sf_item.get(i)));
                            }
                        } else {
                            fls.add(createNewFilelistItem(mGp.remoteMountpoint, sf_item.get(i)));
                        }
                    }
                }

                dir.addAll(fls);
                FileListAdapter.sort(dir);
                parent_event.notifyToListener(true, new Object[]{dir});
            }

            @Override
            public void negativeResponse(Context c,Object[] o) {
                parent_event.notifyToListener(false, o);
                showDialogMsg("E",
                        mContext.getString(R.string.msgs_remote_file_list_create_error),(String)o[0]);
            }
        });
        SmbServerUtil.createSmbServerFileList(mActivity, mGp, opcd, url, mGp.currentSmbServerConfig,n_event);

    }

    private static String buildFullPath(String base, String dir) {
        String t_dir="";
        if (dir.equals("")) t_dir=base;
        else t_dir=base+"/"+dir;
        return t_dir;
    }


    private static String formatRemoteSmbUrl(String url) {
        String result="";
        String smb_url=url.replace("smb://", "");
        result="smb://"+smb_url.replaceAll("///", "/").replaceAll("//", "/");
        return result;
    }

    private void checkRemoteFileExists(String url, ArrayList<String> d_list, SmbServerConfig sc, final NotifyEvent n_event) {
        final ArrayList<FileListItem> remoteFileList=new ArrayList<FileListItem>();

        final ThreadCtrl tc = new ThreadCtrl();
        remoteFileList.clear();
        tc.setEnabled();

        Dialog prog_spin=CommonDialog.showProgressSpinIndicator(mActivity);
        prog_spin.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                tc.setDisabled();//disableAsyncTask();
                mUtil.addDebugMsg(1,"W","Filelist is cancelled.");
            }
        });
        prog_spin.show();

        NotifyEvent ne=new NotifyEvent(mContext);
        ne.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c,Object[] o) {
                prog_spin.dismiss();
                if (tc.isThreadResultSuccess()) {
                    n_event.notifyToListener(true, o);
                } else {
                    String err="";
                    if (tc.isThreadResultCancelled()) err="Filelist was cancelled";
                    else err=tc.getThreadMessage();
                    n_event.notifyToListener(false, new Object[]{err});
                }
            }
            @Override
            public void negativeResponse(Context c,Object[] o) {
                prog_spin.dismiss();
            }
        });

        Thread th = new RetrieveFileList(mGp, tc, url, d_list, sc, ne);
        th.start();
    }

    public void showRemoteProgressView() {
        mActivity.setUiEnabled(false);
        mGp.remoteProgressView.setVisibility(LinearLayout.VISIBLE);
        mGp.remoteProgressView.setBackgroundColor(mGp.dialogBackgroundColor);
        mGp.remoteProgressView.bringToFront();
    }

    private void hideRemoteProgressView() {
        mActivity.setUiEnabled(true);
        mGp.remoteProgressView.setVisibility(LinearLayout.GONE);
    }

    public void showRemoteDialogView() {
        mActivity.setUiEnabled(false);
        mGp.remoteDialogView.setVisibility(LinearLayout.VISIBLE);
        mGp.remoteDialogView.setBackgroundColor(mGp.dialogBackgroundColor);
        mGp.remoteDialogView.bringToFront();
    }

    private void hideRemoteDialogView() {
        mActivity.setUiEnabled(true);
        mGp.remoteDialogView.setVisibility(LinearLayout.GONE);
    }

    public void showLocalDialogView() {
        mActivity.setUiEnabled(false);
        mGp.localDialogView.setVisibility(LinearLayout.VISIBLE);
        mGp.localDialogView.setBackgroundColor(mGp.dialogBackgroundColor);
        mGp.localDialogView.bringToFront();
    }

    private void hideLocalDialogView() {
        mActivity.setUiEnabled(true);
        mGp.localDialogView.setVisibility(LinearLayout.GONE);
    }

    public void showLocalProgressView() {
        mActivity.setUiEnabled(false);
        mGp.localProgressView.setVisibility(LinearLayout.VISIBLE);
        mGp.localProgressView.setBackgroundColor(mGp.dialogBackgroundColor);
        mGp.localProgressView.bringToFront();
    }

    private void hideLocalProgressView() {
        mActivity.setUiEnabled(true);
        mGp.localProgressView.setVisibility(LinearLayout.GONE);
    }

    private ArrayList<LocalStorage> createLocalProfileEntry() {
        ArrayList<LocalStorage> lcl = new ArrayList<LocalStorage>();
        ArrayList<SafStorage3>svl=mGp.safMgr.getSafStorageList();
        for(SafStorage3 sli:svl) {
            LocalStorage lstg=new LocalStorage();
            lstg.storage_saf_file =sli.isSafFile;
            lstg.storage_name=sli.description;
            lstg.storage_root=sli.saf_file;
            lstg.storage_app_directory=sli.appDirectory;
            lstg.storage_mount_point ="";
            lstg.storage_id=sli.saf_file.getUuid();
            lcl.add(lstg);
        }
        return lcl;
    }

    static public FileListItem createNewFilelistItem(String base_url, FileListItem tfli) {
        FileListItem fi=null;
        if (tfli.isDirectory()) {
            fi= new FileListItem(tfli.getName(),
                    true,
                    0,
                    tfli.getLastModified(),
                    false,
                    tfli.canRead(),tfli.canWrite(),
                    tfli.isHidden(),tfli.getPath(),
                    tfli.getListLevel());
            fi.setSubDirItemCount(tfli.getSubDirItemCount());
            fi.setHasExtendedAttr(tfli.hasExtendedAttr());
            fi.setBaseUrl(base_url);;
        } else {
            fi=new FileListItem(tfli.getName(),
                    false,
                    tfli.getLength(),
                    tfli.getLastModified(),
                    false,
                    tfli.canRead(),tfli.canWrite(),
                    tfli.isHidden(),tfli.getPath(),
                    tfli.getListLevel());
            fi.setHasExtendedAttr(tfli.hasExtendedAttr());
            fi.setBaseUrl(base_url);;
        }
        return fi;
    }

    public FileListItem createNewFilelistItem(String base_url, SafFile3 tfli, int sdc, int ll, boolean dir, String parent) {
//        Thread.dumpStack();
        if (dir) {
            String fn=tfli.getName();
            boolean is_hidden=fn.startsWith(".")?true:false;
            final FileListItem fi= new FileListItem(fn,
                    true,
                    -1,
                    tfli.lastModified(),
                    false,
                    true, false,//tfli.canRead(),tfli.canWrite(),
                    is_hidden, parent, //tfli.isHidden(),tfli.getParent(),
                    ll);
            fi.setSubDirItemCount(sdc);
            fi.setBaseUrl(base_url);;
            Thread th=new Thread(){
                @Override
                public void run() {
                    SafFile3 lf=new SafFile3(mContext, tfli.getPath());
                    if (lf.canRead()) {
                        long dir_size= getSafApiAllFileSizeInDirectory(lf, true);
                        fi.setLength(dir_size);
                    } else {
                        long dir_size= getSafApiAllFileSizeInDirectory(tfli, true);
                        fi.setLength(dir_size);
                    }
                    mUiHandler.post(new Runnable(){
                        @Override
                        public void run(){
                            mGp.localFileListAdapter.notifyDataSetChanged();
                        }
                    }) ;
                }
            };
            th.start();
            return fi;
        } else {
            long[] lmod_length=tfli.getLastModifiedAndLength();
            String fn=tfli.getName();
            boolean is_hidden=fn.startsWith(".")?true:false;
            FileListItem fi=new FileListItem(fn,
                    false,
                    lmod_length[1],
                    lmod_length[0],
                    false,
                    true,false,//tfli.canRead(),tfli.canWrite(),
                    is_hidden, parent,//tfli.isHidden(),tfli.getParent(),
                    ll);
            fi.setBaseUrl(base_url);;
            return fi;
        }

    }

    public FileListItem createFileApiFilelistItem(String base_url, File tfli, int sdc, int ll, boolean dir, String parent) {
//        Thread.dumpStack();
        if (dir) {
            String fn=tfli.getName();
            boolean is_hidden=fn.startsWith(".")?true:false;
            final FileListItem fi= new FileListItem(fn,
                    true,
                    -1,
                    tfli.lastModified(),
                    false,
                    true, false,//tfli.canRead(),tfli.canWrite(),
                    is_hidden, parent, //tfli.isHidden(),tfli.getParent(),
                    ll);
            fi.setSubDirItemCount(sdc);
            fi.setBaseUrl(base_url);;
            Thread th=new Thread(){
                @Override
                public void run() {
                    File lf=new File(tfli.getPath());
                    if (lf.canRead()) {
                        long dir_size= getFileApiAllFileSizeInDirectory(lf, true);
                        fi.setLength(dir_size);
                    } else {
                        long dir_size= getFileApiAllFileSizeInDirectory(tfli, true);
                        fi.setLength(dir_size);
                    }
                    mUiHandler.post(new Runnable(){
                        @Override
                        public void run(){
                            mGp.localFileListAdapter.notifyDataSetChanged();
                        }
                    }) ;
                }
            };
            th.start();
            return fi;
        } else {
            String fn=tfli.getName();
            boolean is_hidden=fn.startsWith(".")?true:false;
            FileListItem fi=new FileListItem(fn,
                    false,
                    tfli.length(),
                    tfli.lastModified(),
                    false,
                    true,false,//tfli.canRead(),tfli.canWrite(),
                    is_hidden, parent,//tfli.isHidden(),tfli.getParent(),
                    ll);
            fi.setBaseUrl(base_url);;
            return fi;
        }

    }

    public FileListItem createSafApiFilelistItem(String base_url, final SafFile3 tfli, int sdc, int ll, boolean dir, String parent, ContentProviderClient cpc) {
//        Thread.dumpStack();
        String fn=tfli.getName();
        boolean is_hidden=fn.startsWith(".")?true:false;
        if (dir) {
            final FileListItem fi= new FileListItem(tfli.getName(),
                    true,
                    -1,
                    tfli.lastModified(cpc),
                    false,
                    true, true,//tfli.canRead(),tfli.canWrite(),
                    is_hidden, parent, //tfli.isHidden(),tfli.getParent(),
                    ll);
            fi.setSubDirItemCount(sdc);
            fi.setBaseUrl(base_url);;
            Thread th=new Thread(){
                @Override
                public void run() {
//                    SafFile3 lf=new SafFile3(mContext, tfli.getPath());
                    long dir_size=getSafApiAllFileSizeInDirectory(tfli, true, cpc);
                    fi.setLength(dir_size);
//                    if (tfli.canRead()) {
//                        long dir_size=getSafApiAllFileSizeInDirectory(tfli, true, cpc);
//                        fi.setLength(dir_size);
//                    } else {
//                        long dir_size=getSafApiAllFileSizeInDirectory(tfli, true, cpc);
//                        fi.setLength(dir_size);
//                    }
                    mUiHandler.post(new Runnable(){
                        @Override
                        public void run(){
                            mGp.localFileListAdapter.notifyDataSetChanged();
                        }
                    }) ;
                }
            };
            th.setPriority(Thread.MAX_PRIORITY);
            th.start();
            return fi;
        } else {
            long[] lmod_length=tfli.getLastModifiedAndLength(cpc);
            final FileListItem fi=new FileListItem(tfli.getName(),
                    false,
                    lmod_length[1],
                    lmod_length[0],
                    false,
                    true,true,//tfli.canRead(),tfli.canWrite(),
                    is_hidden, parent,//tfli.isHidden(),tfli.getParent(),
                    ll);
            fi.setBaseUrl(base_url);;

//            Thread th=new Thread(){
//                @Override
//                public void run() {
//                    long[] lmod_length=tfli.getLastModifiedAndLength(cpc);
//                    fi.setLength(lmod_length[1]);
//                    fi.setLastModified(lmod_length[0]);
//                    mUiHandler.post(new Runnable(){
//                        @Override
//                        public void run(){
//                            mGp.localFileListAdapter.notifyDataSetChanged();
//                        }
//                    }) ;
//                }
//            };
//            th.setPriority(Thread.MAX_PRIORITY);
//            th.start();
            return fi;
        }
    }

    static public FileListItem createNewFilelistItem(String base_url, JcifsFile tfli, int sdc, int ll) throws JcifsException {
        FileListItem fi=null;
        boolean has_ea=tfli.getAttributes()<16384?false:true;
        try {
            String fp=tfli.getParent();
            if (fp.endsWith("/")) fp=fp.substring(0,fp.lastIndexOf("/"));
            if (tfli.isDirectory()) {
                fi= new FileListItem(tfli.getName(),
                        true,
                        0,
                        tfli.getLastModified(),
                        false,
                        tfli.canRead(),tfli.canWrite(),
                        tfli.isHidden(),fp,
                        ll);
                fi.setSubDirItemCount(sdc);
                fi.setHasExtendedAttr(has_ea);
                fi.setBaseUrl(base_url);
            } else {
                fi=new FileListItem(tfli.getName(),
                        false,
                        tfli.length(),
                        tfli.getLastModified(),
                        false,
                        tfli.canRead(),tfli.canWrite(),
                        tfli.isHidden(),fp,
                        ll);
                fi.setHasExtendedAttr(has_ea);
                fi.setBaseUrl(base_url);
            }
        } catch(JcifsException e) {

        }
        return fi;
    }

    public static class MountPointHistoryItem implements Serializable {
        public String mp_name="";
        public ArrayList<DirectoryHistoryItem> directory_history=new ArrayList<DirectoryHistoryItem>();
    }
}
