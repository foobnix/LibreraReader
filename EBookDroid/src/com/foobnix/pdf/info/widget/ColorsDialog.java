package com.foobnix.pdf.info.widget;

import com.buzzingandroid.ui.HSVColorPickerDialog.OnColorSelectedListener;
import com.buzzingandroid.ui.HSVColorWheel;
import com.buzzingandroid.ui.HSVValueSlider;
import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.view.CustomSeek;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ColorsDialog {
    int colorTextChoose;
    int colorBgChoose;
    private TextView fontRGB;
    private TextView bgRGB;
    private HSVColorWheel hsvColorWheel1;
    private HSVValueSlider hsvValueSlider1;
    private HSVColorWheel hsvColorWheel2;
    private HSVValueSlider hsvValueSlider2;
    private TextView textPreview;
    private TextView textPreview2;

    TextView isColor, isImage;
    CustomSeek imageTransparency;
    LinearLayout configBGLayout, configBGLayout2;

    ImageView bg1, bg2, bg3, bg4;

    public static interface ColorsDialogResult {
        public void onChooseColor(int colorText, int colorBg);
    }

    Handler handler;

    public ColorsDialog(final FragmentActivity c, final boolean isDayMode, final int colorTextDef, final int colorBgDef, boolean onlyColorBg, final boolean soligBG, final ColorsDialogResult colorsDialogResult) {
        super();
        final View view = LayoutInflater.from(c).inflate(R.layout.dialog_colors, null, false);

        handler = new Handler();

        int colorText = magicBlackColor(colorTextDef);
        int colorBg = magicBlackColor(colorBgDef);

        colorTextChoose = colorText;
        colorBgChoose = colorBg;

        textPreview = (TextView) view.findViewById(R.id.textView1);
        textPreview2 = (TextView) view.findViewById(R.id.textView2);


        isColor = TxtUtils.underlineTextView((TextView) view.findViewById(R.id.isColor));
        isImage = TxtUtils.underlineTextView((TextView) view.findViewById(R.id.isImage));

        textPreview.setText(isDayMode ? R.string.day : R.string.nigth);

        textPreview.setTextColor(colorText);
        textPreview.setBackgroundColor(colorBg);

        textPreview2.setTextColor(colorText);
        textPreview2.setBackgroundColor(colorBg);

        hsvColorWheel1 = (HSVColorWheel) view.findViewById(R.id.HSVColorWheel1);
        hsvValueSlider1 = (HSVValueSlider) view.findViewById(R.id.HSVValueSlider1);

        hsvColorWheel2 = (HSVColorWheel) view.findViewById(R.id.HSVColorWheel2);
        hsvValueSlider2 = (HSVValueSlider) view.findViewById(R.id.HSVValueSlider2);

        fontRGB = (TextView) view.findViewById(R.id.fontRGB);
        bgRGB = (TextView) view.findViewById(R.id.bgRGB);

        imageTransparency = (CustomSeek) view.findViewById(R.id.bgTransparency);
        imageTransparency.init(10, 255, isDayMode ? AppState.get().bgImageDayTransparency : AppState.get().bgImageNightTransparency);
        imageTransparency.setOnSeekChanged(new IntegerResponse() {

            @Override
            public boolean onResultRecive(int result) {
                if (isDayMode) {
                    AppState.get().bgImageDayTransparency = result;
                } else {
                    AppState.get().bgImageNightTransparency = result;
                }
                Bitmap bg = MagicHelper.updateTextViewBG(textPreview2, result, MagicHelper.getImagePath(isDayMode));
                textPreview.setText("");
                textPreview.setBackgroundDrawable(new BitmapDrawable(bg));
                return false;
            }
        });

        configBGLayout = (LinearLayout) view.findViewById(R.id.configBGLayout);
        configBGLayout2 = (LinearLayout) view.findViewById(R.id.configBGLayout2);

        bg1 = (ImageView) view.findViewById(R.id.bg1);
        bg2 = (ImageView) view.findViewById(R.id.bg2);
        bg3 = (ImageView) view.findViewById(R.id.bg3);
        bg4 = (ImageView) view.findViewById(R.id.bg4);

        bg1.setImageBitmap(MagicHelper.loadBitmap(MagicHelper.IMAGE_BG_1));
        bg2.setImageBitmap(MagicHelper.loadBitmap(MagicHelper.IMAGE_BG_2));
        bg3.setImageBitmap(MagicHelper.loadBitmap(MagicHelper.IMAGE_BG_3));

        if (isDayMode) {
            if (AppState.get().bgImageDayPath.startsWith("/")) {
                bg4.setImageBitmap(MagicHelper.loadBitmap(AppState.get().bgImageDayPath));
            }
        } else {
            if (AppState.get().bgImageNightPath.startsWith("/")) {
                bg4.setImageBitmap(MagicHelper.loadBitmap(AppState.get().bgImageNightPath));
            }
        }

        bg1.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isDayMode) {
                    AppState.get().bgImageDayPath = MagicHelper.IMAGE_BG_1;
                } else {
                    AppState.get().bgImageNightPath = MagicHelper.IMAGE_BG_1;
                }

                Bitmap bg = MagicHelper.updateTextViewBG(textPreview2, imageTransparency.getCurrentValue(), MagicHelper.IMAGE_BG_1);
                textPreview.setText("");
                textPreview.setBackgroundDrawable(new BitmapDrawable(bg));

            }
        });
        bg2.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isDayMode) {
                    AppState.get().bgImageDayPath = MagicHelper.IMAGE_BG_2;
                } else {
                    AppState.get().bgImageNightPath = MagicHelper.IMAGE_BG_2;
                }

                Bitmap bg = MagicHelper.updateTextViewBG(textPreview2, imageTransparency.getCurrentValue(), MagicHelper.IMAGE_BG_2);
                textPreview.setText("");
                textPreview.setBackgroundDrawable(new BitmapDrawable(bg));

            }
        });
        bg3.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isDayMode) {
                    AppState.get().bgImageDayPath = MagicHelper.IMAGE_BG_3;
                } else {
                    AppState.get().bgImageNightPath = MagicHelper.IMAGE_BG_3;
                }

                Bitmap bg = MagicHelper.updateTextViewBG(textPreview2, imageTransparency.getCurrentValue(), MagicHelper.IMAGE_BG_3);
                textPreview.setText("");
                textPreview.setBackgroundDrawable(new BitmapDrawable(bg));

            }
        });
        bg4.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                ChooserDialogFragment.chooseFile(c, "").setOnSelectListener(new ResultResponse2<String, Dialog>() {
                    @Override
                    public boolean onResultRecive(String nPath, Dialog dialog) {
                        if (!ExtUtils.isImagePath(nPath)) {
                            Toast.makeText(c, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        if (isDayMode) {
                            AppState.get().bgImageDayPath = nPath;
                        } else {
                            AppState.get().bgImageNightPath = nPath;
                        }
                        bg4.setImageBitmap(MagicHelper.loadBitmap(nPath));

                        Bitmap bg = MagicHelper.updateTextViewBG(textPreview2, imageTransparency.getCurrentValue(), nPath);
                        textPreview.setText("");
                        textPreview.setBackgroundDrawable(new BitmapDrawable(bg));

                        dialog.dismiss();
                        return false;
                    }
                });

            }
        });

        isColor.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                imageTransparency.setVisibility(View.GONE);
                configBGLayout.setVisibility(View.GONE);
                configBGLayout2.setVisibility(View.GONE);
                ((ViewGroup) bgRGB.getParent()).setVisibility(View.VISIBLE);
                hsvValueSlider2.setVisibility(View.VISIBLE);
                hsvColorWheel2.setVisibility(View.VISIBLE);

                textPreview2.setBackgroundColor(colorBgChoose);

                if (isDayMode) {
                    AppState.get().isUseBGImageDay = false;
                } else {
                    AppState.get().isUseBGImageNight = false;
                }

                textPreview.setText(isDayMode ? R.string.day : R.string.nigth);
                textPreview.setTextColor(colorTextChoose);
                textPreview.setBackgroundColor(colorBgChoose);
            }
        });

        isImage.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isDayMode) {
                    AppState.get().isUseBGImageDay = true;
                    colorBgChoose = AppState.COLOR_WHITE;
                    textPreview2.setBackgroundColor(AppState.COLOR_WHITE);
                } else {
                    AppState.get().isUseBGImageNight = true;
                    colorBgChoose = AppState.COLOR_BLACK;
                    textPreview2.setBackgroundColor(AppState.COLOR_BLACK);
                }

                imageTransparency.setVisibility(View.VISIBLE);
                configBGLayout.setVisibility(View.VISIBLE);
                configBGLayout2.setVisibility(View.VISIBLE);
                ((ViewGroup) bgRGB.getParent()).setVisibility(View.GONE);
                hsvValueSlider2.setVisibility(View.GONE);
                hsvColorWheel2.setVisibility(View.GONE);

                Bitmap bg = MagicHelper.updateTextViewBG(textPreview2, imageTransparency.getCurrentValue(), MagicHelper.getImagePath(isDayMode));
                textPreview.setText("");
                textPreview.setBackgroundDrawable(new BitmapDrawable(bg));

            }
        });

        fontRGB.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(c);
                builder.setTitle(R.string.dialog_color_picker);
                final EditText input = new EditText(c);
                input.setText(fontRGB.getText());
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
                            int parseColor = Color.parseColor(input.getText().toString());
                            updateAll(parseColor, colorBgChoose);
                            dialog.dismiss();
                        } catch (Exception e) {
                            dialog.setTitle("Invalid color value");
                        }
                    }
                });
            }
        });

        bgRGB.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(c);
                builder.setTitle(R.string.dialog_color_picker);
                final EditText input = new EditText(c);
                input.setSingleLine();
                input.setText(bgRGB.getText());
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
                            int parseColor = Color.parseColor(input.getText().toString());
                            updateAll(colorTextChoose, parseColor);
                            dialog.dismiss();
                        } catch (Exception e) {
                            dialog.setTitle(R.string.invalid_color_value);
                        }
                    }
                });
            }
        });

        hsvColorWheel1.setListener(new OnColorSelectedListener() {
            @Override
            public void colorSelected(Integer color) {
                LOG.d("hsvColorWheel1 colorSelected", MagicHelper.colorToString(color));
                hsvValueSlider1.setColor(color, true);
                updateRGB(fontRGB, color);
            }
        });

        hsvColorWheel2.setListener(new OnColorSelectedListener() {
            @Override
            public void colorSelected(Integer color) {
                LOG.d("hsvColorWheel2 colorSelected", MagicHelper.colorToString(color));
                hsvValueSlider2.setColor(color, true);
                updateRGB(bgRGB, color);
            }
        });

        hsvValueSlider1.setListener(new OnColorSelectedListener() {
            @Override
            public void colorSelected(final Integer color) {
                textPreview.setTextColor(color);
                textPreview2.setTextColor(color);
                colorTextChoose = color;

                updateRGB(fontRGB, color);
            }
        });

        hsvValueSlider2.setListener(new OnColorSelectedListener() {
            @Override
            public void colorSelected(final Integer color) {
                textPreview.setBackgroundColor(color);
                textPreview2.setBackgroundColor(color);

                colorBgChoose = color;
                updateRGB(bgRGB, color);

            }
        });

        AlertDialog.Builder d = new AlertDialog.Builder(c);
        d.setView(view);
        d.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        d.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                colorTextChoose = magicBlackColor(colorTextChoose);
                colorBgChoose = magicBlackColor(colorBgChoose);

                colorsDialogResult.onChooseColor(colorTextChoose, colorBgChoose);

                AppState.get().save(c);
            }
        });
        d.show();

        TextView isDefaults = TxtUtils.underlineTextView((TextView) view.findViewById(R.id.onDefaults));
        isDefaults.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isDayMode) {
                    AppState.get().bgImageDayTransparency = AppState.DAY_TRANSPARENCY;
                    AppState.get().bgImageDayPath = MagicHelper.IMAGE_BG_1;
                    imageTransparency.reset(AppState.get().bgImageDayTransparency);
                } else {
                    AppState.get().bgImageNightTransparency = AppState.NIGHT_TRANSPARENCY;
                    AppState.get().bgImageNightPath = MagicHelper.IMAGE_BG_2;
                    imageTransparency.reset(AppState.get().bgImageNightTransparency);
                }
                updateAll(colorTextDef, colorBgDef);
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (soligBG) {
                            isColor.performClick();
                        } else {
                            isImage.performClick();
                        }
                    }
                }, 25);

            }
        });

        // hsvColorWheel1.setColor(colorText);
        // hsvValueSlider1.setColor(colorText, false);
        //
        // hsvColorWheel2.setColor(colorBg);
        // hsvValueSlider2.setColor(colorBg, false);
        //
        // updateRGB(fontRGB, colorText);
        // updateRGB(bgRGB, colorBg);

        if (!(AppState.get().isTextFormat() || AppState.get().isCustomizeBgAndColors)) {
            fontRGB.setVisibility(View.GONE);
            hsvColorWheel1.setVisibility(View.GONE);
            hsvValueSlider1.setVisibility(View.GONE);
            view.findViewById(R.id.textFont).setVisibility(View.GONE);
            view.findViewById(R.id.fontLayout).setVisibility(View.GONE);
        }

        updateAll(colorTextChoose, colorBgChoose);
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                if (soligBG) {
                    isColor.performClick();
                } else {
                    isImage.performClick();
                }
            }
        }, 25);

        if (onlyColorBg) {
            isImage.setVisibility(View.GONE);
            isDefaults.setVisibility(View.GONE);

        }

    }

    public void showHideByColor(boolean isColor) {
        if (isColor) {

        } else {

        }
    }

    public int magicBlackColor(int color) {
        if (color == Color.BLACK) {
            return AppState.COLOR_BLACK;
        }
        return color;
    }

    public void updateAll(int defText, int defBg) {
        int colorText = defText;
        int colorBg = defBg;

        hsvColorWheel1.setColor(colorText);
        hsvColorWheel2.setColor(colorBg);

        textPreview.setTextColor(colorText);
        textPreview2.setTextColor(colorText);

        textPreview.setBackgroundColor(colorBg);
        textPreview2.setBackgroundColor(colorBg);

        hsvValueSlider1.setColor(colorText, false);
        hsvValueSlider2.setColor(colorBg, false);

        updateRGB(fontRGB, colorText);
        updateRGB(bgRGB, colorBg);
    }

    public void updateRGB(TextView font, Integer intColor) {
        if (intColor == AppState.COLOR_BLACK) {
            intColor = Color.BLACK;
        }

        String hexColor = MagicHelper.colorToString(intColor);
        font.setText(TxtUtils.underline(hexColor));
    }

}
