package com.codig.CyberPotato;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Window;
import android.widget.TextView;

import com.codig.CyberPotato.widget.PullDownDumperLayout;

public class VersionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_version);
        Window window = this.getWindow();
        window.setGravity(Gravity.TOP);//置顶

        //赋值版本号
        ((TextView)findViewById(R.id.version_Version)).setText("Version: "+CyberPotato.version);

        PullDownDumperLayout pddl=findViewById(R.id.version_PullDownDumper);
        pddl.setHideRatio(0.2);//设置头部处于展开状态时，触发隐藏动画的分界线
    }
}
