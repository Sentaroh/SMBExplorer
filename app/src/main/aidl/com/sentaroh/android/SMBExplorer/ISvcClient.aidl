package com.sentaroh.android.SMBExplorer;

import com.sentaroh.android.SMBExplorer.ISvcCallback;

interface ISvcClient{
	
	void setCallBack(ISvcCallback callback);
	void removeCallBack(ISvcCallback callback);

	void aidlStopService() ;
	
	void aidlSetActivityInBackground() ;
	void aidlSetActivityInForeground() ;
	
	void aidlUpdateNotificationMessage(String msg_text) ;
}