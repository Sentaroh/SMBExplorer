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

import com.sentaroh.android.Utilities3.SafFile3;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

public class LocalStorage implements Externalizable {
    public boolean storage_saf_file =false;
    public String storage_name="";
    public String storage_app_directory="";
    public SafFile3 storage_root =null;
    public String storage_root_path="";
    private String storage_last_use_directory="";
    public String storage_mount_point ="";
    public String storage_id="";

    public int storage_pos_fv=-1;
    public int storage_pos_top=-1;

    public void setLastUseDirectory(String directory) {storage_last_use_directory=directory;}
    public String getLastUseDirectory() {return storage_last_use_directory;}

    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        objectOutput.writeBoolean(storage_saf_file);
        objectOutput.writeUTF(storage_name);
        objectOutput.writeUTF(storage_app_directory);
        objectOutput.writeUTF(storage_last_use_directory);
        objectOutput.writeUTF(storage_mount_point);
        objectOutput.writeUTF(storage_id);
        objectOutput.writeInt(storage_pos_fv);
        objectOutput.writeInt(storage_pos_top);
        objectOutput.writeUTF(storage_root.getPath());
    }

    @Override
    public void readExternal(ObjectInput objectInput) throws ClassNotFoundException, IOException {
        storage_saf_file=objectInput.readBoolean();
        storage_name=objectInput.readUTF();
        storage_app_directory=objectInput.readUTF();
        storage_last_use_directory=objectInput.readUTF();
        storage_mount_point=objectInput.readUTF();
        storage_id=objectInput.readUTF();
        storage_pos_fv=objectInput.readInt();
        storage_pos_top=objectInput.readInt();
        storage_root_path=objectInput.readUTF();
    }
}
