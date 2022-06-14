## mobitool
    usage: /Users/baf/src/libmobi/tools/.libs/mobitool [-cdehimrstuvx7] [-o dir] [-p pid] [-P serial] filename
        without arguments prints document metadata and exits
        -c        dump cover
        -d        dump rawml text record
        -e        create EPUB file (with -s will dump EPUB source)
        -h        show this usage summary and exit
        -i        print detailed metadata
        -m        print records metadata
        -o dir    save output to dir folder
        -p pid    set pid for decryption
        -P serial set device serial for decryption
        -r        dump raw records
        -s        dump recreated source files
        -t        split hybrid file into two parts
        -u        show rusage
        -v        show version and exit
        -x        extract conversion source and log (if present)
        -7        parse KF7 part of hybrid file (by default KF8 part is parsed)

## mobimeta
    usage: mobimeta [-a | -s meta=value[,meta=value,...]] [-d meta[,meta,...]] [-p pid] [-P serial] [-hv] filein [fileout]
        without arguments prints document metadata and exits
        -a ?           list valid meta named keys
        -a meta=value  add metadata
        -d meta        delete metadata
        -s meta=value  set metadata
        -p pid         set pid for decryption
        -P serial      set device serial for decryption
        -h             show this usage summary and exit
        -v             show version and exit

## mobidrm
    usage: mobidrm [-d | -e] [-hv] [-p pid] [-f date] [-t date] [-s serial] [-o dir] filename
        without arguments prints document metadata and exits

        Decrypt options:
        -d        decrypt (required)
        -p pid    set decryption pid (may be specified multiple times)
        -s serial set device serial (may be specified multiple times)

        Encrypt options:
        -e        encrypt (required)
        -s serial set device serial (may be specified multiple times)
        -f date   set validity period from date (yyyy-mm-dd) when encrypting (inclusive)
        -t date   set validity period to date (yyyy-mm-dd) when encrypting (inclusive)

        Common options:
        -o dir    save output to dir folder
        -h        show this usage summary and exit
        -v        show version and exit
