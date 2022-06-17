/*
 * drawfile.h
 * Copyright (C) 2005 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Include file to deal with drawfiles
 *
 * Based on:
 * C header file for DrawFile
 * written by DefMod (May  4 2004) on Tue May  4 13:34:17 2004
 * Jonathan Coxhead, jonathan@doves.demon.co.uk, 21 Aug 1995
 * OSLib---efficient, type-safe, transparent, extensible,
 * register-safe A P I coverage of RISC O S
 * Copyright (C) 1994 Jonathan Coxhead
 *
 * All credit should go to him, but all the bugs are mine
 */

#if !defined(__drawfile_h)
#define __drawfile_h

#include "DeskLib:Sprite.h"
#include "DeskLib:Wimp.h"

#if !defined(BOOL)
#define BOOL int
#define TRUE 1
#define FALSE 0
#endif /* !BOOL */

/*********************
 * Conversion macros *
 *********************/
#define Drawfile_DrawToScreen(i) ((i) / 256)
#define Drawfile_ScreenToDraw(i) ((i) * 256)

/**********************************
 * SWI names and SWI reason codes *
 **********************************/
#define DrawFile_Render                         0x45540
#define DrawFile_BBox                           0x45541
#define DrawFile_DeclareFonts                   0x45542

/********************
 * Type definitions *
 ********************/
typedef unsigned int                            bits;
typedef unsigned char                           byte;

typedef byte drawfile_fontref;

typedef byte drawfile_path_style_flags;

typedef bits drawfile_text_flags;

typedef bits drawfile_render_flags;

typedef bits drawfile_declare_fonts_flags;

typedef bits drawfile_paper_options;

typedef bits drawfile_entry_mode;

typedef enum {
  drawfile_TYPE_FONT_TABLE = 0,
  drawfile_TYPE_TEXT = 1,
  drawfile_TYPE_PATH = 2,
  drawfile_TYPE_SPRITE = 5,
  drawfile_TYPE_GROUP = 6,
  drawfile_TYPE_TAGGED = 7,
  drawfile_TYPE_TEXT_AREA = 9,
  drawfile_TYPE_TEXT_COLUMN = 10,
  drawfile_TYPE_OPTIONS = 11,
  drawfile_TYPE_TRFM_TEXT = 12,
  drawfile_TYPE_TRFM_SPRITE = 13,
  drawfile_TYPE_JPEG = 16
} drawfile_type;

typedef enum {
  drawfile_PATH_END_PATH = 0,
  drawfile_PATH_CONTINUATION = 1,
  drawfile_PATH_MOVE_TO = 2,
  drawfile_PATH_SPECIAL_MOVE_TO = 3,
  drawfile_PATH_CLOSE_GAP = 4,
  drawfile_PATH_CLOSE_LINE = 5,
  drawfile_PATH_BEZIER_TO = 6,
  drawfile_PATH_GAP_TO = 7,
  drawfile_PATH_LINE_TO = 8
} drawfile_path_type;

typedef struct {
  int start;
  int element_count;
  int elements [6];
} draw_dash_pattern;

typedef struct {
  int entries [3] [2];
} os_trfm;

typedef struct {
  void *data;
  size_t length;
} drawfile_info;

typedef struct {
  drawfile_fontref font_ref;
  char font_name [1];
} drawfile_font_def;

typedef struct {
  drawfile_fontref font_ref;
  byte reserved [3];
} drawfile_text_style;

typedef struct {
  drawfile_path_style_flags flags;
  byte reserved;
  byte cap_width;
  byte cap_length;
} drawfile_path_style;

typedef struct {
  drawfile_font_def font_def[1];
} drawfile_font_table;

typedef struct {
  wimp_box bbox;
  palette_entry fill;
  palette_entry bg_hint;
  drawfile_text_style style;
  int xsize;
  int ysize;
  wimp_coord base;
  char text [1];
} drawfile_text;

typedef struct {
  wimp_box bbox;
  palette_entry fill;
  palette_entry outline;
  int width;
  drawfile_path_style style;
  int path [1];
} drawfile_path;

typedef struct {
  wimp_box bbox;
  palette_entry fill;
  palette_entry outline;
  int width;
  drawfile_path_style style;
  draw_dash_pattern pattern;
  int path [1];
} drawfile_path_with_pattern;

typedef struct {
  wimp_box bbox;
  sprite_header header;
  byte data [1];
} drawfile_sprite;

typedef struct  {
  wimp_box bbox;
  char name [12];
  int objects [1];
} drawfile_group;

typedef struct {
  wimp_box bbox;
  drawfile_type tag;
  int object [1];
} drawfile_tagged;

typedef struct {
  wimp_box box;
} drawfile_text_column;

typedef struct {
  struct {
    drawfile_type type;
    int size;
    drawfile_text_column data;
  } columns [1];
} drawfile_text_column_list;

typedef struct {
  drawfile_type type;
  int reserved [2];
  palette_entry fill;
  palette_entry bg_hint;
  char text [1];
} drawfile_area_text;

typedef struct {
  wimp_box bbox;
  drawfile_text_column_list header;
  drawfile_area_text area_text;
} drawfile_text_area;

typedef struct {
  wimp_box bbox;
  int paper_size;
  drawfile_paper_options paper_options;
  double grid_spacing;
  int grid_division;
  BOOL isometric;
  BOOL auto_adjust;
  BOOL show;
  BOOL lock;
  BOOL cm;
  int zoom_mul;
  int zoom_div;
  BOOL zoom_lock;
  BOOL toolbox;
  drawfile_entry_mode entry_mode;
  int undo_size;
} drawfile_options;

typedef struct {
  wimp_box bbox;
  os_trfm trfm;
  drawfile_text_flags flags;
  palette_entry fill;
  palette_entry bg_hint;
  drawfile_text_style style;
  int xsize;
  int ysize;
  wimp_coord base;
  char text [1];
} drawfile_trfm_text;

typedef struct {
  wimp_box bbox;
  os_trfm trfm;
  sprite_header header;
  byte data [1];
} drawfile_trfm_sprite;

typedef struct {
  wimp_box bbox;
  int width;
  int height;
  int xdpi;
  int ydpi;
  os_trfm trfm;
  int len;
  byte data [1];
} drawfile_jpeg;

/* ------------------------------------------------------------------------
 * Type:          drawfile_object
 *
 * Description:   This type is used to declare pointers rather than objects
 */

typedef struct {
  drawfile_type type;
  int size;
  union {
    drawfile_font_table font_table;
    drawfile_text text;
    drawfile_path path;
    drawfile_path_with_pattern path_with_pattern;
    drawfile_sprite sprite;
    drawfile_group group;
    drawfile_tagged tagged;
    drawfile_text_column text_column;
    drawfile_text_area text_area;
    drawfile_options options;
    drawfile_trfm_text trfm_text;
    drawfile_trfm_sprite trfm_sprite;
    drawfile_jpeg jpeg;
  } data;
} drawfile_object;

typedef struct {
  char tag [4];
  int major_version;
  int minor_version;
  char source [12];
  wimp_box bbox;
  drawfile_object objects [1];
} drawfile_diagram;

typedef bits drawfile_bbox_flags;

typedef struct {
  drawfile_object *object;
  drawfile_diagram *diagram;
  drawfile_object *font_table;
  drawfile_declare_fonts_flags flags;
  os_error *error;
} drawfile_declare_fonts_state;

/************************
 * Constant definitions *
 ************************/
#define error_DRAW_FILE_NOT_DRAW                0x20C00u
#define error_DRAW_FILE_VERSION                 0x20C01u
#define error_DRAW_FILE_FONT_TAB                0x20C02u
#define error_DRAW_FILE_BAD_FONT_NO             0x20C03u
#define error_DRAW_FILE_BAD_MODE                0x20C04u
#define error_DRAW_FILE_BAD_FILE                0x20C05u
#define error_DRAW_FILE_BAD_GROUP               0x20C06u
#define error_DRAW_FILE_BAD_TAG                 0x20C07u
#define error_DRAW_FILE_SYNTAX                  0x20C08u
#define error_DRAW_FILE_FONT_NO                 0x20C09u
#define error_DRAW_FILE_AREA_VER                0x20C0Au
#define error_DRAW_FILE_NO_AREA_VER             0x20C0Bu

#define drawfile_PATH_MITRED                    ((drawfile_path_style_flags) 0x0u)
#define drawfile_PATH_ROUND                     ((drawfile_path_style_flags) 0x1u)
#define drawfile_PATH_BEVELLED                  ((drawfile_path_style_flags) 0x2u)
#define drawfile_PATH_BUTT                      ((drawfile_path_style_flags) 0x0u)
#define drawfile_PATH_SQUARE                    ((drawfile_path_style_flags) 0x2u)
#define drawfile_PATH_TRIANGLE                  ((drawfile_path_style_flags) 0x3u)
#define drawfile_PATH_JOIN_SHIFT                0
#define drawfile_PATH_JOIN                      ((drawfile_path_style_flags) 0x3u)
#define drawfile_PATH_END_SHIFT                 2
#define drawfile_PATH_END                       ((drawfile_path_style_flags) 0xCu)
#define drawfile_PATH_START_SHIFT               4
#define drawfile_PATH_START                     ((drawfile_path_style_flags) 0x30u)
#define drawfile_PATH_WINDING_EVEN_ODD          ((drawfile_path_style_flags) 0x40u)
#define drawfile_PATH_DASHED                    ((drawfile_path_style_flags) 0x80u)
#define drawfile_PATH_CAP_WIDTH_SHIFT           16
#define drawfile_PATH_CAP_WIDTH                 ((drawfile_path_style_flags) 0xFF0000u)
#define drawfile_PATH_CAP_LENGTH_SHIFT          24
#define drawfile_PATH_CAP_LENGTH                ((drawfile_path_style_flags) 0xFF000000u)
#define drawfile_TEXT_KERN                      ((drawfile_text_flags) 0x1u)
#define drawfile_TEXT_RIGHT_TO_LEFT             ((drawfile_text_flags) 0x2u)
#define drawfile_TEXT_UNDERLINE                 ((drawfile_text_flags) 0x4u)
#define drawfile_RENDER_BBOXES                  ((drawfile_render_flags) 0x1u)
#define drawfile_RENDER_SUPPRESS                ((drawfile_render_flags) 0x2u)
#define drawfile_RENDER_GIVEN_FLATNESS          ((drawfile_render_flags) 0x4u)
#define drawfile_RENDER_GIVEN_COLOUR_MAPPING    ((drawfile_render_flags) 0x8u)
#define drawfile_NO_DOWNLOAD                    ((drawfile_declare_fonts_flags) 0x1u)
#define drawfile_PAPER_SHOW                     ((drawfile_paper_options) 0x1u)
#define drawfile_PAPER_LANDSCAPE                ((drawfile_paper_options) 0x10u)
#define drawfile_PAPER_DEFAULT                  ((drawfile_paper_options) 0x100u)
#define drawfile_ENTRY_MODE_LINE                ((drawfile_entry_mode) 0x1u)
#define drawfile_ENTRY_MODE_CLOSED_LINE         ((drawfile_entry_mode) 0x2u)
#define drawfile_ENTRY_MODE_CURVE               ((drawfile_entry_mode) 0x4u)
#define drawfile_ENTRY_MODE_CLOSED_CURVE        ((drawfile_entry_mode) 0x8u)
#define drawfile_ENTRY_MODE_RECTANGLE           ((drawfile_entry_mode) 0x10u)
#define drawfile_ENTRY_MODE_ELLIPSE             ((drawfile_entry_mode) 0x20u)
#define drawfile_ENTRY_MODE_TEXT_LINE           ((drawfile_entry_mode) 0x40u)
#define drawfile_ENTRY_MODE_SELECT              ((drawfile_entry_mode) 0x80u)

/*************************
 * Function declarations *
 *************************/

#if defined(__cplusplus)
   extern "C" {
#endif /* __cplusplus */

/* ------------------------------------------------------------------------
 * Function:      drawfile_render()
 *
 * Description:   Calls SWI 0x45540
 *
 * Input:         flags - value of R0 on entry
 *                diagram - value of R1 on entry
 *                size - value of R2 on entry
 *                trfm - value of R3 on entry
 *                clip - value of R4 on entry
 *                flatness - value of R5 on entry
 */

extern os_error *Drawfile_Render (drawfile_render_flags flags,
      drawfile_diagram const *diagram,
      int size,
      os_trfm const *trfm,
      wimp_box const *clip,
      int flatness);

/* ------------------------------------------------------------------------
 * Function:      drawfile_bbox()
 *
 * Description:   Calls SWI 0x45541
 *
 * Input:         flags - value of R0 on entry
 *                diagram - value of R1 on entry
 *                size - value of R2 on entry
 *                trfm - value of R3 on entry
 *                bbox - value of R4 on entry
 */

extern os_error *Drawfile_Bbox (drawfile_bbox_flags flags,
      drawfile_diagram const *diagram,
      int size,
      os_trfm const *trfm,
      wimp_box *bbox);

/* ------------------------------------------------------------------------
 * Function:      Drawfile_DeclareFonts()
 *
 * Description:   Calls SWI 0x45542
 *
 * Input:         flags - value of R0 on entry
 *                diagram - value of R1 on entry
 *                size - value of R2 on entry
 */

extern os_error *Drawfile_DeclareFonts (drawfile_declare_fonts_flags flags,
      drawfile_diagram const *diagram,
      int size);

/* ------------------------------------------------------------------------
 * Function:      Drawfile_CreateDiagram()
 *
 */

extern os_error * Drawfile_CreateDiagram(drawfile_info *info, size_t memory,
	const char *creator, wimp_box box);

extern os_error *Drawfile_AppendObject(drawfile_info *info, size_t memory,
	const drawfile_object *object, BOOL rebind);

extern os_error *Drawfile_RenderDiagram(drawfile_info *info,
	window_redrawblock *redraw, double scale);

extern os_error *Drawfile_VerifyDiagram(drawfile_info *info);

extern void	Drawfile_QueryBox(drawfile_info *info,
	wimp_box *rect, BOOL screenUnits);

#if defined(__cplusplus)
   }
#endif /* __cplusplus */

#endif /* __drawfile.h */
