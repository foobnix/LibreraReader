package com.foobnix.ext;

public class EbookMeta {

    public byte[] coverImage;
    private String title;
    private String author;
    private String sequence;
    private String genre;
    private String annotation;
    private String unzipPath;
    private Integer sIndex;
    private String lang;
    private int pagesCount;
    private String keywords;
    private String year;
    private String publisher;
    private String isbn;

    public EbookMeta(String title, String author, byte[] coverImage) {
        this.title = updateString(title);
        this.author = updateString(author);
        this.coverImage = coverImage;
    }

    public EbookMeta(String title, String author) {
        this(title, author, null, null);
    }

    public EbookMeta(String title, String author, String sequence, String genre) {
        this.title = updateString(title);
        this.author = updateString(author);
        this.sequence = updateString(sequence);
        this.genre = updateString(genre);
    }

    public String updateString(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("  ", " ").trim();
    }

    public static EbookMeta Empty() {
        return new EbookMeta(null, null, null);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public byte[] getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(byte[] coverImage) {
        this.coverImage = coverImage;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public String getUnzipPath() {
        return unzipPath;
    }

    public void setUnzipPath(String unzipPath) {
        this.unzipPath = unzipPath;
    }

    public Integer getsIndex() {
        return sIndex;
    }

    public void setsIndex(Integer sIndex) {
        this.sIndex = sIndex;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public int getPagesCount() {
        return pagesCount;
    }

    public void setPagesCount(int pagesCount) {
        this.pagesCount = pagesCount;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

}
