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

import com.sentaroh.android.Utilities3.MiscUtil;
import com.sentaroh.android.Utilities3.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class FileListItem implements Cloneable, Serializable, Comparable<FileListItem>{
    private static Logger log= LoggerFactory.getLogger(FileListItem.class);
	private static final long serialVersionUID = 1L;
	
	private String fileName;
	private boolean directory =false;
	private long fileLength;
	private long lastModdate;
	private boolean isChecked=false;
	private boolean canRead=false;
	private boolean isHidden=false;
	private boolean canWrite=false;
	private String filePath;
	private boolean childListExpanded=false;
	private int listLevel=0;
	private boolean hideListItem=false;
	private boolean subDirLoaded=false;
	private int subDirItemCount=0;
	private boolean triState=false;
	private boolean enableItem=true;
	private boolean hasExtendedAttr=false;

	private String fileSize="0", fileLastModDate="", fileLastModTime="";
	
	public void dump(String id) {
		log.info("FileName="+fileName+", filePath="+filePath);
        log.info("isDir="+ directory +", Length="+fileLength+
				", lastModdate="+lastModdate+", isChecked="+isChecked+
				", canRead="+canRead+",canWrite="+canWrite+", isHidden="+isHidden+", hasExtendedAttr="+hasExtendedAttr);
        log.info("childListExpanded="+childListExpanded+
				", listLevel=="+listLevel+", hideListItem="+hideListItem+
				", subDirLoaded="+subDirLoaded+", subDirItemCount="+subDirItemCount+
				", triState="+triState+", enableItem="+enableItem);
	};
	
	public FileListItem(String fn){
		fileName = fn;
	}
	
	public FileListItem(String file_name, boolean directory, long file_size, long last_modified, boolean checked,
		                boolean can_read,boolean can_write,boolean hidden, String parent_path, int lvl){
		fileName = file_name;
		fileLength = file_size;
		this.directory =directory;
		lastModdate=last_modified;
		isChecked =checked;
		canRead=can_read;
		canWrite=can_write;
		isHidden=hidden;
		filePath=parent_path;
		listLevel=lvl;
        fileSize=MiscUtil.convertFileSize(fileLength);
        String[] dt=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(lastModdate).split(" ");
        fileLastModDate=dt[0];
        fileLastModTime=dt[1];
	}

    public FileListItem(String file_name, boolean directory, long file_size, long last_modified, boolean checked,
                        String parent_path){
        fileName = file_name;
        fileLength = file_size;
        this.directory =directory;
        lastModdate=last_modified;
        isChecked =checked;
        canRead=true;
        canWrite=true;
        isHidden=false;
        filePath=parent_path;
        listLevel=0;
        fileSize=MiscUtil.convertFileSize(fileLength);
        String[] dt=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(lastModdate).split(" ");
        fileLastModDate=dt[0];
        fileLastModTime=dt[1];
    }
    public String getName(){return fileName;}
	public long getLength(){return fileLength;}
    public void setLength(long length){
        fileLength=length;
        fileSize=MiscUtil.convertFileSize(fileLength);
	}

	public String getFileSize() {return fileSize;}
    public String getFileLastModDate() {return fileLastModDate;}
    public String getFileLastModTime() {return fileLastModTime;}

	public boolean isDirectory(){return directory;}
	public long getLastModified(){return lastModdate;}
	public void setLastModified(long p){
	    lastModdate=p;
        String[] dt=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(lastModdate).split(" ");
        fileLastModDate=dt[0];
        fileLastModTime=dt[1];
	}
	public boolean isChecked(){return isChecked;}
	public void setChecked(boolean p){
		isChecked=p;
		if (p) triState=false;
	};
	public boolean canRead(){return canRead;}
	public void setCanRead(boolean canRead) {
		this.canRead=canRead;
	}
	public boolean canWrite(){return canWrite;}
	public void setCanWrite(boolean canWrite) {
		this.canWrite=canWrite;
	}
	public boolean isHidden(){return isHidden;}
	public String getPath(){return filePath;}
	public void setChildListExpanded(boolean p){childListExpanded=p;}
	public boolean isChildListExpanded(){return childListExpanded;}
	public void setListLevel(int p){listLevel=p;}
	public int getListLevel(){return listLevel;}
	public boolean isHideListItem(){return hideListItem;}
	public void setHideListItem(boolean p){hideListItem=p;}
	public void setSubDirItemCount(int p){subDirItemCount=p;}
	public int getSubDirItemCount(){return subDirItemCount;}
	public boolean isSubDirLoaded() {return subDirLoaded;}
	public void setSubDirLoaded(boolean p) {subDirLoaded=p;}
	public void setTriState(boolean p) {triState=p;}
	public boolean isTriState() {return triState;}
	public void setEnableItem(boolean p) {enableItem=p;}
	public boolean isEnableItem() {return enableItem;}
	public void setHasExtendedAttr(boolean p) {hasExtendedAttr=p;};
	public boolean hasExtendedAttr() {return hasExtendedAttr;}

	private String baseUrl="";
	public void setBaseUrl(String base) {baseUrl=base;}
    public String getBaseUrl() {return baseUrl;}

	@Override
	public int compareTo(FileListItem o) {
        if(this.fileName != null) {
            String cmp_c="F",cmp_n="F";
            if (directory) cmp_c="D";
            if (o.isDirectory()) cmp_n="D";
            cmp_c+=filePath;
            cmp_n+=o.getPath();
            if (!cmp_c.equals(cmp_n)) return cmp_c.compareToIgnoreCase(cmp_n);
            return fileName.compareToIgnoreCase(o.getName());
        } else throw new IllegalArgumentException();
	}

    @Override
    public FileListItem clone() {
        FileListItem npfli = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);

            oos.flush();
            oos.close();

            baos.flush();
            byte[] ba_buff = baos.toByteArray();
            baos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(ba_buff);
            ObjectInputStream ois = new ObjectInputStream(bais);

            npfli = (FileListItem) ois.readObject();
            ois.close();
            bais.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return npfli;
    }

}

