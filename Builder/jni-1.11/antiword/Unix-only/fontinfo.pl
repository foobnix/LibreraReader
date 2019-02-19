#! /usr/bin/perl -w
#
# Generate the fontinformation tables for the required fonts (Linux version)
#

@charnames1 = (
"ellipsis", "trademark", "perthousand", "bullet",
"quoteleft", "quoteright", "guilsinglleft", "guilsinglright",
"quotedblleft", "quotedblright", "quotedblbase", "endash", "emdash",
"minus", "OE", "oe", "dagger", "daggerdbl", "fi", "fl",
"space", "exclamdown", "cent", "sterling", "currency",
"yen", "brokenbar", "section", "dieresis", "copyright",
"ordfeminine", "guillemotleft", "logicalnot", "hyphen", "registered",
"macron", "degree", "plusminus", "twosuperior", "threesuperior",
"acute", "mu", "paragraph", "periodcentered", "cedilla",
"onesuperior", "ordmasculine", "guillemotright", "onequarter",
"onehalf", "threequarters", "questiondown", "Agrave", "Aacute",
"Acircumflex", "Atilde", "Adieresis", "Aring", "AE", "Ccedilla",
"Egrave", "Eacute", "Ecircumflex", "Edieresis", "Igrave", "Iacute",
"Icircumflex", "Idieresis", "Eth", "Ntilde", "Ograve", "Oacute",
"Ocircumflex", "Otilde", "Odieresis", "multiply", "Oslash",
"Ugrave", "Uacute", "Ucircumflex", "Udieresis", "Yacute", "Thorn",
"germandbls", "agrave", "aacute", "acircumflex", "atilde",
"adieresis", "aring", "ae", "ccedilla", "egrave", "eacute",
"ecircumflex", "edieresis", "igrave", "iacute", "icircumflex",
"idieresis", "eth", "ntilde", "ograve", "oacute", "ocircumflex",
"otilde", "odieresis", "divide", "oslash", "ugrave", "uacute",
"ucircumflex", "udieresis", "yacute", "thorn", "ydieresis"
);

@charnames2 = (
"space", "Aogonek", "breve", "Lslash", "currency", "Lcaron",
"Sacute", "section", "dieresis", "Scaron", "Scedilla",
"Tcaron", "Zacute", "hyphen", "Zcaron", "Zdotaccent", "ring",
"aogonek", "ogonek", "lslash", "acute", "lcaron", "sacute",
"caron", "cedilla", "scaron", "scedilla", "tcaron",
"zacute", "hungarumlaut", "zcaron", "zdotaccent", "Racute",
"Aacute", "Acircumflex", "Abreve", "Adieresis", "Lacute",
"Cacute", "Ccedilla", "Ccaron", "Eacute", "Eogonek",
"Edieresis", "Ecaron", "Iacute", "Icircumflex", "Dcaron",
"Dslash", "Nacute", "Ncaron", "Oacute", "Ocircumflex",
"Ohungarumlaut", "Odieresis", "multiply", "Rcaron", "Uring",
"Uacute", "Uhungarumlaut", "Udieresis", "Yacute", "Tcommaaccent",
"germandbls", "racute", "aacute", "acircumflex", "abreve",
"adieresis", "lacute", "cacute", "ccedilla", "ccaron", "eacute",
"eogonek", "edieresis", "ecaron", "iacute", "icircumflex",
"dcaron", "dmacron", "nacute", "ncaron", "oacute", "ocircumflex",
"ohungarumlaut", "odieresis", "divide", "rcaron", "uring",
"uacute", "uhungarumlaut", "udieresis", "yacute", "tcommaaccent",
"dotaccent"
);

$gs_dir1 = '/usr/share/ghostscript/fonts';
$gs_dir2 = '/usr/share/ghostscript/fonts2';

@fontnames = (
"Courier", "Courier-Bold", "Courier-Oblique", "Courier-BoldOblique",
"Times-Roman", "Times-Bold", "Times-Italic", "Times-BoldItalic",
"Helvetica", "Helvetica-Bold", "Helvetica-Oblique", "Helvetica-BoldOblique",
"Palatino-Roman", "Palatino-Bold", "Palatino-Italic", "Palatino-BoldItalic",
"Helvetica-Narrow", "Helvetica-Narrow-Bold", "Helvetica-Narrow-Oblique", "Helvetica-Narrow-BoldOblique",
"Bookman-Light", "Bookman-Demi", "Bookman-LightItalic", "Bookman-DemiItalic",
"AvantGarde-Book", "AvantGarde-Demi", "AvantGarde-BookOblique", "AvantGarde-DemiOblique",
"NewCenturySchlbk-Roman", "NewCenturySchlbk-Bold", "NewCenturySchlbk-Italic", "NewCenturySchlbk-BoldItalic",
);

@files = (
"n022003l.afm", "n022004l.afm", "n022023l.afm", "n022024l.afm",
"n021003l.afm", "n021004l.afm", "n021023l.afm", "n021024l.afm",
"n019003l.afm", "n019004l.afm", "n019023l.afm", "n019024l.afm",
"p052003l.afm", "p052004l.afm", "p052023l.afm", "p052024l.afm",
"n019043l.afm", "n019044l.afm", "n019063l.afm", "n019064l.afm",
"b018012l.afm", "b018015l.afm", "b018032l.afm", "b018035l.afm",
"a010013l.afm", "a010015l.afm", "a010033l.afm", "a010035l.afm",
"c059013l.afm", "c059016l.afm", "c059033l.afm", "c059036l.afm",
);


# Generate the array with the fontnames
sub generate_fontnames
{
	printf STDOUT "static const char *szFontnames[%d] = {\n", $#fontnames + 1;
	for ($n = 0; $n <= $#fontnames; $n++) {
		printf STDOUT "\t\"%s\",\n", $fontnames[$n];
	}
	printf STDOUT "};\n";
}

# Generate the array with the character widths
sub generate_character_widths
{
	my ($char_set, $gs_dir, @charnames) = @_;
	my ($n, $i, $file, $name, $start);
	my (@a, @charwidth);

	if ($char_set == 1) {
		$start = 140;
	} else {
		$start = 160;
	}
	printf STDOUT "static unsigned short ausCharacterWidths%d[%d][256] = {\n", $char_set, $#files + 1;
	for ($n = 0; $n <= $#files; $n++) {
		$file = $files[$n];
		$name = $fontnames[$n];
		open(F_IN, "<$gs_dir/$file") || die "Cannot open $gs_dir/$file";
		printf STDOUT "\t{\t/* %s */\n", $name;
		while (<F_IN>) {
			chop();
			@a = split(/\s+/);
			if ($a[0] eq 'UnderlinePosition') {
				$underlineposition[$n] = $a[1];
			} elsif ($a[0] eq 'UnderlineThickness') {
				#printf STDERR "%d %d\n", $a[0], $a[1];
				$underlinethickness[$n] = $a[1];
			} elsif ($a[0] eq 'C' && $a[2] eq ';' && $a[3] eq 'WX') {
				#printf STDERR "%d %d %s\n", $a[1], $a[4], $a[7];
				if (($a[1] < 0 || $a[1] >= 129) && defined($a[7])) {
					for ($i = 0; $i <= $#charnames; $i++) {
						if ($charnames[$i] eq $a[7]) {
							$charwidth[$start + $i] = $a[4];
							last;
						}
					}
				}
				if ($a[1] >= 0 && $a[1] <= 128 && !defined($charwidth[$a[1]])) {
					$charwidth[$a[1]] = $a[4];
				}
			}
			if (defined($a[7])) {
				for ($i = 0; $i <= $#charnames; $i++) {
					if ($charnames[$i] eq $a[7]) {
						$charwidth[$start + $i] = $a[4];
						last;
					}
				}
			}
		}
		close(F_IN);

		# Set the width of the control characters zero
		for ($i = 0; $i < 32; $i++) {
			$charwidth[$i] = 0;
		}
		# Set the width of the unused characters to zero
		for ($i = 127; $i < $start; $i++) {
			$charwidth[$i] = 0;
		}

		# Print the results
		for ($i = 0; $i < 256; $i += 8) {
			printf STDOUT "\t/* %3d */ ", $i;
			for ($j = 0; $j < 8; $j++) {
				if (!defined($charwidth[$i + $j])) {
					printf STDERR "%d:%s: character %3d is undefined\n", $char_set, $name, $i + $j;
					$charwidth[$i + $j] = 0;
				}
				printf STDOUT "%5d,", $charwidth[$i + $j];
			}
			printf STDOUT "\n";
		}
		printf STDOUT "\t},\n";
		undef @charwidth;
	}
	printf STDOUT "};\n";
}

# Generate the array with the underline information
sub generate_underline_information
{
	printf STDOUT "#if 0 /* Until this array is needed */\n";

	printf STDOUT "static int aiUnderlineInfo[%d][2] = {\n", $#fontnames + 1;
	for ($n = 0; $n <= $#fontnames; $n++) {
		if (!defined($underlineposition[$n])) {
			$underlineposition[$n] = 0;
		}
		if (!defined($underlinethickness[$n])) {
			$underlinethickness[$n] = 0;
		}
		printf STDOUT "\t{ %d, %d },\n", $underlineposition[$n], $underlinethickness[$n];
	}
	printf STDOUT "};\n";

	printf STDOUT "#endif /* 0 */\n";
}


# main()

if ($#fontnames != $#files) {
	die "The fontnames-array and the files-array are of unequel length";
}
if ($#charnames1 != 255 - 140) {
	die "The charname1 table length is $#charnames1";
}
if ($#charnames2 != 255 - 160) {
	die "The charname2 table length is $#charnames2";
}

printf STDOUT "/* THIS FILE IS AUTOMATICALLY GENERATED - DO NOT EDIT! */\n";

&generate_fontnames();
&generate_character_widths(1, $gs_dir1, @charnames1);
&generate_character_widths(2, $gs_dir2, @charnames2);
&generate_underline_information();

exit 0;
