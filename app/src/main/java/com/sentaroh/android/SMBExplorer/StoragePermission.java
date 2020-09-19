package com.sentaroh.android.SMBExplorer;
/*
The MIT License (MIT)
Copyright (c) 2011-2019 Sentaroh

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
import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.SafManager3;

import java.util.ArrayList;

public class StoragePermission {

    private Context mContext=null;
    private ActivityMain mActivity=null;
    private GlobalParameter mGp=null;
    private CommonUtilities mUtil=null;
    private CommonDialog commonDlg=null;
    
    private Dialog mDialog=null;
    
    public StoragePermission(ActivityMain a, GlobalParameter gp) {
        mContext = gp.context;
        mActivity = a;
        mGp = gp;
        mUtil = gp.mUtil;
        commonDlg = gp.commonDlg;
    }

    public boolean isStoragePermissionGranted() {
        ArrayList<String>rows=buildStoragePermissionRequiredList();
        if (rows.size()>0) return true;
        else return false;
    }

    private ArrayList<String> buildStoragePermissionRequiredList() {
        final ArrayList<SafManager3.StorageVolumeInfo>svi_list= SafManager3.getStorageVolumeInfo(mContext);
        final ArrayList<String>rows=new ArrayList<String>();

        for(SafManager3.StorageVolumeInfo ssi:svi_list) {
            if (!mGp.safMgr.isUuidRegistered(ssi.uuid)) {
                if (ssi.uuid.equals(SafManager3.SAF_FILE_PRIMARY_UUID)) {
                    //NOP
                } else {
                    rows.add(ssi.description);
                }
            }
        }
        return rows;
    }

    public void showDialog() {
        initDialog();
    }

    private void initDialog() {
        // カスタムダイアログの生成
        mDialog = new Dialog(mActivity);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setContentView(R.layout.storage_permission_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) mDialog.findViewById(R.id.storage_permission_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        LinearLayout title_view = (LinearLayout) mDialog.findViewById(R.id.storage_permission_dlg_title_view);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        TextView dlg_title = (TextView) mDialog.findViewById(R.id.storage_permission_dlg_title);
        dlg_title.setTextColor(mGp.themeColorList.title_text_color);

        CommonDialog.setDlgBoxSizeLimit(mDialog, true);

        final Button btn_ok = (Button) mDialog.findViewById(R.id.storage_permission_dlg_ok);
//        btn_ok.setBackgroundColor(Color.DKGRAY);
        final Button btn_cancel = (Button) mDialog.findViewById(R.id.storage_permission_dlg_cancel);
//        btn_cancel.setBackgroundColor(Color.TRANSPARENT);//.DKGRAY);
        final TextView tv_msg = (TextView) mDialog.findViewById(R.id.storage_permission_dlg_msg);

        final ListView lv = (ListView) mDialog.findViewById(R.id.storage_permission_dlg_storage_list);
        final ArrayList<SafManager3.StorageVolumeInfo>svi_list=SafManager3.getStorageVolumeInfo(mContext);

        final ArrayList<String>rows=buildStoragePermissionRequiredList();
        if (rows.size()==0) {
            commonDlg.showCommonDialog(false, "W", "There was no storage requiring permissions.","",null);
            return;
        }

        lv.setAdapter(new ArrayAdapter<String>(mActivity, android.R.layout.simple_list_item_multiple_choice, rows));
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        lv.setScrollingCacheEnabled(false);
        lv.setScrollbarFadingEnabled(false);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                mUtil.addDebugMsg(1,"I", "clicked pos="+pos);
                SparseBooleanArray checked = lv.getCheckedItemPositions();
                boolean selected=false;
                for (int i = 0; i <= rows.size(); i++) {
                    if (checked.get(i) == true) {
                        selected=true;
                        break;
                    }
                }
                if (selected) CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                else CommonDialog.setViewEnabled(mActivity, btn_ok, false);
            }
        });

        CommonDialog.setViewEnabled(mActivity, btn_ok, false);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SparseBooleanArray checked = lv.getCheckedItemPositions();
                boolean selected=false;
                for (int i = 0; i <= rows.size(); i++) {
                    if (checked.get(i) == true) {
                        String r_desc=rows.get(i);
                        for(SafManager3.StorageVolumeInfo ssi:svi_list) {
                            if (ssi.description.equals(r_desc)) {
                                mActivity.requestStoragePermissionsByUuid(ssi.uuid);
                            }
                        }
                    }
                }
                mDialog.dismiss();
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });

        mDialog.show();
    }
}
