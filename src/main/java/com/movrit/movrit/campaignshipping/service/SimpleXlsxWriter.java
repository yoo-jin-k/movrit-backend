package com.movrit.movrit.campaignshipping.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 시트 하나짜리 엑셀(.xlsx) 파일을 만듭니다. 외부 라이브러리 없이 xlsx 구조(zip + XML)를 직접 씁니다.
 *
 * <ul>
 *   <li>모든 칸은 글자(inline string)로 저장합니다. 전화번호·우편번호의 앞자리 0이 사라지지 않습니다.
 *   <li>첫 줄(제목 줄)은 굵게 표시하고 열 너비를 넉넉하게 잡습니다.
 *   <li>엑셀 수식으로 해석될 수 있는 값은 글자로 저장되므로 수식 실행(CSV 인젝션) 걱정이 없습니다.
 * </ul>
 */
public final class SimpleXlsxWriter {
  private SimpleXlsxWriter() {}

  /** 제목 줄과 데이터 줄로 xlsx 바이트를 만듭니다. */
  public static byte[] write(String sheetName, List<String> headers, List<List<String>> rows) {
    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
      put(
          zip,
          "[Content_Types].xml",
          "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
              + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
              + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
              + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
              + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
              + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
              + "</Types>");
      put(
          zip,
          "_rels/.rels",
          "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
              + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
              + "</Relationships>");
      put(
          zip,
          "xl/workbook.xml",
          "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
              + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
              + "<sheets><sheet name=\""
              + escape(sheetName)
              + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
      put(
          zip,
          "xl/_rels/workbook.xml.rels",
          "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
              + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
              + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
              + "</Relationships>");
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
      StringBuilder sheet =
          new StringBuilder(
              "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                  + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                  + "<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" state=\"frozen\"/></sheetView></sheetViews>"
                  + "<cols>");
      for (int i = 1; i <= headers.size(); i += 1) {
        sheet.append("<col min=\"").append(i).append("\" max=\"").append(i);
        sheet.append("\" width=\"18\" customWidth=\"1\"/>");
      }
      sheet.append("</cols><sheetData>");
      row(sheet, 1, headers, true);
      for (int i = 0; i < rows.size(); i += 1) row(sheet, i + 2, rows.get(i), false);
      sheet.append("</sheetData></worksheet>");
      put(zip, "xl/worksheets/sheet1.xml", sheet.toString());
      zip.finish();
      return bytes.toByteArray();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private static void row(StringBuilder sheet, int index, List<String> values, boolean bold) {
    sheet.append("<row r=\"").append(index).append("\">");
    for (int i = 0; i < values.size(); i += 1) {
      sheet.append("<c r=\"").append(column(i)).append(index).append("\" t=\"inlineStr\"");
      if (bold) sheet.append(" s=\"1\"");
      sheet.append("><is><t xml:space=\"preserve\">");
      sheet.append(escape(values.get(i) == null ? "" : values.get(i)));
      sheet.append("</t></is></c>");
    }
    sheet.append("</row>");
  }

  /** 0 → A, 25 → Z, 26 → AA */
  static String column(int index) {
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
