package com.movrit.movrit.common.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 엑셀(.xlsx) 파일을 만듭니다. 외부 라이브러리 없이 xlsx 구조(zip + XML)를 직접 씁니다.
 *
 * <ul>
 *   <li>기본은 모든 칸을 글자(inline string)로 저장합니다. 전화번호·우편번호의 앞자리 0이 사라지지 않고, 수식으로 해석되지 않습니다.
 *   <li>숫자 시트({@link Sheet#numeric()})는 숫자 모양의 칸만 숫자로 저장해 엑셀에서 합계·정렬을 바로 할 수 있습니다.
 *   <li>첫 줄(제목 줄)은 굵게 표시하고 고정하며, 열 너비를 넉넉하게 잡습니다.
 * </ul>
 */
public final class SimpleXlsxWriter {
  /** 앞자리 0이 없는 정수·소수만 숫자로 봅니다("010", "1e5" 같은 값은 글자로 남깁니다). */
  private static final Pattern NUMBER = Pattern.compile("^-?(0|[1-9][0-9]{0,14})(\\.[0-9]{1,6})?$");

  private SimpleXlsxWriter() {}

  /**
   * 시트 하나입니다.
   *
   * @param name 시트 이름(엑셀 제한 31자)
   * @param numeric true면 숫자 모양의 칸을 숫자로 저장합니다
   */
  public record Sheet(
      String name, List<String> headers, List<List<String>> rows, boolean numeric) {}

  /** 제목 줄과 데이터 줄로 시트 하나짜리 xlsx를 만듭니다. 모든 칸은 글자로 저장합니다. */
  public static byte[] write(String sheetName, List<String> headers, List<List<String>> rows) {
    return write(List.of(new Sheet(sheetName, headers, rows, false)));
  }

  /** 시트 여러 개로 xlsx를 만듭니다. */
  public static byte[] write(List<Sheet> sheets) {
    if (sheets.isEmpty()) throw new IllegalArgumentException("시트가 하나 이상 있어야 합니다.");
    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
      StringBuilder types =
          new StringBuilder(
              "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                  + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                  + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                  + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                  + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                  + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
      StringBuilder workbook =
          new StringBuilder(
              "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                  + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                  + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                  + "<sheets>");
      StringBuilder rels =
          new StringBuilder(
              "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                  + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
      for (int i = 1; i <= sheets.size(); i += 1) {
        types.append("<Override PartName=\"/xl/worksheets/sheet").append(i);
        types.append(
            ".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        workbook.append("<sheet name=\"").append(escape(sheetName(sheets.get(i - 1).name())));
        workbook.append("\" sheetId=\"").append(i).append("\" r:id=\"rId").append(i).append("\"/>");
        rels.append("<Relationship Id=\"rId").append(i);
        rels.append(
            "\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet");
        rels.append(i).append(".xml\"/>");
      }
      types.append("</Types>");
      workbook.append("</sheets></workbook>");
      rels.append("<Relationship Id=\"rId").append(sheets.size() + 1);
      rels.append(
          "\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
      rels.append("</Relationships>");

      put(zip, "[Content_Types].xml", types.toString());
      put(
          zip,
          "_rels/.rels",
          "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
              + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
              + "</Relationships>");
      put(zip, "xl/workbook.xml", workbook.toString());
      put(zip, "xl/_rels/workbook.xml.rels", rels.toString());
      // 스타일 0: 기본, 스타일 1: 굵게(제목 줄)
      put(
          zip,
          "xl/styles.xml",
          "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
              + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"맑은 고딕\"/></font>"
              + "<font><b/><sz val=\"11\"/><name val=\"맑은 고딕\"/></font></fonts>"
              + "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill>"
              + "<fill><patternFill patternType=\"gray125\"/></fill></fills>"
              + "<borders count=\"1\"><border/></borders>"
              + "<cellStyleXfs count=\"1\">"
              + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
              + "<cellXfs count=\"2\">"
              + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
              + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
              + "</cellXfs>"
              + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
              + "</styleSheet>");
      for (int i = 0; i < sheets.size(); i += 1) {
        put(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", sheetXml(sheets.get(i)));
      }
      zip.finish();
      return bytes.toByteArray();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private static String sheetXml(Sheet data) {
    StringBuilder sheet =
        new StringBuilder(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" state=\"frozen\"/></sheetView></sheetViews>"
                + "<cols>");
    for (int i = 1; i <= data.headers().size(); i += 1) {
      sheet.append("<col min=\"").append(i).append("\" max=\"").append(i);
      sheet.append("\" width=\"18\" customWidth=\"1\"/>");
    }
    sheet.append("</cols><sheetData>");
    row(sheet, 1, data.headers(), true, false);
    for (int i = 0; i < data.rows().size(); i += 1) {
      row(sheet, i + 2, data.rows().get(i), false, data.numeric());
    }
    sheet.append("</sheetData></worksheet>");
    return sheet.toString();
  }

  private static void row(
      StringBuilder sheet, int index, List<String> values, boolean bold, boolean numeric) {
    sheet.append("<row r=\"").append(index).append("\">");
    for (int i = 0; i < values.size(); i += 1) {
      String value = values.get(i) == null ? "" : values.get(i);
      sheet.append("<c r=\"").append(column(i)).append(index).append("\"");
      if (numeric && isNumber(value)) {
        sheet.append("><v>").append(value).append("</v></c>");
        continue;
      }
      sheet.append(" t=\"inlineStr\"");
      if (bold) sheet.append(" s=\"1\"");
      sheet.append("><is><t xml:space=\"preserve\">");
      sheet.append(escape(value));
      sheet.append("</t></is></c>");
    }
    sheet.append("</row>");
  }

  /** 숫자 시트에서 숫자로 저장할 값인지 봅니다. */
  public static boolean isNumber(String value) {
    return NUMBER.matcher(value).matches();
  }

  /** 엑셀 시트 이름 규칙(31자, []:*?/\ 금지)에 맞춥니다. */
  static String sheetName(String name) {
    String cleaned =
        name == null || name.isBlank() ? "Sheet" : name.replaceAll("[\\[\\]:*?/\\\\]", " ");
    return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
  }

  /** 0 → A, 25 → Z, 26 → AA */
  public static String column(int index) {
    StringBuilder name = new StringBuilder();
    int value = index + 1;
    while (value > 0) {
      int rest = (value - 1) % 26;
      name.insert(0, (char) ('A' + rest));
      value = (value - 1) / 26;
    }
    return name.toString();
  }

  /** XML 특수 문자를 바꾸고, XML에 쓸 수 없는 제어 문자는 지웁니다. */
  static String escape(String value) {
    StringBuilder out = new StringBuilder(value.length());
    for (char ch : value.toCharArray()) {
      switch (ch) {
        case '&' -> out.append("&amp;");
        case '<' -> out.append("&lt;");
        case '>' -> out.append("&gt;");
        case '"' -> out.append("&quot;");
        default -> {
          if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') out.append(ch);
        }
      }
    }
    return out.toString();
  }

  private static void put(ZipOutputStream zip, String name, String content) throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    zip.write(content.getBytes(StandardCharsets.UTF_8));
    zip.closeEntry();
  }
}
