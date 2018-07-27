package com.foobnix.hypen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
 * Copyright 2015 Mathew Kurian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * DefaultHyphenator.java
 * @author Mathew Kurian
 *
 * From TextJustify-Android Library v2.0
 * https://github.com/bluejamesbond/TextJustify-Android
 *
 * Please report any issues
 * https://github.com/bluejamesbond/TextJustify-Android/issues
 *
 * Date: 1/27/15 3:35 AM
 */

/*
 * @file   DefaultHyphenator.java
 * @author Murilo Andrade
 * @date   2014-10-20
 */

/*
 * Several performance and memory optimizations
 *
 * @author Martin Fietz
 * @date   2015-09-06
 */

public class DefaultHyphenator {

    private TrieNode trie;
    private int leftMin;
    private int rightMin;

    public HyphenPattern pattern;

    public DefaultHyphenator(HyphenPattern pattern) {
        this.pattern = pattern;
        this.trie = this.createTrie(pattern.patternObject);
        this.leftMin = pattern.leftMin;
        this.rightMin = pattern.rightMin;
    }

    private TrieNode createTrie(Map<Integer, String> patternObject) {

        TrieNode t, tree = new TrieNode();

        for (Map.Entry<Integer, String> entry : patternObject.entrySet()) {
            int key = entry.getKey();
            String value = entry.getValue();

            int numPatterns = value.length() / key;
            for (int i = 0; i < numPatterns; i++) {
                String pattern = value.substring(i * key, (i + 1) * key);
                t = tree;

                for (int c = 0; c < pattern.length(); c++) {
                    char chr = pattern.charAt(c);
                    if (Character.isDigit(chr)) {
                        continue;
                    }
                    int codePoint = pattern.codePointAt(c);
                    if (t.codePoint.get(codePoint) == null) {
                        t.codePoint.put(codePoint, new TrieNode());
                    }
                    t = t.codePoint.get(codePoint);
                }

                List<Integer> list = new ArrayList<Integer>();
                int digitStart = -1;
                for (int p = 0; p < pattern.length(); p++) {
                    if (Character.isDigit(pattern.charAt(p))) {
                        if (digitStart < 0) {
                            digitStart = p;
                        }
                        if (p == pattern.length() - 1) {
                            // last number in the pattern
                            String number = pattern.substring(digitStart, pattern.length());
                            list.add(Integer.valueOf(number));
                        }
                    } else if (digitStart >= 0) {
                        // we reached the end of the current number
                        String number = pattern.substring(digitStart, p);
                        list.add(Integer.valueOf(number));
                        digitStart = -1;
                    } else {
                        list.add(0);
                    }
                }
                t.points = new int[list.size()];
                for (int k = 0; k < list.size(); k++) {
                    t.points[k] = list.get(k);
                }
            }
        }

        return tree;
    }

    public List<String> hyphenate(String word) {
        List<String> result = new ArrayList<String>();

        word = "_" + word + "_";
        String lowercase = word.toLowerCase(Locale.US);

        int wordLength = lowercase.length();

        int[] points = new int[wordLength];
        int[] characterPoints = new int[wordLength];
        for (int i = 0; i < wordLength; i++) {
            points[i] = 0;
            characterPoints[i] = lowercase.codePointAt(i);
        }

        TrieNode node, trie = this.trie;
        int[] nodePoints;
        for (int i = 0; i < wordLength; i++) {
            node = trie;
            for (int j = i; j < wordLength; j++) {
                node = node.codePoint.get(characterPoints[j]);
                if (node != null) {
                    nodePoints = node.points;
                    if (nodePoints != null) {
                        for (int k = 0, nodePointsLength = nodePoints.length; k < nodePointsLength; k++) {
                            points[i + k] = Math.max(points[i + k], nodePoints[k]);
                        }
                    }
                } else {
                    break;
                }
            }
        }


        int start = 1;
        for (int i = 1; i < wordLength - 1; i++) {
            if (i > this.leftMin && i < (wordLength - this.rightMin) && points[i] % 2 > 0) {
                result.add(word.substring(start, i));
                start = i;
            }
        }
        if (start < word.length() - 1) {
            result.add(word.substring(start, word.length() - 1));
        }
        return result;
    }

    /**
     * HyphenaPattern.java is an adaptation of Bram Steins hypher.js-Project:
     * https://github.com/bramstein/Hypher
     *
     * Code from this project belongs to the following license: Copyright (c)
     * 2011, Bram Stein All rights reserved.
     *
     * Redistribution and use in source and binary forms, with or without
     * modification, are permitted provided that the following conditions are
     * met: 1. Redistributions of source code must retain the above copyright
     * notice, this list of conditions and the following disclaimer. 2.
     * Redistributions in binary form must reproduce the above copyright notice,
     * this list of conditions and the following disclaimer in the documentation
     * and/or other materials provided with the distribution. 3. The name of the
     * author may not be used to endorse or promote products derived from this
     * software without specific prior written permission.
     *
     * THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR
     * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
     * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
     * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
     * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
     * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
     * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
     * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
     * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
     * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
     * POSSIBILITY OF SUCH DAMAGE.
     */

    private class TrieNode {
        Map<Integer, TrieNode> codePoint = new HashMap<Integer, TrieNode>();
        int[] points;
    }
}