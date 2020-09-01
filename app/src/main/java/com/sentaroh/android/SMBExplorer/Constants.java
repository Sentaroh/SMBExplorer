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

public class Constants {
	
	public final static String APPLICATION_TAG="SMBExplorer";
	public final static String PACKAGE_NAME="com.sentaroh.android.SMBExplorer";
    public final static String APP_SPECIFIC_DIRECTORY="Android/data/"+PACKAGE_NAME;
	public static final String DEFAULT_PREFS_FILENAME="default_preferences";

    public final static String SERVICE_HEART_BEAT="com.sentaroh.android."+APPLICATION_TAG+".ACTION_SERVICE_HEART_BEAT";

    final public static int FILEIO_PARM_LOCAL_CREATE = 1;
	final public static int FILEIO_PARM_LOCAL_RENAME = 2;
	final public static int FILEIO_PARM_LOCAL_DELETE = 3;
	final public static int FILEIO_PARM_REMOTE_CREATE = 4;
	final public static int FILEIO_PARM_REMOTE_RENAME = 5;
	final public static int FILEIO_PARM_REMOTE_DELETE = 6;
	final public static int FILEIO_PARM_COPY_REMOTE_TO_LOCAL = 7;
	final public static int FILEIO_PARM_COPY_REMOTE_TO_REMOTE = 8;
	final public static int FILEIO_PARM_COPY_LOCAL_TO_LOCAL = 9;
	final public static int FILEIO_PARM_COPY_LOCAL_TO_REMOTE = 10;
	final public static int FILEIO_PARM_MOVE_REMOTE_TO_LOCAL = 11;
	final public static int FILEIO_PARM_MOVE_REMOTE_TO_REMOTE = 12;
	final public static int FILEIO_PARM_MOVE_LOCAL_TO_LOCAL = 13;
	final public static int FILEIO_PARM_MOVE_LOCAL_TO_REMOTE = 14;
	final public static int FILEIO_PARM_DOWLOAD_REMOTE_FILE = 15;
	final public static String SMBEXPLORER_PROFILE_NAME = "config_list.xml";
	
	final public static int MAX_DLG_BOX_SIZE_WIDTH=600; 
	final public static int MAX_DLG_BOX_SIZE_HEIGHT=0;
	
	final public static String SMBEXPLORER_TAB_LOCAL="Local";
    final public static int SMBEXPLORER_TAB_POS_LOCAL=0;
	final public static String SMBEXPLORER_TAB_REMOTE="Remote";
	final public static int SMBEXPLORER_TAB_POS_REMOTE=1;

    public static final String SMBEXPLORER_KEY_STORE_ALIAS = "SMBExplorer";
}
