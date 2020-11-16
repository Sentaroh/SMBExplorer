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

import com.sentaroh.jcifs.JcifsAuth;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

class SmbServerConfig implements Serializable, Cloneable, Comparable<SmbServerConfig>{
    private String profileVersion="1.0.0";
	private String profileType="R";
	private String profileName="No name";
    private String profileDomain="";
	private String profileUser="";
	private String profilePass="";
	private String profileAddr="";
	private String profilePort="";
	private String profileShare="";
    private String profileSmbLevel=String.valueOf(JcifsAuth.JCIFS_FILE_SMB214);
    private boolean profileSmbOptionIpcSigningEnforced=true;
    private boolean profileSmbOptionUseSMB2Negotiation=false;
	private boolean profileIsChecked=false;

    @Override
    public SmbServerConfig clone() {
        SmbServerConfig npfli = null;
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

            npfli = (SmbServerConfig) ois.readObject();
            ois.close();
            bais.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return npfli;
    }

    public SmbServerConfig(){}

    public SmbServerConfig(String pfn,
                           String domain, String pf_user, String pf_pass, String pf_addr, String pf_port, String pf_share){
        profileType = SERVER_TYPE_SMB;
        profileName = pfn;
        profileDomain = domain;
        profileUser = pf_user;
        profilePass = pf_pass;
        profileAddr = pf_addr;
        profilePort = pf_port;
        profileShare = pf_share;
        profileIsChecked = false;

    }

    public void setVersion(String version) {profileVersion=version;}
    public String getVersion() {return profileVersion;}

    public String getType(){return profileType;}
    public static final String SERVER_TYPE_SMB="R";
    public void setType(String type) {profileType=type;}
	public String getName(){return profileName;}
    public void setName(String name){profileName=name;}

    public String getSmbDomain(){return profileDomain;}
    public void setSmbDomain(String domain){profileDomain=domain;}

    public String getSmbUser(){return profileUser;}
    public void setSmbUser(String user){profileUser=user;}

	public String getSmbPass(){return profilePass;}
    public void setSmbPassword(String pass){profilePass=pass;}

	public String getSmbHost(){return profileAddr;}
    public void setSmbHost(String addr){profileAddr=addr;}

	public String getSmbPort(){return profilePort;}
    public void setSmbPort(String port){profilePort=port;}

	public String getSmbShare(){return profileShare;}
    public void setSmbShare(String share){profileShare=share;}

    public String getSmbLevel(){return profileSmbLevel;}
    public void setSmbLevel(String level){profileSmbLevel=level;}

    public boolean isSmbOptionIpcSigningEnforced(){return profileSmbOptionIpcSigningEnforced;}
    public void setSmbOptionIpcSigningEnforced(boolean p){profileSmbOptionIpcSigningEnforced=p;}

    public boolean isSmbOptionUseSMB2Negotiation(){return profileSmbOptionUseSMB2Negotiation;}
    public void setSmbOptionUseSMB2Negotiation(boolean p){profileSmbOptionUseSMB2Negotiation=p;}

    public boolean isChecked(){return profileIsChecked;}
	public void setChecked(boolean p){profileIsChecked=p;}

	@Override
	public int compareTo(SmbServerConfig o) {
		if(this.profileName != null)
			return this.profileName.toLowerCase().compareTo(o.getName().toLowerCase()) ;
//			return this.filename.toLowerCase().compareTo(o.getName().toLowerCase()) * (-1);
		else
			throw new IllegalArgumentException();
	}
}
