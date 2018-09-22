package org.ebookdroid.core;

import android.text.TextUtils;

import org.ebookdroid.droids.mupdf.codec.TextWord;

import java.util.LinkedList;
import java.util.Locale;

public class PageSearcher {

    private OnWordSearched listener;
    private String textForSearch;
    private LinkedList<WordData> words = new LinkedList<>();

    public void setListener(OnWordSearched listener) {
        this.listener = listener;
    }

    public void setTextForSearch(String textForSearch) {
        this.textForSearch = textForSearch.toLowerCase(Locale.US).replaceAll("\\W", "");
    }

    private String getTextRow() {
        StringBuffer stringBuffer = new StringBuffer();
        for (WordData word : words) {
            stringBuffer.append(word.wordText);
        }
        return stringBuffer.toString();
    }

    public void searchAtPage(Page page) {
        if (page==null)return;
        if (page.texts==null)return;
        for (TextWord[] line : page.texts) {
            for (TextWord textWord : line) {
                if (textWord==null)continue;
                addWord(new WordData(textWord, page));
            }
        }
    }

    public void addWord(WordData word) {
        if (word == null) return;
        if (TextUtils.isEmpty(textForSearch)) return;

        words.add(word);
        String textRow = getTextRow();
        if (textRow.contains(textForSearch)) {
            WordData removedWord = null;
            while (textRow.contains(textForSearch)) {
                removedWord = words.removeFirst();
                textRow=getTextRow();
            }
            words.addFirst(removedWord);
            for (WordData wordData : words) {
                if (listener != null) listener.onSearch(wordData.word, wordData.data);
            }
            words.clear();
        }
        textRow=getTextRow();
        while (words.size() > 0
                && textRow.length() - words.getFirst().wordText.length() > textForSearch.length()
                && textRow.length() - words.getLast().wordText.length() > textForSearch.length()
                ) {
            words.removeFirst();
            textRow=getTextRow();
        }
    }

    public interface OnWordSearched {
        void onSearch(TextWord word, Object page);
    }

    public static class WordData {
        String wordText;
        TextWord word;
        Object data;

        public WordData(TextWord word, Object data) {
            this.word = word;
            this.data = data;
            wordText = word.w.toLowerCase(Locale.US).replaceAll("\\W", "");
        }
    }


}
