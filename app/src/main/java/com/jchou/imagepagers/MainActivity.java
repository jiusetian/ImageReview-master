package com.jchou.imagepagers;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;

import com.jchou.imagereview.ui.ImagePagerActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

    private MainAdapter mainAdapter;

    //图片集合
    private ArrayList<String> urls;

    //存放返回时当前页码
    private Bundle bundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置使用分享元素
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        setContentView(R.layout.activity_main);
        urls=new ArrayList<>();
        //为了显示效果，重复添加了三次
        urls.add("http://static.fdc.com.cn/avatar/sns/1486263782969.png");
        urls.add("http://static.fdc.com.cn/avatar/sns/1485055822651.png");
        urls.add("http://static.fdc.com.cn/avatar/sns/1486194909983.png");
        urls.add("http://static.fdc.com.cn/avatar/sns/1486194996586.png");
        urls.add("http://static.fdc.com.cn/avatar/sns/1486195059137.png");
        urls.add("http://static.fdc.com.cn/avatar/sns/1486173497249.png");
        urls.add("http://static.fdc.com.cn/avatar/sns/1486173526402.png");
        urls.add("http://static.fdc.com.cn/avatar/sns/1486173639603.png");
        urls.add("http://static.fdc.com.cn/avatar/sns/1486172566083.png");

        initView();
    }

    private void initView() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        mainAdapter = new MainAdapter(urls);
        mainAdapter.setOnItemClickListener(new MainAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int pos) {
                ImagePagerActivity.startImagePage(MainActivity.this,
                        urls,pos,recyclerView.getLayoutManager().findViewByPosition(pos));
            }
        });
        recyclerView.setAdapter(mainAdapter);

        //设置转场动画的共享元素，因为跳转和返回都会调用，需要判断bundle是否为空
        setExitSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                if (bundle!=null){
                    int index = bundle.getInt(ImagePagerActivity.STATE_POSITION,0);
                    sharedElements.clear();
                    sharedElements.put("img", recyclerView.getLayoutManager().findViewByPosition(index));
                    bundle=null;
                }
            }
        });
    }


    //返回的时候获取数据
    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        bundle = data.getExtras();
        int currentPosition = bundle.getInt(ImagePagerActivity.STATE_POSITION,0);
        //做相应的滚动
        recyclerView.scrollToPosition(currentPosition);
        //暂时延迟 Transition 的使用，直到我们确定了共享元素的确切大小和位置才使用
        //postponeEnterTransition后不要忘记调用startPostponedEnterTransition
        ActivityCompat.postponeEnterTransition(this);
        recyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                recyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                // TODO: figure out why it is necessary to request layout here in order to get a smooth transition.
                recyclerView.requestLayout();
                //共享元素准备好后调用startPostponedEnterTransition来恢复过渡效果
                ActivityCompat.startPostponedEnterTransition(MainActivity.this);
                return true;
            }
        });
    }
}
