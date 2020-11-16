package com.sentaroh.android.SMBExplorer.Log;
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

import android.os.Bundle;

import com.sentaroh.android.Utilities3.LogUtil.CommonLogManagementFragment;


public class LogManagementFragment extends CommonLogManagementFragment {
	public static LogManagementFragment newInstance(boolean retainInstance, String title) {
		LogManagementFragment frag = new LogManagementFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("retainInstance", retainInstance);
        bundle.putString("title", title);
        bundle.putString("msgtext", "send essage");
        bundle.putString("enableMsg", "Do you want to enable the log?");
        frag.setArguments(bundle);
        return frag;
    }

}
