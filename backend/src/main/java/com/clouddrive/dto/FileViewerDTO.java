package com.clouddrive.dto;

import java.util.List;

/**
 * Structured payload for rendering rich inline previews of uploaded files
 * (DOCX documents, XLSX/CSV spreadsheets, PPTX slides, ZIP archives, and plain text).
 */
public class FileViewerDTO {

    private Long id;
    private String name;
    private String extension;
    private String contentType;
    private Long size;
    private String kind;
    private String textContent;
    private List<DocBlock> blocks;
    private List<SheetData> sheets;
    private List<SlideData> slides;
    private List<ArchiveEntry> entries;
    private String note;

    public FileViewerDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }

    public List<DocBlock> getBlocks() { return blocks; }
    public void setBlocks(List<DocBlock> blocks) { this.blocks = blocks; }

    public List<SheetData> getSheets() { return sheets; }
    public void setSheets(List<SheetData> sheets) { this.sheets = sheets; }

    public List<SlideData> getSlides() { return slides; }
    public void setSlides(List<SlideData> slides) { this.slides = slides; }

    public List<ArchiveEntry> getEntries() { return entries; }
    public void setEntries(List<ArchiveEntry> entries) { this.entries = entries; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public static class DocBlock {
        private String type; // "heading" | "paragraph" | "list" | "table"
        private int level;
        private String text;
        private List<List<String>> rows;

        public DocBlock() {}

        public static DocBlock heading(int level, String text) {
            DocBlock b = new DocBlock();
            b.type = "heading";
            b.level = level;
            b.text = text;
            return b;
        }

        public static DocBlock paragraph(String text) {
            DocBlock b = new DocBlock();
            b.type = "paragraph";
            b.text = text;
            return b;
        }

        public static DocBlock listItem(String text) {
            DocBlock b = new DocBlock();
            b.type = "list";
            b.text = text;
            return b;
        }

        public static DocBlock table(List<List<String>> rows) {
            DocBlock b = new DocBlock();
            b.type = "table";
            b.rows = rows;
            return b;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public List<List<String>> getRows() { return rows; }
        public void setRows(List<List<String>> rows) { this.rows = rows; }
    }

    public static class SheetData {
        private String name;
        private List<List<String>> rows;
        private boolean truncated;

        public SheetData() {}

        public SheetData(String name, List<List<String>> rows, boolean truncated) {
            this.name = name;
            this.rows = rows;
            this.truncated = truncated;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public List<List<String>> getRows() { return rows; }
        public void setRows(List<List<String>> rows) { this.rows = rows; }

        public boolean isTruncated() { return truncated; }
        public void setTruncated(boolean truncated) { this.truncated = truncated; }
    }

    public static class SlideData {
        private int slideNumber;
        private String title;
        private List<String> paragraphs;

        public SlideData() {}

        public SlideData(int slideNumber, String title, List<String> paragraphs) {
            this.slideNumber = slideNumber;
            this.title = title;
            this.paragraphs = paragraphs;
        }

        public int getSlideNumber() { return slideNumber; }
        public void setSlideNumber(int slideNumber) { this.slideNumber = slideNumber; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public List<String> getParagraphs() { return paragraphs; }
        public void setParagraphs(List<String> paragraphs) { this.paragraphs = paragraphs; }
    }

    public static class ArchiveEntry {
        private String name;
        private long size;
        private boolean directory;

        public ArchiveEntry() {}

        public ArchiveEntry(String name, long size, boolean directory) {
            this.name = name;
            this.size = size;
            this.directory = directory;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }

        public boolean isDirectory() { return directory; }
        public void setDirectory(boolean directory) { this.directory = directory; }
    }
}
