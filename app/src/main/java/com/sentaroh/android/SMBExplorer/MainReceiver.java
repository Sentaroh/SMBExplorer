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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;

import com.sentaroh.android.SMBExplorer.Log.LogUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainReceiver extends BroadcastReceiver {
    private static Logger slf4jLog = LoggerFactory.getLogger(MainReceiver.class);

    private static Context mContext = null;

    private static GlobalParameter mGp = null;

    private static LogUtil mLog = null;

    @Override
    final public void onReceive(Context c, Intent received_intent) {
        mContext = c;
        if (mGp == null) {
            mGp = GlobalWorkArea.getAllocatedGlobalParameters();
        }
        String action = received_intent.getAction();
        slf4jLog.info("Receiver action="+action);
        if (action != null && mContext!=null && mGp!=null) {
            if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                mGp.setUsbMediaPath("");
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                Intent in = new Intent(mContext, MainService.class);
                in.setAction(action);
                in.setData(received_intent.getData());
                if (received_intent.getExtras() != null) in.putExtras(received_intent.getExtras());
                try {
                    mContext.startService(in);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                Intent in = new Intent(mContext, MainService.class);
                in.setAction(action);
                in.setData(received_intent.getData());
                if (received_intent.getExtras() != null) in.putExtras(received_intent.getExtras());
                try {
                    mContext.startService(in);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                mGp.setUsbMediaPath("");
            } else {
            }
        }
    }
}
