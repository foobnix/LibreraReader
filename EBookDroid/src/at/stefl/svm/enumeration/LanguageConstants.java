package at.stefl.svm.enumeration;

public class LanguageConstants {
    
    public static final int LANGUAGE_MASK_PRIMARY = 0x03ff;
    
    public static final int LANGUAGE_DONTKNOW = 0x03FF; /* yes, the mask */
    public static final int LANGUAGE_NONE = 0x00FF;
    public static final int LANGUAGE_HID_HUMAN_INTERFACE_DEVICE = 0x04FF;
    public static final int LANGUAGE_SYSTEM = 0x0000; /* OOo/SO definition */
    
    /*
     * The Invariant Locale (Locale ID = 0x007f) is a locale that can be used by
     * applications when a consistent and locale-independent result is required.
     * The invariant locale can be used, for example, when comparing character
     * strings using the CompareString() API and a consistent result regardless
     * of the User Locale is expected. The settings of the Invariant Locale are
     * similar to US-English international standards, but should not be used to
     * display formatted data.
     */
    /* NOTE: this is taken from the MS documentation! Not supported by OOo/SO! */
    public static final int LANGUAGE_INVARIANT = 0x007F;
    
    public static final int LANGUAGE_AFRIKAANS = 0x0436;
    public static final int LANGUAGE_ALBANIAN = 0x041C;
    public static final int LANGUAGE_ALSATIAN_FRANCE = 0x0484;
    public static final int LANGUAGE_AMHARIC_ETHIOPIA = 0x045E;
    public static final int LANGUAGE_ARABIC_ALGERIA = 0x1401;
    public static final int LANGUAGE_ARABIC_BAHRAIN = 0x3C01;
    public static final int LANGUAGE_ARABIC_EGYPT = 0x0C01;
    public static final int LANGUAGE_ARABIC_IRAQ = 0x0801;
    public static final int LANGUAGE_ARABIC_JORDAN = 0x2C01;
    public static final int LANGUAGE_ARABIC_KUWAIT = 0x3401;
    public static final int LANGUAGE_ARABIC_LEBANON = 0x3001;
    public static final int LANGUAGE_ARABIC_LIBYA = 0x1001;
    public static final int LANGUAGE_ARABIC_MOROCCO = 0x1801;
    public static final int LANGUAGE_ARABIC_OMAN = 0x2001;
    public static final int LANGUAGE_ARABIC_QATAR = 0x4001;
    public static final int LANGUAGE_ARABIC_SAUDI_ARABIA = 0x0401;
    public static final int LANGUAGE_ARABIC_SYRIA = 0x2801;
    public static final int LANGUAGE_ARABIC_TUNISIA = 0x1C01;
    public static final int LANGUAGE_ARABIC_UAE = 0x3801;
    public static final int LANGUAGE_ARABIC_YEMEN = 0x2401;
    public static final int LANGUAGE_ARABIC_PRIMARY_ONLY = 0x0001; /*
                                                                    * primary
                                                                    * only, not
                                                                    * a locale!
                                                                    */
    public static final int LANGUAGE_ARMENIAN = 0x042B;
    public static final int LANGUAGE_ASSAMESE = 0x044D;
    public static final int LANGUAGE_AZERI = 0x002C; /*
                                                      * primary only, not a
                                                      * locale!
                                                      */
    public static final int LANGUAGE_AZERI_CYRILLIC = 0x082C;
    public static final int LANGUAGE_AZERI_LATIN = 0x042C;
    public static final int LANGUAGE_BASHKIR_RUSSIA = 0x046D;
    public static final int LANGUAGE_BASQUE = 0x042D;
    public static final int LANGUAGE_BELARUSIAN = 0x0423;
    public static final int LANGUAGE_BENGALI = 0x0445; /* in India */
    public static final int LANGUAGE_BENGALI_BANGLADESH = 0x0845;
    public static final int LANGUAGE_BOSNIAN_LATIN_BOSNIA_HERZEGOVINA = 0x141A;
    public static final int LANGUAGE_BOSNIAN_CYRILLIC_BOSNIA_HERZEGOVINA = 0x201A;
    public static final int LANGUAGE_BOSNIAN_BOSNIA_HERZEGOVINA = LANGUAGE_BOSNIAN_LATIN_BOSNIA_HERZEGOVINA; /*
                                                                                                              * TODO
                                                                                                              * :
                                                                                                              * remove
                                                                                                              * ,
                                                                                                              * only
                                                                                                              * for
                                                                                                              * langtab
                                                                                                              * .
                                                                                                              * src
                                                                                                              * &
                                                                                                              * localize
                                                                                                              * .
                                                                                                              * sdf
                                                                                                              * compatibility
                                                                                                              */
    public static final int LANGUAGE_BRETON_FRANCE = 0x047E; /*
                                                              * obsoletes
                                                              * LANGUAGE_USER_BRETON
                                                              * = 0x0629;
                                                              */
    public static final int LANGUAGE_BULGARIAN = 0x0402;
    public static final int LANGUAGE_BURMESE = 0x0455;
    public static final int LANGUAGE_CATALAN = 0x0403;
    public static final int LANGUAGE_CHEROKEE_UNITED_STATES = 0x045C;
    public static final int LANGUAGE_CHINESE = 0x0004; /*
                                                        * primary only, not a
                                                        * locale!
                                                        */
    public static final int LANGUAGE_CHINESE_HONGKONG = 0x0C04;
    public static final int LANGUAGE_CHINESE_MACAU = 0x1404;
    public static final int LANGUAGE_CHINESE_SIMPLIFIED = 0x0804;
    public static final int LANGUAGE_CHINESE_SINGAPORE = 0x1004;
    public static final int LANGUAGE_CHINESE_TRADITIONAL = 0x0404;
    /* public static final int LANGUAGE_CHINESE_SIMPLIFIED = 0x0004; *//*
                                                                        * artificial
                                                                        * political
                                                                        * ?
                                                                        * Defined
                                                                        * as
                                                                        * 'zh-CHS'
                                                                        * by MS.
                                                                        * Primary
                                                                        * only!
                                                                        */
    /* public static final int LANGUAGE_CHINESE_TRADITIONAL = 0x7C04; *//*
                                                                         * artificial
                                                                         * political
                                                                         * ?
                                                                         * Defined
                                                                         * as
                                                                         * 'zh-CHT'
                                                                         * by
                                                                         * MS.
                                                                         */
    public static final int LANGUAGE_CORSICAN_FRANCE = 0x0483;
    public static final int LANGUAGE_CROATIAN = 0x041A;
    public static final int LANGUAGE_CROATIAN_BOSNIA_HERZEGOVINA = 0x101A;
    public static final int LANGUAGE_CZECH = 0x0405;
    public static final int LANGUAGE_DANISH = 0x0406;
    public static final int LANGUAGE_DARI_AFGHANISTAN = 0x048C; /*
                                                                 * AKA
                                                                 * Zoroastrian
                                                                 * Dari
                                                                 */
    public static final int LANGUAGE_DHIVEHI = 0x0465; /* AKA Divehi */
    public static final int LANGUAGE_DUTCH = 0x0413;
    public static final int LANGUAGE_DUTCH_BELGIAN = 0x0813;
    public static final int LANGUAGE_EDO = 0x0466;
    public static final int LANGUAGE_ENGLISH = 0x0009; /*
                                                        * primary only, not a
                                                        * locale!
                                                        */
    public static final int LANGUAGE_ENGLISH_AUS = 0x0C09;
    public static final int LANGUAGE_ENGLISH_BELIZE = 0x2809;
    public static final int LANGUAGE_ENGLISH_CAN = 0x1009;
    public static final int LANGUAGE_ENGLISH_CARRIBEAN = 0x2409;
    public static final int LANGUAGE_ENGLISH_EIRE = 0x1809;
    public static final int LANGUAGE_ENGLISH_HONG_KONG_SAR = 0x3C09;
    public static final int LANGUAGE_ENGLISH_INDIA = 0x4009;
    public static final int LANGUAGE_ENGLISH_INDONESIA = 0x3809;
    public static final int LANGUAGE_ENGLISH_JAMAICA = 0x2009;
    public static final int LANGUAGE_ENGLISH_MALAYSIA = 0x4409;
    public static final int LANGUAGE_ENGLISH_NZ = 0x1409;
    public static final int LANGUAGE_ENGLISH_PHILIPPINES = 0x3409;
    public static final int LANGUAGE_ENGLISH_SAFRICA = 0x1C09;
    public static final int LANGUAGE_ENGLISH_SINGAPORE = 0x4809;
    public static final int LANGUAGE_ENGLISH_TRINIDAD = 0x2C09;
    public static final int LANGUAGE_ENGLISH_UK = 0x0809;
    public static final int LANGUAGE_ENGLISH_US = 0x0409;
    public static final int LANGUAGE_ENGLISH_ZIMBABWE = 0x3009;
    public static final int LANGUAGE_ESTONIAN = 0x0425;
    public static final int LANGUAGE_FAEROESE = 0x0438;
    public static final int LANGUAGE_FARSI = 0x0429;
    public static final int LANGUAGE_FILIPINO = 0x0464;
    public static final int LANGUAGE_FINNISH = 0x040B;
    public static final int LANGUAGE_FRENCH = 0x040C;
    public static final int LANGUAGE_FRENCH_BELGIAN = 0x080C;
    public static final int LANGUAGE_FRENCH_CAMEROON = 0x2C0C;
    public static final int LANGUAGE_FRENCH_CANADIAN = 0x0C0C;
    public static final int LANGUAGE_FRENCH_COTE_D_IVOIRE = 0x300C;
    
    public static final int LANGUAGE_FRENCH_HAITI = 0x3C0C;
    public static final int LANGUAGE_FRENCH_LUXEMBOURG = 0x140C;
    public static final int LANGUAGE_FRENCH_MALI = 0x340C;
    public static final int LANGUAGE_FRENCH_MONACO = 0x180C;
    public static final int LANGUAGE_FRENCH_MOROCCO = 0x380C;
    public static final int LANGUAGE_FRENCH_NORTH_AFRICA = 0xE40C;
    public static final int LANGUAGE_FRENCH_REUNION = 0x200C;
    public static final int LANGUAGE_FRENCH_SENEGAL = 0x280C;
    public static final int LANGUAGE_FRENCH_SWISS = 0x100C;
    public static final int LANGUAGE_FRENCH_WEST_INDIES = 0x1C0C;
    public static final int LANGUAGE_FRENCH_ZAIRE = 0x240C;
    public static final int LANGUAGE_FRISIAN_NETHERLANDS = 0x0462;
    public static final int LANGUAGE_FULFULDE_NIGERIA = 0x0467;
    public static final int LANGUAGE_GAELIC_IRELAND = 0x083C;
    public static final int LANGUAGE_GAELIC_SCOTLAND = 0x043C;
    public static final int LANGUAGE_GALICIAN = 0x0456;
    public static final int LANGUAGE_GEORGIAN = 0x0437;
    public static final int LANGUAGE_GERMAN = 0x0407;
    public static final int LANGUAGE_GERMAN_AUSTRIAN = 0x0C07;
    public static final int LANGUAGE_GERMAN_LIECHTENSTEIN = 0x1407;
    public static final int LANGUAGE_GERMAN_LUXEMBOURG = 0x1007;
    public static final int LANGUAGE_GERMAN_SWISS = 0x0807;
    public static final int LANGUAGE_GREEK = 0x0408;
    public static final int LANGUAGE_GUARANI_PARAGUAY = 0x0474;
    public static final int LANGUAGE_GUJARATI = 0x0447;
    public static final int LANGUAGE_HAUSA_NIGERIA = 0x0468;
    public static final int LANGUAGE_HAWAIIAN_UNITED_STATES = 0x0475;
    public static final int LANGUAGE_HEBREW = 0x040D;
    public static final int LANGUAGE_HINDI = 0x0439;
    public static final int LANGUAGE_HUNGARIAN = 0x040E;
    public static final int LANGUAGE_IBIBIO_NIGERIA = 0x0469;
    public static final int LANGUAGE_ICELANDIC = 0x040F;
    public static final int LANGUAGE_IGBO_NIGERIA = 0x0470;
    public static final int LANGUAGE_INDONESIAN = 0x0421;
    public static final int LANGUAGE_INUKTITUT_SYLLABICS_CANADA = 0x045D;
    public static final int LANGUAGE_INUKTITUT_LATIN_CANADA = 0x085D;
    public static final int LANGUAGE_ITALIAN = 0x0410;
    public static final int LANGUAGE_ITALIAN_SWISS = 0x0810;
    public static final int LANGUAGE_JAPANESE = 0x0411;
    public static final int LANGUAGE_KALAALLISUT_GREENLAND = 0x046F; /*
                                                                      * obsoletes
                                                                      * LANGUAGE_USER_KALAALLISUT
                                                                      * =
                                                                      * 0x062A;
                                                                      */
    public static final int LANGUAGE_KANNADA = 0x044B;
    public static final int LANGUAGE_KANURI_NIGERIA = 0x0471;
    public static final int LANGUAGE_KASHMIRI = 0x0460;
    public static final int LANGUAGE_KASHMIRI_INDIA = 0x0860;
    public static final int LANGUAGE_KAZAK = 0x043F;
    public static final int LANGUAGE_KHMER = 0x0453;
    public static final int LANGUAGE_KICHE_GUATEMALA = 0x0486; /*
                                                                * AKA K'iche',
                                                                * West Central
                                                                * Quiche,
                                                                */
    public static final int LANGUAGE_KINYARWANDA_RWANDA = 0x0487; /*
                                                                   * obsoletes
                                                                   * LANGUAGE_USER_KINYARWANDA
                                                                   * = 0x0621;
                                                                   */
    public static final int LANGUAGE_KIRGHIZ = 0x0440; /* AKA Kyrgyz */
    public static final int LANGUAGE_KONKANI = 0x0457;
    public static final int LANGUAGE_KOREAN = 0x0412;
    public static final int LANGUAGE_KOREAN_JOHAB = 0x0812;
    public static final int LANGUAGE_LAO = 0x0454;
    public static final int LANGUAGE_LATIN = 0x0476; /*
                                                      * obsoletes
                                                      * LANGUAGE_USER_LATIN =
                                                      * 0x0610;
                                                      */
    public static final int LANGUAGE_LATVIAN = 0x0426;
    public static final int LANGUAGE_LITHUANIAN = 0x0427;
    public static final int LANGUAGE_LITHUANIAN_CLASSIC = 0x0827;
    public static final int LANGUAGE_LUXEMBOURGISH_LUXEMBOURG = 0x046E; /*
                                                                         * obsoletes
                                                                         * LANGUAGE_USER_LUXEMBOURGISH
                                                                         * =
                                                                         * 0x0630
                                                                         * ;
                                                                         */
    public static final int LANGUAGE_MACEDONIAN = 0x042F;
    public static final int LANGUAGE_MALAY = 0x003E; /*
                                                      * primary only, not a
                                                      * locale!
                                                      */
    public static final int LANGUAGE_MALAYALAM = 0x044C; /* in India */
    public static final int LANGUAGE_MALAY_BRUNEI_DARUSSALAM = 0x083E;
    public static final int LANGUAGE_MALAY_MALAYSIA = 0x043E;
    public static final int LANGUAGE_MALTESE = 0x043A;
    public static final int LANGUAGE_MANIPURI = 0x0458;
    public static final int LANGUAGE_MAORI_NEW_ZEALAND = 0x0481; /*
                                                                  * obsoletes
                                                                  * LANGUAGE_USER_MAORI
                                                                  * = 0x0620;
                                                                  */
    public static final int LANGUAGE_MAPUDUNGUN_CHILE = 0x047A; /*
                                                                 * AKA
                                                                 * Araucanian
                                                                 */
    public static final int LANGUAGE_MARATHI = 0x044E;
    public static final int LANGUAGE_MOHAWK_CANADA = 0x047C;
    public static final int LANGUAGE_MONGOLIAN = 0x0450; /* Cyrillic script */
    public static final int LANGUAGE_MONGOLIAN_MONGOLIAN = 0x0850;
    public static final int LANGUAGE_NEPALI = 0x0461;
    public static final int LANGUAGE_NEPALI_INDIA = 0x0861;
    public static final int LANGUAGE_NORWEGIAN = 0x0014; /*
                                                          * primary only, not a
                                                          * locale!
                                                          */
    public static final int LANGUAGE_NORWEGIAN_BOKMAL = 0x0414;
    public static final int LANGUAGE_NORWEGIAN_NYNORSK = 0x0814;
    public static final int LANGUAGE_OCCITAN_FRANCE = 0x0482; /*
                                                               * obsoletes
                                                               * LANGUAGE_USER_OCCITAN
                                                               * = 0x0625;
                                                               */
    public static final int LANGUAGE_ORIYA = 0x0448;
    public static final int LANGUAGE_OROMO = 0x0472;
    public static final int LANGUAGE_PAPIAMENTU = 0x0479;
    public static final int LANGUAGE_PASHTO = 0x0463;
    public static final int LANGUAGE_POLISH = 0x0415;
    public static final int LANGUAGE_PORTUGUESE = 0x0816;
    public static final int LANGUAGE_PORTUGUESE_BRAZILIAN = 0x0416;
    public static final int LANGUAGE_PUNJABI = 0x0446;
    public static final int LANGUAGE_PUNJABI_PAKISTAN = 0x0846;
    public static final int LANGUAGE_QUECHUA_BOLIVIA = 0x046B;
    public static final int LANGUAGE_QUECHUA_ECUADOR = 0x086B;
    public static final int LANGUAGE_QUECHUA_PERU = 0x0C6B;
    public static final int LANGUAGE_RHAETO_ROMAN = 0x0417;
    public static final int LANGUAGE_ROMANIAN = 0x0418;
    public static final int LANGUAGE_ROMANIAN_MOLDOVA = 0x0818;
    public static final int LANGUAGE_RUSSIAN = 0x0419;
    public static final int LANGUAGE_RUSSIAN_MOLDOVA = 0x0819;
    public static final int LANGUAGE_SAMI_NORTHERN_NORWAY = 0x043B;
    public static final int LANGUAGE_SAMI_LAPPISH = LANGUAGE_SAMI_NORTHERN_NORWAY; /*
                                                                                    * the
                                                                                    * old
                                                                                    * MS
                                                                                    * definition
                                                                                    */
    public static final int LANGUAGE_SAMI_INARI = 0x243B;
    public static final int LANGUAGE_SAMI_LULE_NORWAY = 0x103B;
    public static final int LANGUAGE_SAMI_LULE_SWEDEN = 0x143B;
    public static final int LANGUAGE_SAMI_NORTHERN_FINLAND = 0x0C3B;
    public static final int LANGUAGE_SAMI_NORTHERN_SWEDEN = 0x083B;
    public static final int LANGUAGE_SAMI_SKOLT = 0x203B;
    public static final int LANGUAGE_SAMI_SOUTHERN_NORWAY = 0x183B;
    public static final int LANGUAGE_SAMI_SOUTHERN_SWEDEN = 0x1C3B;
    public static final int LANGUAGE_SANSKRIT = 0x044F;
    public static final int LANGUAGE_SEPEDI = 0x046C;
    public static final int LANGUAGE_NORTHERNSOTHO = LANGUAGE_SEPEDI; /*
                                                                       * just an
                                                                       * alias
                                                                       * for the
                                                                       * already
                                                                       * existing
                                                                       * localization
                                                                       */
    public static final int LANGUAGE_SERBIAN = 0x001A; /*
                                                        * primary only, not a
                                                        * locale!
                                                        */
    public static final int LANGUAGE_SERBIAN_CYRILLIC = 0x0C1A; /*
                                                                 * MS lists this
                                                                 * as Serbian
                                                                 * (Cyrillic,
                                                                 * Serbia)
                                                                 * 'sr-Cyrl-SP',
                                                                 * but they use
                                                                 * 'SP' since at
                                                                 * least
                                                                 * Windows2003
                                                                 * where it was
                                                                 * Serbia and
                                                                 * Montenegro!
                                                                 */
    public static final int LANGUAGE_SERBIAN_CYRILLIC_BOSNIA_HERZEGOVINA = 0x1C1A;
    public static final int LANGUAGE_SERBIAN_LATIN = 0x081A; /*
                                                              * MS lists this as
                                                              * Serbian (Latin,
                                                              * Serbia)
                                                              * 'sr-Latn-SP',
                                                              * but they use
                                                              * 'SP' since at
                                                              * least
                                                              * Windows2003
                                                              * where it was
                                                              * Serbia and
                                                              * Montenegro!
                                                              */
    public static final int LANGUAGE_SERBIAN_LATIN_BOSNIA_HERZEGOVINA = 0x181A;
    public static final int LANGUAGE_SERBIAN_LATIN_NEUTRAL = 0x7C1A; /*
                                                                      * MS lists
                                                                      * this as
                                                                      * 'sr'
                                                                      * only.
                                                                      * What a
                                                                      * mess.
                                                                      */
    public static final int LANGUAGE_SESOTHO = 0x0430; /*
                                                        * also called Sutu now
                                                        * by MS
                                                        */
    public static final int LANGUAGE_SINDHI = 0x0459;
    public static final int LANGUAGE_SINDHI_PAKISTAN = 0x0859;
    public static final int LANGUAGE_SINHALESE_SRI_LANKA = 0x045B;
    public static final int LANGUAGE_SLOVAK = 0x041B;
    public static final int LANGUAGE_SLOVENIAN = 0x0424;
    public static final int LANGUAGE_SOMALI = 0x0477;
    public static final int LANGUAGE_UPPER_SORBIAN_GERMANY = 0x042E; /*
                                                                      * obsoletes
                                                                      * LANGUAGE_USER_UPPER_SORBIAN
                                                                      * =
                                                                      * 0x0623;
                                                                      */
    public static final int LANGUAGE_LOWER_SORBIAN_GERMANY = 0x082E; /*
                                                                      * obsoletes
                                                                      * LANGUAGE_USER_LOWER_SORBIAN
                                                                      * =
                                                                      * 0x0624;.
                                                                      * NOTE:
                                                                      * the
                                                                      * primary
                                                                      * ID is
                                                                      * identical
                                                                      * to Upper
                                                                      * Sorbian,
                                                                      * which is
                                                                      * not
                                                                      * quite
                                                                      * correct
                                                                      * because
                                                                      * they're
                                                                      * distinct
                                                                      * languages
                                                                      */
    public static final int LANGUAGE_SORBIAN = LANGUAGE_UPPER_SORBIAN_GERMANY; // =
    // LANGUAGE_USER_UPPER_SORBIAN;
    // /*
    // a
    // strange
    // MS
    // definition
    // */
    public static final int LANGUAGE_SPANISH_DATED = 0x040A; /*
                                                              * old collation,
                                                              * not supported,
                                                              * see #i94435#
                                                              */
    public static final int LANGUAGE_SPANISH_ARGENTINA = 0x2C0A;
    public static final int LANGUAGE_SPANISH_BOLIVIA = 0x400A;
    public static final int LANGUAGE_SPANISH_CHILE = 0x340A;
    public static final int LANGUAGE_SPANISH_COLOMBIA = 0x240A;
    public static final int LANGUAGE_SPANISH_COSTARICA = 0x140A;
    public static final int LANGUAGE_SPANISH_DOMINICAN_REPUBLIC = 0x1C0A;
    public static final int LANGUAGE_SPANISH_ECUADOR = 0x300A;
    public static final int LANGUAGE_SPANISH_EL_SALVADOR = 0x440A;
    public static final int LANGUAGE_SPANISH_GUATEMALA = 0x100A;
    public static final int LANGUAGE_SPANISH_HONDURAS = 0x480A;
    public static final int LANGUAGE_SPANISH_LATIN_AMERICA = 0xE40A; /*
                                                                      * no
                                                                      * locale
                                                                      * possible
                                                                      */
    public static final int LANGUAGE_SPANISH_MEXICAN = 0x080A;
    public static final int LANGUAGE_SPANISH_MODERN = 0x0C0A;
    public static final int LANGUAGE_SPANISH_NICARAGUA = 0x4C0A;
    public static final int LANGUAGE_SPANISH_PANAMA = 0x180A;
    public static final int LANGUAGE_SPANISH_PARAGUAY = 0x3C0A;
    public static final int LANGUAGE_SPANISH_PERU = 0x280A;
    public static final int LANGUAGE_SPANISH_PUERTO_RICO = 0x500A;
    public static final int LANGUAGE_SPANISH_UNITED_STATES = 0x540A;
    public static final int LANGUAGE_SPANISH_URUGUAY = 0x380A;
    public static final int LANGUAGE_SPANISH_VENEZUELA = 0x200A;
    public static final int LANGUAGE_SPANISH = LANGUAGE_SPANISH_MODERN; /*
                                                                         * modern
                                                                         * collation
                                                                         * , see
                                                                         * #
                                                                         * i94435
                                                                         * #
                                                                         */
    public static final int LANGUAGE_SWAHILI = 0x0441; /* Kenya */
    public static final int LANGUAGE_SWEDISH = 0x041D;
    public static final int LANGUAGE_SWEDISH_FINLAND = 0x081D;
    public static final int LANGUAGE_SYRIAC = 0x045A;
    public static final int LANGUAGE_TAJIK = 0x0428;
    public static final int LANGUAGE_TAMAZIGHT_ARABIC = 0x045F;
    public static final int LANGUAGE_TAMAZIGHT_LATIN = 0x085F;
    public static final int LANGUAGE_TAMAZIGHT_TIFINAGH = 0x0C5F;
    public static final int LANGUAGE_TAMIL = 0x0449;
    public static final int LANGUAGE_TATAR = 0x0444;
    public static final int LANGUAGE_TELUGU = 0x044A;
    public static final int LANGUAGE_THAI = 0x041E;
    public static final int LANGUAGE_TIBETAN = 0x0451;
    public static final int LANGUAGE_DZONGKHA = 0x0851;
    public static final int LANGUAGE_TIBETAN_BHUTAN = LANGUAGE_DZONGKHA; /*
                                                                          * a MS
                                                                          * error
                                                                          * ,
                                                                          * see
                                                                          * #
                                                                          * i53497
                                                                          * #
                                                                          */
    public static final int LANGUAGE_TIGRIGNA_ERITREA = 0x0873;
    public static final int LANGUAGE_TIGRIGNA_ETHIOPIA = 0x0473;
    public static final int LANGUAGE_TSONGA = 0x0431;
    public static final int LANGUAGE_TSWANA = 0x0432; /*
                                                       * AKA Setsuana, for South
                                                       * Africa
                                                       */
    public static final int LANGUAGE_TURKISH = 0x041F;
    public static final int LANGUAGE_TURKMEN = 0x0442;
    public static final int LANGUAGE_UIGHUR_CHINA = 0x0480;
    public static final int LANGUAGE_UKRAINIAN = 0x0422;
    public static final int LANGUAGE_URDU = 0x0020; /*
                                                     * primary only, not a
                                                     * locale!
                                                     */
    public static final int LANGUAGE_URDU_INDIA = 0x0820;
    public static final int LANGUAGE_URDU_PAKISTAN = 0x0420;
    public static final int LANGUAGE_UZBEK_CYRILLIC = 0x0843;
    public static final int LANGUAGE_UZBEK_LATIN = 0x0443;
    public static final int LANGUAGE_VENDA = 0x0433;
    public static final int LANGUAGE_VIETNAMESE = 0x042A;
    public static final int LANGUAGE_WELSH = 0x0452;
    public static final int LANGUAGE_WOLOF_SENEGAL = 0x0488;
    public static final int LANGUAGE_XHOSA = 0x0434; /* AKA isiZhosa */
    public static final int LANGUAGE_YAKUT_RUSSIA = 0x0485;
    public static final int LANGUAGE_YI = 0x0478; /* Sichuan Yi */
    public static final int LANGUAGE_YIDDISH = 0x043D;
    public static final int LANGUAGE_YORUBA = 0x046A;
    public static final int LANGUAGE_ZULU = 0x0435;
    
    /* Not real, but used for legacy. */
    public static final int LANGUAGE_USER1 = 0x0201;
    public static final int LANGUAGE_USER2 = 0x0202;
    public static final int LANGUAGE_USER3 = 0x0203;
    public static final int LANGUAGE_USER4 = 0x0204;
    public static final int LANGUAGE_USER5 = 0x0205;
    public static final int LANGUAGE_USER6 = 0x0206;
    public static final int LANGUAGE_USER7 = 0x0207;
    public static final int LANGUAGE_USER8 = 0x0208;
    public static final int LANGUAGE_USER9 = 0x0209;
    /* Don't use higher USER values here, we reserve them for extension. */
    
    /*
     * ! use only for import/export of MS documents, number formatter maps it to
     * ! LANGUAGE_SYSTEM and then to effective system language
     */
    public static final int LANGUAGE_SYSTEM_DEFAULT = 0x0800;
    
    /*
     * ! use only for import/export of MS documents, number formatter maps it to
     * ! LANGUAGE_SYSTEM and then to effective system language
     */
    public static final int LANGUAGE_PROCESS_OR_USER_DEFAULT = 0x0400;
    
    /*
     * And now the extensions we define, valid from 0x0610 to 0x07FF with
     * sublanguage ID 0x01 (default) 0x0A00 to 0x0BFF with sublanguage ID 0x02
     * ... 0x8200 to 0x83FF with sublanguage ID 0x20 0x8600 to 0x87FF with
     * sublanguage ID 0x21 ... 0xFA00 to 0xFBFF with sublanguage ID 0x3E 0xFE00
     * to 0xFFFF with sublanguage ID 0x3F
     * Obsolete OOo user defines now have other values assigned by MS, and
     * different name. Mapping an obsolete value to ISO code should work
     * provided that such a mapping exists in
     * i18npool/source/isolang/isolang.cxx, but mapping ISO back to LANGID will
     * return the new value.
     */
    public static final int LANGUAGE_OBSOLETE_USER_LATIN = 0x0610;
    public static final int LANGUAGE_USER_LATIN = LANGUAGE_LATIN;
    public static final int LANGUAGE_USER_ESPERANTO = 0x0611; /*
                                                               * no locale
                                                               * possible
                                                               */
    public static final int LANGUAGE_USER_INTERLINGUA = 0x0612; /*
                                                                 * no locale,
                                                                 * but
                                                                 * conventions
                                                                 */
    public static final int LANGUAGE_OBSOLETE_USER_MAORI = 0x0620;
    public static final int LANGUAGE_USER_MAORI = LANGUAGE_MAORI_NEW_ZEALAND;
    public static final int LANGUAGE_OBSOLETE_USER_KINYARWANDA = 0x0621;
    public static final int LANGUAGE_USER_KINYARWANDA = LANGUAGE_KINYARWANDA_RWANDA;
    /* was reserved for Northern Sotho but never used: 0x0622 *//*
                                                                 * obsoleted by
                                                                 * LANGUAGE_SEPEDI
                                                                 */
    public static final int LANGUAGE_OBSOLETE_USER_UPPER_SORBIAN = 0x0623;
    public static final int LANGUAGE_USER_UPPER_SORBIAN = LANGUAGE_UPPER_SORBIAN_GERMANY;
    public static final int LANGUAGE_OBSOLETE_USER_LOWER_SORBIAN = 0x0624;
    public static final int LANGUAGE_USER_LOWER_SORBIAN = LANGUAGE_LOWER_SORBIAN_GERMANY;
    public static final int LANGUAGE_OBSOLETE_USER_OCCITAN = 0x0625;
    public static final int LANGUAGE_USER_OCCITAN = LANGUAGE_OCCITAN_FRANCE; /*
                                                                              * reserved
                                                                              * to
                                                                              * languedocian
                                                                              */
    
    public static final int LANGUAGE_USER_KOREAN_NORTH = 0x8012; /*
                                                                  * North Korean
                                                                  * as opposed
                                                                  * to South
                                                                  * Korean,
                                                                  * makeLangID(
                                                                  * 0x20,
                                                                  * getPrimaryLanguage
                                                                  * (
                                                                  * LANGUAGE_KOREAN
                                                                  * ))
                                                                  */
    public static final int LANGUAGE_USER_KURDISH_TURKEY = 0x0626; /*
                                                                    * sublang
                                                                    * 0x01,
                                                                    * Latin
                                                                    * script
                                                                    */
    public static final int LANGUAGE_USER_KURDISH_SYRIA = 0x0A26; /*
                                                                   * sublang
                                                                   * 0x02, Latin
                                                                   * script
                                                                   */
    public static final int LANGUAGE_USER_KURDISH_IRAQ = 0x0E26; /*
                                                                  * sublang
                                                                  * 0x03, Arabic
                                                                  * script
                                                                  */
    public static final int LANGUAGE_USER_KURDISH_IRAN = 0x1226; /*
                                                                  * sublang
                                                                  * 0x04, Arabic
                                                                  * script
                                                                  */
    public static final int LANGUAGE_USER_SARDINIAN = 0x0627;
    /* was reserved for Dzongkha but turned down with #i53497#: 0x0628 *//*
                                                                          * obsoleted
                                                                          * by
                                                                          * LANGUAGE_DZONGKHA
                                                                          */
    public static final int LANGUAGE_USER_SWAHILI_TANZANIA = 0x8041; /*
                                                                      * makeLangID(
                                                                      * 0x20,
                                                                      * getPrimaryLanguage
                                                                      * (
                                                                      * LANGUAGE_SWAHILI
                                                                      * ))
                                                                      */
    public static final int LANGUAGE_OBSOLETE_USER_BRETON = 0x0629;
    public static final int LANGUAGE_USER_BRETON = LANGUAGE_BRETON_FRANCE;
    public static final int LANGUAGE_OBSOLETE_USER_KALAALLISUT = 0x062A;
    public static final int LANGUAGE_USER_KALAALLISUT = LANGUAGE_KALAALLISUT_GREENLAND;
    public static final int LANGUAGE_USER_SWAZI = 0x062B;
    public static final int LANGUAGE_USER_NDEBELE_SOUTH = 0x062C;
    public static final int LANGUAGE_USER_TSWANA_BOTSWANA = 0x8032; /*
                                                                     * makeLangID(
                                                                     * 0x20,
                                                                     * getPrimaryLanguage
                                                                     * (
                                                                     * LANGUAGE_TSWANA
                                                                     * ))
                                                                     */
    public static final int LANGUAGE_USER_MOORE = 0x062D;
    public static final int LANGUAGE_USER_BAMBARA = 0x062E;
    public static final int LANGUAGE_USER_AKAN = 0x062F;
    public static final int LANGUAGE_OBSOLETE_USER_LUXEMBOURGISH = 0x0630;
    public static final int LANGUAGE_USER_LUXEMBOURGISH = LANGUAGE_LUXEMBOURGISH_LUXEMBOURG;
    public static final int LANGUAGE_USER_FRIULIAN = 0x0631;
    public static final int LANGUAGE_USER_FIJIAN = 0x0632;
    public static final int LANGUAGE_USER_AFRIKAANS_NAMIBIA = 0x8036; /*
                                                                       * makeLangID(
                                                                       * 0x20,
                                                                       * getPrimaryLanguage
                                                                       * (
                                                                       * LANGUAGE_AFRIKAANS
                                                                       * ))
                                                                       */
    public static final int LANGUAGE_USER_ENGLISH_NAMIBIA = 0x8009; /*
                                                                     * makeLangID(
                                                                     * 0x20,
                                                                     * getPrimaryLanguage
                                                                     * (
                                                                     * LANGUAGE_ENGLISH_US
                                                                     * ))
                                                                     */
    public static final int LANGUAGE_USER_WALLOON = 0x0633;
    public static final int LANGUAGE_USER_COPTIC = 0x0634;
    public static final int LANGUAGE_USER_CHUVASH = 0x0635;
    public static final int LANGUAGE_USER_GASCON = 0x0636; /* Gascon France */
    public static final int LANGUAGE_USER_GERMAN_BELGIUM = 0x8007; /*
                                                                    * makeLangID(
                                                                    * 0x20,
                                                                    * getPrimaryLanguage
                                                                    * (
                                                                    * LANGUAGE_GERMAN
                                                                    * ))
                                                                    */
    public static final int LANGUAGE_USER_CATALAN_VALENCIAN = 0x8003; /*
                                                                       * makeLangID(
                                                                       * 0x20,
                                                                       * getPrimaryLanguage
                                                                       * (
                                                                       * LANGUAGE_CATALAN
                                                                       * ))
                                                                       */
    public static final int LANGUAGE_USER_HAUSA_GHANA = 0x8068; /*
                                                                 * makeLangID(
                                                                 * 0x20,
                                                                 * getPrimaryLanguage
                                                                 * (
                                                                 * LANGUAGE_HAUSA_NIGERIA
                                                                 * ))
                                                                 */
    public static final int LANGUAGE_USER_EWE_GHANA = 0x0637;
    public static final int LANGUAGE_USER_ENGLISH_GHANA = 0x8409; /*
                                                                   * makeLangID(
                                                                   * 0x21,
                                                                   * getPrimaryLanguage
                                                                   * (
                                                                   * LANGUAGE_ENGLISH_US
                                                                   * ))
                                                                   */
    public static final int LANGUAGE_USER_TAGALOG = 0x0638;
    public static final int LANGUAGE_USER_LINGALA_DRCONGO = 0x0639;
    public static final int LANGUAGE_USER_SANGO = 0x063A;
    public static final int LANGUAGE_USER_GANDA = 0x063B;
    public static final int LANGUAGE_USER_LOW_GERMAN = 0x063C;
    public static final int LANGUAGE_USER_HILIGAYNON = 0x063D;
    public static final int LANGUAGE_USER_NYANJA = 0x063E;
    public static final int LANGUAGE_USER_KASHUBIAN = 0x063F;
    public static final int LANGUAGE_USER_SPANISH_CUBA = 0x800A; /*
                                                                  * makeLangID(
                                                                  * 0x20,
                                                                  * getPrimaryLanguage
                                                                  * (
                                                                  * LANGUAGE_SPANISH
                                                                  * ))
                                                                  */
    public static final int LANGUAGE_USER_TETUN = 0x0640;
    public static final int LANGUAGE_USER_QUECHUA_NORTH_BOLIVIA = 0x0641;
    public static final int LANGUAGE_USER_QUECHUA_SOUTH_BOLIVIA = 0x0642;
    public static final int LANGUAGE_USER_SERBIAN_CYRILLIC_SERBIA = 0x8C1A; /*
                                                                             * makeLangID
                                                                             * (
                                                                             * 0x20
                                                                             * +
                                                                             * 0x03
                                                                             * ,
                                                                             * getPrimaryLanguage
                                                                             * (
                                                                             * LANGUAGE_SERBIAN_CYRILLIC
                                                                             * )
                                                                             * )
                                                                             */
    public static final int LANGUAGE_USER_SERBIAN_LATIN_SERBIA = 0x881A; /*
                                                                          * makeLangID
                                                                          * (
                                                                          * 0x20
                                                                          * +
                                                                          * 0x02
                                                                          * ,
                                                                          * getPrimaryLanguage
                                                                          * (
                                                                          * LANGUAGE_SERBIAN_LATIN
                                                                          * ))
                                                                          */
    public static final int LANGUAGE_USER_SERBIAN_CYRILLIC_MONTENEGRO = 0xCC1A; /*
                                                                                 * makeLangID
                                                                                 * (
                                                                                 * 0x20
                                                                                 * +
                                                                                 * 0x13
                                                                                 * ,
                                                                                 * getPrimaryLanguage
                                                                                 * (
                                                                                 * LANGUAGE_SERBIAN_CYRILLIC
                                                                                 * )
                                                                                 * )
                                                                                 */
    public static final int LANGUAGE_USER_SERBIAN_LATIN_MONTENEGRO = 0xC81A; /*
                                                                              * makeLangID
                                                                              * (
                                                                              * 0x20
                                                                              * +
                                                                              * 0x12
                                                                              * ,
                                                                              * getPrimaryLanguage
                                                                              * (
                                                                              * LANGUAGE_SERBIAN_LATIN
                                                                              * )
                                                                              * )
                                                                              */
    public static final int LANGUAGE_USER_SAMI_KILDIN_RUSSIA = 0x803B; /*
                                                                        * makeLangID
                                                                        * (
                                                                        * 0x20,
                                                                        * getPrimaryLanguage
                                                                        * (
                                                                        * LANGUAGE_SAMI_NORTHERN_NORWAY
                                                                        * ))
                                                                        */
    public static final int LANGUAGE_USER_BODO_INDIA = 0x0643;
    public static final int LANGUAGE_USER_DOGRI_INDIA = 0x0644;
    public static final int LANGUAGE_USER_MAITHILI_INDIA = 0x0645;
    public static final int LANGUAGE_USER_SANTALI_INDIA = 0x0646;
    public static final int LANGUAGE_USER_TETUN_TIMOR_LESTE = 0x0A40; /*
                                                                       * makeLangID(
                                                                       * 0x20,
                                                                       * getPrimaryLanguage
                                                                       * (
                                                                       * LANGUAGE_USER_TETUN
                                                                       * ))
                                                                       */
    public static final int LANGUAGE_USER_TOK_PISIN = 0x0647;
    public static final int LANGUAGE_USER_SHUSWAP = 0x0648;
    public static final int LANGUAGE_USER_ARABIC_CHAD = 0x8001; /*
                                                                 * makeLangID(
                                                                 * 0x20,
                                                                 * getPrimaryLanguage
                                                                 * (
                                                                 * LANGUAGE_ARABIC_SAUDI_ARABIA
                                                                 * ))
                                                                 */
    public static final int LANGUAGE_USER_ARABIC_COMOROS = 0x8401; /*
                                                                    * makeLangID(
                                                                    * 0x21,
                                                                    * getPrimaryLanguage
                                                                    * (
                                                                    * LANGUAGE_ARABIC_SAUDI_ARABIA
                                                                    * ))
                                                                    */
    public static final int LANGUAGE_USER_ARABIC_DJIBOUTI = 0x8801; /*
                                                                     * makeLangID(
                                                                     * 0x22,
                                                                     * getPrimaryLanguage
                                                                     * (
                                                                     * LANGUAGE_ARABIC_SAUDI_ARABIA
                                                                     * ))
                                                                     */
    public static final int LANGUAGE_USER_ARABIC_ERITREA = 0x8C01; /*
                                                                    * makeLangID(
                                                                    * 0x23,
                                                                    * getPrimaryLanguage
                                                                    * (
                                                                    * LANGUAGE_ARABIC_SAUDI_ARABIA
                                                                    * ))
                                                                    */
    public static final int LANGUAGE_USER_ARABIC_ISRAEL = 0x9001; /*
                                                                   * makeLangID(
                                                                   * 0x24,
                                                                   * getPrimaryLanguage
                                                                   * (
                                                                   * LANGUAGE_ARABIC_SAUDI_ARABIA
                                                                   * ))
                                                                   */
    public static final int LANGUAGE_USER_ARABIC_MAURITANIA = 0x9401; /*
                                                                       * makeLangID(
                                                                       * 0x25,
                                                                       * getPrimaryLanguage
                                                                       * (
                                                                       * LANGUAGE_ARABIC_SAUDI_ARABIA
                                                                       * ))
                                                                       */
    public static final int LANGUAGE_USER_ARABIC_PALESTINE = 0x9801; /*
                                                                      * makeLangID(
                                                                      * 0x26,
                                                                      * getPrimaryLanguage
                                                                      * (
                                                                      * LANGUAGE_ARABIC_SAUDI_ARABIA
                                                                      * ))
                                                                      */
    public static final int LANGUAGE_USER_ARABIC_SOMALIA = 0x9C01; /*
                                                                    * makeLangID(
                                                                    * 0x27,
                                                                    * getPrimaryLanguage
                                                                    * (
                                                                    * LANGUAGE_ARABIC_SAUDI_ARABIA
                                                                    * ))
                                                                    */
    public static final int LANGUAGE_USER_ARABIC_SUDAN = 0xA001; /*
                                                                  * makeLangID(
                                                                  * 0x28,
                                                                  * getPrimaryLanguage
                                                                  * (
                                                                  * LANGUAGE_ARABIC_SAUDI_ARABIA
                                                                  * ))
                                                                  */
    public static final int LANGUAGE_USER_ANCIENT_GREEK = 0x0649;
    public static final int LANGUAGE_USER_ASTURIAN = 0x064A;
    public static final int LANGUAGE_USER_LATGALIAN = 0x064B;
    public static final int LANGUAGE_USER_MAORE = 0x064C;
    public static final int LANGUAGE_USER_BUSHI = 0x064D;
    public static final int LANGUAGE_USER_TAHITIAN = 0x064E;
    public static final int LANGUAGE_USER_MALAGASY_PLATEAU = 0x064F;
    public static final int LANGUAGE_USER_PAPIAMENTU_ARUBA = 0x8079; /*
                                                                      * makeLangID(
                                                                      * 0x20,
                                                                      * getPrimaryLanguage
                                                                      * (
                                                                      * LANGUAGE_PAPIAMENTU
                                                                      * ))
                                                                      */
    public static final int LANGUAGE_USER_SARDINIAN_CAMPIDANESE = 0x0650;
    public static final int LANGUAGE_USER_SARDINIAN_GALLURESE = 0x0651;
    public static final int LANGUAGE_USER_SARDINIAN_LOGUDORESE = 0x0652;
    public static final int LANGUAGE_USER_SARDINIAN_SASSARESE = 0x0653;
    public static final int LANGUAGE_USER_BAFIA = 0x0654;
    public static final int LANGUAGE_USER_GIKUYU = 0x0655;
    public static final int LANGUAGE_USER_RUSYN_UKRAINE = 0x0656;
    public static final int LANGUAGE_USER_RUSYN_SLOVAKIA = 0x8256; /*
                                                                    * makeLangID(
                                                                    * 0x20,
                                                                    * getPrimaryLanguage
                                                                    * (
                                                                    * LANGUAGE_USER_RUSYN_UKRAINE
                                                                    * ))
                                                                    */
    public static final int LANGUAGE_USER_YIDDISH_US = 0x083D; /*
                                                                * makeLangID(
                                                                * 0x20,
                                                                * getPrimaryLanguage
                                                                * (
                                                                * LANGUAGE_YIDDISH
                                                                * ))
                                                                */
    public static final int LANGUAGE_USER_LIMBU = 0x0657;
    public static final int LANGUAGE_USER_LOJBAN = 0x0658; /* no locale */
    public static final int LANGUAGE_OBSOLETE_USER_KABYLE = 0x0659;
    public static final int LANGUAGE_USER_KABYLE = LANGUAGE_TAMAZIGHT_LATIN;
    public static final int LANGUAGE_USER_HAITIAN = 0x065A;
    public static final int LANGUAGE_USER_BEEMBE = 0x065B;
    public static final int LANGUAGE_USER_BEKWEL = 0x065C;
    public static final int LANGUAGE_USER_KITUBA = 0x065D;
    public static final int LANGUAGE_USER_LARI = 0x065E;
    public static final int LANGUAGE_USER_MBOCHI = 0x065F;
    public static final int LANGUAGE_USER_TEKE_IBALI = 0x0660;
    public static final int LANGUAGE_USER_TEKE_TYEE = 0x0661;
    public static final int LANGUAGE_USER_VILI = 0x0662;
    public static final int LANGUAGE_USER_PORTUGUESE_ANGOLA = 0x8016; /*
                                                                       * makeLangID(
                                                                       * 0x20,
                                                                       * getPrimaryLanguage
                                                                       * (
                                                                       * LANGUAGE_PORTUGUESE
                                                                       * ))
                                                                       */
    public static final int LANGUAGE_USER_MANX = 0x0663;
    public static final int LANGUAGE_USER_TEKE_EBOO = 0x0664;
    public static final int LANGUAGE_USER_ARAGONESE = 0x0665;
    public static final int LANGUAGE_USER_KEYID = 0x0666; /*
                                                           * key id
                                                           * pseudolanguage
                                                           */
    public static final int LANGUAGE_USER_PALI_LATIN = 0x0667;
    public static final int LANGUAGE_USER_KYRGYZ_CHINA = 0x0668; /*
                                                                  * not derived
                                                                  * from
                                                                  * LANGUAGE_KIRGHIZ
                                                                  * as these may
                                                                  * be different
                                                                  * scripts, see
                                                                  * http
                                                                  * ://www.omniglot
                                                                  * .
                                                                  * com/writing/
                                                                  * kirghiz.htm
                                                                  */
    public static final int LANGUAGE_USER_KOMI_ZYRIAN = 0x0669;
    public static final int LANGUAGE_USER_KOMI_PERMYAK = 0x066A;
    public static final int LANGUAGE_USER_PITJANTJATJARA = 0x066B;
    public static final int LANGUAGE_USER_ENGLISH_MALAWI = 0x8809; /*
                                                                    * makeLangID(
                                                                    * 0x22,
                                                                    * getPrimaryLanguage
                                                                    * (
                                                                    * LANGUAGE_ENGLISH_UK
                                                                    * ))
                                                                    */
    public static final int LANGUAGE_USER_ERZYA = 0x066C;
    public static final int LANGUAGE_USER_MARI_MEADOW = 0x066D;
    public static final int LANGUAGE_USER_SYSTEM_CONFIG = 0xFFFE; /*
                                                                   * not a
                                                                   * locale, to
                                                                   * be used
                                                                   * only in
                                                                   * configuration
                                                                   * context to
                                                                   * obtain
                                                                   * system
                                                                   * default,
                                                                   * primary
                                                                   * 0x3fe, sub
                                                                   * 0x3f
                                                                   */
    
    private LanguageConstants() {}
    
}