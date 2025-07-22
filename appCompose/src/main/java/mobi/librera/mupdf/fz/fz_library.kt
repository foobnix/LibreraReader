package mobi.librera.mupdf.fz

import com.sun.jna.Library
import com.sun.jna.Pointer

interface fz_library : Library {

    fun fz_new_context_imp(
        alloc: Pointer?,
        locks: Pointer?,
        max_store: Int,
        version: String
    ): Pointer


    fun fz_open_document(ctx: Pointer?, filename: String): Pointer

    fun fz_open_document_with_stream(ctx: Pointer?, magic: String, stream: Pointer): Pointer
    fun fz_open_document_with_buffer(ctx: Pointer?, magic: String, stream: Pointer): Pointer


    fun fz_open_memory(ctx: Pointer?, array: ByteArray, size: Int): Pointer
    fun fz_new_buffer_from_data(ctx: Pointer?, array: ByteArray, size: Int): Pointer

    fun fz_keep_buffer(ctx: Pointer?, buffer: Pointer): Pointer

    fun fz_register_document_handlers(ctx: Pointer?): Pointer?

    fun fz_load_page(ctx: Pointer?, doc: Pointer?, pageNumber: Int): Pointer

    fun fz_bound_page(ctx: Pointer?, page: Pointer): fz_rect

    fun fz_count_pages(fzContext: Pointer?, fzDocument: Pointer?): Int

    fun fz_new_pixmap_with_bbox(
        fzContext: Pointer?,
        device: Pointer?,
        bbox: fz_irect?,
        un: Pointer?,
        alpha: Int
    ): Pointer?

    fun fz_var_imp(variable: Pointer?)
    fun fz_do_catch(fzContext: Pointer?): Int
    fun fz_do_try(fzContext: Pointer?): Int
    fun fz_push_try(fzContext: Pointer?): Pointer
    
    fun setjmp(fzPushTry: Pointer): Int
    fun sigsetjmp(buf: Pointer, value: Int): Int

    fun fz_device_bgr(fzContext: Pointer?): Pointer?

    fun fz_device_rgb(fzContext: Pointer?): Pointer?

    fun fz_clear_pixmap_with_value(fzContext: Pointer?, fzPixmap: Pointer?, color: Int)

    fun fz_new_draw_device(fzContext: Pointer?, fzMatrix: fz_matrix, fzPixmap: Pointer?): Pointer?

    fun fz_run_page(
        fzContext: Pointer?,
        fzPage: Pointer?,
        fzDev: Pointer?,
        fzMatrix: fz_matrix?,
        unknown: Pointer?
    )


    fun fz_pixmap_height(fzContext: Pointer?, pixmap: Pointer?): Int

    fun fz_pixmap_width(fzContext: Pointer?, pixmap: Pointer?): Int

    fun fz_pixmap_size(fzContext: Pointer?, pixmap: Pointer?): Int

    fun fz_pixmap_stride(fzContext: Pointer?, pixmap: Pointer?): Int

    fun fz_pixmap_samples(fzContext: Pointer?, pixmap: Pointer?): Pointer

    fun fz_drop_page(fzContext: Pointer?, fzPage: Pointer)
    fun fz_drop_pixmap(fzContext: Pointer?, fzPixmap: Pointer?)
    fun fz_close_device(fzContext: Pointer?, fzDev: Pointer?)
    fun fz_drop_device(fzContext: Pointer?, fzDev: Pointer?)
    fun fz_drop_document(fzContext: Pointer?, fzDocument: Pointer?)
    fun fz_drop_context(fzContext: Pointer?)

    fun fz_layout_document(
        fzContext: Pointer?,
        fzDocument: Pointer?,
        fl: Float,
        fl1: Float,
        fl2: Float
    )

    fun fz_set_user_css(fzContext: Pointer?, s: String)
    fun fz_set_use_document_css(fzContext: Pointer?, i: Int)

    fun fz_load_outline(fzContext: Pointer?, fzDocument: Pointer?): fz_outline?
    fun fz_drop_outline(fzContext: Pointer?, fzOutline: fz_outline?)


}