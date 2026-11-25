package com.clouddrive.service;

import com.clouddrive.dto.FileViewerDTO;
import com.clouddrive.dto.FileViewerDTO.ArchiveEntry;
import com.clouddrive.dto.FileViewerDTO.DocBlock;
import com.clouddrive.dto.FileViewerDTO.SheetData;
import com.clouddrive.dto.FileViewerDTO.SlideData;
import com.clouddrive.entity.FileItem;
import com.clouddrive.entity.User;
import com.clouddrive.storage.StorageProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extracts structured inline preview payloads for Office documents (.docx, .xlsx, .pptx),
 * delimited tables (.csv, .tsv), archives (.zip, .jar), and plain text files using JDK XML/ZIP APIs.
 */
@Service
public class FileViewerService {

    private static final int MAX_XML_ENTRY_BYTES = 8 * 1024 * 1024;
    private static final int MAX_TEXT_PREVIEW_BYTES = 2 * 1024 * 1024;
    private static final int MAX_TABLE_ROWS = 500;
    private static final int MAX_TABLE_COLS = 50;
    private static final int MAX_ARCHIVE_ENTRIES = 500;
    private static final Pattern SLIDE_NUM_PATTERN = Pattern.compile("ppt/slides/slide(\\d+)\\.xml");
    private static final Pattern SHEET_NUM_PATTERN = Pattern.compile("xl/worksheets/sheet(\\d+)\\.xml");

    private final FileStorageService fileStorageService;
    private final StorageProvider storageProvider;

    public FileViewerService(FileStorageService fileStorageService, StorageProvider storageProvider) {
        this.fileStorageService = fileStorageService;
        this.storageProvider = storageProvider;
    }

    @Transactional(readOnly = true)
    public FileViewerDTO buildViewerData(User user, Long fileId) {
        FileItem item = fileStorageService.getFileItem(user, fileId);
        if (item.isDirectory()) {
            throw new IllegalArgumentException("Cannot preview a folder");
        }

        String ext = item.getExtension() != null ? item.getExtension().toLowerCase() : "";
        FileViewerDTO dto = new FileViewerDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setExtension(ext);
        dto.setContentType(item.getContentType());
        dto.setSize(item.getSize());

        try (InputStream is = storageProvider.loadAsStream(item.getDiskPath())) {
            switch (ext) {
                case "docx" -> parseDocx(is, dto);
                case "xlsx" -> parseXlsx(is, dto);
                case "pptx" -> parsePptx(is, dto);
                case "csv" -> parseDelimitedText(is, ',', dto);
                case "tsv" -> parseDelimitedText(is, '\t', dto);
                case "zip", "jar" -> parseArchive(is, dto);
                default -> {
                    if (isTextLike(ext, item.getContentType())) {
                        byte[] bytes = readBounded(is, MAX_TEXT_PREVIEW_BYTES);
                        dto.setKind("text");
                        dto.setTextContent(new String(bytes, StandardCharsets.UTF_8));
                    } else {
                        dto.setKind("unsupported");
                        dto.setNote("Inline preview is not available for ." + (ext.isEmpty() ? "binary" : ext) + " files. Use Download to open locally.");
                    }
                }
            }
        } catch (Exception e) {
            dto.setKind("unsupported");
            dto.setNote("Unable to render inline preview: " + e.getMessage());
        }

        return dto;
    }

    private void parseDocx(InputStream is, FileViewerDTO dto) throws Exception {
        byte[] documentXml = null;
        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    documentXml = readBounded(zis, MAX_XML_ENTRY_BYTES);
                    break;
                }
            }
        }

        if (documentXml == null) {
            dto.setKind("unsupported");
            dto.setNote("Document content XML (word/document.xml) was not found inside this DOCX file.");
            return;
        }

        Document doc = parseSecureXml(documentXml);
        Element body = findFirstChildByLocalName(doc.getDocumentElement(), "body");
        List<DocBlock> blocks = new ArrayList<>();

        if (body != null) {
            collectDocxBlocks(body, blocks);
        }

        dto.setKind("docx");
        dto.setBlocks(blocks);
        if (blocks.isEmpty()) {
            dto.setNote("This document has no readable text blocks (it may contain only images or embedded objects).");
        }
    }

    private void collectDocxBlocks(Element container, List<DocBlock> blocks) {
        NodeList children = container.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            String local = localName(child);
            if ("p".equals(local)) {
                DocBlock block = extractDocxParagraph((Element) child);
                if (block != null) {
                    blocks.add(block);
                }
            } else if ("tbl".equals(local)) {
                DocBlock table = extractDocxTable((Element) child);
                if (table != null) {
                    blocks.add(table);
                }
            } else if ("sdt".equals(local) || "sdtContent".equals(local) || "smartTag".equals(local) || "customXml".equals(local)) {
                collectDocxBlocks((Element) child, blocks);
            }
        }
    }

    private DocBlock extractDocxParagraph(Element p) {
        String styleVal = null;
        boolean isList = false;

        Element pPr = findFirstChildByLocalName(p, "pPr");
        if (pPr != null) {
            Element pStyle = findFirstChildByLocalName(pPr, "pStyle");
            if (pStyle != null) {
                styleVal = getAttrByLocalName(pStyle, "val");
            }
            if (findFirstChildByLocalName(pPr, "numPr") != null) {
                isList = true;
            }
        }

        StringBuilder sb = new StringBuilder();
        collectDocxText(p, sb);
        String text = sb.toString().trim();
        if (text.isEmpty()) {
            return null;
        }

        if (styleVal != null) {
            String lower = styleVal.toLowerCase(Locale.ROOT);
            if ("title".equals(lower) || "heading1".equals(lower) || "heading 1".equals(lower)) {
                return DocBlock.heading(1, text);
            }
            if ("subtitle".equals(lower) || "heading2".equals(lower) || "heading 2".equals(lower)) {
                return DocBlock.heading(2, text);
            }
            if (lower.startsWith("heading")) {
                return DocBlock.heading(3, text);
            }
        }

        return isList ? DocBlock.listItem(text) : DocBlock.paragraph(text);
    }

    private void collectDocxText(Node node, StringBuilder sb) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            String local = localName(child);
            // Skip tracked-change deletions, raw Word field instructions, and AlternateContent Fallbacks
            if ("delText".equals(local) || "instrText".equals(local) || "del".equals(local) || "Fallback".equals(local)) {
                continue;
            }
            if ("t".equals(local)) {
                sb.append(child.getTextContent());
            } else if ("tab".equals(local)) {
                sb.append("    ");
            } else if ("br".equals(local) || "cr".equals(local)) {
                sb.append('\n');
            } else {
                collectDocxText(child, sb);
            }
        }
    }

    private DocBlock extractDocxTable(Element tbl) {
        List<List<String>> rows = new ArrayList<>();
        NodeList children = tbl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node tr = children.item(i);
            if (tr.getNodeType() != Node.ELEMENT_NODE || !"tr".equals(localName(tr))) continue;

            List<String> rowCells = new ArrayList<>();
            NodeList trChildren = tr.getChildNodes();
            for (int j = 0; j < trChildren.getLength(); j++) {
                Node tc = trChildren.item(j);
                if (tc.getNodeType() != Node.ELEMENT_NODE || !"tc".equals(localName(tc))) continue;

                StringBuilder cellText = new StringBuilder();
                NodeList cellChildren = tc.getChildNodes();
                for (int k = 0; k < cellChildren.getLength(); k++) {
                    Node p = cellChildren.item(k);
                    if (p.getNodeType() == Node.ELEMENT_NODE && "p".equals(localName(p))) {
                        StringBuilder pText = new StringBuilder();
                        collectDocxText(p, pText);
                        String trimmed = pText.toString().trim();
                        if (!trimmed.isEmpty()) {
                            if (cellText.length() > 0) cellText.append('\n');
                            cellText.append(trimmed);
                        }
                    }
                }
                rowCells.add(cellText.toString());
            }
            if (!rowCells.isEmpty()) {
                rows.add(rowCells);
            }
        }
        return rows.isEmpty() ? null : DocBlock.table(rows);
    }

    private void parseXlsx(InputStream is, FileViewerDTO dto) throws Exception {
        byte[] sharedStringsXml = null;
        byte[] workbookXml = null;
        byte[] workbookRelsXml = null;
        Map<String, byte[]> worksheetXmlMap = new TreeMap<>();

        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if ("xl/sharedStrings.xml".equals(name)) {
                    sharedStringsXml = readBounded(zis, MAX_XML_ENTRY_BYTES);
                } else if ("xl/workbook.xml".equals(name)) {
                    workbookXml = readBounded(zis, MAX_XML_ENTRY_BYTES);
                } else if ("xl/_rels/workbook.xml.rels".equals(name)) {
                    workbookRelsXml = readBounded(zis, MAX_XML_ENTRY_BYTES);
                } else if (SHEET_NUM_PATTERN.matcher(name).matches()) {
                    worksheetXmlMap.put(name, readBounded(zis, MAX_XML_ENTRY_BYTES));
                }
            }
        }

        List<String> sharedStrings = parseXlsxSharedStrings(sharedStringsXml);
        List<SheetRef> sheetRefs = resolveXlsxSheetOrder(workbookXml, workbookRelsXml, worksheetXmlMap);

        List<SheetData> sheets = new ArrayList<>();
        for (SheetRef ref : sheetRefs) {
            byte[] xml = worksheetXmlMap.get(ref.targetPath);
            if (xml != null) {
                sheets.add(parseXlsxWorksheet(ref.name, xml, sharedStrings));
            }
        }

        dto.setKind("table");
        dto.setSheets(sheets);
        if (sheets.isEmpty()) {
            dto.setNote("No readable worksheets found in this Excel workbook.");
        }
    }

    private List<String> parseXlsxSharedStrings(byte[] xml) throws Exception {
        List<String> strings = new ArrayList<>();
        if (xml == null) return strings;

        Document doc = parseSecureXml(xml);
        NodeList siList = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < siList.getLength(); i++) {
            Node si = siList.item(i);
            if (si.getNodeType() != Node.ELEMENT_NODE || !"si".equals(localName(si))) continue;
            StringBuilder sb = new StringBuilder();
            collectXlsxStringText(si, sb);
            strings.add(sb.toString());
        }
        return strings;
    }

    private void collectXlsxStringText(Node node, StringBuilder sb) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            String local = localName(child);
            if ("rPh".equals(local)) continue; // Ignore phonetic guides
            if ("t".equals(local)) {
                sb.append(child.getTextContent());
            } else {
                collectXlsxStringText(child, sb);
            }
        }
    }

    private record SheetRef(String name, String targetPath) {}

    private List<SheetRef> resolveXlsxSheetOrder(byte[] workbookXml, byte[] relsXml, Map<String, byte[]> worksheetXmlMap) throws Exception {
        Map<String, String> relTargets = new HashMap<>();
        if (relsXml != null) {
            Document relDoc = parseSecureXml(relsXml);
            NodeList rels = relDoc.getDocumentElement().getChildNodes();
            for (int i = 0; i < rels.getLength(); i++) {
                Node r = rels.item(i);
                if (r.getNodeType() != Node.ELEMENT_NODE || !"Relationship".equals(localName(r))) continue;
                String id = ((Element) r).getAttribute("Id");
                String target = ((Element) r).getAttribute("Target");
                if (!target.isEmpty()) {
                    if (target.startsWith("/")) target = target.substring(1);
                    if (target.startsWith("../")) target = target.substring(3);
                    if (!target.startsWith("xl/")) target = "xl/" + target;
                    relTargets.put(id, target);
                }
            }
        }

        List<SheetRef> refs = new ArrayList<>();
        if (workbookXml != null) {
            Document wbDoc = parseSecureXml(workbookXml);
            Element sheetsEl = findFirstChildByLocalName(wbDoc.getDocumentElement(), "sheets");
            if (sheetsEl != null) {
                NodeList sheetNodes = sheetsEl.getChildNodes();
                int fallbackIdx = 1;
                for (int i = 0; i < sheetNodes.getLength(); i++) {
                    Node s = sheetNodes.item(i);
                    if (s.getNodeType() != Node.ELEMENT_NODE || !"sheet".equals(localName(s))) continue;
                    Element el = (Element) s;
                    String name = el.getAttribute("name");
                    if (name.isEmpty()) name = "Sheet " + fallbackIdx;
                    String rId = getAttrByLocalName(el, "id");
                    String target = rId != null ? relTargets.get(rId) : null;
                    if (target == null || !worksheetXmlMap.containsKey(target)) {
                        target = "xl/worksheets/sheet" + fallbackIdx + ".xml";
                    }
                    if (worksheetXmlMap.containsKey(target)) {
                        refs.add(new SheetRef(name, target));
                    }
                    fallbackIdx++;
                }
            }
        }

        if (refs.isEmpty()) {
            List<String> sortedKeys = new ArrayList<>(worksheetXmlMap.keySet());
            sortedKeys.sort(Comparator.comparingInt(k -> {
                Matcher m = SHEET_NUM_PATTERN.matcher(k);
                return m.matches() ? Integer.parseInt(m.group(1)) : Integer.MAX_VALUE;
            }));
            for (int i = 0; i < sortedKeys.size(); i++) {
                refs.add(new SheetRef("Sheet " + (i + 1), sortedKeys.get(i)));
            }
        }
        return refs;
    }

    private SheetData parseXlsxWorksheet(String sheetName, byte[] xml, List<String> sharedStrings) throws Exception {
        Document doc = parseSecureXml(xml);
        Element sheetData = findFirstChildByLocalName(doc.getDocumentElement(), "sheetData");
        List<List<String>> rows = new ArrayList<>();
        boolean truncated = false;
        int maxColsSeen = 0;

        if (sheetData != null) {
            NodeList rowNodes = sheetData.getChildNodes();
            for (int i = 0; i < rowNodes.getLength(); i++) {
                Node r = rowNodes.item(i);
                if (r.getNodeType() != Node.ELEMENT_NODE || !"row".equals(localName(r))) continue;
                if (rows.size() >= MAX_TABLE_ROWS) {
                    truncated = true;
                    break;
                }

                List<String> rowValues = new ArrayList<>();
                NodeList cellNodes = r.getChildNodes();
                for (int j = 0; j < cellNodes.getLength(); j++) {
                    Node c = cellNodes.item(j);
                    if (c.getNodeType() != Node.ELEMENT_NODE || !"c".equals(localName(c))) continue;
                    Element cell = (Element) c;

                    int colIdx = parseColumnIndex(cell.getAttribute("r"), rowValues.size());
                    if (colIdx >= MAX_TABLE_COLS) {
                        truncated = true;
                        continue;
                    }
                    while (rowValues.size() <= colIdx) {
                        rowValues.add("");
                    }

                    String val = extractXlsxCellValue(cell, sharedStrings);
                    rowValues.set(colIdx, val);
                }

                // Skip completely empty trailing rows only if sheet is empty so far
                boolean hasAny = rowValues.stream().anyMatch(s -> !s.isEmpty());
                if (hasAny || !rows.isEmpty()) {
                    maxColsSeen = Math.max(maxColsSeen, rowValues.size());
                    rows.add(rowValues);
                }
            }
        }

        // Normalize rectangular grid width
        for (List<String> r : rows) {
            while (r.size() < maxColsSeen) {
                r.add("");
            }
        }

        return new SheetData(sheetName, rows, truncated);
    }

    private int parseColumnIndex(String cellRef, int fallback) {
        if (cellRef == null || cellRef.isEmpty()) return fallback;
        int col = 0;
        boolean foundLetter = false;
        for (int i = 0; i < cellRef.length(); i++) {
            char ch = cellRef.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                col = col * 26 + (ch - 'A' + 1);
                foundLetter = true;
            } else if (ch >= 'a' && ch <= 'z') {
                col = col * 26 + (ch - 'a' + 1);
                foundLetter = true;
            } else {
                break;
            }
        }
        return foundLetter ? col - 1 : fallback;
    }

    private String extractXlsxCellValue(Element cell, List<String> sharedStrings) {
        String type = cell.getAttribute("t");
        if ("inlineStr".equals(type)) {
            Element is = findFirstChildByLocalName(cell, "is");
            if (is != null) {
                StringBuilder sb = new StringBuilder();
                collectXlsxStringText(is, sb);
                return sb.toString();
            }
            return "";
        }

        Element v = findFirstChildByLocalName(cell, "v");
        if (v == null) return "";
        String raw = v.getTextContent();
        if (raw == null) return "";

        if ("s".equals(type)) {
            try {
                int idx = Integer.parseInt(raw.trim());
                return (idx >= 0 && idx < sharedStrings.size()) ? sharedStrings.get(idx) : raw;
            } catch (NumberFormatException e) {
                return raw;
            }
        }
        if ("b".equals(type)) {
            return "1".equals(raw.trim()) ? "TRUE" : "FALSE";
        }
        return raw;
    }

    private void parsePptx(InputStream is, FileViewerDTO dto) throws Exception {
        Map<Integer, byte[]> slideXmlByNumber = new TreeMap<>();
        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Matcher m = SLIDE_NUM_PATTERN.matcher(entry.getName());
                if (m.matches()) {
                    slideXmlByNumber.put(Integer.parseInt(m.group(1)), readBounded(zis, MAX_XML_ENTRY_BYTES));
                }
            }
        }

        List<SlideData> slides = new ArrayList<>();
        for (Map.Entry<Integer, byte[]> entry : slideXmlByNumber.entrySet()) {
            Document doc = parseSecureXml(entry.getValue());
            List<String> paragraphs = new ArrayList<>();
            collectPptxParagraphs(doc.getDocumentElement(), paragraphs);
            String title = paragraphs.isEmpty() ? "Slide " + entry.getKey() : paragraphs.get(0);
            List<String> body = paragraphs.size() > 1 ? paragraphs.subList(1, paragraphs.size()) : List.of();
            slides.add(new SlideData(entry.getKey(), title, body));
        }

        dto.setKind("slides");
        dto.setSlides(slides);
        if (slides.isEmpty()) {
            dto.setNote("No readable slides found in this presentation.");
        }
    }

    private void collectPptxParagraphs(Node node, List<String> paragraphs) {
        if (node.getNodeType() != Node.ELEMENT_NODE) return;
        if ("p".equals(localName(node))) {
            StringBuilder sb = new StringBuilder();
            collectDrawingText(node, sb);
            String line = sb.toString().trim();
            if (!line.isEmpty()) {
                paragraphs.add(line);
            }
            return;
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            collectPptxParagraphs(children.item(i), paragraphs);
        }
    }

    private void collectDrawingText(Node node, StringBuilder sb) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            String local = localName(child);
            if ("t".equals(local)) {
                sb.append(child.getTextContent());
            } else if ("br".equals(local)) {
                sb.append('\n');
            } else {
                collectDrawingText(child, sb);
            }
        }
    }

    private void parseDelimitedText(InputStream is, char delimiter, FileViewerDTO dto) throws IOException {
        byte[] bytes = readBounded(is, MAX_TEXT_PREVIEW_BYTES);
        String text = new String(bytes, StandardCharsets.UTF_8);

        List<List<String>> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean truncated = false;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(ch);
                }
            } else {
                if (ch == '"') {
                    inQuotes = true;
                } else if (ch == delimiter) {
                    if (currentRow.size() < MAX_TABLE_COLS) {
                        currentRow.add(field.toString());
                    }
                    field.setLength(0);
                } else if (ch == '\r' || ch == '\n') {
                    if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                        i++;
                    }
                    if (currentRow.size() < MAX_TABLE_COLS) {
                        currentRow.add(field.toString());
                    }
                    field.setLength(0);
                    if (!currentRow.isEmpty()) {
                        rows.add(currentRow);
                        currentRow = new ArrayList<>();
                        if (rows.size() >= MAX_TABLE_ROWS) {
                            truncated = true;
                            break;
                        }
                    }
                } else {
                    field.append(ch);
                }
            }
        }

        if (!truncated && (field.length() > 0 || !currentRow.isEmpty())) {
            if (currentRow.size() < MAX_TABLE_COLS) {
                currentRow.add(field.toString());
            }
            rows.add(currentRow);
        }

        int maxCols = rows.stream().mapToInt(List::size).max().orElse(0);
        for (List<String> r : rows) {
            while (r.size() < maxCols) r.add("");
        }

        dto.setKind("table");
        dto.setTextContent(text);
        dto.setSheets(List.of(new SheetData(dto.getName(), rows, truncated)));
    }

    private void parseArchive(InputStream is, FileViewerDTO dto) throws IOException {
        List<ArchiveEntry> entries = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entries.size() >= MAX_ARCHIVE_ENTRIES) {
                    dto.setNote("Showing first " + MAX_ARCHIVE_ENTRIES + " archive entries.");
                    break;
                }
                entries.add(new ArchiveEntry(entry.getName(), Math.max(0L, entry.getSize()), entry.isDirectory()));
            }
        }
        dto.setKind("archive");
        dto.setEntries(entries);
    }

    private Document parseSecureXml(byte[] xmlBytes) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xmlBytes));
    }

    private byte[] readBounded(InputStream is, int maxBytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = is.read(buffer, 0, Math.min(buffer.length, maxBytes - total))) > 0) {
            baos.write(buffer, 0, read);
            total += read;
            if (total >= maxBytes) break;
        }
        return baos.toByteArray();
    }

    private boolean isTextLike(String ext, String contentType) {
        if (Set.of("txt", "md", "log", "yml", "yaml", "ini", "env", "conf", "properties",
                "js", "jsx", "ts", "tsx", "html", "css", "scss", "py", "java", "json",
                "xml", "sql", "sh", "bat", "ps1", "rs", "go", "c", "cpp", "h", "svg").contains(ext)) {
            return true;
        }
        return contentType != null && contentType.startsWith("text/");
    }

    private String localName(Node node) {
        String local = node.getLocalName();
        if (local != null) return local;
        String name = node.getNodeName();
        int idx = name.indexOf(':');
        return idx >= 0 ? name.substring(idx + 1) : name;
    }

    private Element findFirstChildByLocalName(Element parent, String targetLocalName) {
        if (parent == null) return null;
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && targetLocalName.equals(localName(child))) {
                return (Element) child;
            }
        }
        return null;
    }

    private String getAttrByLocalName(Element el, String targetLocalName) {
        var attrs = el.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            if (targetLocalName.equals(localName(attr))) {
                return attr.getNodeValue();
            }
        }
        return null;
    }
}
