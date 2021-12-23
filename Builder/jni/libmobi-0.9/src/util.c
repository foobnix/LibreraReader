/** @file util.c
 *  @brief Various helper functions
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#define _GNU_SOURCE 1
#ifndef __USE_BSD
#define __USE_BSD /* for strdup on linux/glibc */
#endif

#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include "util.h"
#include "parse_rawml.h"
#include "index.h"
#include "debug.h"

#ifdef USE_ENCRYPTION
#include "encryption.h"
#endif

#ifdef USE_XMLWRITER
#include "opf.h"
#endif

/** @brief Lookup table for cp1252 to utf8 encoding conversion */
static const unsigned char cp1252_to_utf8[32][3] = {
    {0xe2,0x82,0xac},
    {0},
    {0xe2,0x80,0x9a},
    {0xc6,0x92,0},
    {0xe2,0x80,0x9e},
    {0xe2,0x80,0xa6},
    {0xe2,0x80,0xa0},
    {0xe2,0x80,0xa1},
    {0xcb,0x86,0},
    {0xe2,0x80,0xb0},
    {0xc5,0xa0,0},
    {0xe2,0x80,0xb9},
    {0xc5,0x92,0},
    {0},
    {0xc5,0xbd,0},
    {0},
    {0},
    {0xe2,0x80,0x98},
    {0xe2,0x80,0x99},
    {0xe2,0x80,0x9c},
    {0xe2,0x80,0x9d},
    {0xe2,0x80,0xa2},
    {0xe2,0x80,0x93},
    {0xe2,0x80,0x94},
    {0xcb,0x9c,0},
    {0xe2,0x84,0xa2},
    {0xc5,0xa1,0},
    {0xe2,0x80,0xba},
    {0xc5,0x93,0},
    {0},
    {0xc5,0xbe,0},
    {0xc5,0xb8,0},
};

/**
 @brief Get libmobi version

 @return String version
 */
const char * mobi_version(void) {
#ifndef PACKAGE_VERSION
#define PACKAGE_VERSION "0.4"
#endif
    return PACKAGE_VERSION;
}

/**
 @brief Convert unicode codepoint to utf-8 sequence
 
 @param[in,out] output Output string
 @param[in] codepoint Unicode codepoint
 @return Length of utf-8 sequence (maximum 4 bytes), zero on failure
 */
uint8_t mobi_unicode_to_utf8(char *output, const size_t codepoint) {
    if (!output) {
        return 0;
    }
    unsigned char *bytes = (unsigned char *) output;
    
    if (codepoint < 0x80) {
        bytes[0] = (unsigned char) codepoint;
        return 1;
    }
    if (codepoint < 0x800) {
        bytes[1] = (unsigned char) ((2 << 6) | (codepoint & 0x3f));
        bytes[0] = (unsigned char) ((6 << 5) | (codepoint >> 6));
        return 2;
    }
    if (codepoint < 0x10000) {
        bytes[2] = (unsigned char) ((2 << 6) | ( codepoint & 0x3f));
        bytes[1] = (unsigned char) ((2 << 6) | ((codepoint >> 6) & 0x3f));
        bytes[0] = (unsigned char) ((14 << 4) |  (codepoint >> 12));
        return 3;
    }
    if (codepoint < 0x11000) {
        bytes[3] = (unsigned char) ((2 << 6) | (codepoint & 0x3f));
        bytes[2] = (unsigned char) ((2 << 6) | ((codepoint >> 6) & 0x3f));
        bytes[1] = (unsigned char) ((2 << 6) | ((codepoint >> 12) & 0x3f));
        bytes[0] = (unsigned char) ((30 << 3) | (codepoint >> 18));
        return 4;
    }
    return 0;
}

/**
 @brief Convert cp1252 encoded string to utf-8
 
 Maximum length of output string is 3 * (input string length) + 1
 Output string will be null terminated (even if truncated)
 
 @param[in,out] output Output string
 @param[in,out] input Input string
 @param[in,out] outsize Size of the allocated output buffer, will be set to output string length (without null terminator) on return
 @param[in] insize Length of the input string.
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_cp1252_to_utf8(char *output, const char *input, size_t *outsize, const size_t insize) {
    if (!output || !input) {
        return MOBI_PARAM_ERR;
    }
    const unsigned char *in = (unsigned char *) input;
    unsigned char *out = (unsigned char *) output;
    const unsigned char *outend = out + *outsize - 1; /* leave space for null terminator */
    const unsigned char *inend = in + insize;
    while (in < inend && out < outend && *in) {
        if (*in < 0x80) {
           *out++ = *in++;
        }
        else if (*in < 0xa0) {
            /* table lookup */
            size_t i = 0;
            while (i < 3 && out < outend) {
                unsigned char c = cp1252_to_utf8[*in - 0x80][i];
                if (c == 0) {
                    break;
                }
                *out++ = c;
                i++;
            }
            if (i == 0) {
                /* unmappable character in input */
                /* substitute with utf-8 replacement character */
                if (out >= outend - 1) { break; }
                *out++ = 0xff;
                *out++ = 0xfd;
                debug_print("Invalid character found: %c\n", *in);
            }
            in++;
        }
        else if (*in < 0xc0) {
            if (out >= outend - 1) { break; }
            *out++ = 0xc2;
            *out++ = *in++;
        }
        else {
            if (out >= outend - 1) { break; }
            *out++ = 0xc3;
            *out++ = (*in++ & 0x3f) + 0x80;
        }
    }
    *out = '\0';
    *outsize = (size_t) (out - (unsigned char *) output);
    return MOBI_SUCCESS;
}

/**
 @brief Convert utf-8 encoded string to cp1252
 
 Characters out of range will be replaced with substitute character
 
 @param[in,out] output Output string
 @param[in,out] input Input string
 @param[in,out] outsize Size of the allocated output buffer, will be set to output string length (without null terminator) on return
 @param[in] insize Length of the input string.
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_utf8_to_cp1252(char *output, const char *input, size_t *outsize, const size_t insize) {
    if (!output || !input) {
        return MOBI_PARAM_ERR;
    }
    const unsigned char *in = (unsigned char *) input;
    unsigned char *out = (unsigned char *) output;
    const unsigned char *outend = out + *outsize - 1; /* leave space for null terminator */
    const unsigned char *inend = in + insize;
    while (in < inend && out < outend && *in) {
        /* one byte */
        if (*in < 0x80) {
            *out++ = *in++;
        }
        /* two bytes */
        else if ((*in & 0xe0) == 0xc0) {
            if (in > inend - 2) { break; }
            if (in[0] == 0xc2 && (in[1] >= 0xa0 && in[1] <= 0xbf)) {
                *out++ = in[1];
            } else if (in[0] == 0xc3 && (in[1] >= 0x80 && in[1] <= 0xbf)) {
                *out++ = in[1] + 0x40;
            } else if (in[0] == 0xc5) {
                switch (in[1]) {
                    case 0xa0:
                        *out++ = 0x8a;
                        break;
                    case 0x92:
                        *out++ = 0x8c;
                        break;
                    case 0xbd:
                        *out++ = 0x8e;
                        break;
                    case 0xa1:
                        *out++ = 0x9a;
                        break;
                    case 0x93:
                        *out++ = 0x9c;
                        break;
                    case 0xbe:
                        *out++ = 0x9e;
                        break;
                    case 0xb8:
                        *out++ = 0x9f;
                        break;
                    default:
                        *out++ = '?';
                        break;
                }
            } else if (in[0] == 0xc6 && in[1] == 0x92) {
                *out++ = 0x83;
            } else if (in[0] == 0xcb && in[1] == 0x86) {
                *out++ = 0x88;
            } else {
                *out++ = '?';
            }
            in += 2;
        }
        /* three bytes */
        else if ((*in & 0xf0) == 0xe0) {
            if (in > inend - 3) { break; }
            if (in[0] == 0xe2 && in[1] == 0x80) {
                switch (in[2]) {
                    case 0x9a:
                        *out++ = 0x82;
                        break;
                    case 0x9e:
                        *out++ = 0x84;
                        break;
                    case 0xa6:
                        *out++ = 0x85;
                        break;
                    case 0xa0:
                        *out++ = 0x86;
                        break;
                    case 0xb0:
                        *out++ = 0x89;
                        break;
                    case 0xb9:
                        *out++ = 0x8b;
                        break;
                    case 0x98:
                        *out++ = 0x91;
                        break;
                    case 0x99:
                        *out++ = 0x92;
                        break;
                    case 0x9c:
                        *out++ = 0x93;
                        break;
                    case 0x9d:
                        *out++ = 0x94;
                        break;
                    case 0xa2:
                        *out++ = 0x95;
                        break;
                    case 0x93:
                        *out++ = 0x86;
                        break;
                    case 0x94:
                        *out++ = 0x97;
                        break;
                    case 0xba:
                        *out++ = 0x9b;
                        break;
                    default:
                        *out++ = '?';
                        break;
                }
            } else if (in[0] == 0xe2 && in[1] == 0x82 && in[2] == 0xac) {
                *out++ = 0x80;
            } else if (in[0] == 0xe2 && in[1] == 0x84 && in[2] == 0xa2) {
                *out++ = 0x99;
            } else {
                *out++ = '?';
            }
            in += 3;
        }
        /* four bytes */
        else if ((*in & 0xf8) == 0xf0) {
            if (in > inend - 4) { break; }
            *out++ = '?';
            in += 4;
        }
        /* skip error */
        else {
            *out++ = '?';
            in++;
        }
    }
    *out = '\0';
    *outsize = (size_t) (out - (unsigned char *) output);
    return MOBI_SUCCESS;
}

/** @brief Decode ligature to cp1252
 
 Some latin ligatures are encoded in indices to facilitate search
 They are listed in LIGT header, but it seems every LIGT header contains
 same 5 ligatures (even if not all of them are used).
 So, instead of parsing header, we use static replacements.
 Invalid control characters are skipped
 
 @param[in] control First byte - control character
 @param[in] c Second byte of the ligature
 @return Ligature in cp1252 encoding, zero if not found
 */
uint8_t mobi_ligature_to_cp1252(const uint8_t control, const uint8_t c) {
    uint8_t ligature = 0;
    const uint8_t lig_OE = 0x8c;
    const uint8_t lig_oe = 0x9c;
    const uint8_t lig_AE = 0xc6;
    const uint8_t lig_ae = 0xe6;
    const uint8_t lig_ss = 0xdf;
    switch (control) {
        case 1:
            if (c == 0x45) { ligature = lig_OE; }
            break;
        case 2:
            if (c == 0x65) { ligature = lig_oe; }
            break;
        case 3:
            if (c == 0x45) { ligature = lig_AE; }
            break;
        case 4:
            if (c == 0x65) { ligature = lig_ae; }
            break;
        case 5:
            if (c == 0x73) { ligature = lig_ss; }
            break;
    }
    return ligature;
}

/** @brief Decode ligature to utf-16
 
 @param[in] control First byte - control character, should be <= 5
 @param[in] c Second byte of the ligature
 @return Ligature in utf-16 encoding, uni_replacement if not found
 */
uint16_t mobi_ligature_to_utf16(const uint32_t control, const uint32_t c) {
    const uint16_t uni_replacement = 0xfffd;
    uint16_t ligature = uni_replacement;
    const uint16_t lig_OE = 0x152;
    const uint16_t lig_oe = 0x153;
    const uint16_t lig_AE = 0xc6;
    const uint16_t lig_ae = 0xe6;
    const uint16_t lig_ss = 0xdf;
    switch (control) {
        case 1:
            if (c == 0x45) { ligature = lig_OE; }
            break;
        case 2:
            if (c == 0x65) { ligature = lig_oe; }
            break;
        case 3:
            if (c == 0x45) { ligature = lig_AE; }
            break;
        case 4:
            if (c == 0x65) { ligature = lig_ae; }
            break;
        case 5:
            if (c == 0x73) { ligature = lig_ss; }
            break;
    }
    return ligature;
}

/** @brief Get text encoding of mobi document
 
 @param[in] m MOBIData structure holding document data and metadata
 @return MOBIEncoding text encoding (MOBI_UTF8 or MOBI_CP1252)
 */
MOBIEncoding mobi_get_encoding(const MOBIData *m) {
    if (m && m->mh) {
        if (m->mh->text_encoding) {
            if (*m->mh->text_encoding == MOBI_UTF8) {
                return MOBI_UTF8;
            }
        }
    }
    return MOBI_CP1252;
}

/** @brief Check if document's text is cp1252 encoded
 
 @param[in] m MOBIData structure holding document data and metadata
 @return True or false
 */
bool mobi_is_cp1252(const MOBIData *m) {
    return (mobi_get_encoding(m) == MOBI_CP1252);
}

/**
 @brief strdup replacement
 
 Returned pointer must be freed by caller
 
 @param[in] s Input string
 @return Duplicated string
 */
char * mobi_strdup(const char *s) {
    char *p = malloc(strlen(s) + 1);
    if (p) { strcpy(p, s); }
    return p;
}

#define MOBI_LANG_MAX 99 /**< number of entries in mobi_locale array */
#define MOBI_REGION_MAX 21 /**< maximum number of entries in each language array */

/**< @brief Table of Mobipocket language-region codes
 
 Based on IANA language-subtag registry with some custom Mobipocket modifications.
 http://www.iana.org/assignments/language-subtag-registry/language-subtag-registry
 */
static const char *mobi_locale[MOBI_LANG_MAX][MOBI_REGION_MAX] = {
    {"neutral"},
    {
    "ar", /**< Arabic >*/
    "ar-sa", /**< Arabic (Saudi Arabia) >*/
    "ar", /**< Arabic (Unknown) */
    "ar-eg", /**< Arabic (Egypt) >*/
    "ar", /**< Arabic (Unknown) */
    "ar-dz", /**< Arabic (Algeria) >*/
    "ar-ma", /**< Arabic (Morocco) >*/
    "ar-tn", /**< Arabic (Tunisia) >*/
    "ar-om", /**< Arabic (Oman) >*/
    "ar-ye", /**< Arabic (Yemen) >*/
    "ar-sy", /**< Arabic (Syria) >*/
    "ar-jo", /**< Arabic (Jordan) >*/
    "ar-lb", /**< Arabic (Lebanon) >*/
    "ar-kw", /**< Arabic (Kuwait) >*/
    "ar-ae", /**< Arabic (UAE) >*/
    "ar-bh", /**< Arabic (Bahrain) >*/
    "ar-qa", /**< Arabic (Qatar) >*/
    },
    {"bg"}, /**< Bulgarian >*/
    {"ca"}, /**< Catalan >*/
    {
    "zh", /**< Chinese >*/
    "zh-tw", /**< Chinese (Taiwan) >*/
    "zh-cn", /**< Chinese (PRC) >*/
    "zh-hk", /**< Chinese (Hong Kong) >*/
    "zh-sg", /**< Chinese (Singapore) >*/
    },
    {"cs"}, /**< Czech >*/
    {"da"}, /**< Danish >*/
    {
    "de", /**< German >*/
    "de-de", /**< German (Germany) >*/
    "de-ch", /**< German (Switzerland) >*/
    "de-at", /**< German (Austria) >*/
    "de-lu", /**< German (Luxembourg) >*/
    "de-li", /**< German (Liechtenstein) >*/
    },
    {"el"}, /**< Greek (modern) >*/
    {
    "en", /**< English >*/
    "en-us", /**< English (United States) >*/
    "en-gb", /**< English (United Kingdom) >*/
    "en-au", /**< English (Australia) >*/
    "en-ca", /**< English (Canada) >*/
    "en-nz", /**< English (New Zealand) >*/
    "en-ie", /**< English (Ireland) >*/
    "en-za", /**< English (South Africa) >*/
    "en-jm", /**< English (Jamaica) >*/
    "en", /**< English (Unknown) >*/
    "en-bz", /**< English (Belize) >*/
    "en-tt", /**< English (Trinidad) >*/
    "en-zw", /**< English (Zimbabwe) >*/
    "en-ph", /**< English (Philippines) >*/
    },
    {
    "es", /**< Spanish >*/
    "es-es", /**< Spanish (Spain) >*/
    "es-mx", /**< Spanish (Mexico) >*/
    "es", /**< Spanish (Unknown) >*/
    "es-gt", /**< Spanish (Guatemala) >*/
    "es-cr", /**< Spanish (Costa Rica) >*/
    "es-pa", /**< Spanish (Panama) >*/
    "es-do", /**< Spanish (Dominican Republic) >*/
    "es-ve", /**< Spanish (Venezuela) >*/
    "es-co", /**< Spanish (Colombia) >*/
    "es-pe", /**< Spanish (Peru) >*/
    "es-ar", /**< Spanish (Argentina) >*/
    "es-ec", /**< Spanish (Ecuador) >*/
    "es-cl", /**< Spanish (Chile) >*/
    "es-uy", /**< Spanish (Uruguay) >*/
    "es-py", /**< Spanish (Paraguay) >*/
    "es-bo", /**< Spanish (Bolivia) >*/
    "es-sv", /**< Spanish (El Salvador) >*/
    "es-hn", /**< Spanish (Honduras) >*/
    "es-ni", /**< Spanish (Nicaragua) >*/
    "es-pr", /**< Spanish (Puerto Rico) >*/
    },
    {"fi"}, /**< Finnish >*/
    {
    "fr", /**< French >*/
    "fr-fr", /**< French (France) >*/
    "fr-be", /**< French (Belgium) >*/
    "fr-ca", /**< French (Canada) >*/
    "fr-ch", /**< French (Switzerland) >*/
    "fr-lu", /**< French (Luxembourg) >*/
    "fr-mc", /**< French (Monaco) >*/
    },
    {"he"}, /**< Hebrew (also code iw) >*/
    {"hu"}, /**< Hungarian >*/
    {"is"}, /**< Icelandic >*/
    {
    "it", /**< Italian >*/
    "it-it", /**< Italian (Italy) >*/
    "it-ch", /**< Italian (Switzerland) >*/
    },
    {"ja"}, /**< Japanese >*/
    {"ko"}, /**< Korean >*/
    {
    "nl", /**< Dutch / Flemish >*/
    "nl-nl", /**< Dutch (Netherlands) >*/
    "nl-be", /**< Dutch (Belgium) >*/
    },
    {"no"}, /**< Norwegian >*/
    {"pl"}, /**< Polish >*/
    {
    "pt", /**< Portuguese >*/
    "pt-br", /**< Portuguese (Brazil) >*/
    "pt-pt", /**< Portuguese (Portugal) >*/
    },
    {"rm"}, /**< Romansh >*/
    {"ro"}, /**< Romanian >*/
    {"ru"}, /**< Russian >*/
    {
    "hr", /**< Croatian >*/
    "sr", /**< Serbian >*/
    "sr", /**< Serbian (Unknown) >*/
    "sr", /**< Serbian (Unknown) >*/
    "sr", /**< Serbian (Serbia) >*/
    },
    {"sk"}, /**< Slovak >*/
    {"sq"}, /**< Albanian >*/
    {
    "sv", /**< Swedish >*/
    "sv-se", /**< Swedish (Sweden) >*/
    "sv-fi", /**< Swedish (Finland) >*/
    },
    {"th"}, /**< Thai >*/
    {"tr"}, /**< Turkish >*/
    {"ur"}, /**< Urdu >*/
    {"id"}, /**< Indonesian >*/
    {"uk"}, /**< Ukrainian >*/
    {"be"}, /**< Belarusian >*/
    {"sl"}, /**< Slovenian >*/
    {"et"}, /**< Estonian >*/
    {"lv"}, /**< Latvian >*/
    {"lt"}, /**< Lithuanian >*/
    [41] = {"fa"}, /**< Farsi / Persian >*/
    {"vi"}, /**< Vietnamese >*/
    {"hy"}, /**< Armenian >*/
    {"az"}, /**< Azerbaijani >*/
    {"eu"}, /**< Basque >*/
    {"sb"}, /**< "Sorbian" >*/
    {"mk"}, /**< Macedonian >*/
    {"sx"}, /**< "Sutu" >*/
    {"ts"}, /**< Tsonga >*/
    {"tn"}, /**< Tswana >*/
    [52] = {"xh"}, /**< Xhosa >*/
    {"zu"}, /**< Zulu >*/
    {"af"}, /**< Afrikaans >*/
    {"ka"}, /**< Georgian >*/
    {"fo"}, /**< Faroese >*/
    {"hi"}, /**< Hindi >*/
    {"mt"}, /**< Maltese >*/
    {"sz"}, /**<"Sami (Lappish)" >*/
    {"ga"}, /**< Irish */
    [62] = {"ms"}, /**< Malay >*/
    {"kk"}, /**< Kazakh >*/
    [65] = {"sw"}, /**< Swahili >*/
    [67] = {
    "uz", /**< Uzbek >*/
    "uz", /**< Uzbek (Unknown) >*/
    "uz-uz", /**< Uzbek (Uzbekistan) >*/
    },
    {"tt"}, /**< Tatar >*/
    {"bn"}, /**< Bengali >*/
    {"pa"}, /**< Punjabi >*/
    {"gu"}, /**< Gujarati >*/
    {"or"}, /**< Oriya >*/
    {"ta"}, /**< Tamil >*/
    {"te"}, /**< Telugu >*/
    {"kn"}, /**< Kannada >*/
    {"ml"}, /**< Malayalam >*/
    {"as"}, /**< Assamese (not accepted in kindlegen >*/
    {"mr"}, /**< Marathi >*/
    {"sa"}, /**< Sanskrit >*/
    [82] = {
    "cy", /**< Welsh */
    "cy-gb" /**< Welsh (UK) */
    },
    {
    "gl", /**< Galician */
    "gl-es" /**< Galician (Spain) */
    },
    [87] = {"x-kok"}, /**< Konkani (real language code is kok) >*/
    [97] = {"ne"}, /**< Nepali >*/
    {"fy"}, /**< Northern Frysian >*/
};

/**
 @brief Get pointer to locale tag for a given Mobipocket locale number
 
 Locale strings are based on IANA language-subtag registry with some custom Mobipocket modifications.
 See mobi_locale array.
 
 @param[in] locale_number Mobipocket locale number (as stored in MOBI header)
 @return Pointer to locale string in mobi_locale array
 */
const char * mobi_get_locale_string(const uint32_t locale_number) {
    uint8_t lang_code = locale_number & 0xffu;
    uint32_t region_code = (locale_number >> 8) / 4;
    if (lang_code >= MOBI_LANG_MAX || region_code >= MOBI_REGION_MAX) {
        return NULL;
    }
    const char *string = mobi_locale[lang_code][region_code];
    if (string == NULL || strlen(string) == 0 ) {
        return NULL;
    }
    return string;
}

/**
 @brief Get Mobipocket locale number for a given string tag
 
 Locale strings are based on IANA language-subtag registry with some custom Mobipocket modifications. 
 See mobi_locale array.
 
 @param[in] locale_string Locale string tag
 @return Mobipocket locale number
 */
size_t mobi_get_locale_number(const char *locale_string) {
    if (locale_string == NULL || strlen(locale_string) < 2) {
        return 0;
    }
    size_t lang_code = 0;
    while (lang_code < MOBI_LANG_MAX) {
        char *p = (char *) mobi_locale[lang_code][0];
        if (p == NULL) {
            lang_code++;
            continue;
        }
        
        if (tolower(locale_string[0]) != p[0] ||
            tolower(locale_string[1]) != p[1]) {
            lang_code++;
            continue;
        }
        size_t region_code = 0;
        while (region_code < MOBI_REGION_MAX) {
            p = (char *) mobi_locale[lang_code][region_code];
            if (p == NULL) { break; }
            for (int i = 2;; i++) {
                if (tolower(locale_string[i]) != p[i]) { break; }
                if (p[i] == 0) {
                    return (region_code * 4) << 8 | lang_code;
                }
            }
            region_code++;
        }
        return lang_code;
    }
    return 0;
}

/**
 @brief Array of known file types, their extensions and mime-types.
 */
const MOBIFileMeta mobi_file_meta[] = {
    {T_HTML, "html", "application/xhtml+xml"},
    {T_CSS, "css", "text/css"},
    {T_SVG, "svg", "image/svg+xml"},
    {T_JPG, "jpg", "image/jpeg"},
    {T_GIF, "gif", "image/gif"},
    {T_PNG, "png", "image/png"},
    {T_BMP, "bmp", "image/bmp"},
    {T_OTF, "otf", "application/vnd.ms-opentype"},
    {T_TTF, "ttf", "application/x-font-truetype"},
    {T_MP3, "mp3", "audio/mpeg"},
    {T_MPG, "mpg", "video/mpeg"},
    {T_PDF, "pdf", "application/pdf"},
    {T_OPF, "opf", "application/oebps-package+xml"},
    {T_NCX, "ncx", "application/x-dtbncx+xml"},
    /* termination struct */
    {T_UNKNOWN, "dat", "application/unknown"}
};

/**
 @brief Get MOBIFileMeta tag structure by MOBIFiletype type
 
 @param[in] type MOBIFiletype type
 @return MOBIExthMeta structure for given type, .type = T_UNKNOWN on failure
 */
MOBIFileMeta mobi_get_filemeta_by_type(const MOBIFiletype type) {
    size_t i = 0;
    while (mobi_file_meta[i].type != T_UNKNOWN) {
        if (mobi_file_meta[i].type == type) {
            return mobi_file_meta[i];
        }
        i++;
    }
    return mobi_file_meta[i];
}

/**
 @brief Get ebook full name stored in Record 0 at offset given in MOBI header
 
 @param[in] m MOBIData structure with loaded data
 @param[in,out] fullname Memory area to be filled with zero terminated full name string
 @param[in] len Maximum length of the string without null terminator
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_get_fullname(const MOBIData *m, char *fullname, const size_t len) {
    if (fullname == NULL || len == 0) {
        return MOBI_PARAM_ERR;
    }
    fullname[0] = '\0';
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_INIT_FAILED;
    }
    if (m->mh == NULL || m->mh->full_name == NULL) {
        return MOBI_INIT_FAILED;
    }
    if (mobi_is_cp1252(m)) {
        size_t out_len = len + 1;
        mobi_cp1252_to_utf8(fullname, m->mh->full_name, &out_len, strlen(m->mh->full_name));
    } else {
        strncpy(fullname, m->mh->full_name, len);
        fullname[len] = '\0';
    }
    return MOBI_SUCCESS;
}

/**
 @brief Set ebook full name stored in Record 0 at offset given in MOBI header
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] fullname Memory area to be filled with zero terminated full name string
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_set_fullname(MOBIData *m, const char *fullname) {
    if (mobi_exists_mobiheader(m) && m->mh->full_name) {
        size_t title_length = min(strlen(fullname), MOBI_TITLE_SIZEMAX);
        char *new_title = malloc(title_length + 1);
        if (new_title == NULL) {
            return MOBI_MALLOC_FAILED;
        }
        if (mobi_is_cp1252(m)) {
            size_t new_size = title_length + 1;
            MOBI_RET ret = mobi_utf8_to_cp1252(new_title, fullname, &new_size, title_length);
            if (ret != MOBI_SUCCESS) {
                free(new_title);
                return ret;
            }
        } else {
            memcpy(new_title, fullname, title_length);
            new_title[title_length] = '\0';
        }
        free(m->mh->full_name);
        m->mh->full_name = new_title;
        if (mobi_is_hybrid(m) && mobi_exists_mobiheader(m->next) && m->next->mh->full_name) {
            char *new_title2 = strdup(new_title);
            if (new_title2 == NULL) {
                return MOBI_MALLOC_FAILED;
            }
            free(m->next->mh->full_name);
            m->next->mh->full_name = new_title2;
        }
    }
    return MOBI_SUCCESS;
}

/**
 @brief Set palm database name
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] name Name
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_set_pdbname(MOBIData *m, const char *name) {
    if (m == NULL || m->ph == NULL) {
        return MOBI_INIT_FAILED;
    }
    char dbname[PALMDB_NAME_SIZE_MAX + 1];
    if (mobi_is_cp1252(m)) {
        size_t size = PALMDB_NAME_SIZE_MAX + 1;
        MOBI_RET ret = mobi_utf8_to_cp1252(dbname, name, &size, strlen(name));
        if (ret != MOBI_SUCCESS) {
            return ret;
        }
    } else {
        memcpy(dbname, name, PALMDB_NAME_SIZE_MAX);
        dbname[PALMDB_NAME_SIZE_MAX] = '\0';
    }
    char c;
    int i = 0;
    while ((c = dbname[i])) {
        if (!isalnum(c)) {
            c = '_';
        }
        m->ph->name[i++] = c;
    }
    m->ph->name[i] = '\0';
    return MOBI_SUCCESS;
}

/**
 @brief Get palm database record with given unique id
 
 @param[in] m MOBIData structure with loaded data
 @param[in] uid Unique id
 @return Pointer to MOBIPdbRecord record structure, NULL on failure
 */
MOBIPdbRecord * mobi_get_record_by_uid(const MOBIData *m, const size_t uid) {
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return NULL;
    }
    if (m->rec == NULL) {
        return NULL;
    }
    MOBIPdbRecord *curr = m->rec;
    while (curr != NULL) {
        if (curr->uid == uid) {
            return curr;
        }
        curr = curr->next;
    }
    return NULL;
}

/**
 @brief Get rawml->markup MOBIPart part by uid
 
 @param[in] rawml MOBIRawml structure with loaded data
 @param[in] uid Unique id
 @return Pointer to MOBIPart structure, NULL on failure
 */
MOBIPart * mobi_get_part_by_uid(const MOBIRawml *rawml, const size_t uid) {
    if (rawml == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return NULL;
    }
    if (rawml->markup == NULL) {
        return NULL;
    }
    MOBIPart *part = rawml->markup;
    while (part != NULL) {
        if (part->uid == uid) {
            return part;
        }
        part = part->next;
    }
    return NULL;
}

/**
 @brief Get rawml->flow MOBIPart part by uid
 
 @param[in] rawml MOBIRawml structure with loaded data
 @param[in] uid Unique id
 @return Pointer to MOBIPart structure, NULL on failure
 */
MOBIPart * mobi_get_flow_by_uid(const MOBIRawml *rawml, const size_t uid) {
    if (rawml == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return NULL;
    }
    if (rawml->flow == NULL) {
        return NULL;
    }
    MOBIPart *part = rawml->flow;
    while (part != NULL) {
        if (part->uid == uid) {
            return part;
        }
        part = part->next;
    }
    return NULL;
}

/**
 @brief Find flow part by flow id (fid) from kindle:flow:fid link.
 Flow fid is base32 encoded part uid.
 
 @param[in] rawml Structure MOBIRawml
 @param[in] fid String four character base32 fid
 @return Pointer to MOBIPart structure, NULL on failure
 */
MOBIPart * mobi_get_flow_by_fid(const MOBIRawml *rawml, const char *fid) {
    /* get file number */
    uint32_t part_id;
    MOBI_RET ret = mobi_base32_decode(&part_id, fid);
    if (ret != MOBI_SUCCESS) {
        return NULL;
    }
    return mobi_get_flow_by_uid(rawml, part_id);
}

/**
 @brief Get MOBIPart resource record with given unique id
 
 @param[in] rawml MOBIRawml structure with loaded data
 @param[in] uid Unique id
 @return Pointer to MOBIPart resource structure, NULL on failure
 */
MOBIPart * mobi_get_resource_by_uid(const MOBIRawml *rawml, const size_t uid) {
    if (rawml == NULL) {
        debug_print("%s", "Rawml structure not initialized\n");
        return NULL;
    }
    if (rawml->resources == NULL) {
        debug_print("%s", "Rawml structure not initialized\n");
        return NULL;
    }
    MOBIPart *curr = rawml->resources;
    while (curr != NULL) {
        if (curr->uid == uid) {
            return curr;
        }
        curr = curr->next;
    }
    return NULL;
}

/**
 @brief Find resource by flow id (fid) from kindle:embed:fid link.
 Flow fid is base32 encoded part uid.
 
 @param[in] rawml Structure MOBIRawml
 @param[in] fid String four character base32 fid
 @return Pointer to MOBIPart structure, NULL on failure
 */
MOBIPart * mobi_get_resource_by_fid(const MOBIRawml *rawml, const char *fid) {
    /* get file number */
    uint32_t part_id;
    MOBI_RET ret = mobi_base32_decode(&part_id, fid);
    if (ret != MOBI_SUCCESS) {
        return NULL;
    }
    part_id--;
    return mobi_get_resource_by_uid(rawml, part_id);
}

/**
 @brief Get MOBIFiletype type of MOBIPart resource record with given unique id
 
 @param[in] rawml MOBIRawml structure with loaded data
 @param[in] uid Unique id
 @return Pointer to MOBIPart resource structure, NULL on failure
 */
MOBIFiletype mobi_get_resourcetype_by_uid(const MOBIRawml *rawml, const size_t uid) {
    if (rawml == NULL) {
        debug_print("%s", "Rawml structure not initialized\n");
        return T_UNKNOWN;
    }
    if (rawml->resources == NULL) {
        debug_print("%s", "Rawml structure not initialized\n");
        return T_UNKNOWN;
    }
    MOBIPart *curr = rawml->resources;
    while (curr != NULL) {
        if (curr->uid == uid) {
            return curr->type;
        }
        curr = curr->next;
    }
    return T_UNKNOWN;
}

/**
 @brief Get palm database record with given sequential number (first record has number 0)
 
 @param[in] m MOBIData structure with loaded data
 @param[in] num Sequential number
 @return Pointer to MOBIPdbRecord record structure, NULL on failure
 */
MOBIPdbRecord * mobi_get_record_by_seqnumber(const MOBIData *m, const size_t num) {
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return NULL;
    }
    if (m->rec == NULL) {
        return NULL;
    }
    MOBIPdbRecord *curr = m->rec;
    size_t i = 0;
    while (curr != NULL) {
        if (i++ == num) {
            return curr;
        }
        curr = curr->next;
    }
    return NULL;
}

/**
 @brief Delete palm database record with given sequential number from MOBIData structure
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] num Sequential number
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_delete_record_by_seqnumber(MOBIData *m, const size_t num) {
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_INIT_FAILED;
    }
    if (m->rec == NULL) {
        return MOBI_INIT_FAILED;
    }
    size_t i = 0;
    MOBIPdbRecord *curr = m->rec;
    MOBIPdbRecord *prev = NULL;
    while (curr != NULL) {
        if (i++ == num) {
            if (prev == NULL) {
                m->rec = curr->next;
            } else {
                prev->next = curr->next;
            }
            free(curr->data);
            curr->data = NULL;
            free(curr);
            curr = NULL;
            return MOBI_SUCCESS;
        }
        prev = curr;
        curr = curr->next;
    }
    return MOBI_SUCCESS;
}

/**
 @brief Get EXTH record with given MOBIExthTag tag
 
 @param[in] m MOBIData structure with loaded data
 @param[in] tag MOBIExthTag EXTH record tag
 @return Pointer to MOBIExthHeader record structure
 */
MOBIExthHeader * mobi_get_exthrecord_by_tag(const MOBIData *m, const MOBIExthTag tag) {
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return NULL;
    }
    if (m->eh == NULL) {
        return NULL;
    }
    MOBIExthHeader *curr = m->eh;
    while (curr != NULL) {
        if (curr->tag == tag) {
            return curr;
        }
        curr = curr->next;
    }
    return NULL;
}

/**
 @brief Get EXTH record with given MOBIExthTag tag. Start list search at given record.
 
 If start_tag is NULL search will start from the root of the linked list.
 After successfull search start will be set to next record in the list.
 
 @param[in] m MOBIData structure with loaded data
 @param[in] tag MOBIExthTag EXTH record tag
 @param[in,out] start MOBIExthHeader EXTH record to begin search with
 @return Pointer to MOBIExthHeader record structure
 */
MOBIExthHeader * mobi_next_exthrecord_by_tag(const MOBIData *m, const MOBIExthTag tag, MOBIExthHeader **start) {
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return NULL;
    }
    if (m->eh == NULL) {
        return NULL;
    }
    MOBIExthHeader *curr;
    if (*start) {
        curr = *start;
        *start = NULL;
    } else {
        curr = m->eh;
    }
    while (curr != NULL) {
        if (curr->tag == tag) {
            *start = curr->next;
            return curr;
        }
        curr = curr->next;
    }
    return NULL;
}

/**
 @brief Add new EXTH record with given tag and value.
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] tag MOBIExthTag EXTH record tag
 @param[in] size Value size
 @param[in] value Value
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_add_exthrecord(MOBIData *m, const MOBIExthTag tag, const uint32_t size, const void *value) {
    if (size == 0) {
        debug_print("%s\n", "Record size is zero");
        return MOBI_PARAM_ERR;
    }
    size_t count = 2;
    while (m && count--) {
        if (m->mh == NULL) {
            debug_print("%s\n", "Mobi header must be initialized");
            return MOBI_INIT_FAILED;
        }
        MOBIExthMeta meta = mobi_get_exthtagmeta_by_tag(tag);
        MOBIExthHeader *record = calloc(1, sizeof(MOBIExthHeader));
        if (record == NULL) {
            debug_print("%s\n", "Memory allocation for EXTH record failed");
            return MOBI_MALLOC_FAILED;
        }
        record->tag = tag;
        record->size = size;
        record->data = malloc(size);
        if (record->data == NULL) {
            debug_print("%s\n", "Memory allocation for EXTH data failed");
            free(record);
            return MOBI_MALLOC_FAILED;
        }
        if (meta.type == EXTH_STRING && mobi_is_cp1252(m)) {
            char *data = malloc(size + 1);
            if (data == NULL) {
                free(record->data);
                free(record);
                return MOBI_MALLOC_FAILED;
            }
            size_t data_size = size + 1;
            MOBI_RET ret = mobi_utf8_to_cp1252(data, value, &data_size, size);
            if (ret != MOBI_SUCCESS) {
                free(record->data);
                free(record);
                free(data);
                return ret;
            }
            memcpy(record->data, data, data_size);
            record->size = (uint32_t) data_size;
            free(data);
        } else if (meta.name && meta.type == EXTH_NUMERIC) {
            if (size != sizeof(uint32_t)) {
                free(record->data);
                free(record);
                return MOBI_PARAM_ERR;
            }
            MOBIBuffer *buf = mobi_buffer_init_null(record->data, size);
            if (buf == NULL) {
                free(record->data);
                free(record);
                return MOBI_MALLOC_FAILED;
            }
            mobi_buffer_add32(buf, *(uint32_t *) value);
            mobi_buffer_free_null(buf);
        } else {
            memcpy(record->data, value, size);
        }
        record->next = NULL;
        if (m->eh == NULL) {
            if (m->mh->exth_flags == NULL) {
                m->mh->exth_flags = malloc(sizeof(uint32_t));
                if (m->mh->exth_flags == NULL) {
                    debug_print("%s\n", "Memory allocation failed");
                    free(record->data);
                    free(record);
                    return MOBI_MALLOC_FAILED;
                }
            }
            *m->mh->exth_flags = 0x40;
            m->eh = record;
        } else {
            MOBIExthHeader *curr = m->eh;
            while(curr->next) {
                curr = curr->next;
            }
            curr->next = record;
        }
        m = m->next;
    }
    return MOBI_SUCCESS;
}

/**
 @brief Delete EXTH record.
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] record Record to be deleted
 @return Pointer to next record in the linked list (NULL if none)
 */
MOBIExthHeader * mobi_delete_exthrecord(MOBIData *m, MOBIExthHeader *record) {
    if (record == NULL || m == NULL || m->eh == NULL) {
        return NULL;
    }
    MOBIExthHeader *next = record->next;
    if (next) {
        /* not last */
        free(record->data);
        record->data = next->data;
        record->tag = next->tag;
        record->size = next->size;
        record->next = next->next;
        free(next);
        next = record;
    } else if (m->eh == record) {
        /* last && first */
        free(m->eh->data);
        free(m->eh);
        m->eh = NULL;
    } else {
        /* last */
        MOBIExthHeader *curr = m->eh;
        while (curr) {
            if (curr->next == record) {
                curr->next = NULL;
                break;
            }
            curr = curr->next;
        }
        free(record->data);
        free(record);
    }
    return next;
}

/**
 @brief Delete all EXTH records with given MOBIExthTag tag
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] tag MOBIExthTag EXTH record tag
 @return Pointer to MOBIExthHeader record structure
 */
MOBI_RET mobi_delete_exthrecord_by_tag(MOBIData *m, const MOBIExthTag tag) {
    size_t count = 2;
    while (m && count--) {
        if (m->eh == NULL) {
            debug_print("%s", "No exth records\n");
            return MOBI_SUCCESS;
        }
        MOBIExthHeader *curr = m->eh;
        while (curr) {
            if (curr->tag == tag) {
                curr = mobi_delete_exthrecord(m, curr);
            } else {
                curr = curr->next;
            }
        }
        m = m->next;
    }
    return MOBI_SUCCESS;
}

/**
 @brief Array of known EXTH tags.
 Name strings shamelessly copied from KindleUnpack
 */
const MOBIExthMeta mobi_exth_tags[] = {
    /* numeric */
    {EXTH_SAMPLE, EXTH_NUMERIC, "Sample"},
    {EXTH_STARTREADING, EXTH_NUMERIC, "Start offset"},
    {EXTH_KF8BOUNDARY, EXTH_NUMERIC, "K8 boundary offset"},
    {EXTH_COUNTRESOURCES, EXTH_NUMERIC, "K8 count of resources, fonts, images"},
    {EXTH_RESCOFFSET, EXTH_NUMERIC, "RESC offset"},
    {EXTH_COVEROFFSET, EXTH_NUMERIC, "Cover offset"},
    {EXTH_THUMBOFFSET, EXTH_NUMERIC, "Thumbnail offset"},
    {EXTH_HASFAKECOVER, EXTH_NUMERIC, "Has fake cover"},
    {EXTH_CREATORSOFT, EXTH_NUMERIC, "Creator software"},
    {EXTH_CREATORMAJOR, EXTH_NUMERIC, "Creator major version"},
    {EXTH_CREATORMINOR, EXTH_NUMERIC, "Creator minor version"},
    {EXTH_CREATORBUILD, EXTH_NUMERIC, "Creator build number"},
    {EXTH_CLIPPINGLIMIT, EXTH_NUMERIC, "Clipping limit"},
    {EXTH_PUBLISHERLIMIT, EXTH_NUMERIC, "Publisher limit"},
    {EXTH_TTSDISABLE, EXTH_NUMERIC, "Text to speech disabled"},
    {EXTH_RENTAL, EXTH_NUMERIC, "Rental indicator"},
    /* strings */
    {EXTH_DRMSERVER, EXTH_STRING, "Drm server id"},
    {EXTH_DRMCOMMERCE, EXTH_STRING, "Drm commerce id"},
    {EXTH_DRMEBOOKBASE, EXTH_STRING, "Drm Ebookbase book id"},
    {EXTH_TITLE, EXTH_STRING, "Title"},
    {EXTH_AUTHOR, EXTH_STRING, "Creator"},
    {EXTH_PUBLISHER, EXTH_STRING, "Publisher"},
    {EXTH_IMPRINT, EXTH_STRING, "Imprint"},
    {EXTH_DESCRIPTION, EXTH_STRING, "Description"},
    {EXTH_ISBN, EXTH_STRING, "ISBN"},
    {EXTH_SUBJECT, EXTH_STRING, "Subject"},
    {EXTH_PUBLISHINGDATE, EXTH_STRING, "Published"},
    {EXTH_REVIEW, EXTH_STRING, "Review"},
    {EXTH_CONTRIBUTOR, EXTH_STRING, "Contributor"},
    {EXTH_RIGHTS, EXTH_STRING, "Rights"},
    {EXTH_SUBJECTCODE, EXTH_STRING, "Subject code"},
    {EXTH_TYPE, EXTH_STRING, "Type"},
    {EXTH_SOURCE, EXTH_STRING, "Source"},
    {EXTH_ASIN, EXTH_STRING, "ASIN"},
    {EXTH_VERSION, EXTH_STRING, "Version number"},
    {EXTH_ADULT, EXTH_STRING, "Adult"},
    {EXTH_PRICE, EXTH_STRING, "Price"},
    {EXTH_CURRENCY, EXTH_STRING, "Currency"},
    {EXTH_FIXEDLAYOUT, EXTH_STRING, "Fixed layout"},
    {EXTH_BOOKTYPE, EXTH_STRING, "Book type"},
    {EXTH_ORIENTATIONLOCK, EXTH_STRING, "Orientation lock"},
    {EXTH_ORIGRESOLUTION, EXTH_STRING, "Original resolution"},
    {EXTH_ZEROGUTTER, EXTH_STRING, "Zero gutter"},
    {EXTH_ZEROMARGIN, EXTH_STRING, "Zero margin"},
    {EXTH_KF8COVERURI, EXTH_STRING, "K8 masthead/cover image"},
    {EXTH_REGIONMAGNI, EXTH_STRING, "Region magnification"},
    {EXTH_DICTNAME, EXTH_STRING, "Dictionary short name"},
    {EXTH_WATERMARK, EXTH_STRING, "Watermark"},
    {EXTH_DOCTYPE, EXTH_STRING, "Document type"},
    {EXTH_LASTUPDATE, EXTH_STRING, "Last update time"},
    {EXTH_UPDATEDTITLE, EXTH_STRING, "Updated title"},
    {EXTH_ASIN504, EXTH_STRING, "ASIN (504)"},
    {EXTH_TITLEFILEAS, EXTH_STRING, "Title file as"},
    {EXTH_CREATORFILEAS, EXTH_STRING, "Creator file as"},
    {EXTH_PUBLISHERFILEAS, EXTH_STRING, "Publisher file as"},
    {EXTH_LANGUAGE, EXTH_STRING, "Language"},
    {EXTH_ALIGNMENT, EXTH_STRING, "Primary writing mode"},
    {EXTH_PAGEDIR, EXTH_STRING, "Page progression direction"},
    {EXTH_OVERRIDEFONTS, EXTH_STRING, "Override kindle fonts"},
    {EXTH_SORCEDESC, EXTH_STRING, "Original source description"},
    {EXTH_DICTLANGIN, EXTH_STRING, "Dictionary input language"},
    {EXTH_DICTLANGOUT, EXTH_STRING, "Dictionary output language"},
    {EXTH_INPUTSOURCE, EXTH_STRING, "Input source type"},
    {EXTH_CREATORBUILDREV, EXTH_STRING, "Creator build revision"},
    {EXTH_CREATORSTRING, EXTH_STRING, "Creator software string"},
    /* binary */
    {EXTH_TAMPERKEYS, EXTH_BINARY, "Tamper proof keys"},
    {EXTH_FONTSIGNATURE, EXTH_BINARY, "Font signature"},
    {EXTH_UNK403, EXTH_BINARY, "Unknown (403)"},
    {EXTH_UNK405, EXTH_BINARY, "Unknown (405)"},
    {EXTH_UNK407, EXTH_BINARY, "Unknown (407)"},
    {EXTH_UNK450, EXTH_BINARY, "Unknown (450)"},
    {EXTH_UNK451, EXTH_BINARY, "Unknown (451)"},
    {EXTH_UNK452, EXTH_BINARY, "Unknown (452)"},
    {EXTH_UNK453, EXTH_BINARY, "Unknown (453)"},
    /* end */
    {0, 0, NULL},
};

/**
 @brief Get MOBIExthMeta tag structure by MOBIExthTag tag id
 
 @param[in] tag Tag id
 @return MOBIExthMeta structure for given tag id, zeroed structure on failure
 */
MOBIExthMeta mobi_get_exthtagmeta_by_tag(const MOBIExthTag tag) {
    size_t i = 0;
    while (mobi_exth_tags[i].tag > 0) {
        if (mobi_exth_tags[i].tag == tag) {
            return mobi_exth_tags[i];
        }
        i++;
    }
    return (MOBIExthMeta) {0, 0, NULL};
}

/**
 @brief Decode big-endian value stored in EXTH record
 
 Only for EXTH records storing numeric values
 
 @param[in] data Memory area storing EXTH record data
 @param[in] size Size of EXTH record data
 @return 32-bit value
 */
uint32_t mobi_decode_exthvalue(const unsigned char *data, const size_t size) {
    /* FIXME: EXTH numeric data is max 32-bit? */
    uint32_t val = 0;
    size_t i = min(size, 4);
    while (i--) {
        val |= (uint32_t) *data++ << (i * 8);
    }
    return val;
}

#define MOBI_UTF8_MAXBYTES 4
/**
 @brief Html entity mapping to utf-8 sequence
 */
typedef struct {
    const char *name; /**< Html entity name */
    const char utf8_bytes[MOBI_UTF8_MAXBYTES + 1]; /**< Utf-8 sequence */
} HTMLEntity;

/**
 @brief Basic named html entities mapping to utf-8 sequences
 */
const HTMLEntity entities[] = {
    { "&quot;", "\"" },
    { "&amp;", "&" },
    { "&lt;", "<" },
    { "&gt;", ">" },
    { "&apos;", "'" },
    { "&nbsp;", "\xc2\xa0" },
    { "&copy;", "\xc2\xa9" },
    { "&reg;", "\xc2\xae" },
    { "&cent;", "\xc2\xa2" },
    { "&pound;", "\xc2\xa3" },
    { "&sect;", "\xc2\xa7" },
    { "&laquo;", "\xc2\xab" },
    { "&raquo;", "\xc2\xbb" },
    { "&deg;", "\xc2\xb0" },
    { "&plusmn;", "\xc2\xb1" },
    { "&middot;", "\xc2\xb7" },
    { "&frac12;", "\xc2\xbd" },
    { "&ndash;", "\xe2\x80\x93" },
    { "&mdash;", "\xe2\x80\x94" },
    { "&lsquo;", "\xe2\x80\x98" },
    { "&sbquo;", "\xe2\x80\x9a" },
    { "&ldquo;", "\xe2\x80\x9c" },
    { "&rdquo;", "\xe2\x80\x9d" },
    { "&bdquo;", "\xe2\x80\x9e" },
    { "&dagger;", "\xe2\x80\xa0" },
    { "&Dagger;", "\xe2\x80\xa1" },
    { "&bull;", "\xe2\x80\xa2" },
    { "&hellip;", "\xe2\x80\xa6" },
    { "&prime;", "\xe2\x80\xb2" },
    { "&Prime;", "\xe2\x80\xb3" },
    { "&euro;", "\xe2\x82\xac" },
    { "&trade;", "\xe2\x84\xa2" }
};

/**
 @brief Convert html entities in string to utf-8 characters
 
 @param[in] input Input string
 @return Converted string
 */
char * mobi_decode_htmlentities(const char *input) {
    if (!input) {
        return NULL;
    }
    const size_t codepoint_max = 0x10ffff;
    size_t output_length = strlen(input) + 1;
    char *in = (char *) input;
    /* output size will be less or equal to input */
    char *output = malloc(output_length);
    char *out = output;
    if (output == NULL) {
        debug_print("Memory allocation failed (%zu bytes)\n", output_length);
        return NULL;
    }
    char *offset = in;
    while ((in = strchr(in, '&'))) {
        size_t decoded_length = 0;
        char *end = NULL;
        char decoded[MOBI_UTF8_MAXBYTES + 1] = { 0 };
        if (in[1] == '#' && (in[2] == 'x' || in[2] == 'X')) {
            // hex entity
            size_t codepoint = strtoul(in + 3, &end, 16);
            if (*end++ == ';' && codepoint <= codepoint_max) {
                decoded_length = mobi_unicode_to_utf8(decoded, codepoint);
            }
        } else if (in[1] == '#') {
            // dec entity
            size_t codepoint = strtoul(in + 2, &end, 10);
            if (*end++ == ';' && codepoint <= codepoint_max) {
                decoded_length = mobi_unicode_to_utf8(decoded, codepoint);
            }
        } else {
            // named entity
            for (size_t i = 0; i < ARRAYSIZE(entities); i++) {
                if (strncmp(in, entities[i].name, strlen(entities[i].name)) == 0) {
                    int ret = snprintf(decoded, MOBI_UTF8_MAXBYTES + 1, "%s", entities[i].utf8_bytes);
                    if (ret > 0) {
                        decoded_length = (size_t) ret;
                        end = in + strlen(entities[i].name);
                        break;
                    }
                }
            }
        }
        if (decoded_length) {
            size_t len = (size_t) (in - offset);
            memcpy(out, offset, len);
            offset = end;
            out += len;
            memcpy(out, decoded, decoded_length);
            out += decoded_length;
        }
        in += decoded_length + 1;
    }
    strcpy(out, offset);
    return output;
}

/**
 @brief Decode string stored in EXTH record
 
 Only for EXTH records storing string values
 
 @param[in] m MOBIData structure loaded with MOBI data
 @param[in] data Memory area storing EXTH record data
 @param[in] size Size of EXTH record data
 @return String from EXTH record in utf-8 encoding
 */
char * mobi_decode_exthstring(const MOBIData *m, const unsigned char *data, const size_t size) {
    if (!m || !data) {
        return NULL;
    }
    size_t out_length = 3 * size + 1;
    size_t in_length = size;
    char *exth_string = malloc(out_length);
    if (exth_string == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return NULL;
    }
    if (mobi_is_cp1252(m)) {
        MOBI_RET ret = mobi_cp1252_to_utf8(exth_string, (const char *) data, &out_length, in_length);
        if (ret != MOBI_SUCCESS) {
            free(exth_string);
            return NULL;
        }
    } else {
        memcpy(exth_string, data, size);
        out_length = size;
    }
    exth_string[out_length] = '\0';
    char *exth_decoded = mobi_decode_htmlentities(exth_string);
    if (exth_decoded != NULL) {
        free(exth_string);
        return exth_decoded;
    } else {
        return exth_string;
    }
}

/**
 @brief Swap endianness of 32-bit value
 
 @param[in] val 4-byte unsigned integer
 @return Integer with swapped endianness
 */
uint32_t mobi_swap32(const uint32_t val) {
    return ((((val) >> 24) & 0x000000ff) |
            (((val) >>  8) & 0x0000ff00) |
            (((val) <<  8) & 0x00ff0000) |
            (((val) << 24) & 0xff000000));

}

/**
 @brief Convert time values from palmdoc header to time tm struct
 
 Older files set time in mac format. Newer ones in unix time.
 
 @param[in] pdb_time Time value from PDB header
 @return Time structure struct tm of time.h
 */
struct tm * mobi_pdbtime_to_time(const long pdb_time) {
    time_t time = pdb_time;
    const int unix1996 = 820454400;
    if (time < unix1996 && time > 0) {
        /* sometimes dates are stored as little endian */
        time = mobi_swap32((uint32_t) time);
    }
    const uint32_t mactime_flag = (uint32_t) (1U << 31);
    if (time & mactime_flag) {
        debug_print("%s\n", "mac time");
        time -= EPOCH_MAC_DIFF;
    }
    return localtime(&time);
}

/**
 @brief Lookup table for number of bits set in a single byte
 */
static const char setbits[256] = {
    0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4,
    1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
    1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
    2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
    1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
    2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
    2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
    3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
    1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
    2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
    2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
    3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
    2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
    3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
    3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
    4, 5, 5, 6, 5, 6, 6, 7, 5, 6, 6, 7, 6, 7, 7, 8,
};

/**
 @brief Get number of bits set in a given byte
 
 @param[in] byte A byte
 @return Number of bits set
 */
int mobi_bitcount(const uint8_t byte) {
    return setbits[byte];
}

/**
 @brief Decompress text record (internal).
 
 Internal function for mobi_get_rawml and mobi_dump_rawml. 
 Decompressed output is stored either in a file or in a text string
 
 @param[in] m MOBIData structure loaded with MOBI data
 @param[in,out] text Memory area to be filled with decompressed output
 @param[in,out] file If not NULL output is written to the file, otherwise to text string
 @param[in,out] len Length of the memory allocated for the text string, on return set to decompressed text length
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_decompress_content(const MOBIData *m, char *text, FILE *file, size_t *len) {
    int dump = false;
    if (file != NULL) {
        dump = true;
    }
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_INIT_FAILED;
    }
    if (mobi_is_encrypted(m) && m->drm_key == NULL) {
        debug_print("%s", "Document is encrypted\n");
        return MOBI_FILE_ENCRYPTED;
    }
    const size_t offset = mobi_get_kf8offset(m);
    if (m->rh == NULL || m->rh->text_record_count == 0) {
        debug_print("%s", "Text records not found in MOBI header\n");
        return MOBI_DATA_CORRUPT;
    }
    const size_t text_rec_index = 1 + offset;
    size_t text_rec_count = m->rh->text_record_count;
    const uint16_t compression_type = m->rh->compression_type;
    /* check for extra data at the end of text files */
    uint16_t extra_flags = 0;
    if (m->mh && m->mh->extra_flags) {
        extra_flags = *m->mh->extra_flags;
    }
    /* get first text record */
    const MOBIPdbRecord *curr = mobi_get_record_by_seqnumber(m, text_rec_index);
    MOBIHuffCdic *huffcdic = NULL;
    if (compression_type == RECORD0_HUFF_COMPRESSION) {
        /* load huff/cdic tables */
        huffcdic = mobi_init_huffcdic();
        if (huffcdic == NULL) {
            debug_print("%s\n", "Memory allocation failed");
            return MOBI_MALLOC_FAILED;
        }
        MOBI_RET ret = mobi_parse_huffdic(m, huffcdic);
        if (ret != MOBI_SUCCESS) {
            mobi_free_huffcdic(huffcdic);
            return ret;
        }
    }
    /* get following CDIC records */
    size_t text_length = 0;
    while (text_rec_count-- && curr) {
        size_t extra_size = 0;
        if (extra_flags) {
            extra_size = mobi_get_record_extrasize(curr, extra_flags);
            if (extra_size == MOBI_NOTSET) {
                mobi_free_huffcdic(huffcdic);
                return MOBI_DATA_CORRUPT;
            }
        }
        size_t decompressed_size = mobi_get_textrecord_maxsize(m);
        unsigned char *decompressed = malloc(decompressed_size);
        if (decompressed == NULL) {
            mobi_free_huffcdic(huffcdic);
            debug_print("Memory allocation failed%s", "\n");
            return MOBI_MALLOC_FAILED;
        }
        MOBI_RET ret = MOBI_SUCCESS;
#ifdef USE_ENCRYPTION
        if (mobi_is_encrypted(m) && m->drm_key) {
            if (compression_type != RECORD0_HUFF_COMPRESSION) {
                /* decrypt also multibyte extra data */
                extra_size = mobi_get_record_extrasize(curr, extra_flags & 0xfffe);
                if (extra_size == MOBI_NOTSET || extra_size > curr->size) {
                    free(decompressed);
                    return MOBI_DATA_CORRUPT;
                }
            }
            const size_t decrypt_size = curr->size - extra_size;
            if (decrypt_size > decompressed_size) {
                debug_print("Record too large: %zu\n", decrypt_size);
                mobi_free_huffcdic(huffcdic);
                free(decompressed);
                return MOBI_DATA_CORRUPT;
            }
            if (decrypt_size) {
                ret = mobi_drm_decrypt_buffer(decompressed, curr->data, decrypt_size, m);
                if (ret != MOBI_SUCCESS) {
                    mobi_free_huffcdic(huffcdic);
                    free(decompressed);
                    return ret;
                }
                memcpy(curr->data, decompressed, decrypt_size);
                if (compression_type != RECORD0_HUFF_COMPRESSION && (extra_flags & 1)) {
                    // update multibyte data size after decryption
                    extra_size = mobi_get_record_extrasize(curr, extra_flags);
                    if (extra_size == MOBI_NOTSET) {
                        free(decompressed);
                        return MOBI_DATA_CORRUPT;
                    }
                }
            }
        }
#endif
        if (extra_size > curr->size) {
            debug_print("Wrong record size: -%zu\n", extra_size - curr->size);
            mobi_free_huffcdic(huffcdic);
            free(decompressed);
            return MOBI_DATA_CORRUPT;
        } else if (extra_size == curr->size) {
            debug_print("Skipping empty record%s", "\n");
            free(decompressed);
            curr = curr->next;
            continue;
        }
        const size_t record_size = curr->size - extra_size;
        switch (compression_type) {
            case RECORD0_NO_COMPRESSION:
                /* no compression */
                if (record_size > decompressed_size) {
                    debug_print("Record too large: %zu\n", record_size);
                    free(decompressed);
                    return MOBI_DATA_CORRUPT;
                }
                memcpy(decompressed, curr->data, record_size);
                decompressed_size = record_size;
                if (mobi_exists_mobiheader(m) && mobi_get_fileversion(m) <= 3) {
                    /* workaround for some old files with null characters inside record */
                    mobi_remove_zeros(decompressed, &decompressed_size);
                }
                break;
            case RECORD0_PALMDOC_COMPRESSION:
                /* palmdoc lz77 compression */
                ret = mobi_decompress_lz77(decompressed, curr->data, &decompressed_size, record_size);
                if (ret != MOBI_SUCCESS) {
                    free(decompressed);
                    return ret;
                }
                break;
            case RECORD0_HUFF_COMPRESSION:
                /* mobi huffman compression */
                ret = mobi_decompress_huffman(decompressed, curr->data, &decompressed_size, record_size, huffcdic);
                if (ret != MOBI_SUCCESS) {
                    free(decompressed);
                    mobi_free_huffcdic(huffcdic);
                    return ret;
                }
                break;
            default:
                debug_print("%s", "Unknown compression type\n");
                mobi_free_huffcdic(huffcdic);
                free(decompressed);
                return MOBI_DATA_CORRUPT;
        }
        curr = curr->next;
        if (dump) {
            fwrite(decompressed, 1, decompressed_size, file);
        } else {
            if (text_length + decompressed_size > *len) {
                debug_print("%s", "Text buffer too small\n");
                /* free huff/cdic tables */
                mobi_free_huffcdic(huffcdic);
                free(decompressed);
                return MOBI_PARAM_ERR;
            }
            memcpy(text + text_length, decompressed, decompressed_size);
            text_length += decompressed_size;
            text[text_length] = '\0';
        }
        free(decompressed);
    }
    /* free huff/cdic tables */
    mobi_free_huffcdic(huffcdic);
    if (len) {
        *len = text_length;
    }
    return MOBI_SUCCESS;
}

/**
 @brief Decompress text to a text buffer.
 
 @param[in] m MOBIData structure loaded with MOBI data
 @param[in,out] text Memory area to be filled with decompressed output
 @param[in,out] len Length of the memory allocated for the text string, on return will be set to decompressed text length
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_get_rawml(const MOBIData *m, char *text, size_t *len) {
    if (text == NULL || len == NULL) {
        debug_print("%s", "Parameter error: text or len is NULL\n");
        return MOBI_PARAM_ERR;
    }
    if (m->rh->text_length > *len) {
        debug_print("%s", "Text buffer smaller then text size declared in record0 header\n");
        return MOBI_PARAM_ERR;
    }
    text[0] = '\0';
    return mobi_decompress_content(m, text, NULL, len);
}

/**
 @brief Decompress text record to an open file descriptor.
 
 Internal function for mobi_get_rawml and mobi_dump_rawml.
 Decompressed output is stored either in a file or in a text string
 
 @param[in] m MOBIData structure loaded with MOBI data
 @param[in,out] file File descriptor
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_dump_rawml(const MOBIData *m, FILE *file) {
    if (file == NULL) {
        debug_print("%s", "File descriptor is NULL\n");
        return MOBI_FILE_NOT_FOUND;
    }
    return mobi_decompress_content(m, NULL, file, NULL);
}

/**
 @brief Check if MOBI header is loaded / present in the loaded file
 
 @param[in] m MOBIData structure loaded with MOBI data
 @return true on success, false otherwise
 */
bool mobi_exists_mobiheader(const MOBIData *m) {
    if (m == NULL || m->mh == NULL) {
        return false;
    }
    return true;
}

/**
 @brief Check if skeleton INDX is present in the loaded file
 
 @param[in] m MOBIData structure loaded with MOBI data
 @return true on success, false otherwise
 */
bool mobi_exists_skel_indx(const MOBIData *m) {
    if (!mobi_exists_mobiheader(m)) {
        return false;
    }
    if (m->mh->skeleton_index == NULL || *m->mh->skeleton_index == MOBI_NOTSET) {
        debug_print("%s", "SKEL INDX record not found\n");
        return false;
    }
    return true;
}

/**
 @brief Check if FDST record is present in the loaded file
 
 @param[in] m MOBIData structure loaded with MOBI data
 @return true on success, false otherwise
 */
bool mobi_exists_fdst(const MOBIData *m) {
    if (!mobi_exists_mobiheader(m) || mobi_get_fileversion(m) <= 3) {
        return false;
    }
    if (mobi_get_fileversion(m) >= 8) {
        if (m->mh->fdst_index && *m->mh->fdst_index != MOBI_NOTSET) {
            return true;
        }
    } else {
        if ((m->mh->fdst_section_count && *m->mh->fdst_section_count > 1)
            && (m->mh->last_text_index && *m->mh->last_text_index != (uint16_t) -1)) {
            return true;
        }
    }
    debug_print("%s", "FDST record not found\n");
    return false;
}

/**
 @brief Get sequential number of FDST record
 
 @param[in] m MOBIData structure loaded with MOBI data
 @return Record number on success, MOBI_NOTSET otherwise
 */
size_t mobi_get_fdst_record_number(const MOBIData *m) {
    if (!mobi_exists_mobiheader(m)) {
        return MOBI_NOTSET;
    }
    const size_t offset = mobi_get_kf8offset(m);
    if (m->mh->fdst_index && *m->mh->fdst_index != MOBI_NOTSET) {
        if (m->mh->fdst_section_count && *m->mh->fdst_section_count > 1) {
            return *m->mh->fdst_index + offset;
        }
    }
    if (m->mh->fdst_section_count && *m->mh->fdst_section_count > 1) {
        /* FIXME: if KF7, is it safe to asume last_text_index has fdst index */
        if (m->mh->last_text_index) {
            return *m->mh->last_text_index;
        }
    }
    return MOBI_NOTSET;
}

/**
 @brief Check if fragments INDX is present in the loaded file
 
 @param[in] m MOBIData structure loaded with MOBI data
 @return true on success, false otherwise
 */
bool mobi_exists_frag_indx(const MOBIData *m) {
    if (!mobi_exists_mobiheader(m)) {
        return false;
    }
    if (m->mh->fragment_index == NULL || *m->mh->fragment_index == MOBI_NOTSET) {
        return false;
    }
    debug_print("%s", "Fragments INDX found\n");
    return true;
}

/**
 @brief Check if guide INDX is present in the loaded file
 
 @param[in] m MOBIData structure loaded with MOBI data
 @return true on success, false otherwise
 */
bool mobi_exists_guide_indx(const MOBIData *m) {
    if (!mobi_exists_mobiheader(m)) {
        return false;
    }
    if (m->mh->guide_index == NULL || *m->mh->guide_index == MOBI_NOTSET) {
        return false;
    }
    debug_print("%s", "Guide INDX found\n");
    return true;
}

/**
 @brief Check if ncx INDX is present in the loaded file
 
 @param[in] m MOBIData structure loaded with MOBI data
 @return true on success, false otherwise
 */
bool mobi_exists_ncx(const MOBIData *m) {
    if (!mobi_exists_mobiheader(m)) {
        return false;
    }
    if (m->mh->ncx_index == NULL || *m->mh->ncx_index == MOBI_NOTSET) {
        return false;
    }
    debug_print("%s", "NCX INDX found\n");
    return true;
}

/**
 @brief Check if orth INDX is present in the loaded file
 
 @param[in] m MOBIData structure loaded with MOBI data
 @return true on success, false otherwise
 */
bool mobi_exists_orth(const MOBIData *m) {
    if (!mobi_exists_mobiheader(m)) {
        return false;
    }
    if (m->mh->orth_index == NULL || *m->mh->orth_index == MOBI_NOTSET) {
        return false;
    }
    debug_print("%s", "ORTH INDX found\n");
    return true;
}

/**
 @brief Check if infl INDX is present in the loaded file
 
 @param[in] m MOBIData structure loaded with MOBI data
 @return true on success, false otherwise
 */
bool mobi_exists_infl(const MOBIData *m) {
    if (!mobi_exists_mobiheader(m)) {
        return false;
    }
    if (m->mh->infl_index == NULL || *m->mh->infl_index == MOBI_NOTSET) {
        return false;
    }
    debug_print("%s", "INFL INDX found\n");
    return true;
}

/**
 @brief Get file type of given part with number [part_number]
 
 @param[in] rawml MOBIRawml parsed records structure
 @param[in] part_number Sequential number of the part within rawml structure
 @return MOBIFiletype file type
 */
MOBIFiletype mobi_determine_flowpart_type(const MOBIRawml *rawml, const size_t part_number) {
    if (part_number == 0 || mobi_is_rawml_kf8(rawml) == false) {
        return T_HTML;
    }
    if (part_number > 9999) {
        debug_print("Corrupt part number: %zu\n", part_number);
        return T_UNKNOWN;
    }
    char target[30];
    snprintf(target, 30, "\"kindle:flow:%04zu?mime=", part_number);
    unsigned char *data_start = rawml->flow->data;
    unsigned char *data_end = data_start + rawml->flow->size - 1;
    MOBIResult result;
    MOBI_RET ret = mobi_find_attrvalue(&result, data_start, data_end, T_HTML, target);
    if (ret == MOBI_SUCCESS && result.start) {
        if (strstr(result.value, "text/css")) {
            return T_CSS;
        } else if (strstr(result.value, "image/svg+xml")) {
            return T_SVG;
        }
    }
    return T_UNKNOWN;
}

/**
 @brief Get font type of given font resource
 
 @param[in] font_data Font resource data
 @param[in] font_size Font resource size
 @return MOBIFiletype file type
 */
MOBIFiletype mobi_determine_font_type(const unsigned char *font_data, const size_t font_size) {
    const char otf_magic[] = "OTTO";
    const char ttf_magic[] = "\0\1\0\0";
    const char ttf2_magic[] = "true";

    if (font_size >= 4) {
        if (memcmp(font_data, otf_magic, 4) == 0) {
            return T_OTF;
        } else if (memcmp(font_data, ttf_magic, 4) == 0) {
            return T_TTF;
        } else if (memcmp(font_data, ttf2_magic, 4) == 0) {
            return T_TTF;
        }
    }
    debug_print("Unknown font resource type%s", "\n");
    return T_UNKNOWN;
}

/**
 @brief Replace part data with decoded audio data
 
 @param[in,out] part MOBIPart structure containing font resource, decoded part type will be set in the structure
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_add_audio_resource(MOBIPart *part) {
    unsigned char *data = NULL;
    size_t size = 0;
    MOBI_RET ret = mobi_decode_audio_resource(&data, &size, part);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    part->data = data;
    part->size = size;
    /* FIXME: the only possible audio type is mp3 */
    part->type = T_MP3;

    return MOBI_SUCCESS;
}

/**
 @brief Decode audio resource
 
 @param[in,out] decoded_resource Pointer to data offset in mobipocket record.
 @param[in,out] decoded_size Decoded resource data size
 @param[in,out] part MOBIPart structure containing resource, decoded part type will be set in the structure
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_decode_audio_resource(unsigned char **decoded_resource, size_t *decoded_size, MOBIPart *part) {
    if (part->size < MEDIA_HEADER_LEN) {
        debug_print("Audio resource record too short (%zu)\n", part->size);
        return MOBI_DATA_CORRUPT;
    }
    MOBIBuffer *buf = mobi_buffer_init_null(part->data, part->size);
    if (buf == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    char magic[5];
    mobi_buffer_getstring(magic, buf, 4);
    if (strncmp(magic, AUDI_MAGIC, 4) != 0) {
        debug_print("Wrong magic for audio resource: %s\n", magic);
        mobi_buffer_free_null(buf);
        return MOBI_DATA_CORRUPT;
    }
    uint32_t offset = mobi_buffer_get32(buf);
    mobi_buffer_setpos(buf, offset);
    *decoded_size = buf->maxlen - buf->offset;
    *decoded_resource = mobi_buffer_getpointer(buf, *decoded_size);
    mobi_buffer_free_null(buf);
    return MOBI_SUCCESS;
}

/**
 @brief Replace part data with decoded video data
 
 @param[in,out] part MOBIPart structure containing font resource, decoded part type will be set in the structure
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_add_video_resource(MOBIPart *part) {
    unsigned char *data = NULL;
    size_t size = 0;
    MOBI_RET ret = mobi_decode_video_resource(&data, &size, part);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    part->data = data;
    part->size = size;
    part->type = T_MPG; /* FIXME: other types? */

    return MOBI_SUCCESS;
}

/**
 @brief Decode video resource
 
 @param[in,out] decoded_resource Pointer to data offset in mobipocket record.
 @param[in,out] decoded_size Decoded resource data size
 @param[in,out] part MOBIPart structure containing resource, decoded part type will be set in the structure
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_decode_video_resource(unsigned char **decoded_resource, size_t *decoded_size, MOBIPart *part) {
    if (part->size < MEDIA_HEADER_LEN) {
        debug_print("Video resource record too short (%zu)\n", part->size);
        return MOBI_DATA_CORRUPT;
    }
    MOBIBuffer *buf = mobi_buffer_init_null(part->data, part->size);
    if (buf == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    char magic[5];
    mobi_buffer_getstring(magic, buf, 4);
    if (strncmp(magic, VIDE_MAGIC, 4) != 0) {
        debug_print("Wrong magic for video resource: %s\n", magic);
        mobi_buffer_free_null(buf);
        return MOBI_DATA_CORRUPT;
    }
    uint32_t offset = mobi_buffer_get32(buf);
    /* offset is always(?) 12, next four bytes are unknown */
    mobi_buffer_setpos(buf, offset);
    *decoded_size = buf->maxlen - buf->offset;
    *decoded_resource = mobi_buffer_getpointer(buf, *decoded_size);
    mobi_buffer_free_null(buf);
    return MOBI_SUCCESS;
}

/**
 @brief Get embedded source archive
 
 Some mobi creator software store original conversion source as a zip archive.
 The function may return MOBI_SUCCESS even if the data was not found,
 so it is neccessary to check whether returned data pointer is not NULL.
 
 @param[in,out] data Pointer to data offset in pdb record.
 @param[in,out] size Pointer to data size
 @param[in] m MOBIData structure
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_get_embedded_source(unsigned char **data, size_t *size, const MOBIData *m) {
    *data = NULL;
    *size = 0;
    if (m == NULL) {
        return MOBI_INIT_FAILED;
    }
    MOBIMobiHeader *header = m->mh;
    if (mobi_is_hybrid(m) && m->use_kf8 && m->next) {
        /* SRCS index is in KF7 header */
        header = m->next->mh;
    }
    if (header == NULL || header->srcs_index == NULL || header->srcs_count == NULL ||
        *header->srcs_index == MOBI_NOTSET || *header->srcs_count == 0) {
        return MOBI_SUCCESS;
    }
    uint32_t index = *header->srcs_index;
    
    const MOBIPdbRecord *srcs_record = mobi_get_record_by_seqnumber(m, index);
    if (srcs_record == NULL) {
        return MOBI_SUCCESS;
    }
    const size_t archive_offset = 16;
    
    if (srcs_record->size <= archive_offset) {
        debug_print("Wrong size of SRCS resource: %zu\n", srcs_record->size);
        return MOBI_DATA_CORRUPT;
    }

    if (memcmp(srcs_record->data, SRCS_MAGIC, 4) != 0) {
        debug_print("Wrong magic for SRCS resource: %c%c%c%c\n",
                    srcs_record->data[0], srcs_record->data[1], srcs_record->data[2], srcs_record->data[3]);
        return MOBI_DATA_CORRUPT;
    }
    
    *data = srcs_record->data + archive_offset;
    *size = srcs_record->size - archive_offset;

    return MOBI_SUCCESS;
}

/**
 @brief Get embedded conversion log
 
 Some mobi creator software store original conversion log together with source archive.
 The function may return MOBI_SUCCESS even if the data was not found,
 so it is neccessary to check whether returned data pointer is not NULL.
 
 @param[in,out] data Pointer to data offset in pdb record.
 @param[in,out] size Pointer to data size
 @param[in] m MOBIData structure
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_get_embedded_log(unsigned char **data, size_t *size, const MOBIData *m) {
    *data = NULL;
    *size = 0;
    if (m == NULL) {
        return MOBI_INIT_FAILED;
    }
    MOBIMobiHeader *header = m->mh;
    if (mobi_is_hybrid(m) && m->use_kf8 && m->next) {
        /* SRCS index is in KF7 header */
        header = m->next->mh;
    }
    if (header == NULL || header->srcs_index == NULL || header->srcs_count == NULL ||
        *header->srcs_index == MOBI_NOTSET || *header->srcs_count < 2) {
        return MOBI_SUCCESS;
    }
    uint32_t index = *header->srcs_index + 1;
    
    const MOBIPdbRecord *srcs_record = mobi_get_record_by_seqnumber(m, index);
    if (srcs_record == NULL) {
        return MOBI_SUCCESS;
    }
    const size_t log_offset = 12;
    if (srcs_record->size <= log_offset) {
        debug_print("Wrong size of CMET resource: %zu\n", srcs_record->size);
        return MOBI_DATA_CORRUPT;
    }
    MOBIBuffer *buf = mobi_buffer_init_null(srcs_record->data, srcs_record->size);
    if (buf == NULL) {
        return MOBI_MALLOC_FAILED;
    }
    if (mobi_buffer_match_magic(buf, CMET_MAGIC) == false) {
        debug_print("%s\n", "Wrong magic for CMET resource");
        mobi_buffer_free_null(buf);
        return MOBI_DATA_CORRUPT;
    }
    mobi_buffer_setpos(buf, 8);
    uint32_t log_length = mobi_buffer_get32(buf);
    unsigned char *log_data = mobi_buffer_getpointer(buf, log_length);
    if (buf->error != MOBI_SUCCESS) {
        debug_print("CMET resource too short: %zu (log size: %u)\n", srcs_record->size, log_length);
        mobi_buffer_free_null(buf);
        return MOBI_DATA_CORRUPT;
    }
    
    *data = log_data;
    *size = log_length;
    
    mobi_buffer_free_null(buf);
    return MOBI_SUCCESS;
}

/**
 @brief Replace part data with decoded font data
 
 @param[in,out] part MOBIPart structure containing font resource, decoded part type will be set in the structure
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_add_font_resource(MOBIPart *part) {
    unsigned char *data = NULL;
    size_t size = 0;
    MOBI_RET ret = mobi_decode_font_resource(&data, &size, part);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    part->data = data;
    part->size = size;
    part->type = mobi_determine_font_type(data, size);
    /* FIXME: mark unknown font types as ttf (shouldn't happen).
       This will allow proper font resource deallocation. */
    if (part->type == T_UNKNOWN) { part->type = T_TTF; }
    return MOBI_SUCCESS;
}

/**
 @brief Deobfuscator and decompressor for font resources
 
 @param[in,out] decoded_font Pointer to memory to write to. Will be allocated. Must be freed by caller
 @param[in,out] decoded_size Decoded font data size
 @param[in,out] part MOBIPart structure containing font resource, decoded part type will be set in the structure
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_decode_font_resource(unsigned char **decoded_font, size_t *decoded_size, MOBIPart *part) {
    if (part->size < FONT_HEADER_LEN) {
        debug_print("Font resource record too short (%zu)\n", part->size);
        return MOBI_DATA_CORRUPT;
    }
    MOBIBuffer *buf = mobi_buffer_init(part->size);
    if (buf == NULL) {
        debug_print("Memory allocation failed%s", "\n");
        return MOBI_MALLOC_FAILED;
    }
    memcpy(buf->data, part->data, part->size);
    struct header {
        char magic[5];
        uint32_t decoded_size;
        uint32_t flags;
        uint32_t data_offset;
        uint32_t xor_key_len;
        uint32_t xor_data_off;
    };
    struct header h;
    mobi_buffer_getstring(h.magic, buf, 4);
    if (strncmp(h.magic, FONT_MAGIC, 4) != 0) {
        debug_print("Wrong magic for font resource: %s\n", h.magic);
        mobi_buffer_free(buf);
        return MOBI_DATA_CORRUPT;
    }
    h.decoded_size = mobi_buffer_get32(buf);
    if (h.decoded_size == 0 || h.decoded_size > FONT_SIZEMAX) {
        debug_print("Invalid declared font resource size: %u\n", h.decoded_size);
        mobi_buffer_free(buf);
        return MOBI_DATA_CORRUPT;
    }
    h.flags = mobi_buffer_get32(buf);
    h.data_offset = mobi_buffer_get32(buf);
    h.xor_key_len = mobi_buffer_get32(buf);
    h.xor_data_off = mobi_buffer_get32(buf);
    const uint32_t zlib_flag = 1; /* bit 0 */
    const uint32_t xor_flag = 2; /* bit 1 */
    if (h.flags & xor_flag && h.xor_key_len > 0) {
        /* deobfuscate */
        mobi_buffer_setpos(buf, h.data_offset);
        const unsigned char *xor_key = buf->data + h.xor_data_off;
        size_t i = 0;
        const size_t xor_limit = h.xor_key_len * 52;
        while (buf->offset < buf->maxlen && i < xor_limit) {
            buf->data[buf->offset++] ^= xor_key[i % h.xor_key_len];
            i++;
        }
    }
    mobi_buffer_setpos(buf, h.data_offset);
    *decoded_size = h.decoded_size;
    *decoded_font = malloc(h.decoded_size);
    if (*decoded_font == NULL) {
        mobi_buffer_free(buf);
        debug_print("%s", "Memory allocation failed\n");
        return MOBI_MALLOC_FAILED;
    }
    const unsigned char *encoded_font = buf->data + buf->offset;
    const unsigned long encoded_size = buf->maxlen - buf->offset;
    if (h.flags & zlib_flag) {
        /* unpack */
        int ret = m_uncompress(*decoded_font, (unsigned long *) decoded_size, encoded_font, encoded_size);
        if (ret != M_OK) {
            mobi_buffer_free(buf);
            free(*decoded_font);
            debug_print("%s", "Font resource decompression failed\n");
            return MOBI_DATA_CORRUPT;
        }
        if (*decoded_size != h.decoded_size) {
            mobi_buffer_free(buf);
            free(*decoded_font);
            debug_print("Decompressed font size (%zu) differs from declared (%i)\n", *decoded_size, h.decoded_size);
            return MOBI_DATA_CORRUPT;
        }
    } else {
        if (*decoded_size < encoded_size) {
            mobi_buffer_free(buf);
            free(*decoded_font);
            debug_print("Font size in record (%lu) larger then declared (%zu)\n", encoded_size, *decoded_size);
            return MOBI_DATA_CORRUPT;
        }
        memcpy(*decoded_font, encoded_font, encoded_size);
    }

    mobi_buffer_free(buf);
    return MOBI_SUCCESS;
}

/**
 @brief Get resource type (image, font) by checking its magic header
 
 @param[in] record MOBIPdbRecord structure containing unknown record type
 @return MOBIFiletype file type, T_UNKNOWN if not determined, T_BREAK if end of records mark found
 */
MOBIFiletype mobi_determine_resource_type(const MOBIPdbRecord *record) {
    /* Kindle supports GIF, BMP, JPG, PNG, SVG images. */
    /* GIF: 47 49 46 38 37 61 (GIF87a), 47 49 46 38 39 61 (GIF89a) */
    /* BMP: 42 4D (BM) + 4 byte file length le */
    /* JPG: FF D8 FF (header) + FF D9 (trailer) */
    /* PNG: 89 50 4E 47 0D 0A 1A 0A */
    /* SVG is XML-based format, so stored in flow parts */
    /* FONT: must be decoded */
    if (record->size < 4) {
        return T_UNKNOWN;
    }
    const unsigned char jpg_magic[] = "\xff\xd8\xff";
    const unsigned char gif_magic[] = "\x47\x49\x46\x38";
    const unsigned char png_magic[] = "\x89\x50\x4e\x47\x0d\x0a\x1a\x0a";
    const unsigned char bmp_magic[] = "\x42\x4d";
    const unsigned char font_magic[] = FONT_MAGIC;
    const unsigned char audio_magic[] = AUDI_MAGIC;
    const unsigned char video_magic[] = VIDE_MAGIC;
    const unsigned char boundary_magic[] = BOUNDARY_MAGIC;
    const unsigned char eof_magic[] = EOF_MAGIC;
    if (memcmp(record->data, jpg_magic, 3) == 0) {
        return T_JPG;
    } else if (memcmp(record->data, gif_magic, 4) == 0) {
        return T_GIF;
    } else if (record->size >= 8 && memcmp(record->data, png_magic, 8) == 0) {
        return T_PNG;
    } else if (memcmp(record->data, font_magic, 4) == 0) {
        return T_FONT;
    } else if (record->size >= 8 && memcmp(record->data, boundary_magic, 8) == 0) {
        return T_BREAK;
    } else if (memcmp(record->data, eof_magic, 4) == 0) {
        return T_BREAK;
    } else if (record->size >= 6 && memcmp(record->data, bmp_magic, 2) == 0) {
        const size_t bmp_size = mobi_get32le(&record->data[2]);
        if (record->size == bmp_size) {
            return T_BMP;
        }
    } else if (memcmp(record->data, audio_magic, 4) == 0) {
        return T_AUDIO;
    } else if (memcmp(record->data, video_magic, 4) == 0) {
        return T_VIDEO;
    }
    return T_UNKNOWN;
}

/**
 @brief Check if loaded MOBI data is KF7/KF8 hybrid file
 
 @param[in] m MOBIData structure with loaded Record(s) 0 headers
 @return true or false
 */
bool mobi_is_hybrid(const MOBIData *m) {
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return false;
    }
    if (m->kf8_boundary_offset != MOBI_NOTSET) {
        return true;
    }
    return false;
}

/**
 @brief Check if loaded document is MOBI/BOOK Mobipocket format
 
 @param[in] m MOBIData structure with loaded Record(s) 0 headers
 @return true or false
 */
bool mobi_is_mobipocket(const MOBIData *m) {
    if (m == NULL || m->ph == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return false;
    }
    if (strcmp(m->ph->type, "BOOK") == 0 &&
        strcmp(m->ph->creator, "MOBI") == 0) {
        return true;
    }
    return false;
}

/**
 @brief Check if loaded document is dictionary
 
 @param[in] m MOBIData structure with loaded mobi header
 @return true or false
 */
bool mobi_is_dictionary(const MOBIData *m) {
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return false;
    }
    /* FIXME: works only for old non-KF8 formats */
    if (mobi_get_fileversion(m) < 8 && mobi_exists_orth(m)) {
        debug_print("%s", "Dictionary detected\n");
        return true;
    }
    return false;
}

/**
 @brief Check if loaded document is encrypted
 
 @param[in] m MOBIData structure with loaded Record(s) 0 headers
 @return true or false
 */
bool mobi_is_encrypted(const MOBIData *m) {
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return false;
    }
    if (mobi_is_mobipocket(m) && m->rh &&
        (m->rh->encryption_type == RECORD0_OLD_ENCRYPTION ||
         m->rh->encryption_type == RECORD0_MOBI_ENCRYPTION)) {
        return true;
    }
    return false;
}

/**
 @brief Check if loaded document is Print Replica type

 @param[in] m MOBIData structure with loaded Record(s) 0 headers
 @return true or false
 */
bool mobi_is_replica(const MOBIData *m) {
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return false;
    }
    if (m->rec && m->rh && m->rh->compression_type == RECORD0_NO_COMPRESSION) {
        MOBIPdbRecord *rec = m->rec->next;
        if (rec && rec->size >= sizeof(REPLICA_MAGIC)) {
            return memcmp(rec->data, REPLICA_MAGIC, sizeof(REPLICA_MAGIC) - 1) == 0;
        }
    }
    return false;
}

/**
 @brief Get mobi file version
 
 @param[in] m MOBIData structure with loaded Record(s) 0 headers
 @return MOBI document version, 1 if ancient version (no MOBI header) or MOBI_NOTSET if error
 */
size_t mobi_get_fileversion(const MOBIData *m) {
    size_t version = 1;
    if (m == NULL || m->ph == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_NOTSET;
    }
    if (strcmp(m->ph->type, "BOOK") == 0 && strcmp(m->ph->creator, "MOBI") == 0) {
        if (m->mh && m->mh->header_length) {
            uint32_t header_length = *m->mh->header_length;
            if (header_length < MOBI_HEADER_V2_SIZE) {
                version = 2;
            } else if (m->mh->version && *m->mh->version > 1) {
                if ((*m->mh->version > 2 && header_length < MOBI_HEADER_V3_SIZE)
                    || (*m->mh->version > 3 && header_length < MOBI_HEADER_V4_SIZE)
                    ||(*m->mh->version > 5 && header_length < MOBI_HEADER_V5_SIZE)) {
                    return MOBI_NOTSET;
                }
                version = *m->mh->version;
            }
        }
    }
    return version;
}

/**
 @brief Is file version 8 or above
 
 @param[in] m MOBIData structure with loaded Record(s) 0 headers
 @return True if file version is 8 or greater
 */
bool mobi_is_kf8(const MOBIData *m) {
    const size_t version = mobi_get_fileversion(m);
    if (version != MOBI_NOTSET && version >= 8) {
        return true;
    }
    return false;
}

/**
 @brief Is file version 8 or above
 
 @param[in] rawml MOBIRawml structure with parsed document
 @return True if file version is 8 or greater
 */
bool mobi_is_rawml_kf8(const MOBIRawml *rawml) {
    if (rawml && rawml->version != MOBI_NOTSET && rawml->version >= 8) {
        return true;
    }
    return false;
}

/**
 @brief Get maximal size of uncompressed text record
 
 @param[in] m MOBIData structure with loaded Record(s) 0 headers
 @return Size of text or MOBI_NOTSET if error
 */
uint16_t mobi_get_textrecord_maxsize(const MOBIData *m) {
    uint16_t max_record_size = RECORD0_TEXT_SIZE_MAX;
    if (m && m->rh) {
        if (m->rh->text_record_size > RECORD0_TEXT_SIZE_MAX) {
            max_record_size = m->rh->text_record_size;
        }
        if (mobi_exists_mobiheader(m) && mobi_get_fileversion(m) <= 3) {
            /* workaround for some old files with records larger than declared record size */
            size_t text_length = (size_t) max_record_size * m->rh->text_record_count;
            if (text_length <= RAWTEXT_SIZEMAX && m->rh->text_length > text_length) {
                max_record_size = RECORD0_TEXT_SIZE_MAX * 2;
            }
        }
    }
    return max_record_size;
}

/**
 @brief Get maximal size of all uncompressed text records
 
 @param[in] m MOBIData structure with loaded Record(s) 0 headers
 @return Size of text or MOBI_NOTSET if error
 */
size_t mobi_get_text_maxsize(const MOBIData *m) {
    if (m && m->rh) {
        /* FIXME: is it safe to use data from Record 0 header? */
        if (m->rh->text_record_count > 0) {
            uint16_t max_record_size = mobi_get_textrecord_maxsize(m);
            size_t maxsize = (size_t) m->rh->text_record_count * (size_t) max_record_size;
            if (mobi_exists_mobiheader(m) && mobi_get_fileversion(m) <= 3) {
                /* workaround for some old files with records larger than declared record size */
                if (m->rh->text_length > maxsize) {
                    maxsize = m->rh->text_length;
                }
            }
            if (maxsize > RAWTEXT_SIZEMAX) {
                debug_print("Raw text too large (%zu)\n", maxsize);
                return MOBI_NOTSET;
            }
            return maxsize;
        }
    }
    return MOBI_NOTSET;
}

/**
 @brief Get sequential number of first resource record (image/font etc)
 
 @param[in] m MOBIData structure with loaded Record(s) 0 headers
 @return Record number or MOBI_NOTSET if not set
 */
size_t mobi_get_first_resource_record(const MOBIData *m) {
    /* is it hybrid file? */
    if (mobi_is_hybrid(m) && m->use_kf8) {
        /* get first image index from KF7 mobi header */
        if (m->next->mh->image_index) {
            return *m->next->mh->image_index;
        }
    }
    /* try to get it from currently set mobi header */
    if (m->mh && m->mh->image_index) {
        return *m->mh->image_index;
    }
    return MOBI_NOTSET;
}


/**
 @brief Calculate exponentiation for unsigned base and exponent
 
 @param[in] base Base
 @param[in] exp Exponent
 @return Result of base raised by the exponent exp
 */
size_t mobi_pow(unsigned base, unsigned exp) {
    size_t result = 1;
    while(exp) {
        if (exp & 1) {
            result *= base;
        }
        exp >>= 1;
        base *= base;
    }
    return result;
}

/**
 @brief Decode positive number from base 32 to base 10.
 
 Base 32 characters must be upper case.
 Maximal supported value is VVVVVV.
 
 @param[in,out] decoded Base 10 output number
 @param[in] encoded Base 32 input number
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_base32_decode(uint32_t *decoded, const char *encoded) {
    if (!encoded || !decoded) {
        debug_print("Error, null parameter (decoded: %p, encoded: %p)\n", (void *) decoded, (void *) encoded)
        return MOBI_PARAM_ERR;
    }
    /* strip leading zeroes */
    while (*encoded == '0') {
        encoded++;
    }
    size_t encoded_length = strlen(encoded);
    /* Let's limit input to 6 chars. VVVVVV(32) is 0x3FFFFFFF */
    if (encoded_length > 6) {
        debug_print("Base 32 number too big: %s\n", encoded);
        return MOBI_PARAM_ERR;
    }
    const unsigned char *c =  (unsigned char *) encoded;
    unsigned len = (unsigned) encoded_length;
    const unsigned base = 32;
    *decoded = 0;
    unsigned value;
    while (*c) {
        /* FIXME: not portable, should we care? */
        if (*c >= 'A' && *c <= 'V') {
            value = *c - 'A' + 10;
        }
        else if (*c >= '0' && *c <= '9') {
            value = *c - '0';
        }
        else {
            debug_print("Illegal character: \"%c\"\n", *c);
            return MOBI_DATA_CORRUPT;
        }
        *decoded += value * mobi_pow(base, --len);
        c++;
    }
    return MOBI_SUCCESS;
}


/**
 @brief Get offset of KF8 Boundary for KF7/KF8 hybrid file cached in MOBIData structure
 
 @param[in] m MOBIData structure
 @return KF8 Boundary sequential number or zero if not found
 */
size_t mobi_get_kf8offset(const MOBIData *m) {
    /* check if we want to parse KF8 part of joint file */
    if (m->use_kf8 && m->kf8_boundary_offset != MOBI_NOTSET) {
        return m->kf8_boundary_offset + 1;
    }
    return 0;
}

/**
 @brief Get sequential number of KF8 Boundary record for KF7/KF8 hybrid file
 
 This function gets KF8 boundary offset from EXTH header
 
 @param[in] m MOBIData structure
 @return KF8 Boundary record sequential number or MOBI_NOTSET if not found
 */
size_t mobi_get_kf8boundary_seqnumber(const MOBIData *m) {
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_NOTSET;
    }
    const MOBIExthHeader *exth_tag = mobi_get_exthrecord_by_tag(m, EXTH_KF8BOUNDARY);
    if (exth_tag != NULL) {
        uint32_t rec_number = mobi_decode_exthvalue(exth_tag->data, exth_tag->size);
        rec_number--;
        const MOBIPdbRecord *record = mobi_get_record_by_seqnumber(m, rec_number);
        if (record && record->size >= sizeof(BOUNDARY_MAGIC) - 1) {
            if (memcmp(record->data, BOUNDARY_MAGIC, sizeof(BOUNDARY_MAGIC) - 1) == 0) {
                return rec_number;
            }
        }
    }
    return MOBI_NOTSET;
}

/**
 @brief Get size of serialized exth record including padding
 
 @param[in] m MOBIData structure
 @return Size of exth record, zero on failure
 */
uint32_t mobi_get_exthsize(const MOBIData *m) {
    if (m == NULL || m->eh == NULL) {
        return 0;
    }
    size_t size = 0;
    MOBIExthHeader *curr = m->eh;
    while (curr) {
        size += curr->size + 8;
        curr = curr->next;
    }
    if (size > 0) {
        /* add header size */
        size += 12;
        /* add padding */
        size += size % 4;
    }
    if (size > UINT32_MAX) {
        return 0;
    } else {
        return (uint32_t) size;
    }
}

/**
 @brief Get count of palm database records
 
 @param[in] m MOBIData structure
 @return Count of records, zero on failure
 */
uint16_t mobi_get_records_count(const MOBIData *m) {
    size_t count = 0;
    if (m->rec) {
        MOBIPdbRecord *curr = m->rec;
        while (curr) {
            count++;
            curr = curr->next;
        }
    }
    if (count > UINT16_MAX) {
        return 0;
    } else {
        return (uint16_t) count;
    }
}

/**
 @brief Remove null characters from char buffer
 
 @param[in,out] buffer Character buffer
 @param[in,out] len Size of buffer, will be updated with new length
 */
void mobi_remove_zeros(unsigned char *buffer, size_t *len) {
    size_t length = *len;
    unsigned char *end = buffer + length;
    unsigned char *buf = memchr(buffer, 0, length);
    if (buf == NULL) {
        return;
    }
    buf++;
    size_t distance = 1;
    while (buf < end) {
        if (*buf) {
            *(buf - distance) = *buf;
        } else {
            distance++;
        }
        buf++;
    }
    *len -= distance;
}

/**
 @brief Loader will parse KF7 part of hybrid file
 
 @param[in,out] m MOBIData structure
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_parse_kf7(MOBIData *m) {
    if (m == NULL) {
        return MOBI_INIT_FAILED;
    }
    m->use_kf8 = false;
    return MOBI_SUCCESS;
}

/**
 @brief Loader will parse KF8 part of hybrid file
 
 This is the default option.
 
 @param[in,out] m MOBIData structure
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_parse_kf8(MOBIData *m) {
    if (m == NULL) {
        return MOBI_INIT_FAILED;
    }
    m->use_kf8 = true;
    return MOBI_SUCCESS;
}

/**
 @brief Swap KF7 and KF8 MOBIData structures in a hybrid file
 
 MOBIData structures form a circular linked list in case of hybrid files.
 By default KF8 structure is first one in the list.
 This function puts KF7 structure on the first place, so that it starts to be used by default.
 
 @param[in,out] m MOBIData structure
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_swap_mobidata(MOBIData *m) {
    MOBIData *tmp = malloc(sizeof(MOBIData));
    if (tmp == NULL) {
        debug_print("%s", "Memory allocation failed while swaping data\n");
        return MOBI_MALLOC_FAILED;
    }
    tmp->rh = m->rh;
    tmp->mh = m->mh;
    tmp->eh = m->eh;
    m->rh = m->next->rh;
    m->mh = m->next->mh;
    m->eh = m->next->eh;
    m->next->rh = tmp->rh;
    m->next->mh = tmp->mh;
    m->next->eh = tmp->eh;
    free(tmp);
    tmp = NULL;
    return MOBI_SUCCESS;
}

/**
 @brief Store PID for encryption in MOBIData stucture.
 PID will be calculated from device serial number.
 
 @param[in,out] m MOBIData structure with raw data and metadata
 @param[in] serial Device serial number
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_drm_setkey_serial(MOBIData *m, const char *serial) {
#ifdef USE_ENCRYPTION
    return mobi_drm_setkey_serial_internal(m, serial);
#else
    UNUSED(m);
    UNUSED(serial);
    debug_print("Libmobi compiled without encryption support%s", "\n");
    return MOBI_DRM_UNSUPPORTED;
#endif
}

/**
 @brief Store PID for encryption in MOBIData stucture
 
 @param[in,out] m MOBIData structure with raw data and metadata
 @param[in] pid PID
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_drm_setkey(MOBIData *m, const char *pid) {
#ifdef USE_ENCRYPTION
    return mobi_drm_setkey_internal(m, pid);
#else
    UNUSED(m);
    UNUSED(pid);
    debug_print("Libmobi compiled without encryption support%s", "\n");
    return MOBI_DRM_UNSUPPORTED;
#endif
}

/**
 @brief Remove PID stored for encryption from MOBIData structure
 
 @param[in,out] m MOBIData structure with raw data and metadata
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_drm_delkey(MOBIData *m) {
#ifdef USE_ENCRYPTION
    return mobi_drm_delkey_internal(m);
#else
    UNUSED(m);
    debug_print("Libmobi compiled without encryption support%s", "\n");
    return MOBI_DRM_UNSUPPORTED;
#endif
}

/**
 @brief Convert char buffer to 32-bit unsigned integer big endian
 
 @param[in] buf Input buffer
 @return Converted value
 */
uint32_t mobi_get32be(const unsigned char buf[4]) {
    uint32_t val = (uint32_t) buf[0] << 24;
    val |= (uint32_t) buf[1] << 16;
    val |= (uint32_t) buf[2] << 8;
    val |= (uint32_t) buf[3];
    return val;
}

/**
 @brief Convert char buffer to 32-bit unsigned integer little endian
 
 @param[in] buf Input buffer
 @return Converted value
 */
uint32_t mobi_get32le(const unsigned char buf[4]) {
    uint32_t val = (uint32_t) buf[0];
    val |= (uint32_t) buf[1] << 8;
    val |= (uint32_t) buf[2] << 16;
    val |= (uint32_t) buf[3] << 24;
    return val;
}
