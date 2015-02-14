package be.digitalia.fosdem.utils;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

/**
 * Created by Abhishek on 14/02/15.
 */
public class ImageUtil {
    private static RequestQueue mQueue;
    private static ImageLoader mImageLoader;


    public static ImageLoader getImageLoader(Context context) {
        if (mImageLoader == null) {

            //mImageLoader = new ImageLoader(ImageUtil.getReqQueue(context), new DiskBitmapCache(context.getExternalCacheDir()));
            mImageLoader = new ImageLoader(ImageUtil.getReqQueue(context), new BitmapLruCache(context));

        }

        return mImageLoader;

    }


    public static RequestQueue getReqQueue(Context context) {
        if (mQueue == null)
            mQueue = Volley.newRequestQueue(context);


        return mQueue;
    }


}