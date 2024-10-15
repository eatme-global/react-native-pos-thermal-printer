package com.posthermalprinter.helper;
import android.os.Build;
import androidx.annotation.RequiresApi;

import com.posthermalprinter.util.ColumnItem;
import com.posthermalprinter.util.FontSize;
import com.posthermalprinter.util.TextAlignment;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for processing text for printing purposes.
 * This class provides methods for text manipulation, alignment, and font size selection.
 */
public class TextProcessor {
  @RequiresApi(api = Build.VERSION_CODES.N)
  private static List<String> splitTextIntoLinesWithWordWrap(String text, int width) {
    List<String> lines = new ArrayList<>();
    String[] words = text.split("\\s+");
    StringBuilder currentLine = new StringBuilder();
    int currentLineWidth = 0;

    for (int i = 0; i < words.length; i++) {
      String word = words[i];
      int wordWidth = getVisualWidth(word);

      if (wordWidth > width) {
        // If the current line is not empty, add it to lines
        if (currentLineWidth > 0) {
          lines.add(currentLine.toString().trim());
          currentLine = new StringBuilder();
          currentLineWidth = 0;
        }

        // Split the long word
        String remainingWord = word;
        while (remainingWord.length() > 0) {
          String chunk = takeChunkOfVisualWidth(remainingWord, width);
          lines.add(chunk);
          remainingWord = remainingWord.substring(chunk.length());
        }
      } else if (currentLineWidth + wordWidth + (currentLineWidth > 0 ? 1 : 0) <= width) {
        // Add word to the current line
        if (currentLineWidth > 0) {
          currentLine.append(" ");
          currentLineWidth++;
        }
        currentLine.append(word);
        currentLineWidth += wordWidth;
      } else {
        // Start a new line
        lines.add(currentLine.toString().trim());
        currentLine = new StringBuilder(word);
        currentLineWidth = wordWidth;
      }
    }

    // Add the last line if it's not empty
    if (currentLine.length() > 0) {
      lines.add(currentLine.toString().trim());
    }

    return lines;
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  private static List<String> splitTextIntoLinesWithoutWordWrap(String text, int width) {
    List<String> lines = new ArrayList<>();
    for (int i = 0; i < text.length(); i += width) {
      int end = Math.min(i + width, text.length());
      lines.add(text.substring(i, end));
    }
    return lines;
  }


  /**
   * Splits text into lines based on a specified width.
   *
   * @param text      The text to split.
   * @param width     The maximum width of each line.
   * @param wrapWords Whether to wrap words or split them.
   * @return A list of strings, each representing a line of text.
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public static List<String> splitTextIntoLines(String text, int width, boolean wrapWords) {
    if (wrapWords) {
      return splitTextIntoLinesWithWordWrap(text, width);
    } else {
      return splitTextIntoLinesWithoutWordWrap(text, width);
    }
  }


  /**
   * Calculates the visual width of a string, considering Chinese characters as double-width.
   *
   * @param str The string to measure.
   * @return The visual width of the string.
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public static int getVisualWidth(String str) {
    int visualWidth = 0;
    for (char c : str.toCharArray()) {
      visualWidth += (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) ? 2 : 1;
    }
    return visualWidth;
  }

  /**
   * Takes a chunk of a string up to a specified visual width.
   *
   * @param str      The string to process.
   * @param maxWidth The maximum visual width of the chunk.
   * @return A substring that fits within the specified width.
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public static String takeChunkOfVisualWidth(String str, int maxWidth) {
    int currentWidth = 0;
    int endIndex = 0;
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      int charWidth = (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) ? 2 : 1;
      if (currentWidth + charWidth > maxWidth) break;
      currentWidth += charWidth;
      endIndex = i + 1;
    }
    return str.substring(0, endIndex);
  }

  /**
   * Parses a string representation of text alignment into a TextAlignment enum.
   *
   * @param alignment The string representation of alignment.
   * @return The corresponding TextAlignment enum value.
   */
  public static TextAlignment parseAlignment(String alignment) {
      return switch (alignment) {
          case "CENTER" -> TextAlignment.CENTER;
          case "RIGHT" -> TextAlignment.RIGHT;
          default -> TextAlignment.LEFT;
      };
  }

  /**
   * Checks if a string contains any Chinese characters.
   *
   * @param text The text to check.
   * @return true if the text contains Chinese characters, false otherwise.
   */
  public static boolean containsChineseCharacter(String text) {
    for (char c : text.toCharArray()) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Checks if any column in a list of ColumnItems contains Chinese characters.
   *
   * @param columns The list of ColumnItems to check.
   * @return true if any column contains Chinese characters, false otherwise.
   */
  public static boolean columnsContainChineseCharacters(List<ColumnItem> columns) {
    for (ColumnItem column : columns) {
      if (column.getLines() != null) {
        for (String line : column.getLines()) {
          if (containsChineseCharacter(line)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Selects the appropriate byte sequence for a given font size.
   *
   * @param fontSize The FontSize enum value.
   * @return A byte array representing the font size selection command.
   */
  public static byte[] selectFontSize(FontSize fontSize) {
    int widthMultiplier;
    int heightMultiplier = switch (fontSize) {
        case WIDE -> {
            widthMultiplier = 2;
            yield 1;
        }
        case TALL -> {
            widthMultiplier = 1;
            yield 2;
        }
        case BIG -> {
            widthMultiplier = 2;
            yield 2;
        }
        default -> {
            widthMultiplier = 1;
            yield 1;
        }
    };

      return selectFont(widthMultiplier, heightMultiplier);
  }

  /**
   * Generates a font selection command based on width and height multipliers.
   *
   * @param heightMultiplier The height multiplier (1-8).
   * @param widthMultiplier  The width multiplier (1-8).
   * @return A byte array representing the font selection command.
   */
  public static byte[] selectFont(int heightMultiplier , int widthMultiplier) {
    if (widthMultiplier < 1) widthMultiplier = 1;
    if (widthMultiplier > 8) widthMultiplier = 8;
    if (heightMultiplier < 1) heightMultiplier = 1;
    if (heightMultiplier > 8) heightMultiplier = 8;

    int n = (widthMultiplier - 1) | ((heightMultiplier - 1) << 4);

      return new byte[]{29, 33, (byte)n};
  }

  /**
   * Parses a string representation of font size into a FontSize enum.
   *
   * @param fontSize The string representation of font size.
   * @return The corresponding FontSize enum value.
   */
  public static FontSize parseFontSize(String fontSize) {
    return switch (fontSize) {
      case "WIDE" -> FontSize.WIDE;
      case "TALL" -> FontSize.TALL;
      case "BIG" -> FontSize.BIG;
      default -> FontSize.NORMAL;
    };
  }

  /**
   * Pads or truncates text to fit a specified width and alignment.
   *
   * @param text      The text to pad or truncate.
   * @param width     The desired width of the resulting string.
   * @param alignment The desired text alignment.
   * @return The padded or truncated text.
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public static String padText(String text, int width, TextAlignment alignment) {
    int visualWidth = getVisualWidth(text);

    if (visualWidth <= width) {
      String padding = " ".repeat(width - visualWidth);
      switch (alignment) {
        case LEFT:
          return text + padding;
        case RIGHT:
          return padding + text;
        case CENTER:
          int leftPad = padding.length() / 2;
          int rightPad = padding.length() - leftPad;
          return " ".repeat(leftPad) + text + " ".repeat(rightPad);
        default:
          return text;
      }
    } else {
      // Text overflow handling
        return switch (alignment) {
            case LEFT -> truncateToVisualWidth(text, width);
            case RIGHT -> text.substring(Math.max(0, text.length() - width));
            case CENTER -> {
                int startIndex = (text.length() - width) / 2;
                yield text.substring(startIndex, Math.min(startIndex + width, text.length()));
            }
            default -> text;
        };
    }
  }

  /**
   * Truncates a string to fit within a specified visual width.
   *
   * @param text     The text to truncate.
   * @param maxWidth The maximum visual width allowed.
   * @return The truncated text.
   */
  public static String truncateToVisualWidth(String text, int maxWidth) {
    int currentWidth = 0;
    int endIndex = 0;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      int charWidth = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
        Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) ? 2 : 1;
      if (currentWidth + charWidth > maxWidth) break;
      currentWidth += charWidth;
      endIndex = i + 1;
    }
    return text.substring(0, endIndex);
  }

}
