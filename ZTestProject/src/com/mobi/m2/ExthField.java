package com.mobi.m2;

import java.util.HashMap;

public enum ExthField {

    Missing(0, "Not Defined", ExthFieldType.Array),

    DrmServerId(1, "drm_server_id", ExthFieldType.Array),
    DrmCommerceId(2, "drm_commerce_id", ExthFieldType.Array),
    DrmEbookbaseBookId(3, "drm_ebookbase_book_id", ExthFieldType.Array),
    Author(100, "author", ExthFieldType.Text),
    Publisher(101, "publisher", ExthFieldType.Text),
    Imprint(102, "imprint", ExthFieldType.Text),
    Description(103, "description", ExthFieldType.Text),
    ISBN(104, "ISBN", ExthFieldType.Text),
    Subject(105, "subject", ExthFieldType.Text),
    PublishingDate(106, "publishingdate", ExthFieldType.Text),
    Review(107, "review", ExthFieldType.Text),
    Contributor(108, "contributor", ExthFieldType.Text),
    Rights(109, "rights", ExthFieldType.Text),
    SubjectCode(110, "subjectcode", ExthFieldType.Text),
    BookType(111, "type", ExthFieldType.Text),
    Source(112, "source", ExthFieldType.Text),
    ASIN(113, "ASIN", ExthFieldType.Text),
    VersionNumber(114, "versionnumber", ExthFieldType.Text),
    IsSample(115, "is sample", ExthFieldType.Array),
    StartReadingPos(116, "start reading pos", ExthFieldType.Array),
    Adult(117, "is adult", ExthFieldType.Array),
    RetailPrice(118, "retail price", ExthFieldType.Text),
    RetailPriceCurrency(119, "retail price currency", ExthFieldType.Text),
    Kf8BoundaryOffset(121, "KF8 BOUNDARY Offset", ExthFieldType.Array),
    CountOfResources(125, "count of resources", ExthFieldType.Array),
    Kf8CoverURI(129, "KF8 cover URI", ExthFieldType.Text),
    Unknown01(131, "Unknown", ExthFieldType.Array),
    DictionaryShortName(200, "Dictionary short name", ExthFieldType.Text),
    CoverOffset(201, "coveroffset", ExthFieldType.Array),
    ThumbOffset(202, "thumboffset", ExthFieldType.Array),
    HasFakeCover(203, "hasfakecover", ExthFieldType.Bool),
    CreatorSoftwareId(204, "Creator Software ID", ExthFieldType.Array),
    CreatorMajorVersion(205, "Creator Major Version", ExthFieldType.Array),
    CreatorMinorVersion(206, "Creator Minor Version", ExthFieldType.Array),
    CreatorBuildNumber(207, "Creator Build Number", ExthFieldType.Array),

    Watermark(208, "watermark", ExthFieldType.Text),
    // Watermark is string which looks like:
    // atv:kin:1:xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx:yyyyyyyyyyyyyyyyyyyyyyyyyyyy
    // Where first and second parts are Base64 encoded data. First part (128 bytes long) contains Book ID and User ID of Amazon user.
    // It's split as:
    //  16 bytes = hash
    //  48 bytes = book id
    //  32 bytes = user id
    //  32 bytes = unknown

    TamperProofKeys(209, "tamper proof keys", ExthFieldType.Array),
    FontSignature(300, "fontsignature", ExthFieldType.Array),
    clippinglimit(401, "clippinglimit", ExthFieldType.Array),
    publisherlimit(402, "publisherlimit", ExthFieldType.Array),
    Unknown02(403, "Unknown", ExthFieldType.Array),
    IsTTSDisabled(404, "Text to Speech Disabled", ExthFieldType.Bool),
    IsRental(405, "is rental", ExthFieldType.Bool),
    RentExpirationDate(406, "Rent Expiration Date", ExthFieldType.Array),
    Unknown03(407, "Unknown", ExthFieldType.Array),
    Unknown04(450, "Unknown", ExthFieldType.Array),
    Unknown05(451, "Unknown", ExthFieldType.Array),
    Unknown06(452, "Unknown", ExthFieldType.Array),
    Unknown07(453, "Unknown", ExthFieldType.Array),
    CDEType(501, "cdetype", ExthFieldType.Text),
    LastUpdateTime(502, "lastupdatetime", ExthFieldType.Array),
    UpdatedTitle(503, "updatedtitle", ExthFieldType.Text),
    ASIN2(504, "ASIN", ExthFieldType.Text),
    Language(524, "language", ExthFieldType.Text),
    Alignment(525, "alignment", ExthFieldType.Array),
    SomeDateTime01(526, "File creation?", ExthFieldType.Text),
    Unknown08(528, "Bool as text", ExthFieldType.Text),
    Unknown09(534, "Type of source file?", ExthFieldType.Text),
    CreatorBuildNumber2(535, "Creator Build Number", ExthFieldType.Array),
    Unknown10(536, "Resolution?", ExthFieldType.Text),
    Unknown11(542, "Unknown", ExthFieldType.Text),
    ;

    private static final HashMap<Integer, ExthField> byIdMap = new HashMap<Integer, ExthField>();

    static {
        for (ExthField field : ExthField.values())
            byIdMap.put(field.getId(), field);
    }

    public static boolean isKnown(int id) {
        return (id > 0) && byIdMap.containsKey(id);
    }

    public static ExthField getById(int id) {
        return isKnown(id) ? byIdMap.get(id) : Missing;
    }

    private int id;
    private String title;
    private ExthFieldType type;

    ExthField(int id, String title, ExthFieldType type) {
        this.id = id;
        this.title = title;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public ExthFieldType getType() {
        return type;
    }
}

