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
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.sentaroh.android.Utilities3.ContextButton.ContextButtonUtil;
import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.NotifyEvent;

import java.util.ArrayList;
import java.util.Comparator;

import static android.view.KeyEvent.KEYCODE_BACK;

/**
 * Created by sentaroh on 2018/03/07.
 */

public class SmbServerListEditor {
    private CommonDialog commonDlg = null;

    private GlobalParameter mGp = null;
    private Context mContext = null;
    private ActivityMain mActivity = null;
    private CommonUtilities mUtil = null;
    private SmbServerListAdapter mSmbServerListAdapter = null;

    private boolean mAdapterChanged = false;

    private Dialog mDialog = null;

    private ImageButton mContextButtonAdd = null;
    private ImageButton mContextButtonCopy = null;
    private ImageButton mContextButtonRename = null;
    private ImageButton mContextButtonDelete = null;
    private ImageButton mContextButtonSelectAll = null;
    private ImageButton mContextButtonUnselectAll = null;

    private LinearLayout mContextButtonAddView = null;
    private LinearLayout mContextButtonCopyView = null;
    private LinearLayout mContextButtonRenameView = null;
    private LinearLayout mContextButtonDeleteView = null;
    private LinearLayout mContextButtonSelectAllView = null;
    private LinearLayout mContextButtonUnselectAllView = null;

    private ArrayList<SmbServerConfig> mSmbConfigList =new ArrayList<SmbServerConfig>();

    public SmbServerListEditor(ActivityMain a, GlobalParameter gp) {
        mContext = gp.context;
        mActivity = a;
        mGp = gp;
        mUtil = gp.mUtil;
        commonDlg = gp.commonDlg;
        initDialog();
    }

    private void initDialog() {
        // カスタムダイアログの生成
        mDialog = new Dialog(mActivity);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setContentView(R.layout.smb_server_list_edit_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) mDialog.findViewById(R.id.smb_server_list_edit_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        LinearLayout title_view = (LinearLayout) mDialog.findViewById(R.id.smb_server_list_edit_dlg_title_view);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        TextView dlg_title = (TextView) mDialog.findViewById(R.id.smb_server_list_edit_dlg_title);
        dlg_title.setTextColor(mGp.themeColorList.title_text_color);

        CommonDialog.setDlgBoxSizeLimit(mDialog, true);

        final ImageButton btn_ok = (ImageButton) mDialog.findViewById(R.id.smb_server_list_edit_dlg_save);
//        btn_ok.setBackgroundColor(Color.DKGRAY);
        final ImageButton btn_cancel = (ImageButton) mDialog.findViewById(R.id.smb_server_list_edit_dlg_close);
        btn_cancel.setBackgroundColor(Color.TRANSPARENT);//.DKGRAY);
        final TextView tv_msg = (TextView) mDialog.findViewById(R.id.smb_server_list_edit_dlg_msg);

        final ListView lv = (ListView) mDialog.findViewById(R.id.smb_server_list_edit_dlg_smb_server_list_view);

        mSmbConfigList.addAll(mGp.smbConfigList);
        mSmbServerListAdapter = new SmbServerListAdapter(mActivity, R.layout.smb_server_list_edit_list_item, mSmbConfigList);
        lv.setAdapter(mSmbServerListAdapter);

        tv_msg.setText("No SMB server definition");
        if (mSmbServerListAdapter.getCount() == 0) {
            tv_msg.setVisibility(TextView.VISIBLE);
        }

        createContextView(mDialog);
        setContextButtonListener(mDialog);

        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                mSmbServerListAdapter.setSelectMode(true);
                mSmbServerListAdapter.getItem(i).setChecked(true);
                mSmbServerListAdapter.notifyDataSetChanged();
                setContextButtonMode(mDialog, mSmbServerListAdapter);
                return true;
            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                Log.v("","before="+adapter.getItem(i).scheduleName);
                if (mSmbServerListAdapter.isSelectMode()) {
                    if (mSmbServerListAdapter.getItem(i).isChecked()) {
                        mSmbServerListAdapter.getItem(i).setChecked(false);
                    } else {
                        mSmbServerListAdapter.getItem(i).setChecked(true);
                    }
                    mSmbServerListAdapter.notifyDataSetChanged();
                    setContextButtonMode(mDialog, mSmbServerListAdapter);
                } else {
                    final SmbServerConfig ssi=mSmbServerListAdapter.getItem(i);
                    NotifyEvent ntfy = new NotifyEvent(mContext);
                    ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            SmbServerConfig pfli=(SmbServerConfig)objects[0];
                            mSmbServerListAdapter.remove(ssi);
                            mSmbServerListAdapter.add(pfli);
                            mSmbServerListAdapter.sort();
                            mSmbServerListAdapter.notifyDataSetChanged();
                            btn_ok.setEnabled(true);
                            btn_ok.setAlpha(1.0f);
                        }

                        @Override
                        public void negativeResponse(Context context, Object[] objects) {
                        }
                    });
                    SmbServerEditor sm = new SmbServerEditor("EDIT", mActivity, mGp, ssi, ntfy);
                }
            }
        });
        btn_ok.setEnabled(false);
        btn_ok.setAlpha(0.3f);

        setContextButtonMode(mDialog, mSmbServerListAdapter);

        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                setContextButtonMode(mDialog, mSmbServerListAdapter);
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {
            }
        });
        mSmbServerListAdapter.setCbNotify(ntfy);

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSmbServerListAdapter.sort();
                mGp.smbConfigList.clear();
                mGp.smbConfigList.addAll(mSmbConfigList);
                SmbServerUtil.saveSmbServerConfigList(mContext, mGp);
                SmbServerUtil.updateSmbShareSpinner(mGp);
                SmbServerUtil.replaceCurrentSmbServerConfig(mGp);
                mDialog.dismiss();
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btn_ok.isEnabled()) {
                    NotifyEvent ntfy=new NotifyEvent(mContext);
                    ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            mDialog.dismiss();
                        }
                        @Override
                        public void negativeResponse(Context context, Object[] objects) {}
                    });
                    CommonDialog cd=new CommonDialog(mContext, mActivity.getSupportFragmentManager());
                    cd.showCommonDialog(true, "W", "Some config were changed, do you want to quit without save.", "", ntfy);
                } else {
                    mDialog.dismiss();
                }
            }
        });

        mDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int kc, KeyEvent keyEvent) {
                switch (kc) {
                    case KEYCODE_BACK:
                        if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                            if (mSmbServerListAdapter.isSelectMode()) {
                                mSmbServerListAdapter.setSelectMode(false);
                                setContextButtonMode(mDialog, mSmbServerListAdapter);
                            } else {
                                btn_cancel.performClick();
                            }
                        }
                        return true;
                    default:
                }
                return false;
            }
        });
        mDialog.show();
    }

    private void setContextButtonMode(Dialog dialog, SmbServerListAdapter adapter) {
        boolean selected = false;
        int sel_cnt = 0;
        boolean enabled = false, disabled = false;
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).isChecked()) {
                selected = true;
                sel_cnt++;
            }
        }

        mContextButtonAddView.setVisibility(LinearLayout.VISIBLE);
        mContextButtonCopyView.setVisibility(LinearLayout.INVISIBLE);
        mContextButtonRenameView.setVisibility(LinearLayout.INVISIBLE);
        mContextButtonDeleteView.setVisibility(LinearLayout.INVISIBLE);
        mContextButtonSelectAllView.setVisibility(LinearLayout.VISIBLE);
        mContextButtonUnselectAllView.setVisibility(LinearLayout.INVISIBLE);

//        final ImageButton btn_ok = (ImageButton) mDialog.findViewById(R.id.smb_server_list_edit_dlg_save);
//        final ImageButton btn_cancel = (ImageButton) mDialog.findViewById(R.id.smb_server_list_edit_dlg_close);

        if (mSmbServerListAdapter.isSelectMode()) {
            if (sel_cnt == 0) {
                mContextButtonAddView.setVisibility(LinearLayout.INVISIBLE);
                mContextButtonCopyView.setVisibility(LinearLayout.INVISIBLE);
                mContextButtonRenameView.setVisibility(LinearLayout.INVISIBLE);
                mContextButtonDeleteView.setVisibility(LinearLayout.INVISIBLE);
                mContextButtonSelectAllView.setVisibility(LinearLayout.VISIBLE);
                mContextButtonUnselectAllView.setVisibility(LinearLayout.INVISIBLE);
            } else if (sel_cnt == 1) {
                mContextButtonAddView.setVisibility(LinearLayout.INVISIBLE);
                mContextButtonCopyView.setVisibility(LinearLayout.VISIBLE);
                mContextButtonRenameView.setVisibility(LinearLayout.VISIBLE);
                mContextButtonDeleteView.setVisibility(LinearLayout.VISIBLE);
                mContextButtonSelectAllView.setVisibility(LinearLayout.VISIBLE);
                mContextButtonUnselectAllView.setVisibility(LinearLayout.VISIBLE);
            } else if (sel_cnt >= 2) {
                mContextButtonAddView.setVisibility(LinearLayout.INVISIBLE);
                mContextButtonCopyView.setVisibility(LinearLayout.INVISIBLE);
                mContextButtonRenameView.setVisibility(LinearLayout.INVISIBLE);
                mContextButtonDeleteView.setVisibility(LinearLayout.VISIBLE);
                mContextButtonSelectAllView.setVisibility(LinearLayout.VISIBLE);
                mContextButtonUnselectAllView.setVisibility(LinearLayout.VISIBLE);
            }
        } else {
            mContextButtonAddView.setVisibility(LinearLayout.VISIBLE);
            mContextButtonCopyView.setVisibility(LinearLayout.INVISIBLE);
            mContextButtonRenameView.setVisibility(LinearLayout.INVISIBLE);
            mContextButtonDeleteView.setVisibility(LinearLayout.INVISIBLE);
            mContextButtonUnselectAllView.setVisibility(LinearLayout.INVISIBLE);
            if (adapter.getCount() == 0) {
                mContextButtonSelectAllView.setVisibility(LinearLayout.INVISIBLE);
            }
        }

    }

    private void setContextButtonEnabled(final ImageButton btn, boolean enabled) {
        if (enabled) {
            btn.postDelayed(new Runnable() {
                @Override
                public void run() {
                    btn.setEnabled(true);
                }
            }, 1000);
        } else {
            btn.setEnabled(false);
        }
    }

    private void setContextButtonListener(final Dialog dialog) {
        final TextView tv_msg = (TextView) dialog.findViewById(R.id.smb_server_list_edit_dlg_msg);
        final ImageButton btn_ok = (ImageButton) mDialog.findViewById(R.id.smb_server_list_edit_dlg_save);

        mContextButtonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        SmbServerConfig si = (SmbServerConfig) objects[0];
                        mSmbServerListAdapter.add(si);
                        mSmbServerListAdapter.sort();
                        mSmbServerListAdapter.notifyDataSetChanged();
                        tv_msg.setVisibility(TextView.GONE);
                        setContextButtonMode(dialog, mSmbServerListAdapter);
                        btn_ok.setEnabled(true);
                        btn_ok.setAlpha(1.0f);
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                SmbServerEditor sm = new SmbServerEditor("ADD", mActivity, mGp, new SmbServerConfig(), ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextButtonAdd, "Add");

        mContextButtonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        for (int i = mSmbServerListAdapter.getCount() - 1; i >= 0; i--) {
                            if (mSmbServerListAdapter.getItem(i).isChecked()) {
                                mSmbServerListAdapter.remove(mSmbServerListAdapter.getItem(i));
                            }
                        }
                        if (mSmbServerListAdapter.getCount() == 0) {
                            mSmbServerListAdapter.setSelectMode(false);
                            tv_msg.setVisibility(TextView.VISIBLE);
                        }
                        setContextButtonMode(dialog, mSmbServerListAdapter);
                        mSmbServerListAdapter.notifyDataSetChanged();
                        btn_ok.setEnabled(true);
                        btn_ok.setAlpha(1.0f);
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                String del_list = "";
                for (int i = 0; i < mSmbServerListAdapter.getCount(); i++) {
                    if (mSmbServerListAdapter.getItem(i).isChecked()) {
                        del_list += mSmbServerListAdapter.getItem(i).getName() + "\n";
                    }
                }
                commonDlg.showCommonDialog(true, "W",
                        "Delete SMB server",
                        "Do you want delete following item(s). " + "\n" + del_list, ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextButtonDelete, "Delete");

        mContextButtonRename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
//                        mSmbServerListAdapter.setSelectMode(false);
                        mSmbServerListAdapter.sort();
                        mSmbServerListAdapter.unselectAll();
                        setContextButtonMode(dialog, mSmbServerListAdapter);
                        btn_ok.setEnabled(true);
                        btn_ok.setAlpha(1.0f);
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                String del_list = "";
                SmbServerConfig si = null;
                for (int i = mSmbServerListAdapter.getCount() - 1; i >= 0; i--) {
                    if (mSmbServerListAdapter.getItem(i).isChecked()) {
                        del_list += mSmbServerListAdapter.getItem(i).getName() + "\n";
                        si = mSmbServerListAdapter.getItem(i);
                        break;
                    }
                }
                renameItem(si, ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextButtonRename, "Rename");

        mContextButtonCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        SmbServerConfig si = (SmbServerConfig) objects[0];
//                        mSmbServerListAdapter.setSelectMode(false);
                        mSmbServerListAdapter.add(si);
                        mSmbServerListAdapter.unselectAll();
                        mSmbServerListAdapter.sort();
                        btn_ok.setEnabled(true);
                        btn_ok.setAlpha(1.0f);
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                SmbServerConfig si = null;
                for (int i = mSmbServerListAdapter.getCount() - 1; i >= 0; i--) {
                    if (mSmbServerListAdapter.getItem(i).isChecked()) {
                        si = mSmbServerListAdapter.getItem(i);
                        break;
                    }
                }
                SmbServerConfig new_si = si.clone();
                SmbServerEditor sm = new SmbServerEditor("COPY", mActivity, mGp, new_si, ntfy);

            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextButtonCopy, "Copy");

        mContextButtonSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSmbServerListAdapter.setSelectMode(true);
                mSmbServerListAdapter.selectAll();
                setContextButtonMode(dialog, mSmbServerListAdapter);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextButtonSelectAll, "Select all");

        mContextButtonUnselectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSmbServerListAdapter.setSelectMode(false);
                mSmbServerListAdapter.unselectAll();
                setContextButtonMode(dialog, mSmbServerListAdapter);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextButtonUnselectAll, "Unselect all");

    }

    private void renameItem(final SmbServerConfig si, final NotifyEvent p_ntfy) {

        // カスタムダイアログの生成
        final Dialog dialog = new Dialog(mActivity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.single_item_input_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.single_item_input_title);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        title.setTextColor(mGp.themeColorList.title_text_color);

//		final TextView dlg_msg = (TextView) dialog.findViewById(R.id.single_item_input_msg);
        final TextView dlg_cmp = (TextView) dialog.findViewById(R.id.single_item_input_name);
        final Button btn_ok = (Button) dialog.findViewById(R.id.single_item_input_ok_btn);
        final Button btn_cancel = (Button) dialog.findViewById(R.id.single_item_input_cancel_btn);
        final EditText etInput = (EditText) dialog.findViewById(R.id.single_item_input_dir);

        title.setText("Rename SMB server");

        dlg_cmp.setVisibility(TextView.GONE);
        CommonDialog.setDlgBoxSizeCompact(dialog);
        etInput.setText(si.getName());
        btn_ok.setEnabled(false);
        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                if(SmbServerUtil.getSmbServerConfigItem(arg0.toString(), mGp.smbConfigList)==null) btn_ok.setEnabled(true);
                else btn_ok.setEnabled(false);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }
        });

        //OK button
        btn_ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                String new_name = etInput.getText().toString();

                si.setName(new_name);

                p_ntfy.notifyToListener(true, null);
            }
        });
        // CANCELボタンの指定
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        // Cancelリスナーの指定
        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btn_cancel.performClick();
            }
        });
        dialog.show();

    }

    private void createContextView(Dialog dialog) {
        mContextButtonAddView = (LinearLayout) dialog.findViewById(R.id.context_button_add_view);
        mContextButtonCopyView = (LinearLayout) dialog.findViewById(R.id.context_button_copy_view);
        mContextButtonRenameView = (LinearLayout) dialog.findViewById(R.id.context_button_rename_view);
        mContextButtonDeleteView = (LinearLayout) dialog.findViewById(R.id.context_button_delete_view);
        mContextButtonSelectAllView = (LinearLayout) dialog.findViewById(R.id.context_button_select_all_view);
        mContextButtonUnselectAllView = (LinearLayout) dialog.findViewById(R.id.context_button_unselect_all_view);

        mContextButtonAdd = (ImageButton) dialog.findViewById(R.id.context_button_add);
        mContextButtonCopy = (ImageButton) dialog.findViewById(R.id.context_button_copy);
        mContextButtonRename = (ImageButton) dialog.findViewById(R.id.context_button_rename);
        mContextButtonDelete = (ImageButton) dialog.findViewById(R.id.context_button_delete);
        mContextButtonSelectAll = (ImageButton) dialog.findViewById(R.id.context_button_select_all);
        mContextButtonUnselectAll = (ImageButton) dialog.findViewById(R.id.context_button_unselect_all);
    }

    private class SmbServerListAdapter extends ArrayAdapter<SmbServerConfig> {
        private int layout_id = 0;
        private Context context = null;
        private int text_color = 0;
        private NotifyEvent mCbNotify = null;
        private ArrayList<SmbServerConfig> mScheduleList = null;

        public SmbServerListAdapter(Context c, int textViewResourceId, ArrayList<SmbServerConfig> sl) {
            super(c, textViewResourceId, sl);
            layout_id = textViewResourceId;
            context = c;
            mScheduleList = sl;
        }

        public void setCbNotify(NotifyEvent ntfy) {
            mCbNotify = ntfy;
        }

        public void sort() {
            sort(new Comparator<SmbServerConfig>() {
                @Override
                public int compare(SmbServerConfig lhs, SmbServerConfig rhs) {
                    return lhs.getName().compareToIgnoreCase(rhs.getName());
                }
            });
            notifyDataSetChanged();
        }

        public void selectAll() {
            for (SmbServerConfig si : mScheduleList) si.setChecked(true);
            notifyDataSetChanged();
        }

        public void unselectAll() {
            for (SmbServerConfig si : mScheduleList) {
                si.setChecked(false);
            }
            notifyDataSetChanged();
        }

        private boolean mSelectMode = false;

        public void setSelectMode(boolean select_mode) {
            mSelectMode = select_mode;
            if (!mSelectMode) unselectAll();
        }

        public boolean isSelectMode() {
            return mSelectMode;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            final ViewHolder holder;
            final SmbServerConfig o = getItem(position);
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(layout_id, null);
                holder = new ViewHolder();
                holder.tv_name = (TextView) v.findViewById(R.id.smb_server_list_name);
                holder.tv_info = (TextView) v.findViewById(R.id.smb_server_list_info);
                holder.cbChecked = (CheckBox) v.findViewById(R.id.smb_server_list_checked);
                text_color = holder.tv_name.getCurrentTextColor();
                v.setTag(holder);
            } else {
                holder = (ViewHolder) v.getTag();
            }
            if (o != null) {
                holder.tv_name.setText(o.getName());
                holder.tv_info.setText(o.getSmbHost()+", "+o.getSmbShare()+", SMB="+o.getSmbLevel());

                if (mSelectMode) {
                    holder.cbChecked.setVisibility(CheckBox.VISIBLE);
                } else {
                    holder.cbChecked.setVisibility(CheckBox.INVISIBLE);
                }

                holder.cbChecked.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean isChecked = holder.cbChecked.isChecked();
                        o.setChecked(isChecked);
                        if (mCbNotify != null)
                            mCbNotify.notifyToListener(true, new Object[]{isChecked});
                    }
                });
                holder.cbChecked.setChecked(o.isChecked());
            }
            return v;

        }

        class ViewHolder {
            TextView tv_name, tv_info, tv_enabled, tv_time_info;
            CheckBox cbChecked;
        }
    }


}