package com.posthermalprinter.util;

import android.graphics.Bitmap;

import java.util.List;

/**
 * Represents an item to be printed, such as text, image, or other print commands.
 */
public class PrintItem {

  /**
   * Enumerates the types of print items supported.
   */
  public enum Type {TEXT, FEED, CUT, COLUMN, IMAGE, QRCODE, CASHBOX}

  private final Type type;
  private final String text;
  private final boolean bold;
  private final TextAlignment alignment;
  private final int lines;
  private Bitmap bitmapImage;
  private final List<ColumnItem> columns;
  private final FontSize fontSize;
  private int widthPercentage;
  private boolean wordWrap;

  private float printerWidth;

  private boolean fullWidth;

  /**
   * Constructs a new PrintItem with specified properties.
   *
   * @param type      The type of the print item.
   * @param text      The text content of the item (if applicable).
   * @param bold      Whether the text should be bold.
   * @param alignment The alignment of the text.
   * @param lines     The number of lines to feed or cut.
   * @param columns   The list of column items (for column type).
   * @param fontSize  The font size of the text.
   */
  public PrintItem(Type type, String text, boolean bold, TextAlignment alignment, int lines, List<ColumnItem> columns, FontSize fontSize) {
    this.type = type;
    this.text = text;
    this.bold = bold;
    this.alignment = alignment;
    this.lines = lines;
    this.columns = columns;
    this.fontSize = fontSize;
  }

  /**
   * Gets the type of the print item.
   *
   * @return The type of the print item.
   */
  public Type getType() {
    return type;
  }

  /**
   * Checks if word wrap is enabled for this print item.
   *
   * @return true if word wrap is enabled, false otherwise.
   */
  public boolean getWordWrap() {
    return wordWrap;
  }

  /**
   * Checks if the text should be bold.
   *
   * @return true if the text should be bold, false otherwise.
   */
  public boolean isBold() {
    return bold;
  }

  /**
   * Gets the list of column items for column type print items.
   *
   * @return The list of column items.
   */
  public List<ColumnItem> getColumns() {
    return columns;
  }

  /**
   * Gets the text content of the print item.
   *
   * @return The text content.
   */
  public String getText() {
    return text;
  }

  /**
   * Gets the text alignment of the print item.
   *
   * @return The text alignment.
   */
  public TextAlignment getAlignment() {
    return alignment;
  }

  /**
   * Converts TextAlignment enum to integer value.
   *
   * @return 0 for LEFT, 1 for CENTER, 2 for RIGHT
   */
  public int getAlignmentAsInt() {
    return switch (alignment) {
      case CENTER -> 1;
      case RIGHT -> 2;
      default -> 0;
    };
  }

  /**
   * Gets the bitmap image for image type print items.
   *
   * @return The bitmap image.
   */
  public Bitmap getBitmapImage() {
    return bitmapImage;
  }

  /**
   * Gets the number of lines to feed or cut.
   *
   * @return The number of lines.
   */
  public int getLines() {
    return lines;
  }

  /**
   * Gets the font size of the text.
   *
   * @return The font size.
   */
  public FontSize getFontSize() {
    return fontSize;
  }

  /**
   * Gets the width percentage for image scaling.
   *
   * @return The width percentage.
   */
  public int getWidthPercentage() {
    return widthPercentage;
  }


  public boolean isFullWidth() {
    return fullWidth;
  }

  public float getPrinterWidth() {
    return printerWidth;
  }

  public void setPrinterWidth(float printerWidth) {
    this.printerWidth = printerWidth;
  }

  public void setFullWidth(boolean fullWidth) {
    this.fullWidth = fullWidth;
  }

  /**
   * Sets the bitmap image for image type print items.
   *
   * @param bitmapImage The bitmap image to set.
   */
  public void setBitmap(Bitmap bitmapImage) {
    this.bitmapImage = bitmapImage;
  }

  /**
   * Sets the width percentage for image scaling.
   *
   * @param widthPercentage The width percentage to set.
   */
  public void setWidthPercentage(int widthPercentage) {
    this.widthPercentage = widthPercentage;
  }

  /**
   * Sets whether word wrap should be enabled for this print item.
   *
   * @param wordWrap true to enable word wrap, false to disable.
   */
  public void setWordWrap(boolean wordWrap) {
    this.wordWrap = wordWrap;
  }
}
