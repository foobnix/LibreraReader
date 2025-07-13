cmake_minimum_required(VERSION 3.4.1)

add_library( lame

             SHARED

             src/main/cpp/lame.c
             src/main/cpp/util.c
             src/main/cpp/psymodel.c
             src/main/cpp/fft.c
             src/main/cpp/tables.c
             src/main/cpp/encoder.c
             src/main/cpp/newmdct.c
             src/main/cpp/quantize_pvt.c
             src/main/cpp/reservoir.c
             src/main/cpp/bitstream.c
             src/main/cpp/VbrTag.c
             src/main/cpp/version.c
             src/main/cpp/takehiro.c
             src/main/cpp/quantize.c
             src/main/cpp/vbrquantize.c
             src/main/cpp/gain_analysis.c
             src/main/cpp/id3tag.c
             src/main/cpp/set_get.c
             src/main/cpp/presets.c )

include_directories( src/main/cpp src/main/cpp/include )

add_definitions( -DHAVE_CONFIG_H -DSTDC_HEADERS -DHAVE_MEMCPY -DHAVE_STRCHR )

find_library( log-lib log )

target_link_libraries( lame ${log-lib} )
