package com.foobnix.pdf.librera;

import android.support.test.runner.AndroidJUnitRunner;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

public class ExampleUnitTest extends  AndroidJUnitRunner  {


    @Test
    public void addition_isCorrect() {
        assertEquals(4,2+2);
        //GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mockContext);

         assertNotNull(getContext());

        //LOG.d(context.getString(R.string.msg_unexpected_error));
        //LOG.d("ExampleUnitTest",account.getDisplayName());

    }
}