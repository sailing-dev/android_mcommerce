package com.example.mcommerce.utils;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {
    public static Bitmap getBitmap(Context context, int drawableRes) {
        Drawable drawable = context.getResources().getDrawable(drawableRes);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static String getRealPathFromURI(Activity activity, Uri contentUri) {

        // can post image
        String [] proj={MediaStore.Images.Media.DATA};
        @SuppressWarnings("deprecation")
        Cursor cursor = activity.managedQuery( contentUri,
                proj, // Which columns to return
                null,       // WHERE clause; which rows to return (all rows)
                null,       // WHERE clause selection arguments (none)
                null); // Order-by clause (ascending by name)
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        return cursor.getString(column_index);
    }

    public static Bitmap rotateBitmap(Bitmap bitmapOrg, int degree){
        int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();

        Matrix matrix = new Matrix();
        matrix.preRotate(degree);

        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, width, height, matrix, true);

        return rotatedBitmap;
    }

    public synchronized static Bitmap getSafeDecodeBitmap(String strFilePath, int maxSize) {
        try {
            if (strFilePath == null)
                return null;
            // Max image size
            int IMAGE_MAX_SIZE = maxSize;

            File file = new File(strFilePath);
            if (file.exists() == false) {
                //DEBUG.SHOW_ERROR(TAG, "[ImageDownloader] SafeDecodeBitmapFile : File does not exist !!");
                return null;
            }

            BitmapFactory.Options bfo 	= new BitmapFactory.Options();
            bfo.inJustDecodeBounds 		= true;

            BitmapFactory.decodeFile(strFilePath, bfo);

            if (IMAGE_MAX_SIZE > 0)
                if(bfo.outHeight * bfo.outWidth >= IMAGE_MAX_SIZE * IMAGE_MAX_SIZE) {
                    bfo.inSampleSize = (int)Math.pow(2, (int)Math.round(Math.log(IMAGE_MAX_SIZE
                            / (double) Math.max(bfo.outHeight, bfo.outWidth)) / Math.log(0.5)));
                }
            bfo.inJustDecodeBounds = false;
            bfo.inPurgeable = true;
            bfo.inDither = true;

            final Bitmap bitmap = BitmapFactory.decodeFile(strFilePath, bfo);

            int degree = GetExifOrientation(strFilePath);

            return GetRotatedBitmap(bitmap, degree);
        }
        catch(OutOfMemoryError ex)
        {
            ex.printStackTrace();

            return null;
        }
    }

    private synchronized static int GetExifOrientation(String filepath) 	{
        int degree = 0;
        ExifInterface exif = null;

        try    {
            exif = new ExifInterface(filepath);
        } catch (IOException e)  {
            Log.e("StylePhoto", "cannot read exif");
            e.printStackTrace();
        }

        if (exif != null) {
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);

            if (orientation != -1) {
                // We only recognize a subset of orientation tag values.
                switch(orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                }
            }
        }

        return degree;
    }

    private synchronized static Bitmap GetRotatedBitmap(Bitmap bitmap, int degrees) 	{
        if ( degrees != 0 && bitmap != null )     {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2 );
            try {
                Bitmap b2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                if (bitmap != b2) {
                    bitmap.recycle();
                    bitmap = b2;
                }
            } catch (OutOfMemoryError ex) {
                // We have no memory to rotate. Return the original bitmap.
            }
        }

        return bitmap;
    }

    private static final String TAG = ImageUtils.class.getSimpleName();

    /**
     * Exif???Exchangeable Image File??????????????????????????????????????????????????????????????????????????????JPEG???????????????????????????
     * ????????????Exif???????????????JPEG?????????????????????????????????????????????????????????????????????????????????????????????ISO???????????????????????????
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????GPS?????????????????????
     * ???????????????Exif=JPEG+?????????????????????????????????????????????????????????JPEG???????????????????????????Exif??????????????????
     * ?????????????????????????????????????????????Exif?????????
     * @param imageFilePath ????????????
     * @return
     * @throws IOException
     */
    public static ExifInterface getExifInterface(String imageFilePath) throws IOException {
        return new ExifInterface(imageFilePath);
    }

    /**
     * ??????????????????????????????
     * @param imageFilePath ???????????????
     * @return  (width, height)???????????????????????????
     */
    public static Point getWidthAndHeight(String imageFilePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        // ??????????????????????????????????????????????????????
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(imageFilePath, options);

        int width = options.outWidth;
        int height = options.outHeight;

        return new Point(width, height);
    }

    public static int getBitmapSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // API 19
            return bitmap.getAllocationByteCount();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {// API 12
            return bitmap.getByteCount();
        }

        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    /**
     * ??????????????????????????????
     * @param is ??????????????????
     * @return   (width, height)???????????????????????????
     */
    public static Point getWidthAndHeight(InputStream is) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        // ??????????????????????????????????????????????????????
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeStream(is, null, options);

        int width = options.outWidth;
        int height = options.outHeight;

        return new Point(width, height);
    }



    /**
     * ????????????
     * @param bitmap    ????????????
     * @param desWidth  ????????????
     * @param desHeight ????????????
     */
    public static Bitmap zoomBitmap(Bitmap bitmap, int desWidth, int desHeight) {
        return zoomBitmap(bitmap, (float) desWidth / bitmap.getWidth(),
                (float) desHeight / bitmap.getHeight());
    }

    /**
     * ????????????
     */
    public static Bitmap zoomBitmap(Bitmap bitmap, float sx, float sy) {
        Matrix matrix = new Matrix();
        matrix.postScale(sx, sy);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
    }

    public static byte[] compress(Bitmap bitmap, Bitmap.CompressFormat format) {
        return compress(bitmap, 75, format);
    }

    public static byte[] compress(Bitmap bitmap, int quality, Bitmap.CompressFormat format) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(format, quality, baos);
        return baos.toByteArray();
    }

    public static boolean saveBitmapToFile(Bitmap bitmap, String filePath, Bitmap.CompressFormat format) {
        return saveBitmapToFile(bitmap, filePath, 75, format);
    }


    public static boolean saveBitmapToFile(Bitmap bitmap, String filePath, int quality, Bitmap.CompressFormat format) {
        FileOutputStream fos = null;
        try {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            fos = new FileOutputStream(file);
            fos.write(compress(bitmap, quality, format));
            return true;
        } catch (Exception e) {
            Log.e(TAG, "saveBitmapToFile()", e);
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "saveBitmapToFile()", e);
                }
            }
        }
    }

    public static Bitmap toBitmap(byte[] data) {
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    public static Bitmap loadBitmapFromView(View v) {
        Bitmap b = Bitmap.createBitmap( v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        v.draw(c);
        return b;
    }

}
