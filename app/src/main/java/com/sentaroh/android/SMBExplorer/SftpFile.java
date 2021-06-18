package com.sentaroh.android.SMBExplorer;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

public class SftpFile {
    private static final Logger log= LoggerFactory.getLogger(SftpFile.class);

    private String mPath, mName, mCurrentDirectory;
    private boolean mIsDirectory=false;
    private boolean mCanRead=false, mCanWrite=false, mCanExecute=false;

    public SftpFile(String url, String account, String password, boolean directory) throws Exception {
//        log.info("init url="+url+", directory="+ directory);
        if (url.startsWith("sftp://")) {
            String body=url.substring(7);
            if (body.startsWith("/")) {
                throw new Exception("SftpFile can not create, host part contained \"/\"");
            }
            String[]url_array=body.split("/");
            String host_part=url_array[0];
            String host="", port="22";
            if (host_part.indexOf(":")>0) {
                //With post
                host=host_part.substring(0, host_part.indexOf(":"));
                port=host_part.substring(host_part.indexOf(":")+1);
            } else {
                //No port
                host=host_part;
            }
            String dir="";
            for(int i=1;i<url_array.length;i++) {
                dir+="/"+url_array[i];
            }
            if (dir.equals("")) dir="/";
            init(host, Integer.parseInt(port), removeRedundantPathSeparator(dir), account, password, directory);
        } else {
            throw new Exception("SftpFile can not create, url not begin with \"sftp://\"");
        }
    }

    public SftpFile(String host, int port, String path, String account, String password, boolean directory) throws Exception {
        init(host, port, path, account, password, directory);
    }

    public SftpFile(String host, int port, String path, String account, String password) throws Exception {
        init(host, port, path, account, password, false);
    }

    private SftpATTRS mAttrs=null;
    public void setAttrs(SftpATTRS attrs) {
        mAttrs=attrs;
    }

    public SftpATTRS getAttrs() {
        return mAttrs;
    }

//    public void setAttrs(long size, long last_modified) {
//        mLength=size;
//        mLastModified=last_modified;
//    }


    public JSch getJSch() {
        return mJsch;
    }

    public Session getSession() {
        return mSession;
    }
    public ChannelSftp getChannel() {
        return mChannel;
    }

    public SftpFile(SftpFile parent, String new_path, boolean directory, boolean can_read, boolean can_write, boolean can_execute) throws Exception{
//        log.info("init parent="+parent.getPath()+", directory="+ directory);
        long b_time=System.currentTimeMillis();
        mIsDirectory=directory;

        mPath=removeRedundantPathSeparator(new_path);

        mJsch = parent.getJSch();
        mSession = parent.getSession();
        mChannel = parent.getChannel();

        mCanRead=can_read;
        mCanWrite=can_write;
        mCanExecute=can_execute;

        if (mPath.lastIndexOf("/")>=0) mName=mPath.substring(mPath.lastIndexOf("/")+1);
        if (mName==null || mName.equals("")) mName=mPath;
        if (directory) {
            try {
                mChannel.cd(mPath);
            } catch(Exception e){
//                e.printStackTrace();
            }
            mCurrentDirectory =mPath;
        } else {
            mCurrentDirectory =mPath.substring(0, mPath.lastIndexOf("/"));
            if (mCurrentDirectory.equals("")) mCurrentDirectory="/";
            mChannel.cd(mCurrentDirectory);
        }
//        log.info("Frm parent mPath="+mPath+", name="+mName+", Directory="+ directory+", CanRead="+canRead()+", CanWrite="+canWrite()+", CanExecute="+canExecute());
    }

    private void init(String host, int port, String path, String account, String password, boolean directory) throws Exception{
        long b_time=System.currentTimeMillis();
        mIsDirectory=directory;
        mPath=removeRedundantPathSeparator(path);

        mJsch = new JSch();
        mSession = mJsch.getSession(account, host, port);
        mSession.setConfig(new Properties());
        mSession.setTimeout(30*1000);
        mSession.setConfig("StrictHostKeyChecking", "no");
        mSession.setPassword(password);
        mSession.connect();
        mChannel = (ChannelSftp) mSession.openChannel("sftp");
        mChannel.connect();
        if (mPath.lastIndexOf("/")>=0) mName=mPath.substring(mPath.lastIndexOf("/")+1);
        if (mName==null || mName.equals("")) mName=mPath;
        mPath=removeRedundantPathSeparator(mPath);
        if (directory) {
            try {
                mChannel.cd(mPath);
            } catch(Exception e){
//                e.printStackTrace();
            }
            mCurrentDirectory =mPath;
        } else {
            mCurrentDirectory =removeRedundantPathSeparator(mPath.substring(0, mPath.lastIndexOf("/")));
            try {
                mChannel.cd(mCurrentDirectory);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        try {
            SftpATTRS attrs=mChannel.lstat(mPath);
            setAttrs(attrs);

            String perm1=attrs.getPermissionsString().substring(1,4);
            mCanRead=perm1.substring(0,1).toUpperCase().equals("R");
            mCanWrite=perm1.substring(1,2).toUpperCase().equals("W");
            mCanExecute=perm1.substring(2,3).toUpperCase().equals("X");
        } catch (SftpException e) {
        }
//        log.info("Init mPath="+mPath+", name="+mName+", Directory="+ directory+", CanRead="+canRead()+", CanWrite="+canWrite()+", CanExecute="+canExecute());
    }

    private boolean hasChildItem() {
        boolean result=false;
        try {
            Vector<ChannelSftp.LsEntry> vector = mChannel.ls(mPath);
//            System.out.println("size="+vector.size());
            result=vector.size()>0?true:false;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private JSch mJsch;
    private Session mSession;
    private ChannelSftp mChannel;

    public String getParent() {
        String result="";
        if (mPath.lastIndexOf("/")==0) {
            result="/";
        } else if (mPath.lastIndexOf("/")<0) {
            result="/";
        } else {
            result=mPath.substring(0, mPath.lastIndexOf("/"));
        }
//        log.info("getParent parent="+result+", path="+mPath+", name="+mName);
        return result;
    }

    public InputStream getInputStream() throws Exception {
        log.info("getInputStream pwd="+mChannel.pwd()+", mPath="+mPath);
        return mChannel.get(mName);
    }

    public boolean createNewFile() throws Exception {
        boolean result=false;

        OutputStream os=getOutputStream();
        os.write("".getBytes());
        os.close();

        return result;
    };

    public OutputStream getOutputStream() throws Exception {
        return mChannel.put(mName);
    }

    public String getPath() {return mPath;}

//    public String getCanonicalPath() {return mPath;}
//
//    public String getAbsolutePath() {return mPath;}

    public String getName() {
        return mName;
    }

//    public void disconnect() throws Exception {
//        mChannel.setMtime(mName,(int)(System.currentTimeMillis()/1000));
//        mSession.disconnect();
//    }

    public String getCurrentDirectory() {
        return mCurrentDirectory;
    }

    static public String removeRedundantPathSeparator(String path) {
        String out=path;
        while(out.indexOf("//")>=0) {
            out=out.replaceAll("//","/");
        }
        return out;
    }

    public boolean canRead() {return mCanRead;}

    public boolean canWrite() {return mCanWrite;}

    public boolean canExecute() {return mCanExecute;}

    public boolean exists() {
        boolean result=exists(mPath);
        return result;
    }

    public boolean delete() {
        boolean exists=exists(mPath);
        boolean result=false;
        if (exists) {
            try {
                if (isDirectory()){
                    if (hasChildItem()) {
                        throw new Exception("Delete directory failed. Directory not empty. Directory="+mPath);
                    }
                    mChannel.rmdir(mPath);
                    result=true;
                } else {
                    mChannel.rm(mPath);
                    result=true;
                }
            } catch(Exception e) {
                result=false;
                e.printStackTrace();
            }
        }
        return result;
    }

    public boolean renameTo(String new_path) {
        boolean result=false;
        if (isDirectory()) {
            try {
                if (!hasChildItem()) {
                    mChannel.rename(getPath(), new_path);
                    result=true;
                } else {
                    throw new Exception("Rename failed. Directory not rmpty. Directory="+mPath);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                mChannel.rename(getPath(), new_path);
                result=true;
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public boolean exists(String fp) {
        String reformed_path=removeRedundantPathSeparator(fp);
        boolean result=false;
        try {
            mChannel.lstat(reformed_path);
            result=true;
        } catch(SftpException e) {
//            e.printStackTrace();
        }
//        log.info("exists fp="+reformed_path+", result="+result);
        return result;
    }

    public String pwd() throws SftpException {
        return mChannel.pwd();
    }

    public boolean mkdir(String fp) {
        boolean result=false;
        String reformed_path=removeRedundantPathSeparator(fp);
        if (!exists(reformed_path)) {
            try {
                mChannel.mkdir(reformed_path);
                result=true;
            } catch(SftpException e) {
//                e.printStackTrace();
            }
        }
//        log.info("mkdir fp="+reformed_path+", result="+result);
        return result;
    }

    public boolean mkdirs(String fp) {
        boolean result=false;
        String[] fp_array=fp.split("/");
        if (fp_array.length>0) {
            String fp_to_be_created="";
            for(String fp_item:fp_array) {
                if (!fp_item.equals("")) {
                    fp_to_be_created+="/"+fp_item;
                    if (!exists(fp_to_be_created)) {
                        if (!mkdir(fp_to_be_created)) {
                            result=false;
                            break;
                        } else {
                            result=true;
                        }
                    }
                }
            }
        }
        return result;
    }

    public ArrayList<SftpFile> listFiles() throws Exception {
        ArrayList<SftpFile>fl=new ArrayList<SftpFile>();
//        log.info("listFiles pwd="+mChannel.pwd()+", mPath="+mPath);
        try {
            if (!mPath.toLowerCase().equals("system volume information")) {
                Vector<ChannelSftp.LsEntry> vector = mChannel.ls(mPath);
                for (ChannelSftp.LsEntry lsEntry : vector) {
                    if (!lsEntry.getFilename().equals(".") && !lsEntry.getFilename().equals("..") &&
                            !lsEntry.getFilename().toLowerCase().equals("system volume information")) {
                        String new_path="";
                        String perm1=lsEntry.getAttrs().getPermissionsString().substring(1,4);
                        String perm2=lsEntry.getAttrs().getPermissionsString().substring(4,7);
                        String perm3=lsEntry.getAttrs().getPermissionsString().substring(7,10);
                        if (mPath.equals("/")) new_path=mPath+lsEntry.getFilename();
                        else new_path=mPath+"/"+lsEntry.getFilename();
                        SftpFile new_item=new SftpFile(this, new_path, lsEntry.getAttrs().isDir(),
                                perm1.substring(0,1).toUpperCase().equals("R"),
                                perm1.substring(1,2).toUpperCase().equals("W"),
                                perm1.substring(2,3).toUpperCase().equals("X"));
                        Date date =new Date(lsEntry.getAttrs().getMTime()*1000L);
                        new_item.setAttrs(lsEntry.getAttrs());

//                    log.info("name="+lsEntry.getFilename()+", ext="+lsEntry.getAttrs().getExtended()+
//                            ", perm="+lsEntry.getAttrs().getPermissionsString()+", perm1="+perm1+", perm2="+perm2+", perm3="+perm3);

                        fl.add(new_item);
                    }
                }
                Collections.sort(fl, new Comparator<SftpFile>(){
                    @Override
                    public int compare(SftpFile o1, SftpFile o2) {
                        String l_key=(o1.isDirectory()?"D":"F").concat(o1.getName());
                        String r_key=(o2.isDirectory()?"D":"F").concat(o2.getName());
                        return (l_key.compareToIgnoreCase(r_key));
                    }
                });
            }
        } catch(Exception e) {
            e.printStackTrace();
            log.info("name="+mPath);
        }
        return fl;
    }

    public int getChildItemCount() throws Exception {
        try {
            Vector<ChannelSftp.LsEntry> vector = mChannel.ls(mPath);
            return vector.size();
        } catch(Exception e) {
            e.printStackTrace();
            return 0;
        }

    }

    public void setLastModified(long time) throws Exception {
        mChannel.setMtime(mName,(int)(time/1000));
    }

    public long getLastModified() {
        return mAttrs.getMTime()*1000;
    }

    public long getLength() {
        return mAttrs.getSize();
    }

    public boolean isDirectory() {
        return mIsDirectory;
    }

}
