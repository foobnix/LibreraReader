package com.mobi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FooterNoteRegexp {

    public static void main(String[] args) {
        String intput0 = "ewa dsf [1]";
        String intput1 = "13,5 %[22]";
        String intput2 = "13,5 %{333}";
        System.out.println(getNoteId(intput0));
    }

    public static String getNoteId(String input) {
        String patternString = "[\\[{]([0-9]+)[\\]}]";

        Matcher m = Pattern.compile(patternString).matcher(input);

        if (m.find()) {
            String id = m.group(0);
            return id;
        }
        return "";
    }
}
