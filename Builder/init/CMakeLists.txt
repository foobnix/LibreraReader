cmake_minimum_required(VERSION 3.22.1)

set(CMAKE_VERBOSE_MAKEFILE ON)

project("mupdf")

# --- MUJS ---

file(GLOB src
        "thirdparty/mujs/one.c"
)

# --- ZLIB ---
#add_definitions(-DHAVE_UNISTD_H)
#add_definitions(-DHAVE_STDARG_H)

file(GLOB src
        "thirdparty/zlib/adler32.c"
        "thirdparty/zlib/compress.c"
        "thirdparty/zlib/crc32.c"
        "thirdparty/zlib/deflate.c"
        "thirdparty/zlib/inffast.c"
        "thirdparty/zlib/inflate.c"
        "thirdparty/zlib/inftrees.c"
        "thirdparty/zlib/trees.c"
        "thirdparty/zlib/uncompr.c"
        "thirdparty/zlib/zutil.c"
)

# --- JBIG2DEC --

file(GLOB src
        "thirdparty/jbig2dec/jbig2.c"
        "thirdparty/jbig2dec/jbig2_arith.c"
        "thirdparty/jbig2dec/jbig2_arith_iaid.c"
        "thirdparty/jbig2dec/jbig2_arith_int.c"
        "thirdparty/jbig2dec/jbig2_generic.c"
        "thirdparty/jbig2dec/jbig2_halftone.c"
        "thirdparty/jbig2dec/jbig2_huffman.c"
        "thirdparty/jbig2dec/jbig2_hufftab.c"
        "thirdparty/jbig2dec/jbig2_image.c"
        "thirdparty/jbig2dec/jbig2_mmr.c"
        "thirdparty/jbig2dec/jbig2_page.c"
        "thirdparty/jbig2dec/jbig2_refinement.c"
        "thirdparty/jbig2dec/jbig2_segment.c"
        "thirdparty/jbig2dec/jbig2_symbol_dict.c"
        "thirdparty/jbig2dec/jbig2_text.c"
)

# --- OPENJPEG ---

add_library(mupdf SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        ${src})

set(zip_SOURCES
        "thirdparty/openjpeg/src/lib/openjp2/bio.c"
        "thirdparty/openjpeg/src/lib/openjp2/cio.c"
        "thirdparty/openjpeg/src/lib/openjp2/dwt.c"
        "thirdparty/openjpeg/src/lib/openjp2/event.c"
        "thirdparty/openjpeg/src/lib/openjp2/function_list.c"
        "thirdparty/openjpeg/src/lib/openjp2/ht_dec.c"
        "thirdparty/openjpeg/src/lib/openjp2/image.c"
        "thirdparty/openjpeg/src/lib/openjp2/invert.c"
        "thirdparty/openjpeg/src/lib/openjp2/j2k.c"
        "thirdparty/openjpeg/src/lib/openjp2/jp2.c"
        "thirdparty/openjpeg/src/lib/openjp2/mct.c"
        "thirdparty/openjpeg/src/lib/openjp2/mqc.c"
        "thirdparty/openjpeg/src/lib/openjp2/openjpeg.c"
        "thirdparty/openjpeg/src/lib/openjp2/pi.c"
        "thirdparty/openjpeg/src/lib/openjp2/sparse_array.c"
        "thirdparty/openjpeg/src/lib/openjp2/t1.c"
        "thirdparty/openjpeg/src/lib/openjp2/t2.c"
        "thirdparty/openjpeg/src/lib/openjp2/tcd.c"
        "thirdparty/openjpeg/src/lib/openjp2/tgt.c"
        "thirdparty/openjpeg/src/lib/openjp2/thread.c"
)
add_library(zip OBJECT ${zip_SOURCES})
target_compile_definitions(zip PUBLIC
        MUTEX_pthread=0
        OPJ_HAVE_INTTYPES_H
        OPJ_HAVE_STDINT_H
        OPJ_STATIC)

#target_link_libraries(mupdf PUBLIC zip)

# --- FREETYPE2 ---

set(FREETYPE_SRC
        "thirdparty/freetype/src/base/ftbase.c"
        "thirdparty/freetype/src/base/ftbbox.c"
        "thirdparty/freetype/src/base/ftbitmap.c"
        "thirdparty/freetype/src/base/ftdebug.c"
        "thirdparty/freetype/src/base/ftfstype.c"
        "thirdparty/freetype/src/base/ftgasp.c"
        "thirdparty/freetype/src/base/ftglyph.c"
        "thirdparty/freetype/src/base/ftinit.c"
        "thirdparty/freetype/src/base/ftstroke.c"
        "thirdparty/freetype/src/base/ftsynth.c"
        "thirdparty/freetype/src/base/ftsystem.c"
        "thirdparty/freetype/src/base/fttype1.c"
        "thirdparty/freetype/src/cff/cff.c"
        "thirdparty/freetype/src/cid/type1cid.c"
        "thirdparty/freetype/src/psaux/psaux.c"
        "thirdparty/freetype/src/pshinter/pshinter.c"
        "thirdparty/freetype/src/psnames/psnames.c"
        "thirdparty/freetype/src/raster/raster.c"
        "thirdparty/freetype/src/sfnt/sfnt.c"
        "thirdparty/freetype/src/smooth/smooth.c"
        "thirdparty/freetype/src/truetype/truetype.c"
        "thirdparty/freetype/src/type1/type1.c"
)


add_library(FREETYPE OBJECT ${FREETYPE_SRC})
target_include_directories(FREETYPE PUBLIC
        "thirdparty/freetype/include"
        "scripts/freetype"
)
target_compile_definitions(FREETYPE PUBLIC
        FT_CONFIG_MODULES_H=\"slimftmodules.h\"
        FT_CONFIG_OPTIONS_H=\"slimftoptions.h\"
        FT2_BUILD_LIBRARY
)

target_link_libraries(mupdf PUBLIC FREETYPE)

# --- GUMBO ---
set(GUMBO_SRC
        "thirdparty/gumbo-parser/src/attribute.c"
        "thirdparty/gumbo-parser/src/char_ref.c"
        "thirdparty/gumbo-parser/src/error.c"
        "thirdparty/gumbo-parser/src/parser.c"
        "thirdparty/gumbo-parser/src/string_buffer.c"
        "thirdparty/gumbo-parser/src/string_piece.c"
        "thirdparty/gumbo-parser/src/tag.c"
        "thirdparty/gumbo-parser/src/tokenizer.c"
        "thirdparty/gumbo-parser/src/utf8.c"
        "thirdparty/gumbo-parser/src/util.c"
        "thirdparty/gumbo-parser/src/vector.c"
)
add_library(GUMBO OBJECT ${GUMBO_SRC})
target_link_libraries(mupdf PUBLIC GUMBO)

# --- HARFBUZZ ---
set(HARFBUZZ_SRC
        "thirdparty/freetype/src/base/ftbase.c"
        "thirdparty/freetype/src/base/ftbbox.c"
        "thirdparty/freetype/src/base/ftbitmap.c"
        "thirdparty/freetype/src/base/ftdebug.c"
        "thirdparty/freetype/src/base/ftfstype.c"
        "thirdparty/freetype/src/base/ftgasp.c"
        "thirdparty/freetype/src/base/ftglyph.c"
        "thirdparty/freetype/src/base/ftinit.c"
        "thirdparty/freetype/src/base/ftstroke.c"
        "thirdparty/freetype/src/base/ftsynth.c"
        "thirdparty/freetype/src/base/ftsystem.c"
        "thirdparty/freetype/src/base/fttype1.c"
        "thirdparty/freetype/src/cff/cff.c"
        "thirdparty/freetype/src/cid/type1cid.c"
        "thirdparty/freetype/src/psaux/psaux.c"
        "thirdparty/freetype/src/pshinter/pshinter.c"
        "thirdparty/freetype/src/psnames/psnames.c"
        "thirdparty/freetype/src/raster/raster.c"
        "thirdparty/freetype/src/sfnt/sfnt.c"
        "thirdparty/freetype/src/smooth/smooth.c"
        "thirdparty/freetype/src/truetype/truetype.c"
        "thirdparty/freetype/src/type1/type1.c"

)


add_library(HARFBUZZ OBJECT ${HARFBUZZ_SRC})

target_include_directories(HARFBUZZ PUBLIC
        "thirdparty/freetype/include"
        "scripts/freetype"
)

target_compile_definitions(HARFBUZZ PUBLIC
        HAVE_FALLBACK=1
        HAVE_FREETYPE
        HAVE_OT
        HAVE_ROUND
        HAVE_UCDN
        HB_NO_MT
        HB_NO_PRAGMA_GCC_DIAGNOSTIC
        hb_malloc_impl=fz_hb_malloc
        hb_calloc_impl=fz_hb_calloc
        hb_free_impl=fz_hb_free
        hb_realloc_impl=fz_hb_realloc
        -w
)
target_compile_options(HARFBUZZ PUBLIC
        -w
        -fno-exceptions
        -fno-rtti
        -fno-threadsafe-statics
        -fvisibility-inlines-hidden
        -ferror-limit=10
)

target_link_libraries(mupdf PUBLIC HARFBUZZ)
