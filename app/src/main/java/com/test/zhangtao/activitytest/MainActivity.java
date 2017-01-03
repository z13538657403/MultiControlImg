package com.test.zhangtao.activitytest;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by zhangtao on 17/1/2.
 */

public class MainActivity extends Activity
{
    private ViewPager mViewPager;

    private int[] mImgs = new int[]{R.drawable.test , R.drawable.test2, R.drawable.test3};

    private ImageView[] mImageViews = new ImageView[mImgs.length];

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mViewPager = (ViewPager) findViewById(R.id.id_viewPager);
        mViewPager.setAdapter(new PagerAdapter()
        {
            @Override
            public int getCount()
            {
                return mImgs.length;
            }

            @Override
            public boolean isViewFromObject(View view, Object object)
            {
                return view == object;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position)
            {
                ZoomImageView imageView = new ZoomImageView(getApplicationContext());
                imageView.setImageResource(mImgs[position]);
                container.addView(imageView);
                mImageViews[position] = imageView;
                return imageView;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object)
            {
                container.removeView(mImageViews[position]);
            }
        });
    }
}
