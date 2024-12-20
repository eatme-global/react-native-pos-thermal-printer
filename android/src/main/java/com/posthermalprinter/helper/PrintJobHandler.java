package com.posthermalprinter.helper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.posthermalprinter.util.*;

import net.posprinter.utils.BitmapProcess;
import net.posprinter.utils.BitmapToByteData;
import net.posprinter.utils.DataForSendToPrinterPos80;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Handles the creation and processing of print jobs.
 */
public class PrintJobHandler {

  /**
   * Processes a PrinterJob and converts it into a list of byte arrays ready for sending to the printer.
   *
   * @param job The PrinterJob to process.
   * @return A List of byte arrays representing the processed print job.
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public static List<byte[]> processDataBeforeSend(PrinterJob job) {
    List<byte[]> list = new ArrayList<>();
//    list.add(DataForSendToPrinterPos80.initializePrinter());
//    list.add(DataForSendToPrinterPos80.initializePrinter());

    byte[] init = {
      0x1B, 0x40,      // Initialize printer (ESC @)
      0x1B, 0x21, 0x00,// Normal text mode
      0x1B, 0x61, 0x00,// Left alignment
      0x1B, 0x74, 0x00,// Select character code table (PC437: USA, Standard Europe)
      0x1D, 0x21, 0x00 // Normal character size
    };
    list.add(init);
    byte[] init = {
      0x1B, 0x40,      // Initialize printer (ESC @)
      0x1B, 0x21, 0x00,// Normal text mode
      0x1B, 0x61, 0x00,// Left alignment
      0x1B, 0x74, 0x00,// Select character code table (PC437: USA, Standard Europe)
      0x1D, 0x21, 0x00 // Normal character size
    };
    list.add(init);
    for (PrintItem item : job.getJobContent()) {
      list.addAll(processItem(item));
    }

    return list;
  }

  /**
   * Processes a single PrintItem and converts it into a list of byte arrays.
   *
   * @param item The PrintItem to process.
   * @return A List of byte arrays representing the processed print item.
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  private static List<byte[]> processItem(PrintItem item) {
    List<byte[]> list = new ArrayList<>();
    // Add font size selection
    list.add(TextProcessor.selectFontSize(item.getFontSize()));

    switch (item.getType()) {
      case TEXT:
        addTextToPrintList(list, item);
        break;
      case COLUMN:
        processColumnItem(list, item);
        break;
      case IMAGE:
        processImageItem(list, item);
        break;
      case QRCODE:
        processQRCodeItem(list, item);
        break;
      case CASHBOX:
        list.add(DataForSendToPrinterPos80.openCashdrawer());
        break;
      case FEED:
        list.add(DataForSendToPrinterPos80.printAndFeedForward(item.getLines()));
        break;
      case CUT:
        list.add(DataForSendToPrinterPos80.selectCutPagerModerAndCutPager(0x42, 0x66));
        break;
    }

    return list;
  }

  /**
   * Processes a QR code print item.
   *
   * @param list The list to add the processed data to.
   * @param item The PrintItem containing QR code data.
   */
  private static void processQRCodeItem(List<byte[]> list, PrintItem item){
    list.add(DataForSendToPrinterPos80.selectAlignment(item.getAlignmentAsInt()));
    list.add(DataForSendToPrinterPos80.printQRcode(4,48,item.getText()));
    list.add(DataForSendToPrinterPos80.printAndFeedLine());
  }

  /**
   * Processes a column print item.
   *
   * @param list The list to add the processed data to.
   * @param item The PrintItem containing column data.
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  private static void processColumnItem(List<byte[]> list, PrintItem item) {
    Charset encodeCharset;
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    try {
      // Set initial charset and model
      if (TextProcessor.columnsContainChineseCharacters(item.getColumns())) {
        encodeCharset = Charset.forName("GBK");
        DataForSendToPrinterPos80.setCharsetName("GBK");
        buffer.write(DataForSendToPrinterPos80.selectChineseCharModel());
        buffer.write(DataForSendToPrinterPos80.setChineseCharLeftAndRightSpace(0, 0));
      } else {
        encodeCharset = Charset.forName("CP437");
        DataForSendToPrinterPos80.setCharsetName("CP437");
        buffer.write(DataForSendToPrinterPos80.CancelChineseCharModel());
        buffer.write(DataForSendToPrinterPos80.selectCharacterCodePage(0));
      }
    try {
      // Set initial charset and model
      if (TextProcessor.columnsContainChineseCharacters(item.getColumns())) {
        encodeCharset = Charset.forName("GBK");
        DataForSendToPrinterPos80.setCharsetName("GBK");
        buffer.write(DataForSendToPrinterPos80.selectChineseCharModel());
        buffer.write(DataForSendToPrinterPos80.setChineseCharLeftAndRightSpace(0, 0));
      } else {
        encodeCharset = Charset.forName("CP437");
        DataForSendToPrinterPos80.setCharsetName("CP437");
        buffer.write(DataForSendToPrinterPos80.CancelChineseCharModel());
        buffer.write(DataForSendToPrinterPos80.selectCharacterCodePage(0));
      }

      if (item.getColumns() != null) {
        List<ColumnItem> columns = item.getColumns();
      if (item.getColumns() != null) {
        List<ColumnItem> columns = item.getColumns();

        // Find max lines
        int maxLines = 0;
        for (ColumnItem col : columns) {
          if (col.getLines() != null) {
            maxLines = Math.max(maxLines, col.getLines().size());
          }
        }
        // Find max lines
        int maxLines = 0;
        for (ColumnItem col : columns) {
          if (col.getLines() != null) {
            maxLines = Math.max(maxLines, col.getLines().size());
          }
        }

        // Process each line
        for (int lineIndex = 0; lineIndex < maxLines; lineIndex++) {
        // Process each line
        for (int lineIndex = 0; lineIndex < maxLines; lineIndex++) {
          StringBuilder lineBuilder = new StringBuilder();

          // Build the line
          for (ColumnItem column : columns) {
            String line = "";
            if (column.getLines() != null && lineIndex < column.getLines().size()) {
              line = column.getLines().get(lineIndex);
            }
          // Build the line
          for (ColumnItem column : columns) {
            String line = "";
            if (column.getLines() != null && lineIndex < column.getLines().size()) {
              line = column.getLines().get(lineIndex);
            }

            if (line == null) {
              line = "";
            }
            if (line == null) {
              line = "";
            }

            TextAlignment alignment = column.getAlignment() != null ?
              column.getAlignment() : TextAlignment.LEFT;
            String formattedText = TextProcessor.padText(line, column.getWidth(), alignment);
            TextAlignment alignment = column.getAlignment() != null ?
              column.getAlignment() : TextAlignment.LEFT;
            String formattedText = TextProcessor.padText(line, column.getWidth(), alignment);

            lineBuilder.append(formattedText);
          }
            lineBuilder.append(formattedText);
          }

          // Write alignment
          buffer.write(DataForSendToPrinterPos80.selectAlignment(0));
          // Write alignment
          buffer.write(DataForSendToPrinterPos80.selectAlignment(0));



          // If bold is needed, enable it once at the start
          if (item.isBold()) {
            // Try using a different emphasis command
            buffer.write(new byte[]{0x1B, 0x21, 0x08}); // ESC ! 8 - Enable emphasis
          }

          // Write the line
          buffer.write(lineBuilder.toString().getBytes(encodeCharset));

          // If bold was enabled, disable it at the end
          if (item.isBold()) {
            buffer.write(new byte[]{0x1B, 0x21, 0x00}); // ESC ! 0 - Reset to normal
          }

          // Line feed
          buffer.write(DataForSendToPrinterPos80.printAndFeedLine());
        }
          // If bold is needed, enable it once at the start
          if (item.isBold()) {
            // Try using a different emphasis command
            buffer.write(new byte[]{0x1B, 0x21, 0x08}); // ESC ! 8 - Enable emphasis
          }

          // Write the line
          buffer.write(lineBuilder.toString().getBytes(encodeCharset));

          // If bold was enabled, disable it at the end
          if (item.isBold()) {
            buffer.write(new byte[]{0x1B, 0x21, 0x00}); // ESC ! 0 - Reset to normal
          }

          // Line feed
          buffer.write(DataForSendToPrinterPos80.printAndFeedLine());
        }
      }

      // Send all data at once
      list.add(buffer.toByteArray());

    } catch (IOException e) {
      e.printStackTrace();

      // Send all data at once
      list.add(buffer.toByteArray());

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  /**
   * Processes an image print item.
   *
   * @param list The list to add the processed data to.
   * @param item The PrintItem containing image data.
   */
  private static void processImageItem(List<byte[]> list, PrintItem item) {
    try {
      // Validate input parameters
      if (list == null || item == null) {
        throw new IllegalArgumentException("List or PrintItem cannot be null");
      }

      Bitmap originalImage = item.getBitmapImage();
      if (originalImage == null) {
        // Handle null bitmap - you might want to log this or handle it differently
        list.add(DataForSendToPrinterPos80.printAndFeedLine());
        return;
      }

      // Maximum dimensions
      int maxWidth = (int) Math.floor((double) (576 * item.getWidthPercentage()) / 100);
      int maxHeight = 300; // Set your desired maximum height here

      // Validate dimensions
      if (maxWidth <= 0) {
        maxWidth = 576; // Default to full width if percentage calculation results in 0 or negative
      }

      // Calculate scaling ratios
      float widthRatio = (float) maxWidth / originalImage.getWidth();
      float heightRatio = (float) maxHeight / originalImage.getHeight();

      // Use the smaller ratio to maintain aspect ratio while fitting within bounds
      float scaleFactor = Math.min(widthRatio, heightRatio);

      // Calculate new dimensions
      int newWidth = (int) (originalImage.getWidth() * scaleFactor);
      int newHeight = (int) (originalImage.getHeight() * scaleFactor);

      // Compress the image to the calculated dimensions
      Bitmap scaledImage = null;
      try {
        scaledImage = BitmapProcess.compressBmpByYourWidth(originalImage, newWidth);
        if (scaledImage == null) {
          throw new IllegalStateException("Failed to compress bitmap");
        }
      } catch (Exception e) {
        // Handle compression failure
        list.add(DataForSendToPrinterPos80.printAndFeedLine());
        return;
      }

      // Create a new bitmap with the full printer width
      Bitmap centeredImage = null;
      try {
        centeredImage = Bitmap.createBitmap(576, newHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(centeredImage);

        // Fill with white
        canvas.drawColor(Color.WHITE);

        // Calculate the left position based on alignment
        float left = 0;

        if (item.getAlignment() == TextAlignment.CENTER) {
          left = (576 - newWidth) / 2f;
        } else if (item.getAlignment() == TextAlignment.RIGHT) {
          left = 576 - newWidth;
        }

        // Draw the scaled image
        canvas.drawBitmap(scaledImage, left, 0f, null);

        // Print the centered image
        list.add(DataForSendToPrinterPos80.printRasterBmp(
          0,
          centeredImage,
          BitmapToByteData.BmpType.Threshold,
          BitmapToByteData.AlignType.Left,
          576
        ));
      } catch (Exception e) {
        // Handle bitmap creation or drawing failure
        list.add(DataForSendToPrinterPos80.printAndFeedLine());
      } finally {
        // Ensure resources are always cleaned up
        try {
          scaledImage.recycle();
        } catch (Exception e) {
          // Ignore recycling errors
        }
        if (centeredImage != null) {
          try {
            centeredImage.recycle();
          } catch (Exception e) {
            // Ignore recycling errors
          }
        }
      }

      // Feed line
      list.add(DataForSendToPrinterPos80.printAndFeedLine());

    } catch (Exception e) {
      // Catch any unexpected errors
      assert list != null;
      list.add(DataForSendToPrinterPos80.printAndFeedLine());
    }
  }


  /**
   * Processes a text print item.
   *
   * @param list The list to add the processed data to.
   * @param item The PrintItem containing text data.
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  private static void addTextToPrintList(List<byte[]> list, PrintItem item)  {
    Charset encodeCharset = Charset.forName("CP437");

    if(TextProcessor.containsChineseCharacter(item.getText())){
      // [This will enable chinese character prints]
      encodeCharset = Charset.forName("GBK");
      list.add(DataForSendToPrinterPos80.selectChineseCharModel());
      list.add(DataForSendToPrinterPos80.setChineseCharLeftAndRightSpace(0, 0));

    } else {
      list.add(DataForSendToPrinterPos80.CancelChineseCharModel());
    }

    // Set alignment
    list.add(DataForSendToPrinterPos80.selectAlignment(item.getAlignmentAsInt()));


    // If bold is needed, enable it once at the start

    // If bold is needed, enable it once at the start
    if (item.isBold()) {
      // Try using a different emphasis command
      list.add(new byte[]{0x1B, 0x21, 0x08}); // ESC ! 8 - Enable emphasis
    }

    int printerLineWidth = calculatePrinterWidth(item.getFontSize());
    List<String> lines = TextProcessor.splitTextIntoLines(item.getText(), printerLineWidth, item.getWordWrap());

    for (String line : lines) {
      // Add text to print
      byte[] textBytes = line.getBytes(encodeCharset);
      list.add(textBytes);
      list.add(DataForSendToPrinterPos80.printAndFeedLine());
    }

    if (item.isBold()) {
      list.add(new byte[]{0x1B, 0x21, 0x00}); // ESC ! 8 - Enable emphasis
    }
  }

  /**
   * Calculates the printer width based on the font size.
   *
   * @param fontSize The FontSize to calculate the width for.
   * @return The calculated printer width.
   */
  private static int calculatePrinterWidth(FontSize fontSize) {
      return switch (fontSize) {
          case WIDE, BIG -> 24;
          default -> 48;
      };
  }

  /**
   * Creates a PrinterJob from the given parameters.
   *
   * @param ip The IP address of the printer.
   * @param content The content to be printed.
   * @param metadata Additional metadata for the print job.
   * @return A new PrinterJob object.
   * @throws IOException If there's an error processing the content.
   */
  @RequiresApi(api = Build.VERSION_CODES.O)
  public static PrinterJob createPrintJob(String ip, ReadableArray content, String metadata) throws IOException {
    List<PrintItem> printItems = createPrintItems(content);
    String jobId = generateUniqueJobId();

    return new PrinterJob(printItems, ip, "PrinterName_" + ip, metadata, jobId);
  }

  /**
   * Generates a unique job ID.
   *
   * @return A unique job ID (Custom UUID) string.
   */
  @RequiresApi(api = Build.VERSION_CODES.O)
  private static String generateUniqueJobId() {
    UUID uuid = UUID.randomUUID();
    Instant now = Instant.now();
    String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(java.time.ZoneId.systemDefault()).format(now);

    return "PJ-" + timestamp + "-" + uuid.toString().substring(0, 8);
  }

  /**
   * Creates a list of PrintItems from the given content.
   *
   * @param content The content to be converted into PrintItems.
   * @return A List of PrintItem objects.
   * @throws IOException If there's an error processing the content.
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public static List<PrintItem> createPrintItems(ReadableArray content) throws IOException {
    List<PrintItem> printItems = new ArrayList<>();

    for (int i = 0; i < content.size(); i++) {
      ReadableMap item = content.getMap(i);

      String text = item.hasKey("text") ? item.getString("text") : "";
      boolean fontWeight = item.hasKey("bold") ? item.getBoolean("bold") : false;
      TextAlignment alignment = TextProcessor.parseAlignment(item.hasKey("alignment") ? Objects.requireNonNull(item.getString("alignment")) : "LEFT");
      int feedLines = item.hasKey("lines") ? item.getInt("lines") : 0;
      String imageUrl = item.hasKey("url") ? item.getString("url") : "";
      FontSize fontSize = TextProcessor.parseFontSize(item.hasKey("fontSize") ? Objects.requireNonNull(item.getString("fontSize")) : "NORMAL");
      int widthPercentage = item.hasKey("width") ? Math.min(item.getInt("width"), 100) : 60;
      boolean wrapWords = item.hasKey("wrapWords") ? item.getBoolean("wrapWords") : false;
      boolean fullWidth = item.hasKey("fullWidth") ? item.getBoolean("fullWidth") : false;
      double printerWidth = item.hasKey("printerWidth") ? item.getDouble("printerWidth") : 576;



      String type = item.getString("type");
      switch (Objects.requireNonNull(type)) {
        case "TEXT":
          PrintItem textItem = new PrintItem(PrintItem.Type.TEXT, text, fontWeight, alignment, feedLines, new ArrayList<>(), fontSize);
          textItem.setWordWrap(wrapWords);
          printItems.add(textItem);
          break;
        case "IMAGE":

          Bitmap bitmap = !Objects.equals(imageUrl, "") ?  ImagePrinter.downloadImageAsBitmap(imageUrl) : null;
          PrintItem imageItem = new PrintItem(PrintItem.Type.IMAGE, imageUrl, fontWeight, alignment, feedLines, new ArrayList<>(), fontSize);
          imageItem.setBitmap(bitmap);
          imageItem.setPrinterWidth((float) printerWidth);


          if(fullWidth){
            imageItem.setWidthPercentage(100);
            imageItem.setFullWidth(true);
          } else {
            imageItem.setWidthPercentage(widthPercentage);
            imageItem.setFullWidth(false);
          }

          printItems.add(imageItem);
          break;
        case "QRCODE":
          printItems.add(new PrintItem(PrintItem.Type.QRCODE, text, fontWeight, alignment, feedLines, new ArrayList<>(), fontSize));
          break;
        case "CASHBOX":
          printItems.add(new PrintItem(PrintItem.Type.CASHBOX, "", false, TextAlignment.LEFT, 0, new ArrayList<>(), fontSize));
          break;
        case "COLUMN":
          ReadableArray columnArray = item.getArray("columns");
          List<ColumnItem> columns = new ArrayList<>();
          if (columnArray != null) {
            for (int j = 0; j < columnArray.size(); j++) {
              ReadableMap columnItem = columnArray.getMap(j);
              String columnText = columnItem.hasKey("text") ?  columnItem.getString("text") : "";
              int width = columnItem.hasKey("width") ? columnItem.getInt("width") : 10;
              boolean wrapWordsColumn = columnItem.hasKey("wrapWords") ? columnItem.getBoolean("wrapWords") : false;

              TextAlignment columnAlignment = TextProcessor.parseAlignment(Objects.requireNonNull(columnItem.getString("alignment")));
              List<String> lines = TextProcessor.splitTextIntoLines(Objects.requireNonNull(columnText), width, wrapWordsColumn);
              columns.add(new ColumnItem(columnAlignment, width, lines));
            }
          }
          printItems.add(new PrintItem(PrintItem.Type.COLUMN, "", fontWeight, TextAlignment.LEFT, 0, columns, fontSize));
          break;
        case "FEED":
          printItems.add(new PrintItem(PrintItem.Type.FEED, "", false, TextAlignment.LEFT, feedLines, new ArrayList<>(), fontSize));
          break;
        case "CUT":
          printItems.add(new PrintItem(PrintItem.Type.CUT, "", false, TextAlignment.LEFT, 0, new ArrayList<>(), fontSize));
          break;
      }
    }

    return printItems;
  }

}
