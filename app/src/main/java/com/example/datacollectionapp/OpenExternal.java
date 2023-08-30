package com.example.datacollectionapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OpenExternal extends ListActivity {

    private Context context;
    File root;
    File txt;

    private List<String> fileList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_external);
        context = getApplicationContext();

        //root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath());
        String folder_example = "APP_COLLECTED_DATA";
        root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath(), folder_example);
        //root = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+ "/EXAMPLE_FILES");

        txt = new File(root, "txt");
        //txt = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath());
        txt = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath(), folder_example);
        //txt = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+ "/EXAMPLE_FILES");
        ListDir(txt);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    void ListDir(File f){
        File [] files = f.listFiles();
        fileList.clear();
        for (File file : files){
            fileList.add(file.getPath());
        }

        ArrayAdapter<String> directoryList = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, fileList);

        setListAdapter(directoryList);
    }

    public void onListItemClick(ListView parent, View v, int position, long id){
        openTxt(fileList.get(position).toString());
    }

    public void openTxt(String path){
        Log.d("Open text: ", "trying to open text....");
        File file = new File(path);
        if (file.exists()){
            Uri p = null;
            if(Build.VERSION.SDK_INT<24) {
                p = Uri.fromFile(file);
            } else {
                p = FileProvider.getUriForFile(context,
                        context.getApplicationContext().getPackageName() + ".provider",
                        file);
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(p, "text/plain");
            //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

            try{
                startActivity(intent);
            }
            catch (ActivityNotFoundException e){

            }
        }
    }
}