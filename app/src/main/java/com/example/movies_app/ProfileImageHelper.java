package com.example.movies_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Photo de profil stockée en Base64 dans Firestore (sans Firebase Storage).
 */
public final class ProfileImageHelper {

    private static final int MAX_SIZE_PX = 400;
    private static final int JPEG_QUALITY = 50;

    private ProfileImageHelper() {
    }

    /** Compresse l'image (max 400×400, qualité 50 %) et retourne une chaîne Base64. */
    public static String uriToBase64(Context context, Uri uri) throws IOException {
        Bitmap source = loadBitmap(context, uri);
        if (source == null) {
            throw new IOException("Image illisible");
        }
        Bitmap scaled = scaleDown(source, MAX_SIZE_PX);
        if (scaled != source) {
            source.recycle();
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream);
        scaled.recycle();
        return android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP);
    }

    public static Bitmap base64ToBitmap(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        try {
            byte[] bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void displayInImageView(ImageView imageView, String profileImageBase64) {
        Bitmap bitmap = base64ToBitmap(profileImageBase64);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(R.drawable.ic_avatar_default);
        }
    }

    private static Bitmap loadBitmap(Context context, Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(source, (decoder, info, src) ->
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE));
        }
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
    }

    private static Bitmap scaleDown(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }
        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newW = Math.round(width * ratio);
        int newH = Math.round(height * ratio);
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true);
    }
}
