package fi.iki.elonen;

import android.content.Context;

import com.foobnix.android.utils.LOG;

import java.io.IOException;
import java.io.InputStream;

public class AssertServer extends NanoHTTPD {

    private Context context;

    public AssertServer(Context context) {
        super(8080);
        this.context = context;
    }

    public void run() {
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public Response serve(IHTTPSession session) {
        final String uri = session.getUri();
        LOG.d("AssertServer", uri);

        try {
            InputStream is = context.getAssets().open(uri);
            return new Response(Response.Status.OK, "pain/text", is, -1);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Response(Response.Status.NOT_FOUND, "pain/text", null, -1);


    }
}
