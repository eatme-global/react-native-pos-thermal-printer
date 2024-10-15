package com.posthermalprinter.util;
import java.util.List;

/**
 * Represents a column in a multi-column print layout.
 * This class encapsulates the properties of a single column, including its alignment,
 * width, and content lines.
 */
public class ColumnItem {

  private final TextAlignment alignment;
  private final int width;
  private final List<String> lines;

  /**
   * Constructs a new ColumnItem with specified properties.
   *
   * @param alignment The text alignment for this column.
   * @param width     The width of the column, typically in characters or print units.
   * @param lines     The list of text lines to be printed in this column.
   */
  public ColumnItem(TextAlignment alignment, int width, List<String> lines) {
    this.alignment = alignment;
    this.width = width;
    this.lines = lines;
  }

  /**
   * Gets the width of the column.
   *
   * @return The width of the column.
   */
  public int getWidth() {
    return width;
  }

  /**
   * Gets the text alignment for this column.
   *
   * @return The TextAlignment enum value representing the alignment.
   */
  public TextAlignment getAlignment() {
    return alignment;
  }

  /**
   * Gets the list of text lines for this column.
   *
   * @return A List of Strings, where each String represents a line of text in the column.
   */
  public List<String> getLines() {
    return lines;
  }
}
