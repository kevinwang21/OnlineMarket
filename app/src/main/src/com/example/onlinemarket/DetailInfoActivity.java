package com.example.onlinemarket;

import org.json.JSONObject;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.onlinemarket.MainActivity.VH;
import com.facebook.drawee.view.SimpleDraweeView;

public class DetailInfoActivity extends Activity implements View.OnClickListener{
    private static final String TAG = "DetailInfoActivity";
    private Button btn;
    private TextView title;
    private TextView desc;
    private TextView price;
    private SimpleDraweeView logo;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail);
        initUI();
    }

    private void initUI(){
        title = (TextView) this.findViewById(R.id.title);
        logo = (SimpleDraweeView) this.findViewById(R.id.pic);
        desc = (TextView) this.findViewById(R.id.desc);
        btn = (Button) this.findViewById(R.id.add);
        price = (TextView) this.findViewById(R.id.price);
        btn.setOnClickListener(this);
        
        Object o = ItemData.obj;
        String p = null;
        String t = null;
        String url = null;
        String d = null;
        if (o instanceof JSONObject){
            JSONObject jo = (JSONObject)o;
            try{
                t = jo.getString("name");
            }catch (Throwable e){
                Log.e(TAG, "", e);
            }
            try{
                p = jo.getString("salePrice");
            }catch (Throwable e){
                Log.e(TAG, "", e);
            }
            try{
                url = jo.getString("thumbnailImage");
            }catch (Throwable e){
                Log.e(TAG, "", e);
            }
            try{
                d = jo.getString("shortDescription");
            }catch (Throwable e){
                Log.e(TAG, "", e);
            }
        }
        if (t == null){
            t = "";
        }
        if (url == null){
            url = "";
        }
        if (p == null){
            p = "$--";
        }else if(!p.startsWith("$")){
            p = "$" + p;
        }
        if (d == null){
            d = "";
        }
        desc.setText(d);
        title.setText(t);
        price.setText(p);
        try{
            logo.setImageURI(Uri.parse(url));
        }catch (Throwable e){
            Log.e(TAG, "", e);
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    @Override
    public void onClick(View v){
        if(v == btn){
            
        }
    }

}
