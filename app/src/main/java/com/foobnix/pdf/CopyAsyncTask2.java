/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.foobnix.pdf;

import java.util.concurrent.ExecutorService;

import android.app.Activity;

public abstract class CopyAsyncTask2 {

    private Activity a;

    public CopyAsyncTask2(Activity a) {
        this.a = a;
    }

    Thread t = new Thread() {
        @Override
        public void run() {
            a.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    onPreExecute();
                }
            });
            final Object result = doInBackground();
            a.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    onPostExecute(result);
                }
            });

        };
    };

    protected abstract void onPreExecute();

    protected abstract Object doInBackground();

    protected void onCancelled() {

    }

    protected void onPostExecute(Object result) {

    }

    public void executeOnExecutor(ExecutorService newSingleThreadExecutor) {
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }

    public void cancel(boolean b) {
    }

}
