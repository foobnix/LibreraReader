
fz_font *load_noto(fz_context *ctx, const char *a, const char *b, const char *c, int idx);
fz_font *load_droid_font(fz_context *ctx, const char *name, int bold, int italic, int needs_exact_metrics);
fz_font *load_droid_cjk_font(fz_context *ctx, const char *name, int ros, int serif);
fz_font *load_droid_fallback_font(fz_context *ctx, int script, int language, int serif, int bold, int italic);

