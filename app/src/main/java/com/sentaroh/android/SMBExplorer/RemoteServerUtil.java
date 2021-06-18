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
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Handler;
import android.util.Xml;

import com.sentaroh.android.Utilities3.Base64Compat;
import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.Dialog.CommonFileSelector2;
import com.sentaroh.android.Utilities3.EncryptUtilV3;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.ThreadCtrl;
import com.sentaroh.android.Utilities3.Widget.CustomSpinnerAdapter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static android.content.Context.MODE_PRIVATE;
import static com.sentaroh.android.SMBExplorer.Constants.SMBEXPLORER_KEY_STORE_ALIAS;
import static com.sentaroh.android.SMBExplorer.Constants.SMBEXPLORER_PROFILE_NAME;
import static com.sentaroh.android.SMBExplorer.RemoteServerConfig.SERVER_TYPE_SFTP;
import static com.sentaroh.android.SMBExplorer.RemoteServerConfig.SERVER_TYPE_SMB;

public class RemoteServerUtil {

    public static RemoteServerConfig getRemoteServerConfigItem(String name, ArrayList<RemoteServerConfig> sl) {
        RemoteServerConfig result=null;
        for(RemoteServerConfig item:sl) {
            if (item.getName().equals(name)) {
                result=item;
                break;
            }
        }
        return result;
    }

    static public void saveRemoteServerConfigList(Context c, GlobalParameter gp) {
        saveRemoteServerConfigList(c, gp, false, null);
    }

    private static final String CONFIG_TAG_CONFIG="config_list";
    private static final String CONFIG_TAG_CONFIG_VERSION="version";
    private static final String CONFIG_TAG_SERVER="server";
    private static final String CONFIG_TAG_SERVER_NAME="name";
    private static final String CONFIG_TAG_SERVER_TYPE ="type";
    private static final String CONFIG_TAG_SERVER_SMB_DOMAIN ="smb_domain";
    private static final String CONFIG_TAG_SERVER_SMB_USER ="smb_user";
    private static final String CONFIG_TAG_SERVER_SMB_PASSWORD ="smb_password";
    private static final String CONFIG_TAG_SERVER_SMB_HOST ="smb_host";
    private static final String CONFIG_TAG_SERVER_SMB_PORT ="smb_port";
    private static final String CONFIG_TAG_SERVER_SMB_SHARE ="smb_share";
    private static final String CONFIG_TAG_SERVER_SFTP_BASE_DIRECTORY ="sftp_base_directory";
    private static final String CONFIG_TAG_SERVER_SMB_LEVEL ="smb_level";

    private static final String CONFIG_TAG_SERVER_SMB_OPTION_IPC_SIGN_ENFORCE ="smb_option_ipc_sign_enforce";
    private static final String CONFIG_TAG_SERVER_SMB_OPTION_USE_SMB2_NEGOTIATION ="smb_option_use_smb2_negotiation";

    static public ArrayList<RemoteServerConfig> createRemoteServerConfigList(Context c, GlobalParameter gp, boolean sdcard, final Uri file_uri) {

        ArrayList<RemoteServerConfig> rem = new ArrayList<RemoteServerConfig>();
        boolean init_smb_list=false;
        InputStream fis = null;
        try {
            String priv_key=null;
            EncryptUtilV3.CipherParms cp_int=null;
            if (sdcard) {
                SafFile3 sf = new SafFile3(c, file_uri);
                if (sf.exists()) {
                    fis = sf.getInputStream();
                } else {
                    gp.commonDlg.showCommonDialog(false,"E",
                            String.format(gp.context.getString(R.string.msgs_local_file_list_create_file_not_found), file_uri.getPath()),"",null);
                    init_smb_list=true;
                }
            } else {
                priv_key= KeyStoreUtility.getGeneratedPassword(gp.context, SMBEXPLORER_KEY_STORE_ALIAS);
                cp_int= EncryptUtilV3.initCipherEnv(priv_key);
                fis = gp.context.openFileInput(SMBEXPLORER_PROFILE_NAME);
            }
            if (!init_smb_list) {
                XmlPullParser xpp = Xml.newPullParser();
                xpp.setInput(new BufferedReader(new InputStreamReader(fis)));
                int eventType = xpp.getEventType();
                String config_ver="";
                while(eventType != XmlPullParser.END_DOCUMENT){
                    switch(eventType){
                        case XmlPullParser.START_DOCUMENT:
                            gp.mUtil.addDebugMsg(2,"I","createSmbServerConfigList Start Document");
                            break;
                        case XmlPullParser.START_TAG:
                            gp.mUtil.addDebugMsg(2,"I","createSmbServerConfigList Start Tag="+xpp.getName());
                            if (xpp.getName().equals(CONFIG_TAG_CONFIG)) {
                                if (xpp.getAttributeCount()==1) {
                                    config_ver=xpp.getAttributeValue(0);
                                    gp.mUtil.addDebugMsg(2,"I","createSmbServerConfigList Version="+xpp.getAttributeValue(0));
                                }
                            } else if (xpp.getName().equals(CONFIG_TAG_SERVER)) {
                                RemoteServerConfig smb_item= createRemoteServerConfigItemFromXmlTag(gp, xpp, cp_int);
                                smb_item.setVersion(config_ver);
                                rem.add(smb_item);
                            }
                            break;
                        case XmlPullParser.TEXT:
                            gp.mUtil.addDebugMsg(2,"I","createSmbServerConfigList Text=" + xpp.getText()+", name="+xpp.getName());
                            break;
                        case XmlPullParser.END_TAG:
                            gp.mUtil.addDebugMsg(2,"I", "createSmbServerConfigList End Tag="+xpp.getName());
                            break;
                        case XmlPullParser.END_DOCUMENT:
                            gp.mUtil.addDebugMsg(2,"I", "createSmbServerConfigList End Document="+xpp.getName());
                            break;
                    }
                    eventType = xpp.next();
                }
                gp.mUtil.addDebugMsg(2,"I","createSmbServerConfigList End of document");

                fis.close();
            }
        } catch (XmlPullParserException e) {
            gp.mUtil.addDebugMsg(1,"I","createSmbServerConfigList XML Parse error, error="+e.getMessage());
            e.printStackTrace();
            init_smb_list=true;
        } catch (FileNotFoundException e) {
            if (sdcard) {
                gp.mUtil.addDebugMsg(1,"E",e.toString());
                gp.commonDlg.showCommonDialog(false,"E", gp.context.getString(R.string.msgs_exception),e.toString(),null);
            }
            init_smb_list=true;
        } catch (IOException e) {
            gp.mUtil.addDebugMsg(0,"E",e.toString());
            gp.commonDlg.showCommonDialog(false,"E", gp.context.getString(R.string.msgs_exception),e.toString(),null);
            init_smb_list=true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collections.sort(rem);
//        init_smb_list=true;
        if (init_smb_list) {
            rem.clear();
            rem.add(new RemoteServerConfig("HOME-D", SERVER_TYPE_SMB, "", "","","192.168.200.128", "", "D"));
            rem.add(new RemoteServerConfig("HOME-E", SERVER_TYPE_SMB, "", "","","192.168.200.128", "", "E"));
            rem.add(new RemoteServerConfig("HOME-F", SERVER_TYPE_SMB, "", "","","192.168.200.128", "", "F"));
            rem.add(new RemoteServerConfig("NAS-3", SERVER_TYPE_SMB, "", "","","192.168.200.40", "", "SHARE"));
            rem.add(new RemoteServerConfig("SRV-D", SERVER_TYPE_SMB, "", "","","192.168.200.10", "", "D"));

            rem.add(new RemoteServerConfig("SFTP-D", SERVER_TYPE_SFTP, "","","192.168.200.128", "", "/d:"));
            rem.add(new RemoteServerConfig("SFTP-E", SERVER_TYPE_SFTP, "","","192.168.200.128", "", "/e:"));
            rem.add(new RemoteServerConfig("SFTP-F", SERVER_TYPE_SFTP, "","","192.168.200.128", "", "/f:"));
            rem.add(new RemoteServerConfig("SFTP-NAS3", SERVER_TYPE_SFTP, "","","192.168.200.40", "", "/data/share"));
            rem.add(new RemoteServerConfig("SFTP-MPAD5", SERVER_TYPE_SFTP, "ssh1","ssh1","192.168.200.193", "2222", "/"));
        }
        return rem;
    }

    static private RemoteServerConfig createRemoteServerConfigItemFromXmlTag(GlobalParameter gp, XmlPullParser xpp, EncryptUtilV3.CipherParms cp_int) {
        RemoteServerConfig smb_item=new RemoteServerConfig();
        int ac=xpp.getAttributeCount();
        for(int i=0;i<ac;i++) {
            gp.mUtil.addDebugMsg(2,"I","createSmbServerConfigItemFromXmlTag Attribute="+xpp.getAttributeName(i)+", Value="+xpp.getAttributeValue(i));
            if (xpp.getAttributeName(i).equals(CONFIG_TAG_SERVER_NAME)) {smb_item.setName(xpp.getAttributeValue(i));}
            else if (xpp.getAttributeName(i).equals(CONFIG_TAG_SERVER_TYPE)) {smb_item.setType(xpp.getAttributeValue(i));}
            else if (xpp.getAttributeName(i).equals(CONFIG_TAG_SERVER_SMB_DOMAIN)) {smb_item.setSmbDomain(xpp.getAttributeValue(i));}
            else if (xpp.getAttributeName(i).equals(CONFIG_TAG_SERVER_SMB_USER)) {
                if (!xpp.getAttributeValue(i).equals("")) {
                    try {
                        byte[] dec_array = Base64Compat.decode(xpp.getAttributeValue(i), Base64Compat.NO_WRAP);
                        String dec_str = EncryptUtilV3.decrypt(dec_array, cp_int);
                        smb_item.setUser(dec_str);
                    } catch(Exception e) {
                    }
                }
            } else if (xpp.getAttributeName(i).equals(CONFIG_TAG_SERVER_SMB_PASSWORD)) {
                if (!xpp.getAttributeValue(i).equals("")) {
                    try {
                        byte[] dec_array = Base64Compat.decode(xpp.getAttributeValue(i), Base64Compat.NO_WRAP);
                        String dec_str = EncryptUtilV3.decrypt(dec_array, cp_int);
                        smb_item.setPassword(dec_str);
                    } catch(Exception e) {
                    }
                }
            } else if (xpp.getAttributeName(i).equals(CONFIG_TAG_SERVER_SMB_HOST)) {smb_item.setHost(xpp.getAttributeValue(i));}
            else if (xpp.getAttributeName(i).equals(CONFIG_TAG_SERVER_SMB_PORT)) {smb_item.setPort(xpp.getAttributeValue(i));}
            else if (xpp.getAttributeName(i).equals(CONFIG_TAG_SERVER_SMB_SHARE)) {smb_item.setSmbShare(xpp.getAttributeValue(i));}
            else if (xpp.getAttributeName(i).equals(CONFIG_TAG_SERVER_SFTP_BASE_DIRECTORY)) {smb_item.setBaseDirectory(xpp.getAttributeValue(i));}
            else if (xpp.getAttributeName(i).equals(CONFIG_TAG_SERVER_SMB_LEVEL)) {smb_item.setSmbLevel(xpp.getAttributeValue(i));}
            else if (xpp.getAttributeName(i).equals(CONFIG_TAG_SERVER_SMB_OPTION_IPC_SIGN_ENFORCE)) {smb_item.setSmbOptionIpcSigningEnforced((xpp.getAttributeValue(i).toLowerCase()).equals("true")?true:false);}
            else if (xpp.getAttributeName(i).equals(CONFIG_TAG_SERVER_SMB_OPTION_USE_SMB2_NEGOTIATION)) {smb_item.setSmbOptionUseSMB2Negotiation((xpp.getAttributeValue(i).toLowerCase()).equals("true")?true:false);}

        }
        return smb_item;
    }

    static public void saveRemoteServerConfigList(Context c, GlobalParameter gp, boolean sdcard, final Uri file_uri) {
        PrintWriter pw;
        BufferedWriter bw = null;
        try {
            String priv_key=null;
            EncryptUtilV3.CipherParms cp_int=null;
            OutputStream profile_out=null;
            if (sdcard) {
                SafFile3 lf = new SafFile3(c, file_uri);
                SafFile3 df=lf.getParentFile();
                if (!df.exists()) df.mkdirs();
                if (!lf.exists()) lf.createNewFile();
                profile_out=lf.getOutputStream();
            } else {
                priv_key= KeyStoreUtility.getGeneratedPassword(gp.context, SMBEXPLORER_KEY_STORE_ALIAS);
                cp_int= EncryptUtilV3.initCipherEnv(priv_key);

                profile_out=gp.context.openFileOutput(SMBEXPLORER_PROFILE_NAME, MODE_PRIVATE);
            }

            if (gp.smbConfigList !=null && gp.smbConfigList.size()>0) {
                try {
                    DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dbuilder = dbfactory.newDocumentBuilder();

                    Document main_document = dbuilder.newDocument();
                    Element config_tag = main_document.createElement(CONFIG_TAG_CONFIG);
                    config_tag.setAttribute(CONFIG_TAG_CONFIG_VERSION, "1.0.2");

                    for(RemoteServerConfig item:gp.smbConfigList) {
                        Element server_tag = main_document.createElement(CONFIG_TAG_SERVER);
                        server_tag.setAttribute(CONFIG_TAG_SERVER_NAME, item.getName());
                        config_tag.appendChild(server_tag);

                        server_tag.setAttribute(CONFIG_TAG_SERVER_TYPE, item.getType());
                        server_tag.setAttribute(CONFIG_TAG_SERVER_SMB_DOMAIN, item.getSmbDomain());
                        if (sdcard) {
                            //Do not write User and Password data
                            server_tag.setAttribute(CONFIG_TAG_SERVER_SMB_USER, item.getUser());
                            server_tag.setAttribute(CONFIG_TAG_SERVER_SMB_PASSWORD, item.getPassword());
                        } else {
                            if (item.getUser()!=null && !item.getUser().equals("")) {
                                String enc =Base64Compat.encodeToString(EncryptUtilV3.encrypt(item.getUser(), cp_int), Base64Compat.NO_WRAP);
                                server_tag.setAttribute(CONFIG_TAG_SERVER_SMB_USER, enc);
                            }
                            if (item.getPassword()!=null &&!item.getPassword().equals("")) {
                                String enc =Base64Compat.encodeToString(EncryptUtilV3.encrypt(item.getPassword(), cp_int), Base64Compat.NO_WRAP);
                                server_tag.setAttribute(CONFIG_TAG_SERVER_SMB_PASSWORD, enc);
                            }
                        }
                        server_tag.setAttribute(CONFIG_TAG_SERVER_SMB_HOST, item.getHost());
                        server_tag.setAttribute(CONFIG_TAG_SERVER_SMB_PORT, item.getPort());
                        server_tag.setAttribute(CONFIG_TAG_SERVER_SMB_SHARE, item.getSmbShare());
                        server_tag.setAttribute(CONFIG_TAG_SERVER_SFTP_BASE_DIRECTORY, item.getBaseDirectory());
                        server_tag.setAttribute(CONFIG_TAG_SERVER_SMB_LEVEL, item.getSmbLevel());
                        server_tag.setAttribute(CONFIG_TAG_SERVER_SMB_OPTION_IPC_SIGN_ENFORCE, item.isSmbOptionIpcSigningEnforced()?"true":"false");
                        server_tag.setAttribute(CONFIG_TAG_SERVER_SMB_OPTION_USE_SMB2_NEGOTIATION, item.isSmbOptionUseSMB2Negotiation()?"true":"false");
                    }

                    main_document.appendChild(config_tag);

                    TransformerFactory tffactory = TransformerFactory.newInstance();
                    Transformer transformer = tffactory.newTransformer();
                    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount","5");

                    StringWriter sw=new StringWriter();
                    transformer.transform(new DOMSource(main_document), new StreamResult(sw));
                    sw.flush();
                    sw.close();
                    pw = new PrintWriter(new OutputStreamWriter(profile_out, "UTF-8"));
//                    String prof=sw.toString().replaceAll("<"+CONFIG_TAG_CONFIG, "\n<"+CONFIG_TAG_CONFIG)
//                            .replaceAll("</"+CONFIG_TAG_CONFIG, "\n</"+CONFIG_TAG_CONFIG)
//                            .replaceAll("<"+CONFIG_TAG_SERVER,"\n     <"+CONFIG_TAG_SERVER);
                    String prof=sw.toString();
                    pw.println(prof);
                    pw.flush();
                    pw.close();
                    gp.mUtil.addDebugMsg(2,"I","out=\n"+prof);
                }catch (TransformerConfigurationException e) {
                    e.printStackTrace();
                } catch (TransformerException e) {
                    e.printStackTrace();
                }

            }
//            profile_out.flush();
//            profile_out.close();
        } catch (IOException e) {
            gp.mUtil.addDebugMsg(0,"E",e.toString());
            gp.commonDlg.showCommonDialog(false,"E",gp.context.getString(R.string.msgs_exception),e.toString(),null);
        } catch (Exception e) {
            e.printStackTrace();
            gp.mUtil.addDebugMsg(0,"E",e.toString());
            gp.commonDlg.showCommonDialog(false,"E",gp.context.getString(R.string.msgs_exception),e.toString(),null);
        }
    }

    static public void importRemoteServerConfigDlg(ActivityMain activity, GlobalParameter gp, final String curr_dir, String file_name) {

        gp.mUtil.addDebugMsg(1,"I","Import profile dlg.");

        NotifyEvent ne=new NotifyEvent(gp.context);
        // set commonDialog response
        ne.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c,Object[] o) {
                Uri file_uri=(Uri)o[0];

                ArrayList<RemoteServerConfig> tfl = createRemoteServerConfigList(activity.getApplicationContext(), gp, true, file_uri);
                if (tfl!=null) {
                    gp.smbConfigList =tfl;
                    saveRemoteServerConfigList(activity.getApplicationContext(), gp);
                    updateRemoteShareSpinner(gp);
                    gp.commonDlg.showCommonDialog(false,"I",gp.context.getString(R.string.msgs_select_import_dlg_success), "", null);
                }

            }

            @Override
            public void negativeResponse(Context c,Object[] o) {}
        });
        boolean include_root=false;
        boolean scoped_storage_mode=gp.safMgr.isScopedStorageMode();
        CommonFileSelector2 fsdf=
                CommonFileSelector2.newInstance(scoped_storage_mode, true, false, CommonFileSelector2.DIALOG_SELECT_CATEGORY_FILE,
                        true, true, curr_dir, "/SMBExplorer", file_name, "Select import file.");
        fsdf.showDialog(false, activity.getSupportFragmentManager(), fsdf, ne);
//        gp.commonDlg.fileSelectorFileOnlyWithCreate(true, curr_dir, "/SMBExplorer",file_name,"Select import file.",ne);
    }

    static public void exportRemoteServerConfigListDlg(ActivityMain activity, GlobalParameter gp, final String curr_dir, final String ifn) {
        gp.mUtil.addDebugMsg(1,"I","Export profile.");

        NotifyEvent ne=new NotifyEvent(gp.context);
        // set commonDialog response
        ne.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                Uri file_uri=(Uri)o[0];
                writeRemoteServerConfigList(activity.getApplicationContext(), gp, file_uri);
            }

            @Override
            public void negativeResponse(Context c,Object[] o) {}
        });
        boolean scoped_storage_mode=gp.safMgr.isScopedStorageMode();
        CommonFileSelector2 fsdf=
                CommonFileSelector2.newInstance(scoped_storage_mode, true, false, CommonFileSelector2.DIALOG_SELECT_CATEGORY_FILE,
                        true, curr_dir, "/SMBExplorer", ifn, "Select import file.");
        fsdf.showDialog(false, activity.getSupportFragmentManager(), fsdf, ne);
//        gp.commonDlg.fileSelectorFileOnlyWithCreate(true, curr_dir, "/SMBExplorer",ifn,"Select export file.",ne);
    }

    static public void writeRemoteServerConfigList(Context c, GlobalParameter gp, final Uri file_uri) {
        gp.mUtil.addDebugMsg(1,"I","Export profile to file");

        SafFile3 lf = new SafFile3(c, file_uri);

        if (lf.exists()) {
            NotifyEvent ne=new NotifyEvent(gp.context);
            // set commonDialog response
            ne.setListener(new NotifyEvent.NotifyEventListener() {
                @Override
                public void positiveResponse(Context c,Object[] o) {
                    saveRemoteServerConfigList(c, gp, true, file_uri);
                    gp.commonDlg.showCommonDialog(false,"I",gp.context.getString(R.string.msgs_select_export_dlg_success),
                            file_uri.getPath(), null);
                }

                @Override
                public void negativeResponse(Context c,Object[] o) {}
            });
            gp.commonDlg.showCommonDialog(true,"I",
                    String.format(gp.context.getString(R.string.msgs_select_export_dlg_override),
                            file_uri.getPath()),"",ne);
            return;
        } else {
            saveRemoteServerConfigList(c, gp, true, file_uri);
            gp.commonDlg.showCommonDialog(false,"I", gp.context.getString(R.string.msgs_select_export_dlg_success),
                    file_uri.getPath(), null);
        }
    }

    static public void createRemoteServerFileList(ActivityMain activity, GlobalParameter gp, String opcd,
                                                  String url, RemoteServerConfig sc, final NotifyEvent n_event) {
        final ArrayList<FileListItem> remoteFileList=new ArrayList<FileListItem>();

        final ThreadCtrl tc = new ThreadCtrl();
        tc.setEnabled();

        final Dialog pi_dialog= CommonDialog.showProgressSpinIndicator(activity);
        pi_dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                tc.setDisabled();//disableAsyncTask();
                gp.mUtil.addDebugMsg(1, "W", "CreateRemoteFileList cancelled.");
            }
        });
        final Handler hndl=new Handler();
        NotifyEvent ne=new NotifyEvent(gp.context);
        ne.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c,Object[] o) {
                pi_dialog.dismiss();
                if (tc.isThreadResultSuccess()) {
                    hndl.post(new Runnable() {
                        @Override
                        public void run() {
                            n_event.notifyToListener(true, new Object[]{remoteFileList});
                        }
                    });
                } else {
                    hndl.post(new Runnable() {
                        @Override
                        public void run() {
                            String err="";
                            if (tc.isThreadResultCancelled()) err="Filelist was cancelled";
                            else err=tc.getThreadMessage();
                            n_event.notifyToListener(false, new Object[]{err});
                        }
                    });
                }
            }
            @Override
            public void negativeResponse(Context c,Object[] o) {
            }
        });

        Thread th = new RetrieveFileList(gp, tc, opcd, url, remoteFileList, sc, ne);
        th.start();
        pi_dialog.show();
    }

    static public void updateRemoteShareSpinner(GlobalParameter gp) {
        final CustomSpinnerAdapter spAdapter = (CustomSpinnerAdapter)gp.remoteFileListDirSpinner.getAdapter();
        int sel_no=gp.remoteFileListDirSpinner.getSelectedItemPosition();
        if (spAdapter.getItem(0).startsWith("---")) {
            spAdapter.clear();
            spAdapter.add("--- Not selected ---");
        } else {
            spAdapter.clear();
        }
        int a_no=0;
        for (int i = 0; i<gp.smbConfigList.size(); i++) {
            spAdapter.add(gp.smbConfigList.get(i).getName());
        }
    }

    static public void replaceCurrentRemoteServerConfig(GlobalParameter gp) {
        if (gp.currentRemoteServerConfig ==null) return;
        for(RemoteServerConfig item:gp.smbConfigList) {
            if (item.getName().equals(gp.currentRemoteServerConfig.getName())) {
                gp.currentRemoteServerConfig =item;
                break;
            }
        }
    }

}
