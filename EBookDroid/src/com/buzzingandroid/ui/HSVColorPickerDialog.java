package com.buzzingandroid.ui;

import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class HSVColorPickerDialog extends AlertDialog {

    private static final int PADDING_DP = 20;

    private static final int CONTROL_SPACING_DP = 20;
    private static final int SELECTED_COLOR_HEIGHT_DP = 50;
    private static final int BORDER_DP = 1;
    private static final int BORDER_COLOR = Color.BLACK;

    private final OnColorSelectedListener listener;
    private int selectedColor;

    public interface OnColorSelectedListener {
        /**
         * @param color
         *            The color code selected, or null if no color. No color is
         *            only possible if
         *            {@link HSVColorPickerDialog#setNoColorButton(int)
         *            setNoColorButton()} has been called on the dialog before
         *            showing it
         */
        public void colorSelected(Integer color);
    }

    public HSVColorPickerDialog(final Context context, int initialColor, final OnColorSelectedListener listener) {
        super(context);
        this.selectedColor = initialColor;
        if (AppState.get().userColor == Color.TRANSPARENT) {
            selectedColor = Color.RED;
        }
        this.listener = listener;

        colorWheel = new HSVColorWheel(context);
        valueSlider = new HSVValueSlider(context);
        int padding = (int) (context.getResources().getDisplayMetrics().density * PADDING_DP);
        int borderSize = (int) (context.getResources().getDisplayMetrics().density * BORDER_DP);
        RelativeLayout layout = new RelativeLayout(context);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        lp.bottomMargin = (int) (context.getResources().getDisplayMetrics().density * CONTROL_SPACING_DP);
        colorWheel.setListener(new OnColorSelectedListener() {
            @Override
            public void colorSelected(Integer color) {
                valueSlider.setColor(color, true);
            }
        });
        colorWheel.setColor(initialColor);
        colorWheel.setId(1);
        layout.addView(colorWheel, lp);

        int selectedColorHeight = (int) (context.getResources().getDisplayMetrics().density * SELECTED_COLOR_HEIGHT_DP);

        FrameLayout valueSliderBorder = new FrameLayout(context);
        valueSliderBorder.setBackgroundColor(BORDER_COLOR);
        valueSliderBorder.setPadding(borderSize, borderSize, borderSize, borderSize);
        valueSliderBorder.setId(2);
        lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, selectedColorHeight + 2 * borderSize);
        lp.bottomMargin = (int) (context.getResources().getDisplayMetrics().density * CONTROL_SPACING_DP);
        lp.addRule(RelativeLayout.BELOW, 1);
        layout.addView(valueSliderBorder, lp);

        valueSlider.setColor(initialColor, false);
        valueSlider.setListener(new OnColorSelectedListener() {
            @Override
            public void colorSelected(Integer color) {
                selectedColor = color;
                selectedColorView.setBackgroundColor(color);
                selectedColorView.setText(MagicHelper.colorToString(color));
            }
        });
        valueSliderBorder.addView(valueSlider);

        FrameLayout selectedColorborder = new FrameLayout(context);
        selectedColorborder.setBackgroundColor(BORDER_COLOR);
        lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, selectedColorHeight + 2 * borderSize);
        selectedColorborder.setPadding(borderSize, borderSize, borderSize, borderSize);
        lp.addRule(RelativeLayout.BELOW, 2);
        layout.addView(selectedColorborder, lp);

        selectedColorView = new TextView(context);
        selectedColorView.setBackgroundColor(selectedColor);
        selectedColorView.setText(MagicHelper.colorToString(selectedColor));
        selectedColorView.setTextColor(Color.WHITE);
        selectedColorView.setGravity(Gravity.CENTER);
        selectedColorView.setTextSize(24);

        selectedColorView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle(R.string.dialog_color_picker);
                final EditText input = new EditText(v.getContext());
                input.setSingleLine();
                input.setText(selectedColorView.getText());
                builder.setView(input);
                builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                final AlertDialog dialog = builder.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            int color = Color.parseColor(input.getText().toString());
                            valueSlider.setColor(color, false);
                            colorWheel.setColor(color);
                            dialog.dismiss();
                        } catch (Exception e) {
                            dialog.setTitle(R.string.invalid_color_value);
                        }
                    }
                });
            }

        });

        selectedColorborder.addView(selectedColorView);

        setButton(BUTTON_NEGATIVE, context.getString(android.R.string.cancel), clickListener);
        setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), clickListener);

        setView(layout, padding, padding, padding, padding);
    }

    private OnClickListener clickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
            case BUTTON_NEGATIVE:
                dialog.dismiss();
                break;
            case BUTTON_NEUTRAL:
                dialog.dismiss();
                listener.colorSelected(-1);
                break;
            case BUTTON_POSITIVE:
                listener.colorSelected(selectedColor);
                break;
            }
        }
    };

    private HSVColorWheel colorWheel;
    private HSVValueSlider valueSlider;

    private TextView selectedColorView;

    /**
     * Adds a button to the dialog that allows a user to select "No color",
     * which will call the listener's
     * {@link OnColorSelectedListener#colorSelected(Integer)
     * colorSelected(Integer)} callback with null as its parameter
     * 
     * @param res
     *            A string resource with the text to be used on this button
     */
    public void setNoColorButton(int res) {
        setButton(BUTTON_NEUTRAL, getContext().getString(res), clickListener);
    }

}