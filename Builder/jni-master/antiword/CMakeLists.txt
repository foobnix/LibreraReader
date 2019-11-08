IF(NOT CMAKE_BUILD_TYPE)
  SET(CMAKE_BUILD_TYPE Release CACHE STRING
    "Choose the type of build, options are: None Debug Release RelWithDebInfo MinSizeRel."
    FORCE)
ENDIF(NOT CMAKE_BUILD_TYPE)

IF (${CMAKE_BUILD_TYPE} STREQUAL Release)
    ADD_DEFINITIONS( -DNDEBUG=1 )
ELSE()
    ADD_DEFINITIONS( -DDEBUG=1 )
ENDIF(${CMAKE_BUILD_TYPE} STREQUAL Release)

INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR})

SET (ANTIWORD_SOURCES 
#    main_u.c 
    asc85enc.c blocklist.c chartrans.c datalist.c depot.c
    dib2eps.c doclist.c fail.c finddata.c findtext.c fontlist.c
    fonts.c fonts_u.c hdrftrlist.c imgexam.c 
#    imgtrans.c jpeg2eps.c
    listlist.c misc.c notes.c options.c out2window.c pdf.c
    pictlist.c 
#    png2eps.c 
    prop0.c prop2.c prop6.c prop8.c
    properties.c propmod.c rowlist.c sectlist.c stylelist.c stylesheet.c
    summary.c tabstop.c unix.c utf8.c word2text.c worddos.c
    wordlib.c wordmac.c wordole.c wordwin.c xmalloc.c
)                                                          

IF (DEFINED CR3_ANTIWORD_PATCH)
    ADD_DEFINITIONS( -DANTIWORD_EXTERNAL_IO=1 )
ELSE ()
SET (ANTIWORD_SOURCES ${ANTIWORD_SOURCES}
#Output routines are implemented in CR
    output.c 
    xml.c
    text.c 
    fmt_text.c 
    postscript.c 
)
ENDIF (DEFINED CR3_ANTIWORD_PATCH)

if (WIN32)
SET (ANTIWORD_SOURCES ${ANTIWORD_SOURCES}
)
endif(WIN32)

ADD_LIBRARY(antiword STATIC ${ANTIWORD_SOURCES})

