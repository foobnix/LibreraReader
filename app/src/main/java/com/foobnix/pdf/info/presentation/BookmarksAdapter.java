package com.foobnix.pdf.info.presentation;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;

import com.foobnix.android.utils.Dips;
import com.foobnix.model.AppAnnotation;
import com.foobnix.pdf.info.AnnotationsPainter;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.BookmarksData;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.pdf.info.wrapper.DocumentController;

import java.util.List;

public class BookmarksAdapter extends BaseAdapter {

    /**
     * A section title in the list - "Bookmarks", "Select".
     *
     * It travels in the same list as the bookmarks, rather than in a second adapter, because
     * everything around this list - the search box, the sorting, the click handlers - already
     * works on one list and one adapter.
     */
    public static class Header extends AppBookmark {
        public final int title;

        public Header(final int title) {
            this.title = title;
            this.text = "";
            this.path = "";
        }
    }

    /** One of the reader's own marks, shown the way it is drawn in the book. */
    public static class Mark extends AppBookmark {
        public final AppAnnotation mark;

        public Mark(final AppAnnotation mark, final String path) {
            this.mark = mark;
            this.text = mark.text;
            this.p = mark.p;
            this.t = mark.t;
            // What finds the page again: the page's text when the engine gave one, else the
            // marked words, which are on that page by definition
            this.pt = TxtUtils.isNotEmpty(mark.pt) ? mark.pt : mark.text;
            setPath(path);
        }
    }

    private final List<AppBookmark> objects;
    private final List<AppBookmark> allData;
    private final boolean submenu;
    private final Context context;
    private int muxnumberOfLines = 3;
    private String higlightText;
    private DocumentController controller;
    private Runnable onRefresh;

    public BookmarksAdapter(Context context,
                            List<AppBookmark> objects,
                            List<AppBookmark> allData,
                            boolean submenu,
                            DocumentController controller,
                            Runnable onRefresh) {
        this.context = context;
        this.objects = objects;
        this.allData = allData;
        this.submenu = submenu;
        this.controller = controller;
        this.onRefresh = onRefresh;
    }

    @Override public View getView(final int position, View convertView, ViewGroup parent) {
        if (objects.get(position) instanceof Header) {
            return headerView(convertView, (Header) objects.get(position));
        }
        View view = convertView == null ?
                    LayoutInflater.from(context).inflate(R.layout.bookmark_item, parent, false) :
                    convertView;

        final AppBookmark bookmark = objects.get(position);

        final TextView textView = (TextView) view.findViewById(R.id.text);
        textView.setMaxLines(muxnumberOfLines);
        textView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        final TextView pageView = (TextView) view.findViewById(R.id.page);
        final TextView titleView = (TextView) view.findViewById(R.id.title);
        final ImageView image = (ImageView) view.findViewById(R.id.image);
        final ImageView cloudImage = (ImageView) view.findViewById(R.id.cloudImage);
        cloudImage.setVisibility(View.GONE);
        final View deleteView = view.findViewById(R.id.remove2);
        deleteView.setVisibility(View.VISIBLE);
        // The cross takes the colour the dialog's buttons are: the reader's marks, or on a light
        // sheet, where those would vanish, the theme colour.
        final int removeColor = TintUtil.isLightSurface(context, android.R.attr.colorBackground)
                                ? TintUtil.color : MagicHelper.getTextOrIconColor();
        TintUtil.setTintImageNoAlpha((ImageView) deleteView, removeColor);
        view.findViewById(R.id.remove).setVisibility(View.GONE);

        ((View) image.getParent()).setVisibility(View.GONE);
        ViewCompat.setElevation(((View) image.getParent()), 0);
        view.setBackgroundColor(Color.TRANSPARENT);

        String pageNumber = TxtUtils.deltaPage(AppSP.get().isCut ?
                                               bookmark.getPage(controller.getPageCount()) * 2 :
                                               bookmark.getPage(controller.getPageCount()));
        titleView.setVisibility(View.GONE);

        if (bookmark instanceof Mark) {
            // Shown as it is on the page, with no page number in front of it - the mark is the
            // words, and the page it is on is in the badge beside it like every other row's
            styleAsMark(textView, ((Mark) bookmark).mark);
        } else if (bookmark.isF) {
            textView.setText("{" + bookmark.getPage(controller.getPageCount()) + "}" + ": " + bookmark.text);
        } else {
            textView.setText(bookmark.getPage(controller.getPageCount()) + ": " + bookmark.text);
        }

        pageView.setText(pageNumber);

        if (AppState.get().appTheme == AppState.THEME_INK && !(bookmark instanceof Mark)) {
            TxtUtils.bold(textView);
            TxtUtils.bold(pageView);
            textView.setTextColor(Color.BLACK);
            pageView.setTextColor(Color.BLACK);
        }

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) pageView.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        pageView.setBackgroundColor(Color.TRANSPARENT);
        pageView.setLayoutParams(layoutParams);
        pageView.setTextColor(textView.getCurrentTextColor());

        deleteView.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                if (bookmark.isF) {
                    controller.floatingBookmark = null;
                }

                if (bookmark instanceof Mark) {
                    controller.removeAnnotation(((Mark) bookmark).mark);
                } else {
                    BookmarksData.get().remove(bookmark);
                }

                objects.remove(bookmark);
                allData.remove(bookmark);
                notifyDataSetChanged();

                if (onRefresh != null) {
                    onRefresh.run();
                }
            }
        });

        return view;
    }

    private View headerView(final View convertView, final Header header) {
        final TextView title = convertView instanceof TextView ? (TextView) convertView : new TextView(context);
        final int pad = Dips.dpToPx(8);
        title.setPadding(pad + Dips.dpToPx(4), pad, pad, Dips.dpToPx(2));
        title.setText(context.getString(header.title).toUpperCase());
        title.setTextColor(TintUtil.color);
        TxtUtils.bold(title);
        return title;
    }

    /**
     * A mark shown as it is on the page - highlighted, underlined or struck through, in the
     * colour it was made with - so the list reads as what the reader actually marked.
     */
    private void styleAsMark(final TextView textView, final AppAnnotation mark) {
        final int color = AnnotationsPainter.parseColor(mark.color);
        final SpannableString text = new SpannableString(mark.text);
        final int end = text.length();
        switch (mark.getType()) {
            case UNDERLINE:
                text.setSpan(new UnderlineSpan(), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                text.setSpan(new ForegroundColorSpan(color), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            case STRIKEOUT:
                text.setSpan(new StrikethroughSpan(), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                text.setSpan(new ForegroundColorSpan(color), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            case HIGHLIGHT:
            default:
                text.setSpan(new BackgroundColorSpan(color), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                // Black on a light colour, white on a dark one, so the words stay readable
                text.setSpan(new ForegroundColorSpan(ColorUtils.calculateLuminance(color) > 0.4 ? Color.BLACK : Color.WHITE),
                        0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
        }
        textView.setText(text);
    }

    @Override public int getViewTypeCount() {
        return 2;
    }

    @Override public int getItemViewType(final int position) {
        return objects.get(position) instanceof Header ? 1 : 0;
    }

    @Override public boolean areAllItemsEnabled() {
        return false;
    }

    @Override public boolean isEnabled(final int position) {
        return !(objects.get(position) instanceof Header);
    }

    @Override public int getCount() {
        return objects.size();
    }

    @Override public Object getItem(int position) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override public long getItemId(int position) {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getMuxnumberOfLines() {
        return muxnumberOfLines;
    }

    public void setMuxnumberOfLines(int muxnumberOfLines) {
        this.muxnumberOfLines = muxnumberOfLines;
    }

    public String getHiglightText() {
        return higlightText;
    }

    public void setHiglightText(String higlightText) {
        this.higlightText = higlightText;
    }
}
