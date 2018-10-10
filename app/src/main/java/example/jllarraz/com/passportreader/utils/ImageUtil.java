package example.jllarraz.com.passportreader.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import org.jnbis.WsqDecoder;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import jj2000.j2k.decoder.Decoder;
import jj2000.j2k.util.ParameterList;


import static org.jmrtd.lds.ImageInfo.WSQ_MIME_TYPE;

public final class ImageUtil {

    private static final String TAG = ImageUtil.class.getSimpleName();

    public static String
            JPEG_MIME_TYPE = "image/jpeg",
            JPEG2000_MIME_TYPE = "image/jp2",
            JPEG2000_ALT_MIME_TYPE = "image/jpeg2000",
            WSQ_MIME_TYPE = "image/x-wsq";

    public static byte[] imageToByteArray(Image image) {
        byte[] data = null;
        if (image.getFormat() == ImageFormat.JPEG) {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            data = new byte[buffer.capacity()];
            buffer.get(data);
            return data;
        } else if (image.getFormat() == ImageFormat.YUV_420_888) {
            data = NV21toJPEG(
                    YUV_420_888toNV21(image),
                    image.getWidth(), image.getHeight());
        }
        return data;
    }

    public static byte[] YUV_420_888toNV21(Image image) {
        byte[] nv21;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }

    private static byte[] NV21toJPEG(byte[] nv21, int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        return out.toByteArray();
    }


    /* IMAGE DECODIFICATION METHODS */


    public  static Bitmap decodeImage(InputStream inputStream, int imageLength, String mimeType) throws IOException {
        /* DEBUG */
        synchronized(inputStream) {
            DataInputStream dataIn = new DataInputStream(inputStream);
            byte[] bytes = new byte[(int)imageLength];
            dataIn.readFully(bytes);
            inputStream = new ByteArrayInputStream(bytes);
        }
        /* END DEBUG */

        if (JPEG2000_MIME_TYPE.equalsIgnoreCase(mimeType) || JPEG2000_ALT_MIME_TYPE.equalsIgnoreCase(mimeType)) {
            org.jmrtd.jj2000.Bitmap bitmap = org.jmrtd.jj2000.JJ2000Decoder.decode(inputStream);
            return toAndroidBitmap(bitmap);
        } else if (WSQ_MIME_TYPE.equalsIgnoreCase(mimeType)) {
            //org.jnbis.Bitmap bitmap = WSQDecoder.decode(inputStream);
            WsqDecoder wsqDecoder = new WsqDecoder();
            org.jnbis.Bitmap bitmap = wsqDecoder.decode(inputStream);
            byte[] byteData = bitmap.getPixels();
            int[] intData = new int[byteData.length];
            for (int j = 0; j < byteData.length; j++) {
                intData[j] = 0xFF000000 | ((byteData[j] & 0xFF) << 16) | ((byteData[j] & 0xFF) << 8) | (byteData[j] & 0xFF);
            }
            return Bitmap.createBitmap(intData, 0, bitmap.getWidth(), bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            //return toAndroidBitmap(bitmap);
        } else {
            return BitmapFactory.decodeStream(inputStream);
        }
    }

    /* ONLY PRIVATE METHODS BELOW */

    private static Bitmap toAndroidBitmap(org.jmrtd.jj2000.Bitmap bitmap) {
        int[] intData = bitmap.getPixels();
        return Bitmap.createBitmap(intData, 0, bitmap.getWidth(), bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

    }
}