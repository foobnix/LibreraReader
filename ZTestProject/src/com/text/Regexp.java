package com.text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regexp {

    public static void main1(String[] args) {
        String input = "blabla.?[12]asdf";

        String patternString = "([0-9]+)";

        Matcher m = Pattern.compile(patternString).matcher(input);

        System.out.println(m.find());
        System.out.println(m.group(0));
    }

    public static void main(String[] args) {
        System.out.println("asdfsdf.fb.pdf.zip".replaceAll("(.zip$)", "").trim());
    }
}
