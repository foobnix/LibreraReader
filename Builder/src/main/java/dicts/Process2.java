package dicts;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;

public class Process2 {


    public static void main(final String[] args) throws Exception {

        System.out.println("hello");
        String file = "/home/data/Downloads/replaces/db/odict/zalizniak.txt";
        String outFile = "/home/data/Downloads/replaces/db/odict/Librera_Словарь_Ударений_[odict.ru].txt";


        BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(file), "Windows-1251"));
        FileWriter out = new FileWriter(outFile);
        String line = "";
        while ((line = input.readLine()) != null) {
            line = line.replaceAll("[ ]+", " ").toLowerCase();

            if (line.trim().length() == 0 || line.startsWith("-")) {
                continue;
            }

            String words[] = line.split(" ");
            if (words.length < 2) {
                continue;
            }
            String key = words[0].trim();
            if (key.length() < 3) {
                continue;
            }

            String num = words[1];
            if (num.equals(":")) {
                num = words[2];
            }

            if (num.contains(",")) {
                num = num.substring(0, num.indexOf(','));
            }
            if (num.contains(".")) {
                num = num.substring(0, num.indexOf('.'));
            }

            System.out.println(words[0] + " : " + words[1]);


            int index = Integer.parseInt(num)-1;
            if (index == -1) {
                continue;
            }
            String value = "";
            if (index == 0) {
                value = repalce(key.charAt(index)) + key.substring(index, key.length());
            } else {
                value = key.substring(0, index) + repalce(key.charAt(index)) + key.substring(index + 1, key.length());
            }
           // value = key.substring(0, index) + "'" + key.substring(index, key.length());


            final String format = String.format("\"%s\" \"%s\" \n", key, value);
            System.out.println(format);
            out.write(format);
        }
        out.close();
        System.out.println("finish");


    }

    public static String repalce(char in) {
        if (in == 'а' || in == 'А') return "а́";
        else if (in == 'о' || in == 'О') return "о́́";
        else if (in == 'и' || in == 'И') return "и́";
        else if (in == 'у' || in == 'У') return "у́";
        else if (in == 'е' || in == 'Е') return "е́́";
        else if (in == 'ы' || in == 'Ы') return "ы́́́";
        else if (in == 'э' || in == 'Э') return "э́";
        else if (in == 'я' || in == 'Я') return "я́";
        else if (in == 'ю' || in == 'Ю') return "ю́";
        else if (in == 'ё' || in == 'Ё') return "е́";

        return String.valueOf(in);
    }
    public static String repalce2(char in) {
        if (in == 'а' || in == 'А') return "аа";
        else if (in == 'о' || in == 'О') return "оо";
        else if (in == 'и' || in == 'И') return "ии";
        else if (in == 'у' || in == 'У') return "уу";
        else if (in == 'е' || in == 'Е') return "ее";
        else if (in == 'ы' || in == 'Ы') return "ыы";
        else if (in == 'э' || in == 'Э') return "ээ";
        else if (in == 'я' || in == 'Я') return "яя";
        else if (in == 'ю' || in == 'Ю') return "юю";
        else if (in == 'ё' || in == 'Ё') return "ёё";

        throw new IllegalArgumentException(String.valueOf(in));
    }
}
