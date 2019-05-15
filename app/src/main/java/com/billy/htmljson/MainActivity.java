package com.billy.htmljson;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog dialog;
    private ListView listView;
    private RecyclerView recyclerView;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        dialog = new ProgressDialog(this);
        listView = (ListView) findViewById(R.id.listView);
        recyclerView = (RecyclerView) findViewById(R.id.recycle_view);
        recyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onClick(View view) {
        switch (view.getId()){
            case R.id.button:
                //new HtmlAyncTask().execute("http://www.djhub.net/api/top?type=downloads");
                getJsonFile();
                break;
        }
    }

    private  void getJsonFile(){
        try {
            InputStream in = this.getAssets().open("downloads.zip");
            String desFolder = this.getFilesDir().getPath() + File.separator + "unzip";
            unZip(in, desFolder);

            String jsonPath = desFolder + File.separator + "downloads.json";
            FileInputStream inputStream = new FileInputStream(jsonPath);

            List<MusicItem> items = parseJson(inputStream);
            //listView.setAdapter(new MusicItemAdapter(MainActivity.this, 0, items));
            //recyclerView.setAdapter();
            recyclerView.setAdapter(new MusicItemRecyclerAdapter(items));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void unZip(InputStream inputStream, String desFolder) {

        File des = new File(desFolder);
        if (!des.exists())
            des.mkdir();

        try {
            ZipInputStream zip = new ZipInputStream(inputStream);
            ZipEntry entry = null;
            while ((entry = zip.getNextEntry()) != null){
                String path = des.getPath() + File.separator + entry.getName();

                if (entry.isDirectory()){
                    File subFolder = new File(path);
                    if (!subFolder.exists())
                        subFolder.mkdir();
                }else {
                    File f = new File(path);

                    FileOutputStream out = new FileOutputStream(path, true);

                    byte[] buffer = new byte[1024];
                    int size;
                    while ((size = zip.read(buffer, 0, buffer.length)) > 0) {
                        out.write(buffer, 0, size);
                    }

                    out.close();
                    zip.closeEntry();
                }
            }

            zip.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ZipException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<MusicItem> parseJson(InputStream is) {
        List<MusicItem> list = new ArrayList<>();

        JsonReader jr = new JsonReader(new InputStreamReader(is));
        try {
            jr.beginArray();

            while (jr.hasNext()) {
                jr.beginObject();

                MusicItem item = new MusicItem();
                while (jr.hasNext()){
                    String name = jr.nextName();
                    if (name.equals("name")){
                        item.setName(jr.nextString());
                    }else if(name.equals("url"))
                        item.setUrl(jr.nextString());
                    else
                        jr.skipValue();
                }

                list.add(item);

                jr.endObject();
            }

            jr.endArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }

    public class HtmlAyncTask extends AsyncTask<String, Double, List<MusicItem>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            dialog.show();
        }

        @Override
        protected List<MusicItem> doInBackground(String... strings) {
            StringBuilder sb = new StringBuilder();
            try {
                URL url = new URL(strings[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                InputStream is = connection.getInputStream();

                List<MusicItem> items = parseJson(is);

                return  items;
                /*
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                while((line = reader.readLine()) != null){
                    sb.append(line);
                }*/
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPostExecute(List<MusicItem> items) {
            super.onPostExecute(items);

            dialog.cancel();

            int count = items == null ? 0 : items.size();

            listView.setAdapter(new MusicItemAdapter(MainActivity.this, 0, items));
            //listView.setAdapter();
            //Toast.makeText(MainActivity.this, String.valueOf(count), Toast.LENGTH_LONG).show();
            //Toast.makeText(MainActivity.this, s, Toast.LENGTH_LONG).show();
        }
    }

    public class MusicItemAdapter extends ArrayAdapter<MusicItem> {

        private List<MusicItem> items;

        public MusicItemAdapter(@NonNull Context context, int resource, @NonNull List<MusicItem> objects) {
            super(context, resource, objects);

            items = objects;
        }


        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            //return super.getView(position, convertView, parent);

            MusicItem item = items.get(position);

            View view = (View) LayoutInflater.from(getContext()).inflate(R.layout.menu_item, null);

            TextView nameTV = view.findViewById(R.id.name_tv);
            nameTV.setText(item.getName());

            TextView urlTV = view.findViewById(R.id.url_tv);
            urlTV.setText(item.getUrl());

            return  view;
        }
    }

    public class MusicItemRecyclerAdapter extends RecyclerView.Adapter<MusicItemRecyclerAdapter.MyViewHolder>{

        private List<MusicItem> items;

        public  MusicItemRecyclerAdapter(List<MusicItem> items){
            this.items = items;
        }


        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_item, parent, false);

            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            MusicItem item = items.get(position);
            holder.name_tv.setText(item.getName());
            holder.url_tv.setText(item.getUrl());
        }

        @Override
        public int getItemCount() {
            return items == null ? 0 : items.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder{
            protected TextView name_tv;
            protected TextView url_tv;

            public MyViewHolder(View itemView) {
                super(itemView);

                name_tv = itemView.findViewById(R.id.name_tv);
                url_tv = itemView.findViewById(R.id.url_tv);
            }
        }
    }
}
