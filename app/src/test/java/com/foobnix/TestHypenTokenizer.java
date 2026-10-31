package com.foobnix;

import static org.junit.Assert.assertEquals;

import com.foobnix.hypen.HypenUtils;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestHypenTokenizer {

    private final List<String> words = new ArrayList<>();

    private final HypenUtils.TokensListener listener = new HypenUtils.TokensListener() {
        @Override
        public void findOther(char ch) {
        }

        @Override
        public void findText(String w) {
            words.add(w);
        }
    };

    @Before
    public void reset() {
        HypenUtils.resetTokenizer();
    }

    @Test
    public void textOfStyleElementIsNotBookText() {
        HypenUtils.tokenize("<style type=\"text/css\">", listener);
        HypenUtils.tokenize(".bt { font-family: Helvetica; }</style> after ", listener);
        assertEquals(List.of("after"), words);
    }

    @Test
    public void selfClosedScriptDoesNotHideTheBook() {
        HypenUtils.tokenize("<script src=\"a.js\" /> after ", listener);
        assertEquals(List.of("after"), words);
    }

    @Test
    public void textOfScriptElementIsNotBookText() {
        HypenUtils.tokenize("<SCRIPT>if (a < b) run();</SCRIPT> after ", listener);
        assertEquals(List.of("after"), words);
    }

    @Test
    public void otherTagsAreLeftAlone() {
        HypenUtils.tokenize("<styled>one</styled> two ", listener);
        assertEquals(List.of("one", "two"), words);
    }
}
