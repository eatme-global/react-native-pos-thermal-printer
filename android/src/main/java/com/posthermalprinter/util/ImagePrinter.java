package com.posthermalprinter.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility class for handling image operations related to printing.
 */
public class ImagePrinter {

  /**
   * Downloads an image from a given URL and converts it to a Bitmap.
   *
   * @param imageUrl The URL of the image to download.
   * @return A Bitmap representation of the downloaded image.
   * @throws IOException If there's an error in opening the connection or reading the image data.
   */
  public static Bitmap downloadImageAsBitmap(String imageUrl) throws IOException {
    URL url = new URL(imageUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoInput(true);
    connection.connect();
    InputStream input = connection.getInputStream();
    Bitmap myBitmap = BitmapFactory.decodeStream(input);
    input.close();
    connection.disconnect();
    return myBitmap;
  }
}
