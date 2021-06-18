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

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.ThemeColorList;
import com.sentaroh.android.Utilities3.ThemeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class FileListAdapter extends BaseAdapter {
	private Activity mActivity;
	private Context mContext;
	private ArrayList<FileListItem>mDataItems=null;
	private boolean mSingleSelectMode=false;
	private boolean mShowLastModified=true;

    private NotifyEvent cb_ntfy=null;

    private int[] mIconImage= new int[] {R.drawable.cc_expanded,
			R.drawable.cc_collapsed,
			R.drawable.cc_folder,
			R.drawable.cc_sheet,
			R.drawable.cc_blank};
	
	private ThemeColorList mThemeColorList;

	public FileListAdapter(Activity a) {
		mActivity = a;
		mContext=mActivity.getApplicationContext();
		mDataItems=new ArrayList<FileListItem>();
		mThemeColorList= ThemeUtil.getThemeColorList(mActivity);
	};

	public FileListAdapter(Activity a, boolean singleSelectMode, boolean showLastModified) {
		mActivity = a;
		mContext=mActivity.getApplicationContext();
		this.mSingleSelectMode=singleSelectMode;
		this.mShowLastModified=showLastModified;
		mDataItems=new ArrayList<FileListItem>();
		mThemeColorList=ThemeUtil.getThemeColorList(mActivity);
	};
	
	@Override
	public int getCount() {return mDataItems.size();}

	@Override
	public FileListItem getItem(int arg0) {return mDataItems.get(arg0);}

	@Override
	public long getItemId(int arg0) {
		return 0;
	}

	public void setShowLastModified(boolean p) {
		mShowLastModified=p;
	};

	public void setSingleSelectMode(boolean p) {
		mSingleSelectMode=p;
	};

	public void setSelected(int pos, boolean selected) {
		if (mSingleSelectMode) setAllItemChecked(false);
		mDataItems.get(pos).setChecked(selected);
	};

	public boolean isSelected(int pos) {
		boolean result=mDataItems.get(pos).isChecked();
		return result;
	};
	
	public boolean isItemSelected() {
		boolean result=false;
		for(FileListItem fli:mDataItems) {
			if (fli.isChecked()) {
				result=true;
				break;
			}
		}
		return result;
	};
	
	public void setAllItemChecked(boolean checked) {
		for (int i=0;i<mDataItems.size();i++) 
			mDataItems.get(i).setChecked(checked);
		notifyDataSetChanged();
	};

    @Override
    public boolean isEnabled(int pos) {
        return mDataItems.get(pos).isEnableItem();
    }

    private boolean mAdapterEnabled=true;
	public void setAdapterEnabled(boolean enabled) {
        mAdapterEnabled=enabled;
    }

    public boolean isAdapterEnabled() {
        return mAdapterEnabled;
    }

	public boolean isSingleSelectMode() {
		return mSingleSelectMode;
	};

	public void removeItem(int dc) {
		mDataItems.remove(dc);
	};

	public void removeItem(FileListItem fi) {
		mDataItems.remove(fi);
	};

	public void add(FileListItem fi) {
		mDataItems.add(fi);
	};

	public void insert(int i, FileListItem fi) {
		mDataItems.add(i,fi);
	};

	public ArrayList<FileListItem> getDataList() {
		return mDataItems;
	};

	public void setDataList(ArrayList<FileListItem> fl) {
		mDataItems=fl;
	};

	public void clear() {
		mDataItems.clear();
	};

	public void sort() {
	    sort(mDataItems);
	};

	static public void sort(ArrayList<FileListItem>fl) {
        Collections.sort(fl, new Comparator<FileListItem>(){
            @Override
            public int compare(FileListItem l, FileListItem r) {
                String l_d=l.isDirectory()?"0":"1";
                String r_d=r.isDirectory()?"0":"1";
                return (l_d+l.getName()).compareToIgnoreCase((r_d+r.getName()));
            }
        });
    }

	public void setCbCheckListener(NotifyEvent ntfy) {
		cb_ntfy=ntfy;
	}

	public void unsetCbCheckListener() {
		cb_ntfy=null;
	}

	public int getCheckedItemCount() {
	    int result=0;
	    for(FileListItem item:mDataItems) if (item.isChecked()) result++;
	    return result;
    }

	private boolean enableListener=true;

	private ColorStateList mPrimaryTextColor=null;
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		 	final ViewHolder holder;
		 	
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.file_list_item, parent, false);
                holder=new ViewHolder();

                holder.ll_file_list_view=(LinearLayout)v.findViewById(R.id.file_list_view);
            	holder.cb_cb1=(CheckBox)v.findViewById(R.id.file_list_checkbox);
            	holder.rb_rb1=(RadioButton)v.findViewById(R.id.file_list_radiobtn);
            	holder.iv_image1=(ImageView)v.findViewById(R.id.file_list_icon);
            	holder.tv_name=(TextView)v.findViewById(R.id.file_list_name);
            	holder.tv_size=(TextView)v.findViewById(R.id.file_list_size);
            	holder.tv_moddate=(TextView)v.findViewById(R.id.file_list_date);
            	holder.tv_modtime=(TextView)v.findViewById(R.id.file_list_time);
                holder.tv_count=(TextView)v.findViewById(R.id.file_list_count);
            	holder.tv_select=(LinearLayout)v.findViewById(R.id.file_list_select_view);

                mPrimaryTextColor=holder.tv_name.getTextColors();

            	v.setTag(holder); 
            } else {
         	   holder= (ViewHolder)v.getTag();
            }
            v.setEnabled(true);
            final FileListItem o = mDataItems.get(position);
            if (o != null) {
                if (isAdapterEnabled()) holder.ll_file_list_view.setAlpha(1.0f);
                else holder.ll_file_list_view.setAlpha(0.3f);
            	if (o.isEnableItem()) {
	            	holder.cb_cb1.setEnabled(true);
            		holder.rb_rb1.setEnabled(true);
	            	holder.iv_image1.setEnabled(true);
					holder.iv_image1.setAlpha(1.0f);
	            	holder.tv_name.setEnabled(true);
	            	holder.tv_size.setEnabled(true);
	            	holder.tv_moddate.setEnabled(true);
					holder.tv_modtime.setEnabled(true);
					holder.tv_count.setEnabled(true);
            	} else {
            		holder.cb_cb1.setEnabled(false);
            		holder.rb_rb1.setEnabled(false);
	            	holder.iv_image1.setEnabled(false);
					holder.iv_image1.setAlpha(0.2f);
	            	holder.tv_name.setEnabled(false);
	            	holder.tv_size.setEnabled(false);
					holder.tv_moddate.setEnabled(false);
					holder.tv_modtime.setEnabled(false);
					holder.tv_count.setEnabled(false);
            	}
        		if (mSingleSelectMode) {
        			holder.cb_cb1.setVisibility(CheckBox.GONE);
        			holder.rb_rb1.setVisibility(RadioButton.VISIBLE);
        		} else {
        			holder.cb_cb1.setVisibility(CheckBox.VISIBLE);
        			holder.rb_rb1.setVisibility(RadioButton.GONE);
        		}
            	if (o.getName().startsWith("---")) {
            		//空処理
            		holder.cb_cb1.setVisibility(CheckBox.GONE);
            		holder.iv_image1.setVisibility(ImageView.GONE);
            		holder.tv_name.setText(o.getName());
            	} else {
                	holder.tv_name.setText(o.getName());
                	if (o.getLength()==-1) holder.tv_size.setText("Calculating");
                	else holder.tv_size.setText(o.getFileSize());
                	if (mShowLastModified) {
                        holder.tv_moddate.setText(o.getFileLastModDate());
                        holder.tv_modtime.setText(o.getFileLastModTime());
                	} else {
//    	            	holder.tv_size.setVisibility(TextView.GONE);
    	            	holder.tv_moddate.setVisibility(TextView.GONE);
    	            	holder.tv_modtime.setVisibility(TextView.GONE);
                	}
                   	if(o.isDirectory()) {
                	    if (!o.getServerType().equals(FileListItem.SERVER_TYPE_LOCAL)) holder.tv_size.setVisibility(TextView.GONE);
						else holder.tv_size.setVisibility(TextView.VISIBLE);
                   		holder.iv_image1.setImageResource(mIconImage[2]); //folder
                   		String ic=String.format("%3d item",o.getSubDirItemCount());
                   		holder.tv_count.setText(ic);
                        holder.tv_count.setVisibility(TextView.VISIBLE);
                   	} else {
                   		holder.iv_image1.setImageResource(mIconImage[3]); //sheet
                        holder.tv_size.setVisibility(TextView.VISIBLE);
                        holder.tv_count.setVisibility(TextView.GONE);
                   	}
                   	if (o.isHidden() || o.hasExtendedAttr()) {
                   		if (o.hasExtendedAttr()) {
                       		holder.tv_name.setTextColor(Color.GREEN);
    		            	holder.tv_size.setTextColor(Color.GREEN);
    		            	holder.tv_moddate.setTextColor(Color.GREEN);
    		            	holder.tv_modtime.setTextColor(Color.GREEN);
    		            	holder.tv_count.setTextColor(Color.GREEN);
                   		} else {
                       		holder.tv_name.setTextColor(Color.GRAY);
    		            	holder.tv_size.setTextColor(Color.GRAY);
    		            	holder.tv_moddate.setTextColor(Color.GRAY);
    		            	holder.tv_modtime.setTextColor(Color.GRAY);
							holder.tv_count.setTextColor(Color.GRAY);
                   		}
                   	} else {
                   		holder.tv_name.setTextColor(mPrimaryTextColor);
		            	holder.tv_size.setTextColor(mPrimaryTextColor);
		            	holder.tv_moddate.setTextColor(mPrimaryTextColor);
		            	holder.tv_modtime.setTextColor(mPrimaryTextColor);
                   	}
            	}
               	final int p = position;
            	if (o.isEnableItem()) {
					holder.cb_cb1.setEnabled(isAdapterEnabled());
					holder.cb_cb1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							setButton(o,p,isChecked);
							notifyDataSetChanged();
						}
					});
					holder.rb_rb1.setEnabled(isAdapterEnabled());
					holder.rb_rb1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							setButton(o,p,isChecked);
							notifyDataSetChanged();
						}
					});
				}
           		if (mSingleSelectMode) holder.rb_rb1.setChecked(mDataItems.get(p).isChecked());
           		else holder.cb_cb1.setChecked(mDataItems.get(p).isChecked());
       			
            }
            return v;
    };

    private void setButton(FileListItem o,int p, boolean isChecked) {
		if (enableListener) {
			enableListener=false;
            if (mSingleSelectMode) {
                if (isChecked) {
                    FileListItem fi;
                    for (int i=0;i<mDataItems.size();i++) {
                        fi=mDataItems.get(i);
                        if (fi.isChecked()&&p!=i) {
                            fi.setChecked(false);
                        }
                    }
                }
            }
            enableListener=true;
		}
		boolean c_chk=o.isChecked();
		o.setChecked(isChecked);
		if (cb_ntfy!=null) cb_ntfy.notifyToListener(isChecked, new Object[]{p, c_chk});

    };
    
	static class ViewHolder {
		 TextView tv_name, tv_moddate, tv_modtime, tv_size, tv_count;
		 LinearLayout tv_select, ll_file_list_view;
		 ImageView iv_image1;
		 CheckBox cb_cb1;
		 RadioButton rb_rb1;
	}

}
