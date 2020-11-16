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


import com.sentaroh.android.Utilities3.SafFile3;

public class FileIoLinkParm {
	FileIoLinkParm () {}
	
    private String fromUrl="", fromDomain="", fromSmbLevel="3", fromUser="", fromPass="", fromBaseUrl="", fromName="";
	private boolean fromSmbOptionIpcSignEnforce=true;
    private boolean fromSmbOptionUseSMB2Negotiation=false;
    private String toUrl="", toDomain="", toSmbLevel="3", toUser="", toPass="", toBaseUrl="", toFileName="";
    private boolean toSmbOptionIpcSignEnforce=true;
    private boolean toSmbOptionUseSMB2Negotiation=false;

    private SafFile3 fromSafRoot=null;
    private SafFile3 toSafRoot=null;

    public void setToSafRoot(SafFile3 rt) {toSafRoot=rt;}
    public SafFile3 getToSafRoot() {return toSafRoot;}
    public void setFromSafRoot(SafFile3 rt) {fromSafRoot=rt;}
    public SafFile3 getFromSafRoot() {return fromSafRoot;}

    public boolean isFromSmbOptionIpcSignEnforce() {return fromSmbOptionIpcSignEnforce;}
    public void setFromSmbOptionIpcSignEnforce(boolean p) {fromSmbOptionIpcSignEnforce=p;}

    public boolean isFromSmbOptionUseSMB2Negotiation() {return fromSmbOptionUseSMB2Negotiation;}
    public void setFromSmbOptionUseSMB2Negotiation(boolean p) {fromSmbOptionUseSMB2Negotiation=p;}

    public boolean isToSmbOptionIpcSignEnforce() {return toSmbOptionIpcSignEnforce;}
    public void setToSmbOptionIpcSignEnforce(boolean p) {toSmbOptionIpcSignEnforce=p;}

    public boolean isToSmbOptionUseSMB2Negotiation() {return toSmbOptionUseSMB2Negotiation;}
    public void setToSmbOptionUseSMB2Negotiation(boolean p) {toSmbOptionUseSMB2Negotiation=p;}

    public void setFromDirectory(String url) {fromUrl=url;}
    public void setFromName(String name) {fromName=name;}
    public void setFromBaseDirectory(String url) {fromBaseUrl=url;}
    public void setFromDomain(String domain) {fromDomain=domain;}
    public void setFromSmbLevel(String smb_level) {fromSmbLevel=smb_level;}
    public void setFromUser(String url) {fromUser=url;}
    public void setFromPass(String url) {fromPass=url;}
    public String getFromDirectory() {return fromUrl;}
    public String getFromName() {return fromName;}
    public String getFromBaseDirectory() {return fromBaseUrl;}
    public String getFromDomain() {return fromDomain;}
    public String getFromSmbLevel() {return fromSmbLevel;}
    public String getFromUser() {return fromUser;}
    public String getFromPass() {return fromPass;}

    public void setToDirectory(String url) {toUrl=url;}
    public void setToName(String name) {toFileName=name;}
    public void setToBaseDirectory(String url) {toBaseUrl=url;}
    public void setToDomain(String domain) {toDomain=domain;}
    public void setToSmbLevel(String smb_level) {toSmbLevel=smb_level;}
    public void setToUser(String url) {toUser=url;}
    public void setToPass(String url) {toPass=url;}
    public String getToDirectory() {return toUrl;}
    public String getToName() {return toFileName;}
    public String getToBaseDirectory() {return toBaseUrl;}
    public String getToDomain() {return toDomain;}
    public String getToSmbLevel() {return toSmbLevel;}
    public String getToUser() {return toUser;}
    public String getToPass() {return toPass;}
    

}
