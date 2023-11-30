package com;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestPage {

    @Test
    public void test1() {
        String input = "Now <a id=\"page142\"></a>sit down";
        String output = "Now <br> page 142<br> sit down";
        String result = input.replaceAll("<a id=\"page(\\d+)\"></a>", "<br>page $1<br>");
        assertEquals(output,result);

    }
}
