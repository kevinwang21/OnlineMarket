package com.example.onlinemarket;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.*;

import com.facebook.drawee.view.SimpleDraweeView;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

public class MainActivity extends Activity implements TextWatcher, View.OnClickListener{
    private static final String TAG = "MainActivity";

    private static final int START_QUERY = 1;

    public static final Object TEST_STR = "test";

    private LayoutInflater inflater = null;

    private ListView list;
    private EditText input;
    private Button btn;
    private Button cancel;
    private View bar;
    private TextView tip;

    private MyHandler handler;
    private HandlerThread t;

    private DataAdapter adapter;
    private static HttpURLConnection curHttpURLConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if (inflater == null){
            inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        }
        setContentView(R.layout.activity_main);
        initUI();

    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (t != null){
            try{
                t.getLooper().quit();
                Thread.sleep(100);
                if(t.isAlive()){
                    t.stop();
                }
            }catch (Throwable e){
                Log.e(TAG, "", e);
            }
        }
        try{
            System.exit(0);
        }catch (Throwable e){
            Log.e(TAG, "", e);
        }
    }

    public static synchronized void setCurHttpURLConnection(HttpURLConnection conn){
        curHttpURLConnection = conn;
    }

    private void initUI(){
        input = (EditText) this.findViewById(R.id.input);
        list = (ListView) this.findViewById(R.id.list);
        btn = (Button) this.findViewById(R.id.ok);
        bar = (View) this.findViewById(R.id.bar);
        cancel = (Button) this.findViewById(R.id.cancel);
        tip = (TextView) this.findViewById(R.id.tip);
        btn.setEnabled(false);
        input.addTextChangedListener(this);

        bar.setVisibility(View.GONE);
        tip.setVisibility(View.GONE);
        btn.setOnClickListener(this);
        list.setAdapter(adapter = new DataAdapter());
        input.setOnEditorActionListener(new OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
                if (!TextUtils.isEmpty(input.getText())){
                    startAsyncQuery(input.getText().toString());
                }
                return true;
            }
        });
        list.setOnItemClickListener(new OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                Object o = adapter != null ? adapter.getItem(position) : null;
                ItemData.obj = o;
                if (o != null){
                    Intent i = new Intent(MainActivity.this, DetailInfoActivity.class);
                    MainActivity.this.startActivity(i);
                }
            }
        });

        new HandlerThread(""){
            protected void onLooperPrepared(){
                synchronized (MainActivity.this){
                    handler = new MyHandler(MainActivity.this);
                    MainActivity.this.notifyAll();
                }
            }
        }.start();

        synchronized (this){
            while (handler == null){
                try{
                    wait();
                }catch (InterruptedException e){
                    Log.e(TAG, "", e);
                }
            }
        }
    }

    private void setInputPanelVisible(boolean visible){
        if (input == null)
            return;
        try{
            InputMethodManager m = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (visible){
                m.showSoftInput(input, InputMethodManager.SHOW_FORCED);
            }else{
                m.hideSoftInputFromWindow(input.getWindowToken(), 0);
            }
        }catch (Throwable e){
            Log.e(TAG, "", e);
        }
    }

    private void updateUI(JSONObject obj, Result res){
        if (res != null){
            switch (res.code) {
            case Result.NETWORK_ERROR:{
                showErrorMsg("Network error, please try again later");
                return;
            }
            case Result.PARSE_JSON_ERROR:{
                showErrorMsg("Parse JSON error, detail:\n" + res.desc);
                return;
            }
            case Result.READ_ERROR:{
                showErrorMsg("Error when reading data from network, detail:\n" + res.desc);
                return;
            }
            case Result.READ_LOCAL_ERROR:{
                showErrorMsg("Error when reading local testing data, detail:\n" + res.desc);
                return;
            }
            }
        }
        list.setVisibility(View.VISIBLE);
        bar.setVisibility(View.GONE);
        tip.setVisibility(View.GONE);
        input.setEnabled(true);
        btn.setEnabled(true);
        adapter.data = obj;
        adapter.notifyDataSetChanged();
        if (obj != null && adapter.getCount() < 1){
            this.showErrorMsg("Sorry, no search result, please try again.");
        }
    };

    private void showErrorMsg(String string){
        list.setVisibility(View.GONE);
        bar.setVisibility(View.GONE);
        input.setEnabled(true);
        btn.setEnabled(true);
        tip.setText(string == null ? "" : string);
        tip.setVisibility(View.VISIBLE);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after){

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count){

    }

    @Override
    public void afterTextChanged(Editable s){
        if (s != null && s.length() > 0){
            btn.setEnabled(true);
            // del.setEnabled(true);
        }else{
            btn.setEnabled(false);
            // del.setEnabled(false);
        }
    }

    @Override
    public void onClick(View v){
        if (v == btn){
            Editable edit = input.getText();
            if (edit != null){
                String in = edit.toString();
                this.startAsyncQuery(in);
            }
        }else if(cancel == v){
            synchronized(MainActivity.class){
                if(MainActivity.curHttpURLConnection != null){
                    try{
                       curHttpURLConnection.disconnect();
                       curHttpURLConnection = null;
                    }catch (Throwable e){
                        Log.e(TAG, "", e);
                    }
                }
            }
        }
    }

    private void startAsyncQuery(String name){
        setInputPanelVisible(false);
        if (TextUtils.isEmpty(name) || handler == null){
            Log.e(TAG, "error");
            return;
        }
        list.setVisibility(View.GONE);
        list.setSelection(0);
        list.smoothScrollToPosition(0);
        tip.setVisibility(View.GONE);
        bar.setVisibility(View.VISIBLE);
        input.setEnabled(false);
        btn.setEnabled(false);
        final String url;
        if(TEST_STR.equals(name)){
            url = name;
        }else if(name.startsWith("http")){
            url = name;
        }else{
           url = "http://www.bestbuy.ca/api/v2/json/search?lang=en&query=" + name;
        }
        handler.sendMessage(handler.obtainMessage(START_QUERY, url));
    }

    private static JSONObject query(String strUrl, Result res){
        if (res == null){
            throw new IllegalArgumentException("Error, null Result");
        }
        String str = "";
        HttpURLConnection conn = null;

        try{
            URL url = new URL(strUrl);
            conn = (HttpURLConnection) url.openConnection();
            setCurHttpURLConnection(conn);
            conn.setConnectTimeout(8 * 1000);
            conn.setReadTimeout(8 * 1000);
            conn.connect();
            if (conn.getResponseCode() != 200){
                Log.e(TAG, "HTTP result is NOT 200!!");
                res.code = Result.NETWORK_ERROR;
                return null;
            }
            InputStream is = conn.getInputStream();
            str = readData(is);
        }catch (Throwable e){
            Log.e(TAG, "", e);
            res.code = Result.READ_ERROR;
            res.desc = e.toString();
            return null;
        }finally{
            setCurHttpURLConnection(null);
            if (conn != null){
                conn.disconnect();
            }
        }
        try{
            JSONObject obj = new JSONObject(str);
            res.code = Result.SUCCESSFUL;
            return obj;
        }catch (Throwable e){
            Log.e(TAG, "", e);
            res.code = Result.PARSE_JSON_ERROR;
            res.desc = e.toString();
        }
        return null;
    }

    private static String readData(InputStream is){
        InputStreamReader isReader = new InputStreamReader(is);
        BufferedReader reader = new BufferedReader(isReader);
        StringBuilder sb = new StringBuilder();
        String line = null;
        try{
            while ((line = reader.readLine()) != null){
                sb.append(line + "/n");
            }
        }catch (Throwable e){
            Log.e(TAG, "", e);
        }finally{
            try{
                isReader.close();
            }catch (Throwable e){
                Log.e(TAG, "", e);
            }
            try{
                reader.close();
            }catch (Throwable e){
                Log.e(TAG, "", e);
            }
            try{
                is.close();
            }catch (Throwable e){
                Log.e(TAG, "", e);
            }
        }
        return sb.toString();
    }

    static class Result{
        public static final int UNKNOWN_ERROR = 0;
        public static final int SUCCESSFUL = 1;
        public static final int NETWORK_ERROR = 2;
        public static final int READ_ERROR = 3;
        public static final int PARSE_JSON_ERROR = 4;
        public static final int READ_LOCAL_ERROR = 5;
        int code;
        String desc;

        Result(){
            code = SUCCESSFUL;
        }
    }

    private static class MyHandler extends Handler{
        private WeakReference<MainActivity> ref;

        private MyHandler(MainActivity a){
            ref = new WeakReference<MainActivity>(a);
        }

        public void handleMessage(Message msg){
            final int w = msg.what;
            switch (w) {
            case START_QUERY:{
                final MainActivity a = ref.get();
                if (a == null)
                    break;
                String str = (String) msg.obj;
                final Result res = new Result();
                final JSONObject obj;
                if(TEST_STR.equals(str)){
                    obj = a.localFromAssets(res);
                }else{
                    obj = query(str, res);
                }
                a.runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        a.updateUI(obj, res);
                    }
                });
                break;
            }
            }
        }
    }

    private class DataAdapter extends BaseAdapter{
        private JSONObject data;
        private JSONArray array;

        public void notifyDataSetChanged(){
            array = null;
            if (data != null){
                try{
                    array = data.getJSONArray("products");
                }catch (Throwable e){
                    Log.e(TAG, "", e);
                }
            }
            super.notifyDataSetChanged();
        }

        @Override
        public int getCount(){
            return array == null ? 0 : array.length();
        }

        @Override
        public Object getItem(int position){
            try{
                return array == null ? null : array.get(position);
            }catch (Throwable e){
                Log.e(TAG, "", e);
            }
            return null;
        }

        @Override
        public long getItemId(int position){
            return position;
        }

        @Override
        public View getView(int p, View convertView, ViewGroup parent){
            Object o = getItem(p);
            String price = null;
            String title = null;
            String url = null;
            if (o instanceof JSONObject){
                JSONObject jo = (JSONObject)o;
                try{
                    title = jo.getString("name");
                }catch (Throwable e){
                    Log.e(TAG, "", e);
                }
                try{
                    price = jo.getString("salePrice");
                }catch (Throwable e){
                    Log.e(TAG, "", e);
                }
                try{
                    url = jo.getString("thumbnailImage");
                }catch (Throwable e){
                    Log.e(TAG, "", e);
                }
            }
            if (title == null){
                title = "";
            }
            if (url == null){
                url = "";
            }
            if (price == null){
                price = "$--";
            }else if(!price.startsWith("$")){
                price = "$" + price;
            }
            ViewGroup vg = (ViewGroup) convertView;
            VH h;
            if (vg == null){
                vg = (ViewGroup) inflater.inflate(R.layout.item, null);
                h = new VH((TextView) vg.findViewById(R.id.price), (TextView) vg.findViewById(R.id.title),
                        (SimpleDraweeView) vg.findViewById(R.id.logo));
                vg.setTag(h);
            }else{
                h = (VH) vg.getTag();
            }
            h.price.setText(price);
            h.title.setText(title);
            try{
                Log.e(TAG, "url:" + url);
                h.logo.setImageURI(Uri.parse(url));
            }catch (Throwable e){
                Log.e(TAG, "", e);
            }
            return vg;
        }

    }

    static class VH{
        private TextView price;
        private TextView title;
        private SimpleDraweeView logo;

        VH(TextView p, TextView t, SimpleDraweeView l){
            price = p;
            logo = l;
            title = t;
        }
    }

    private JSONObject localFromAssets(Result res){
        InputStream in = null;
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder(40960);
        try {
            in = getAssets().open("a.html");
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            boolean b = true;
            int i = 0;
            while ((line = reader.readLine()) != null) {
                if ((line = line.trim()).length() == 0) {
                    continue;
                }
                sb.append(line);
            }
            try{
                JSONObject obj = new JSONObject(sb.toString());
                res.code = Result.SUCCESSFUL;
                return obj;
            }catch (Throwable e){
                Log.e(TAG, "", e);
                res.code = Result.PARSE_JSON_ERROR;
                res.desc = e.toString();
            }
        }catch(Throwable e){
            Log.e(TAG, "", e);
            res.code = Result.READ_LOCAL_ERROR;
        }
        return null;
    }

}
