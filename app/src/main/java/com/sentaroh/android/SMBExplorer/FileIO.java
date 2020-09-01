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

import android.content.ContentProviderClient;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;

import com.sentaroh.android.SMBExplorer.Log.LogUtil;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.ThreadCtrl;
import com.sentaroh.jcifs.JcifsAuth;
import com.sentaroh.jcifs.JcifsException;
import com.sentaroh.jcifs.JcifsFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.util.ArrayList;

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

public class FileIO extends Thread {
	private final static String DEBUG_TAG = "SMBExplorer";
	private NotifyEvent notifyEvent ;
	private ThreadCtrl fileioThreadCtrl;
	private ArrayList<FileIoLinkParm> fileioLinkParm;
	private int file_op_cd;
	private Context mContext=null;
	private Handler uiHandler = new Handler() ;
	private GlobalParameters mGp=null;
	private final static String mAppPackageName="com.sentaroh.android.SMBExplorer";
	private LogUtil mLogUtil=null;
	
	// @Override
	public FileIO(GlobalParameters gp, int op_cd,
				  ArrayList<FileIoLinkParm> alp, ThreadCtrl tc, NotifyEvent ne, Context cc, String lmp) {
		
		mGp=GlobalWorkArea.getGlobalParameters(cc);
		fileioThreadCtrl=tc;
		file_op_cd=op_cd;
		fileioLinkParm=alp;
		notifyEvent=ne;
		
		mContext=cc;

		mLogUtil=new LogUtil(mContext, "FILEIO");

	};
	
    private long taskBeginTime=0;
    
	@Override
	public void run() {
		sendLogMsg("I","Task has started.");
		
		final WakeLock wake_lock=((PowerManager)mContext.getSystemService(Context.POWER_SERVICE))
	    			.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
//	   	    				| PowerManager.ON_AFTER_RELEASE
	    				, "SMBExplorer-ScreenOn");
		final WifiLock wifi_lock=((WifiManager)mContext.getSystemService(Context.WIFI_SERVICE))
				.createWifiLock(WifiManager.WIFI_MODE_FULL, "SMBExplorer-wifi");
		
		if (mGp.fileIoWakeLockRequired) wake_lock.acquire();
		if (mGp.fileIoWifiLockRequired) wifi_lock.acquire();

		try {
			taskBeginTime=System.currentTimeMillis();
			
			boolean fileioTaskResultOk=false;
			for (int i=0;i<fileioLinkParm.size();i++) {
				fileioTaskResultOk=fileOperation(fileioLinkParm.get(i));
				if (!fileioTaskResultOk) 
					break;
			}
			sendLogMsg("I","Task was ended. fileioTaskResultOk=",fileioTaskResultOk+ ", fileioThreadCtrl:",fileioThreadCtrl.toString());
			final String elapsed_time_msg="Task elapsed time="+(System.currentTimeMillis()-taskBeginTime);
			sendLogMsg("I",elapsed_time_msg);
			if (fileioTaskResultOk) {
				fileioThreadCtrl.setThreadResultSuccess();
				sendDebugLogMsg(1,"I","Task was endeded without error.");			
			} else if (fileioThreadCtrl.isEnabled()) {
				fileioThreadCtrl.setThreadResultError();
				sendLogMsg("W","Task was ended with error.");
			} else {
				fileioThreadCtrl.setThreadResultCancelled();
				sendLogMsg("W","Task was cancelled.");
			}
			fileioThreadCtrl.setDisabled();

			uiHandler.post(new Runnable() {// UI thread
				@Override
				public void run() {
					notifyEvent.notifyToListener(true, new Object[]{elapsed_time_msg});
				}
			});		
		} finally {
			if (wake_lock.isHeld()) wake_lock.release();
			if (wifi_lock.isHeld()) wifi_lock.release();
		}
	}

	private JcifsAuth createJcifsAuth(String smb_level, String domain, String user, String pass, boolean ipc_sign_enforce, boolean use_smb2_nego) {
	    JcifsAuth auth=null;
        if (Integer.parseInt(smb_level)==JcifsAuth.JCIFS_FILE_SMB1) auth = new JcifsAuth(Integer.parseInt(smb_level), domain, user, pass);
        else auth=new JcifsAuth(Integer.parseInt(smb_level), domain, user, pass, ipc_sign_enforce, use_smb2_nego);
        return auth;
    }

	private boolean fileOperation(FileIoLinkParm fiop) {
		sendDebugLogMsg(1,"I","FILEIO task invoked.",
                " fromUrl=",fiop.getFromDirectory(), ", fromName=",fiop.getFromName(),", fromBaseUrl=",fiop.getFromBaseDirectory(),", fromSmbLebel=",fiop.getFromSmbLevel(),", fromUser=",fiop.getFromUser(),
                ", FromIPC="+fiop.isFromSmbOptionIpcSignEnforce()+", FromSMB2Nego="+fiop.isFromSmbOptionUseSMB2Negotiation()+
                ", toUrl=",fiop.getToDirectory(), ", toName=",fiop.getToName(), ", toBaseUrl=",fiop.getToBaseDirectory(),", toSmbLebel=",fiop.getToSmbLevel(),", toUser=",fiop.getToUser()+
                ", ToIPC="+fiop.isToSmbOptionIpcSignEnforce()+", ToSMB2Nego="+fiop.isToSmbOptionUseSMB2Negotiation());

		boolean result=false;
        JcifsAuth smb_auth_from =null, smb_auth_to =null;
		switch (file_op_cd) {
			case FILEIO_PARM_LOCAL_CREATE:
				result=createLocalDir(fiop.getToSafRoot(), fiop.getToDirectory()+"/"+fiop.getToName());
				break;
			case FILEIO_PARM_REMOTE_CREATE:
                smb_auth_to =createJcifsAuth(fiop.getToSmbLevel(), fiop.getToDomain(), fiop.getToUser(), fiop.getToPass(), fiop.isToSmbOptionIpcSignEnforce(), fiop.isToSmbOptionUseSMB2Negotiation());
                result=createRemoteDir(smb_auth_to, fiop.getToDirectory()+"/"+fiop.getToName());
				break;
			case FILEIO_PARM_LOCAL_RENAME:
				result=renameLocalItem(fiop.getFromSafRoot(), fiop.getFromDirectory()+"/"+fiop.getFromName(),  fiop.getToSafRoot(), fiop.getToDirectory()+"/"+fiop.getToName());
				break;
			case FILEIO_PARM_REMOTE_RENAME:
                smb_auth_to =createJcifsAuth(fiop.getToSmbLevel(), fiop.getToDomain(), fiop.getToUser(), fiop.getToPass(), fiop.isToSmbOptionIpcSignEnforce(), fiop.isToSmbOptionUseSMB2Negotiation());
				result=renameRemoteItem(smb_auth_to, fiop.getFromDirectory()+"/"+fiop.getFromName()+"/", fiop.getToDirectory()+"/"+fiop.getToName()+"/");
				break;
			case FILEIO_PARM_LOCAL_DELETE:
				result=deleteLocalItem(fiop.getToSafRoot(), fiop.getToDirectory()+"/"+fiop.getToName());
				break;
			case FILEIO_PARM_REMOTE_DELETE:
                smb_auth_to =createJcifsAuth(fiop.getToSmbLevel(), fiop.getToDomain(), fiop.getToUser(), fiop.getToPass(), fiop.isToSmbOptionIpcSignEnforce(), fiop.isToSmbOptionUseSMB2Negotiation());
				result=deleteRemoteItem(smb_auth_to, fiop.getToDirectory()+"/"+fiop.getToName()+"/");
				break;
			case FILEIO_PARM_COPY_REMOTE_TO_LOCAL:
                smb_auth_from =createJcifsAuth(fiop.getFromSmbLevel(), fiop.getFromDomain(), fiop.getFromUser(), fiop.getFromPass(), fiop.isFromSmbOptionIpcSignEnforce(), fiop.isFromSmbOptionUseSMB2Negotiation());
				result=copyMoveRemoteToLocal(false, smb_auth_from, fiop.getFromDirectory()+"/"+fiop.getFromName(), fiop.getToDirectory()+"/"+fiop.getToName());
				break;
			case FILEIO_PARM_COPY_REMOTE_TO_REMOTE:
                smb_auth_from =createJcifsAuth(fiop.getFromSmbLevel(), fiop.getFromDomain(), fiop.getFromUser(), fiop.getFromPass(), fiop.isFromSmbOptionIpcSignEnforce(), fiop.isFromSmbOptionUseSMB2Negotiation());
                smb_auth_to =createJcifsAuth(fiop.getToSmbLevel(), fiop.getToDomain(), fiop.getToUser(), fiop.getToPass(), fiop.isToSmbOptionIpcSignEnforce(), fiop.isToSmbOptionUseSMB2Negotiation());
				result=copyMoveRemoteToRemote(false, smb_auth_from, smb_auth_to, fiop.getFromDirectory()+"/"+fiop.getFromName(), fiop.getToDirectory()+"/"+fiop.getToName());
				break;
			case FILEIO_PARM_COPY_LOCAL_TO_LOCAL:
				result=copyMoveLocalToLocal(false, fiop.getFromDirectory()+"/"+fiop.getFromName(), fiop.getToDirectory()+"/"+fiop.getToName());
				break;
			case FILEIO_PARM_COPY_LOCAL_TO_REMOTE:
                smb_auth_to =createJcifsAuth(fiop.getToSmbLevel(), fiop.getToDomain(), fiop.getToUser(), fiop.getToPass(), fiop.isToSmbOptionIpcSignEnforce(), fiop.isToSmbOptionUseSMB2Negotiation());
				result=copyMoveLocalToRemote(false, smb_auth_to, fiop.getFromDirectory()+"/"+fiop.getFromName(), fiop.getToDirectory()+"/"+fiop.getToName());
				break;
			case FILEIO_PARM_MOVE_REMOTE_TO_LOCAL:
                smb_auth_from =createJcifsAuth(fiop.getFromSmbLevel(), fiop.getFromDomain(), fiop.getFromUser(), fiop.getFromPass(), fiop.isFromSmbOptionIpcSignEnforce(), fiop.isFromSmbOptionUseSMB2Negotiation());
				result=copyMoveRemoteToLocal(true, smb_auth_from, fiop.getFromDirectory()+"/"+fiop.getFromName(), fiop.getToDirectory()+"/"+fiop.getToName());
				break;
			case FILEIO_PARM_MOVE_REMOTE_TO_REMOTE:
                smb_auth_from =createJcifsAuth(fiop.getFromSmbLevel(), fiop.getFromDomain(), fiop.getFromUser(), fiop.getFromPass(), fiop.isFromSmbOptionIpcSignEnforce(), fiop.isFromSmbOptionUseSMB2Negotiation());
                smb_auth_to =createJcifsAuth(fiop.getToSmbLevel(), fiop.getToDomain(), fiop.getToUser(), fiop.getToPass(), fiop.isToSmbOptionIpcSignEnforce(), fiop.isToSmbOptionUseSMB2Negotiation());
				if (fiop.getFromBaseDirectory().equals(fiop.getToBaseDirectory())) {
					result= renameRemoteItem(smb_auth_from, fiop.getFromDirectory()+"/"+fiop.getFromName(), fiop.getToDirectory()+"/"+fiop.getToName());
				} else {
					result=copyMoveRemoteToRemote(true, smb_auth_from, smb_auth_to, fiop.getFromDirectory()+"/"+fiop.getFromName(), fiop.getToDirectory()+"/"+fiop.getToName());
				}
				break;
			case FILEIO_PARM_MOVE_LOCAL_TO_LOCAL:
				result= copyMoveLocalToLocal(true, fiop.getFromDirectory()+"/"+fiop.getFromName(), fiop.getToDirectory()+"/"+fiop.getToName());
				break;
			case FILEIO_PARM_MOVE_LOCAL_TO_REMOTE:
                smb_auth_to =createJcifsAuth(fiop.getToSmbLevel(), fiop.getToDomain(), fiop.getToUser(), fiop.getToPass(), fiop.isToSmbOptionIpcSignEnforce(), fiop.isToSmbOptionUseSMB2Negotiation());
				result=copyMoveLocalToRemote(true, smb_auth_to, fiop.getFromDirectory()+"/"+fiop.getFromName(), fiop.getToDirectory()+"/"+fiop.getToName());
				break;
			case FILEIO_PARM_DOWLOAD_REMOTE_FILE:
                smb_auth_from =createJcifsAuth(fiop.getFromSmbLevel(), fiop.getFromDomain(), fiop.getFromUser(), fiop.getFromPass(), fiop.isFromSmbOptionIpcSignEnforce(), fiop.isFromSmbOptionUseSMB2Negotiation());
                result=downloadRemoteFile(smb_auth_from, fiop.getFromDirectory()+"/"+fiop.getFromName(), fiop.getToDirectory()+"/"+fiop.getToName());
				break;
	
			default:
				break;
		};
		return result;
	}
	
	private static String mPrevProgMsg="";
	private void sendMsgToProgDlg(final String log_msg) {
		if (!mPrevProgMsg.equals(log_msg)) {
			mPrevProgMsg=log_msg;
			uiHandler.post(new Runnable() {// UI thread
				@Override
				public void run() {
					mGp.progressMsgView.setText(log_msg);
				}
			});
            try {
                mGp.svcClient.aidlUpdateNotificationMessage(log_msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
	}

	private void sendLogMsg(final String log_cat, final String ...log_msg) {
        mLogUtil.addLogMsg(log_cat, log_msg);
	}

	private void sendDebugLogMsg(int lvl, final String log_cat, final String ...log_msg) {

		if (mLogUtil.getLogLevel()>0) {
            mLogUtil.addDebugMsg(lvl, log_cat, log_msg);
		}
	}
	
	private boolean createLocalDir(SafFile3 rt, String newUrl) {
    	SafFile3 lf;
    	boolean result = false;
    	if (!fileioThreadCtrl.isEnabled()) return false;
    	sendDebugLogMsg(1,"I","Create local dir item=",newUrl);
    	try {
            SafFile3 csf=new SafFile3(mContext, newUrl);
            result=csf.mkdirs();
    		if (!result) {
    			sendLogMsg("E","Create error msg=",csf.getLastErrorMessage());
    			fileioThreadCtrl.setThreadMessage("Create error msg="+csf.getLastErrorMessage());
    		} else {
    			sendLogMsg("I",newUrl," was created");
    		}
		} catch (Exception e) {
			e.printStackTrace();
			sendLogMsg("E","Create error:",e.toString());
			fileioThreadCtrl.setThreadMessage("Create error:"+e.toString());
			result=false;
			return false;
		}
    	return result;
    }

    private boolean createRemoteDir(JcifsAuth smb_auth, String newUrl) {
    	JcifsFile sf;
    	boolean result = false;
    	if (!fileioThreadCtrl.isEnabled()) return false;
    	sendDebugLogMsg(1,"I","Create remote dir item=",newUrl);
    	try {
    		result=true;
    		sf = new JcifsFile( newUrl,smb_auth);
    		
    		if (sf.exists()) return false;
    		
    		sf.mkdir();
    		sendLogMsg("I",newUrl," was created");
		} catch (Exception e) {
			e.printStackTrace();
			sendLogMsg("E","Create error:",e.toString());
			fileioThreadCtrl.setThreadMessage("Create error:"+e.toString());
			result=false;
			return false;
		}
    	return result;
    }
	
    private boolean renameRemoteItem(JcifsAuth smb_auth, String oldUrl, String newUrl) {
    	JcifsFile sf,sfd;
    	boolean result = false;
    	if (!fileioThreadCtrl.isEnabled()) return false;
    	sendDebugLogMsg(1,"I","Rename remote item=",oldUrl);
    	try {
    		result=true;
    		sf = new JcifsFile( oldUrl,smb_auth );
    		sfd = new JcifsFile( newUrl,smb_auth );
    		
    		sf.renameTo(sfd);
    		sendLogMsg("I",oldUrl," was renamed to ",newUrl);
		} catch (Exception e) {
			e.printStackTrace();
			sendLogMsg("E","Rename error:",e.toString());
			fileioThreadCtrl.setThreadMessage("Rename error:"+e.toString());
			result=false;
			return false;
		}
    	return result;
    }
    
    private boolean renameLocalItem(SafFile3 from_rt, String oldUrl, SafFile3 to_rt, String newUrl) {
    	File sf,sfd;
    	boolean result = false;
    	if (!fileioThreadCtrl.isEnabled()) return false;
    	sendDebugLogMsg(1,"I","Rename local item=",oldUrl);
    	SafFile3 document=null;
    	try {
			document=new SafFile3(mContext, oldUrl);
			SafFile3 new_file=new SafFile3(mContext, newUrl);
			result=document.renameTo(new_file);
        } catch(Exception e) {
    	    e.printStackTrace();
        }
        if (result) {
            sendLogMsg("I",oldUrl," was renamed to ",newUrl);
        } else {
            sendLogMsg("I","Rename was failed, from=",oldUrl," to=",newUrl,"\n",document.getLastErrorMessage());
            fileioThreadCtrl.setThreadMessage("Rename was failed, from="+oldUrl+" to="+newUrl+"\n"+document.getLastErrorMessage());
        }
    	return result;
    }

    private boolean deleteLocalItem(SafFile3 rt, String url) {
    	boolean result = false;
    	if (!fileioThreadCtrl.isEnabled()) return false;
    	sendDebugLogMsg(1,"I","Delete local file entered, File=",url);

        SafFile3 usf = new SafFile3(mContext, url);
        if (usf.isSafFile()) {
            ContentProviderClient client =null;
            try {
                client = mContext.getContentResolver().acquireContentProviderClient(usf.getUri().getAuthority());
                result = deleteSafFile(usf, client);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (client!=null) client.release();
            }
        } else {
            result=deleteOsFile(usf);
        }
    	return result;
    }

    private boolean deleteOsFile(SafFile3 lf) {
        boolean result=false;
        if (lf.isDirectory()) {//ディレクトリの場合
        	if (lf.canRead()) {
				SafFile3[] children = lf.listFiles();//ディレクトリにあるすべてのファイルを処理する
				if (children!=null) {
					for (SafFile3 item:children) {
						if (!fileioThreadCtrl.isEnabled()) return false;
						result=deleteOsFile(item);
						if (!result) {
							return false;
						}
					}
				} else {
					fileioThreadCtrl.setThreadMessage("Directory delete error, Directory="+lf.getPath()+", canRead="+lf.canRead()+", canWrite="+lf.canWrite());
					sendLogMsg("I","Directory delete error, Directory=",lf.getPath());
					return false;
				}
			} else {
				fileioThreadCtrl.setThreadMessage("Directory can not read, Directory="+lf.getPath()+", canRead="+lf.canRead()+", canWrite="+lf.canWrite());
				sendLogMsg("I","Directory can not read, Directory=",lf.getPath());
				return false;
			}
        }
        if (!fileioThreadCtrl.isEnabled()) return false;
//        result=lf.delete();
        result=lf.delete();
        scanMediaStoreLibraryFile(lf.getPath());
        if (result) {
            sendMsgToProgDlg(lf.getName()+" was deleted");
            sendLogMsg("I","File was Deleted. File=",lf.getPath());
        } else {
            sendLogMsg("I","Delete was failed, File=",lf.getPath());
            fileioThreadCtrl.setThreadMessage("Delete was failed, File="+lf.getPath());
        }
        return result;

    }

    private boolean deleteSafFile(SafFile3 lf, ContentProviderClient client) {
    	boolean result=false;
        if (lf.isDirectory(client)) {//ディレクトリの場合
            SafFile3[] children = lf.listFiles(client);//ディレクトリにあるすべてのファイルを処理する
            for (int i=0; i<children.length; i++) {  
            	if (!fileioThreadCtrl.isEnabled()) return false;
            	result=deleteSafFile(children[i], client);
                if (!result) {
                    return false;  
                }  
            }
        }
        if (!fileioThreadCtrl.isEnabled()) return false;
        try {
            SafFile3.deleteDocument(client, lf.getUri());
            scanMediaStoreLibraryFile(lf.getPath());
            result=true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (result) {
    	    sendMsgToProgDlg(lf.getName()+" was deleted");
    	    sendLogMsg("I","File was Deleted. File=",lf.getPath());
	    } else {
    	    sendLogMsg("I","Delete was failed, File=",lf.getPath());
    	    fileioThreadCtrl.setThreadMessage("Delete was failed, File="+lf.getPath());
	    }
	    return result;
        
    }

    private boolean deleteRemoteItem(JcifsAuth smb_auth, String url) {
    	JcifsFile sf;
    	boolean result = false;
    	if (!fileioThreadCtrl.isEnabled()) return false;
    	sendDebugLogMsg(1,"I","Delete remote file entered, File=",url);
    	try {
    		result=true;
			sf = new JcifsFile( url+"/",smb_auth );
			result=deleteRemoteFile(sf);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendLogMsg("E","Remote file delete error:",e.toString());
			fileioThreadCtrl.setThreadMessage("Remote file delete error:"+e.toString());
			result=false;
			return false;
		} catch (JcifsException e) {
            e.printStackTrace();
            sendLogMsg("E","Remote file delete error:",e.toString());
            fileioThreadCtrl.setThreadMessage("Remote file delete error:"+e.toString());
            result=false;
            return false;
        }
        return result;
    }
 
    private boolean deleteRemoteFile(JcifsFile rf) {
    	try {
			if (rf.isDirectory()) {//ディレクトリの場合  
	            JcifsFile[] children = rf.listFiles();//ディレクトリにあるすべてのファイルを処理する
	            for (int i=0; i<children.length; i++) {  
	            	if (!fileioThreadCtrl.isEnabled()) return false;
                    boolean success = deleteRemoteFile(children[i]);
	                if (!success) {  
	                    return false;  
	                }  
	            }
	        }  
		    // 削除  
	        if (!fileioThreadCtrl.isEnabled()) return false;
		    rf.delete();
		    sendMsgToProgDlg(rf.getName().replace("/", "")+" was deleted");
		    sendLogMsg("I","File was Deleted. File=",rf.getPath());
		} catch (JcifsException e) {
			e.printStackTrace();
			sendLogMsg("E","Remote file delete error:",e.toString());
			fileioThreadCtrl.setThreadMessage("Remote file delete error:"+e.toString());
			return false;
		}
	    return true;
        
    }
    
	private String makeRemoteTempFilePath(String  targetUrl) {
		String tmp_wu="";
		String last_sep="";
		if (targetUrl.endsWith("/")) {
			tmp_wu=targetUrl.substring(0,(targetUrl.length()-1));
			last_sep="/";
		} else tmp_wu=targetUrl;
		String target_dir1=tmp_wu.substring(0,tmp_wu.lastIndexOf("/"));
		String target_fn=tmp_wu.replace(target_dir1, "").substring(1);
		String tmp_target=target_dir1+"/SMBExplorer.work.tmp"+last_sep;
		return tmp_target;
	}

    private boolean copyMoveLocalToLocal(boolean move, String fromUrl, String toUrl)  {
        File iLf=null;
        boolean result = false;
        sendDebugLogMsg(1,"I","copyMoveLocalToLocal from=",fromUrl,",to=",toUrl+", move="+move);
        boolean isDirectory=false;
        SafFile3 from_saf=null;
        from_saf=new SafFile3(mContext, fromUrl);
        isDirectory=from_saf.isDirectory();
		if (isDirectory) { // Directory copy
			result=true;
            SafFile3[] children = from_saf.listFiles();
            for (SafFile3 element : children) {
                if (!fileioThreadCtrl.isEnabled()) return false;
                if (!copyMoveLocalToLocal(move, fromUrl+"/"+element.getName(), toUrl+"/"+element.getName() ))
                    return false;
            }
            makeLocalDirsByFilePath(toUrl+"/");
            if (move) {
                from_saf.delete();
                sendLogMsg("I",fromUrl," was moved.");
            }
		} else {
			if (!fileioThreadCtrl.isEnabled()) return false;
			makeLocalDirsByFilePath(toUrl);
            from_saf = new SafFile3(mContext, fromUrl);
            SafFile3 to_saf = new SafFile3(mContext, toUrl);
            long b_time=System.currentTimeMillis();
            if (from_saf.getUuid().equals(to_saf.getUuid()) && move) result=from_saf.moveTo(to_saf);
            else {
                result=copyMoveFileLocalToLocat(move, from_saf, to_saf, fromUrl, toUrl);
            }
            putCopyMoveResultMessage(move, result, from_saf.length(), b_time, fromUrl, toUrl, from_saf.getName());
		}
		return result;
    }

    private boolean copyMoveRemoteToLocal(boolean move, JcifsAuth from_jcifs_auth, String fromUrl, String toUrl)  {
        File iLf=null;
        boolean result = false;
        if (!fileioThreadCtrl.isEnabled()) return false;
        sendDebugLogMsg(1,"I","copyMoveRemoteToLocal from=",fromUrl,",to=",toUrl);
        boolean isDirectory=false;
        try {
            JcifsFile from_jcifs=new JcifsFile(fromUrl, from_jcifs_auth);
            isDirectory=from_jcifs.isDirectory();
            if (isDirectory) { // Directory copy
                result=true;
                JcifsFile[] children = from_jcifs.listFiles();
                for (JcifsFile element : children) {
                    if (!fileioThreadCtrl.isEnabled()) return false;
                    if (!copyMoveRemoteToLocal(move, from_jcifs_auth, fromUrl+element.getName(), toUrl+element.getName() ))
                        return false;
                }
                makeLocalDirsByFilePath(toUrl+"/");
                if (move) {
                    from_jcifs.delete();
                    sendLogMsg("I",fromUrl," was moved.");
                }
            } else {
                if (!fileioThreadCtrl.isEnabled()) return false;
                makeLocalDirsByFilePath(toUrl);
                SafFile3 to_saf = new SafFile3(mContext, toUrl);
                long b_time=System.currentTimeMillis();
                result=copyMoveFileRemoteToLocat(move, from_jcifs, to_saf, fromUrl, toUrl);
                putCopyMoveResultMessage(move, result, from_jcifs.length(), b_time, fromUrl, toUrl, from_jcifs.getName());
            }
        } catch(Exception e) {
            e.printStackTrace();
            sendLogMsg("E","copyMoveRemoteToLocal error:",e.toString());
            fileioThreadCtrl.setThreadMessage("copyMoveRemoteToLocal error:"+e.toString());
            result=false;
        }
        return result;
    }

    private void putCopyMoveResultMessage(boolean move, boolean result, long length, long b_time, String fromUrl, String toUrl, String fn) {
        if (result) {
            if (move) {
                sendMsgToProgDlg(fn + " was moved.");
                sendLogMsg("I",fromUrl," was moved to ",toUrl);
            } else {
                sendMsgToProgDlg(fn + " was copied.");
                sendLogMsg("I",fromUrl," was copied to ",toUrl);
            }
            sendLogMsg("I",length + " bytes transfered in " +
                    (System.currentTimeMillis()-b_time)+" mili seconds at " + calTransferRate(length, (System.currentTimeMillis()-b_time)));
            scanMediaStoreLibraryFile(toUrl);
        } else {
            if (move) sendLogMsg("I","Move was failed. fromUrl=", fromUrl,", toUrl=",toUrl);
            else sendLogMsg("I","Copy was failed. fromUrl=", fromUrl,", toUrl=",toUrl);
        }
    }

    private boolean copyMoveLocalToRemote(boolean move, JcifsAuth to_jcifs_auth, String fromUrl, String toUrl)  {
        File iLf=null;
        boolean result = false;
        sendDebugLogMsg(1,"I","copyMoveRemoteToLocal from=",fromUrl,",to=",toUrl);
        boolean isDirectory=false;
        try {
            SafFile3 from_saf=new SafFile3(mContext, fromUrl);
            isDirectory=from_saf.isDirectory();
            if (isDirectory) { // Directory copy
                result=true;
                SafFile3[] children = from_saf.listFiles();
                for (SafFile3 element : children) {
                    if (!fileioThreadCtrl.isEnabled()) return false;
                    String new_to_url=element.isDirectory()?toUrl+"/"+element.getName()+"/":toUrl+"/"+element.getName();
                    if (!copyMoveLocalToRemote(move, to_jcifs_auth, fromUrl+"/"+element.getName(), new_to_url))
                        return false;
                }
                makeRemoteDirsByFilePath(to_jcifs_auth, toUrl+"/");
                if (move) {
                    from_saf.delete();
                    sendLogMsg("I",fromUrl," was deleted.");
                }
            } else {
                if (!fileioThreadCtrl.isEnabled()) return false;
                makeRemoteDirsByFilePath(to_jcifs_auth, toUrl);
                JcifsFile ohf=new JcifsFile(toUrl+"/",to_jcifs_auth);
                long b_time=System.currentTimeMillis();
                result=copyMoveFileLocalToRemote(move, from_saf, ohf, fromUrl, toUrl);
                putCopyMoveResultMessage(move, result, from_saf.length(), b_time, fromUrl, toUrl, from_saf.getName());
            }
        } catch(Exception e) {
            e.printStackTrace();
            sendLogMsg("E","copyMoveLocalToRemote error:",e.toString());
            fileioThreadCtrl.setThreadMessage("copyMoveLocalToRemote error:"+e.toString());
            result=false;
        }
        return result;
    }

    private boolean copyMoveFileLocalToLocat(boolean move, SafFile3 from_saf, SafFile3 to_saf, String fromUrl, String toUrl) {
	    boolean result=false;
	    String title=move?"Moving":"Copying";
	    SafFile3 temp_out=null;
        try {
            if (to_saf.getAppDirectoryCache()!=null && Build.VERSION.SDK_INT>=24) {
                File tf=new File(to_saf.getAppDirectoryCache()+"/"+to_saf.getName());
                File df=new File(tf.getParent());
                if (!df.exists()) df.mkdirs();
                copyFile(from_saf.getInputStream(), new FileOutputStream(tf), from_saf.length(), title, from_saf.getName(), fromUrl, toUrl);
//                copyFile(from_saf.getInputStream(), new FileOutputStream(temp_os), from_saf.length(), title, from_saf.getName(), fromUrl, toUrl);
                if (!fileioThreadCtrl.isEnabled()) {
                    tf.delete();
                    result=false;
                } else {
                    tf.setLastModified(from_saf.lastModified());
                    temp_out=new SafFile3(mContext, to_saf.getAppDirectoryCache()+"/"+to_saf.getName());
                    to_saf.deleteIfExists();
                    result=temp_out.moveTo(to_saf);
                    if (move && result) result=from_saf.delete();
                }
            } else {
                temp_out=new SafFile3(mContext, toUrl+".tmp");
                temp_out.createNewFile();
                copyFile(from_saf.getInputStream(), temp_out.getOutputStream(), from_saf.length(), title, from_saf.getName(), fromUrl, toUrl);
                if (!fileioThreadCtrl.isEnabled()) {
                    temp_out.deleteIfExists();
                    result=false;
                } else {
                    to_saf.deleteIfExists();
                    result=temp_out.renameTo(to_saf);
                    if (move && result) result=from_saf.delete();
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            sendLogMsg("E","copyMoveFileLocalToLocal error:",e.toString());
            fileioThreadCtrl.setThreadMessage("copyMoveFileLocalToLocal error:"+e.toString());
            if (temp_out!=null) try {if (temp_out.exists()) temp_out.delete();} catch(Exception ex) {}
            result=false;
        }
        return result;
    }

    private boolean copyMoveFileRemoteToLocat(boolean move, JcifsFile from_jcifs, SafFile3 to_saf, String fromUrl, String toUrl) {
        boolean result=false;
        String title=move?"Moving":"Copying";
        SafFile3 temp_out=null;
        try {
            if (to_saf.getAppDirectoryCache()!=null && Build.VERSION.SDK_INT>=24) {
                temp_out=new SafFile3(mContext, to_saf.getAppDirectoryCache()+"/"+to_saf.getName());
                File tf=new File(temp_out.getPath());
                File df=new File(tf.getParent());
                if (!df.exists()) df.mkdirs();
                temp_out.createNewFile();
                copyFile(from_jcifs.getInputStream(), temp_out.getOutputStream(), from_jcifs.length(), title, from_jcifs.getName(), fromUrl, toUrl);
                if (!fileioThreadCtrl.isEnabled()) {
                    temp_out.deleteIfExists();
                    result=false;
                } else {
                    tf.setLastModified(from_jcifs.getLastModified());
                    to_saf.deleteIfExists();
                    result=temp_out.moveTo(to_saf);
                    if (move && result) from_jcifs.delete();
                }
            } else {
                temp_out=new SafFile3(mContext, toUrl+".tmp");
                temp_out.createNewFile();
                copyFile(from_jcifs.getInputStream(), temp_out.getOutputStream(), from_jcifs.length(), title, from_jcifs.getName(), fromUrl, toUrl);
                if (!fileioThreadCtrl.isEnabled()) {
                    temp_out.deleteIfExists();
                    result=false;
                } else {
                    to_saf.deleteIfExists();
                    result=temp_out.renameTo(to_saf);
                    if (move && result) from_jcifs.delete();
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            sendLogMsg("E","copyMoveFileRemoteToLocal error:",e.toString());
            fileioThreadCtrl.setThreadMessage("copyMoveFileRemoteToLocal error:"+e.toString());
            if (temp_out!=null) try {if (temp_out.exists()) temp_out.delete();} catch(Exception ex) {}
            result=false;
        }
        return result;
    }

    private boolean copyMoveFileLocalToRemote(boolean move, SafFile3 from_saf, JcifsFile to_jcifs, String fromUrl, String toUrl) {
        boolean result=false;
        String title=move?"Moving":"Copying";
        JcifsFile temp_out=null;
        try {
            temp_out=new JcifsFile(toUrl+"."+System.currentTimeMillis()+".tmp", to_jcifs.getAuth());
            copyFile(from_saf.getInputStream(), temp_out.getOutputStream(), from_saf.length(), title, from_saf.getName(), fromUrl, toUrl);
            if (!fileioThreadCtrl.isEnabled()) {
                temp_out.delete();
                result=false;
            } else {
                temp_out.setLastModified(from_saf.lastModified());
                if (to_jcifs.exists()) to_jcifs.delete();
                temp_out.renameTo(to_jcifs);
                result=true;
                if (move && result) result=from_saf.delete();
            }
        } catch(Exception e) {
            e.printStackTrace();
            sendLogMsg("E","copyMoveFileLocalToRemote error:",e.toString());
            fileioThreadCtrl.setThreadMessage("copyMoveFileLocalToRemote error:"+e.toString());
            if (temp_out!=null) try {if (temp_out.exists()) temp_out.delete();} catch(Exception ex) {}
            result=false;
        }
        return result;
    }

    private boolean copyMoveFileRemoteToRemote(boolean move, JcifsFile from_jcifs, JcifsFile to_jcifs, String fromUrl, String toUrl) {
        boolean result=false;
        String title=move?"Moving":"Copying";
        JcifsFile temp_out=null;
        try {
            temp_out=new JcifsFile(toUrl+"."+System.currentTimeMillis()+".tmp", to_jcifs.getAuth());
            copyFile(from_jcifs.getInputStream(), temp_out.getOutputStream(), from_jcifs.length(), title, from_jcifs.getName(), fromUrl, toUrl);
            if (!fileioThreadCtrl.isEnabled()) {
                temp_out.delete();
                result=false;
            } else {
                temp_out.setLastModified(from_jcifs.getLastModified());
                if (to_jcifs.exists()) to_jcifs.delete();
                temp_out.renameTo(to_jcifs);
                result=true;
                if (move && result) from_jcifs.delete();
            }
        } catch(Exception e) {
            e.printStackTrace();
            sendLogMsg("E","copyMoveFileRemoteToRemote error:",e.toString());
            fileioThreadCtrl.setThreadMessage("copyMoveFileRemoteToRemote error:"+e.toString());
            if (temp_out!=null) try {if (temp_out.exists()) temp_out.delete();} catch(Exception ex) {}
            result=false;
        }
        return result;
    }

    private boolean copyMoveRemoteToRemote(boolean move, JcifsAuth smb_auth_from, JcifsAuth smb_auth_to, String fromUrl, String toUrl)  {
        JcifsFile ihf,hfd, ohf = null;
        boolean result = false;
        if (!fileioThreadCtrl.isEnabled()) return false;
        sendDebugLogMsg(1,"I","Move Remote to Remote from item=",fromUrl,", to item=",toUrl);
		try {
			ihf = new JcifsFile(fromUrl ,smb_auth_from);
			if (ihf.isDirectory()) { // Directory copy
				result=true;
				hfd = new JcifsFile(fromUrl+"/",smb_auth_from);
				ohf = new JcifsFile(toUrl,smb_auth_from);
				
				String[] children = hfd.list();
				for (String element : children) {
					if (!fileioThreadCtrl.isEnabled()) return false;
	            	boolean success= copyMoveRemoteToRemote(move, smb_auth_from, smb_auth_to, fromUrl+"/"+element, toUrl+"/"+element );
	            	if (!success) return false;
	            }
				makeRemoteDirsByFilePath(smb_auth_to, toUrl+"/");
				if (move) {
				    ihf.delete();
                    sendLogMsg("I",fromUrl," was deleted.");
                }
			} else { // file move
				if (!fileioThreadCtrl.isEnabled()) return false;
				makeRemoteDirsByFilePath(smb_auth_to, toUrl);
                ohf=new JcifsFile(toUrl+"/",smb_auth_to);
                long b_time=System.currentTimeMillis();
                result=copyMoveFileRemoteToRemote(move, ihf, ohf, fromUrl, toUrl);
                putCopyMoveResultMessage(move, result, ihf.length(), b_time, fromUrl, toUrl, ihf.getName());
			}
		} catch (JcifsException e) {
			e.printStackTrace();
			sendLogMsg("E","Move error:",e.toString());
			fileioThreadCtrl.setThreadMessage("Move error:"+e.toString());
			result=false;
		} catch (IOException e) {
			e.printStackTrace();
			sendLogMsg("E","Move error:",e.toString());
			fileioThreadCtrl.setThreadMessage("Move error:"+e.toString());
			result=false;
		}
		return result;
    }
    
    private boolean downloadRemoteFile(JcifsAuth smb_auth, String fromUrl, String toUrl)  {
        JcifsFile hf,hfd;
        SafFile3 tf ;
        boolean result = false;
        sendDebugLogMsg(1,"I","Download Remote file, from item=",fromUrl,", to item=",toUrl);
		try {
			hf = new JcifsFile(fromUrl ,smb_auth);
            if (hf.getAttributes()<16384) { //no EA, copy was done
                makeLocalDirsByFilePath(toUrl);
                result=true;
                tf=new SafFile3(mContext, toUrl);
                if (!isFileDifferent(hf.getLastModified(), hf.length(), tf.lastModified(), tf.length())) {
                    sendDebugLogMsg(1,"I","Download was cancelled because file does not changed.");
                } else {
                    result= copyMoveFileRemoteToLocat(false, hf, tf, fromUrl, toUrl);//, "Downloading");
                }
            } else {
                result=false;
                sendLogMsg("E","EA founded, copy canceled. path=",fromUrl);
                fileioThreadCtrl.setThreadMessage("Download error:"+"EA founded, copy canceled");
            }
		} catch (JcifsException e) {
			e.printStackTrace();
			sendLogMsg("E","Download error:",e.toString());
			fileioThreadCtrl.setThreadMessage("Download error:"+e.toString());
			result=false;
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			sendLogMsg("E","Download error:",e.toString());
			fileioThreadCtrl.setThreadMessage("Download error:"+e.toString());
			result=false;
			return false;
		}
		return result;
    }

    private boolean copyFile(InputStream bis, OutputStream bos, long fileBytes,
                                    String title_header, String file_name, String fromUrl, String toUrl) throws IOException, JcifsException {
        int n=0;
        long tot = 0;
        sendMsgToProgDlg(String.format(title_header+" %s %s%% completed.", file_name,0));
        byte[] io_buff=new byte[1024*1024*2];
        while(( n = bis.read( io_buff)) > 0 ) {
            if (!fileioThreadCtrl.isEnabled()) {
                bis.close();
                bos.close();
                return false;
            }
            bos.write(io_buff, 0, n );
            tot += n;
            if (n<fileBytes)
                sendMsgToProgDlg(String.format(title_header+" %s %s%% completed.", file_name, (tot*100)/fileBytes));
        }
        sendMsgToProgDlg(String.format(title_header+" %s,  %s%% completed.",file_name, 100));
        bis.close();
        bos.flush();
        bos.close();

        return true;
    }

    private void scanMediaStoreLibraryFile(String fp) {
        if (Build.VERSION.SDK_INT<=23) {
            sendLogMsg("I","MediaScanner invoked, fp="+fp);
            MediaScannerConnection.scanFile(mContext, new String[]{fp}, null, null);
        }
	};

    private boolean isFileDifferent(long f1_lm, long f1_fl,long f2_lm, long f2_fl) {
    	boolean result=false;
    	if (f1_fl==f2_fl) {
    		long td=Math.abs(f1_lm-f2_lm);
    		if (td>=3000) result=true;//Allowance time is 3Seconds
    	} else result=true;
    	return result;
    };
    
	private boolean makeRemoteDirsByFilePath(JcifsAuth smb_auth, String targetPath)
					throws MalformedURLException, JcifsException {
		boolean result=false;
		String target_dir1="";
		String target_dir2="";
		if (targetPath.lastIndexOf("/")<=0) return false;
		else {
			if (targetPath.endsWith("/")) {//path is dir
				target_dir1=targetPath.substring(0,targetPath.lastIndexOf("/"));
			} else {
				target_dir1=targetPath;
			}
			target_dir2=target_dir1.substring(0,target_dir1.lastIndexOf("/"));
		}

        result=makeRemoteDirsByDirectoryPath(smb_auth, target_dir2 + "/");
		return result;
	}

    private boolean makeRemoteDirsByDirectoryPath(JcifsAuth smb_auth, String targetPath)
            throws MalformedURLException, JcifsException {
        boolean result=false;
        JcifsFile hf = new JcifsFile(targetPath + "/",smb_auth);
        if (!hf.exists()) {
            hf.mkdirs();
        }
        return result;
    };


    private boolean makeLocalDirsByFilePath(String targetPath) {
		boolean result=false;
		String target_dir="";
		if (targetPath.lastIndexOf("/")<=0) return false;
		else target_dir=targetPath.substring(0,targetPath.lastIndexOf("/"));
		result=makeLocalDirsByDirectoryPath(target_dir);
		return result;
	};

    private boolean makeLocalDirsByDirectoryPath(String targetPath) {
        boolean result=false;
        SafFile3 sf=new SafFile3(mContext, targetPath);
        if (!sf.exists()) result=sf.mkdirs();
        else result=true;
        return result;
    }

    private String calTransferRate(long tb, long tt) {
	    long et=tt;
	    if (et==0L) et=1;
	    String tfs = null;
	    BigDecimal bd_tr;
	    if (tb>(1024*1024)) {//Mib
            BigDecimal dfs1 = new BigDecimal(tb * 1.000);
            BigDecimal dfs2 = new BigDecimal(1024 * 1024.000);
            BigDecimal dfs3 = new BigDecimal("0.000000");
            dfs3 = dfs1.divide(dfs2);
            BigDecimal dft1 = new BigDecimal(et * 1.000);
            BigDecimal dft2 = new BigDecimal(1000.000);
            BigDecimal dft3 = new BigDecimal("0.000000");
            dft3 = dft1.divide(dft2);
            bd_tr = dfs3.divide(dft3, 2, BigDecimal.ROUND_HALF_UP);
            tfs = bd_tr + "MiB/sec";
        } else if (tb>(1024)) {//KiB
            BigDecimal dfs1 = new BigDecimal(tb*1.000);
            BigDecimal dfs2 = new BigDecimal(1024*1.000);
            BigDecimal dfs3 = new BigDecimal("0.000000");
            dfs3=dfs1.divide(dfs2);
            BigDecimal dft1 = new BigDecimal(et*1.000);
            BigDecimal dft2 = new BigDecimal(1000.000);
            BigDecimal dft3 = new BigDecimal("0.000000");
            dft3=dft1.divide(dft2);
            bd_tr=dfs3.divide(dft3,2,BigDecimal.ROUND_HALF_UP);
            tfs=bd_tr+"KiB/sec";

        } else {
		    BigDecimal dfs1 = new BigDecimal(tb*1.000);
		    BigDecimal dfs2 = new BigDecimal(1024*1.000);
		    BigDecimal dfs3 = new BigDecimal("0.000000");
		    dfs3=dfs1.divide(dfs2);
			BigDecimal dft1 = new BigDecimal(et*1.000);
		    BigDecimal dft2 = new BigDecimal(1000.000);
		    BigDecimal dft3 = new BigDecimal("0.000000");
		    dft3=dft1.divide(dft2);
		    if (dft3.toString().equals("0")) bd_tr=new BigDecimal("0");
			else bd_tr=dfs3.divide(dft3,2,BigDecimal.ROUND_HALF_UP);
			tfs=bd_tr+"Bytes/sec";
		}
		
		return tfs;
	};
}
