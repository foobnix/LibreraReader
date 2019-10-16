package at.stefl.svm.enumeration;

import java.util.Map;

import at.stefl.commons.util.InaccessibleSectionException;
import at.stefl.commons.util.collection.CollectionUtil;
import at.stefl.commons.util.object.ObjectTransformer;
import at.stefl.svm.object.action.CommentAction;
import at.stefl.svm.object.action.FillColorAction;
import at.stefl.svm.object.action.FontAction;
import at.stefl.svm.object.action.LineAction;
import at.stefl.svm.object.action.LineColorAction;
import at.stefl.svm.object.action.MapModeAction;
import at.stefl.svm.object.action.NullAction;
import at.stefl.svm.object.action.OverLineColorAction;
import at.stefl.svm.object.action.PixelAction;
import at.stefl.svm.object.action.PointAction;
import at.stefl.svm.object.action.PolyLineAction;
import at.stefl.svm.object.action.PolyPolygonAction;
import at.stefl.svm.object.action.PolygonAction;
import at.stefl.svm.object.action.PopAction;
import at.stefl.svm.object.action.PushAction;
import at.stefl.svm.object.action.RectangleAction;
import at.stefl.svm.object.action.SVMAction;
import at.stefl.svm.object.action.TextAction;
import at.stefl.svm.object.action.TextAlignAction;
import at.stefl.svm.object.action.TextArrayAction;
import at.stefl.svm.object.action.TextColorAction;
import at.stefl.svm.object.action.TextFillColorAction;
import at.stefl.svm.object.action.TextLanguageAction;
import at.stefl.svm.object.action.TextLineColorAction;
import at.stefl.svm.object.action.UnsupportedAction;

public enum ActionType {
    
    NULL(ActionTypeConstants.META_NULL_ACTION, NullAction.class),
    PIXEL(ActionTypeConstants.META_PIXEL_ACTION, PixelAction.class),
    POINT(ActionTypeConstants.META_POINT_ACTION, PointAction.class),
    LINE(ActionTypeConstants.META_LINE_ACTION, LineAction.class),
    RECT(ActionTypeConstants.META_RECT_ACTION, RectangleAction.class),
    ROUND_RECT(ActionTypeConstants.META_ROUNDRECT_ACTION),
    ELLIPSE(ActionTypeConstants.META_ELLIPSE_ACTION),
    ARC(ActionTypeConstants.META_ARC_ACTION),
    PIE(ActionTypeConstants.META_PIE_ACTION),
    CHORD(ActionTypeConstants.META_CHORD_ACTION),
    POLY_LINE(ActionTypeConstants.META_POLYLINE_ACTION, PolyLineAction.class),
    POLYGON(ActionTypeConstants.META_POLYGON_ACTION, PolygonAction.class),
    POLY_POLYGON(ActionTypeConstants.META_POLYPOLYGON_ACTION,
            PolyPolygonAction.class),
    TEXT(ActionTypeConstants.META_TEXT_ACTION, TextAction.class),
    TEXT_ARRAY(ActionTypeConstants.META_TEXTARRAY_ACTION, TextArrayAction.class),
    STRETCH_TEXT(ActionTypeConstants.META_STRETCHTEXT_ACTION),
    TEXT_RECT(ActionTypeConstants.META_TEXTRECT_ACTION),
    BMP(ActionTypeConstants.META_BMP_ACTION),
    BMP_SCALE(ActionTypeConstants.META_BMPSCALE_ACTION),
    BMP_SCALE_PART(ActionTypeConstants.META_BMPSCALEPART_ACTION),
    BMP_EX(ActionTypeConstants.META_BMPEX_ACTION),
    BMP_EX_SCALE(ActionTypeConstants.META_BMPEXSCALE_ACTION),
    BMP_EX_SCALE_PART(ActionTypeConstants.META_BMPEXSCALEPART_ACTION),
    MASK(ActionTypeConstants.META_MASK_ACTION),
    MASK_SCALE(ActionTypeConstants.META_MASKSCALE_ACTION),
    MASK_SCALE_PART(ActionTypeConstants.META_MASKSCALEPART_ACTION),
    GRADIENT(ActionTypeConstants.META_GRADIENT_ACTION),
    HATCH(ActionTypeConstants.META_HATCH_ACTION),
    WALLPAPER(ActionTypeConstants.META_WALLPAPER_ACTION),
    CLIP_REGION(ActionTypeConstants.META_CLIPREGION_ACTION),
    IS_ECT_RECT_CLIP_REGION(ActionTypeConstants.META_ISECTRECTCLIPREGION_ACTION),
    IS_ECT_REGI_ON_CLIP_REGION(
            ActionTypeConstants.META_ISECTREGIONCLIPREGION_ACTION),
    MOVE_CLIP_REGION(ActionTypeConstants.META_MOVECLIPREGION_ACTION),
    LINE_COLOR(ActionTypeConstants.META_LINECOLOR_ACTION, LineColorAction.class),
    FILL_COLOR(ActionTypeConstants.META_FILLCOLOR_ACTION, FillColorAction.class),
    TEXT_COLOR(ActionTypeConstants.META_TEXTCOLOR_ACTION, TextColorAction.class),
    TEXT_FILL_COLOR(ActionTypeConstants.META_TEXTFILLCOLOR_ACTION,
            TextFillColorAction.class), TEXT_ALIGN(
            ActionTypeConstants.META_TEXTALIGN_ACTION, TextAlignAction.class),
    MAP_MODE(ActionTypeConstants.META_MAPMODE_ACTION, MapModeAction.class),
    FONT(ActionTypeConstants.META_FONT_ACTION, FontAction.class), PUSH(
            ActionTypeConstants.META_PUSH_ACTION, PushAction.class), POP(
            ActionTypeConstants.META_POP_ACTION, PopAction.class), RASTER_OP(
            ActionTypeConstants.META_RASTEROP_ACTION), TRANSPARENT(
            ActionTypeConstants.META_TRANSPARENT_ACTION), EPS(
            ActionTypeConstants.META_EPS_ACTION), REF_POINT(
            ActionTypeConstants.META_REFPOINT_ACTION), TEXT_LINE_COLOR(
            ActionTypeConstants.META_TEXTLINECOLOR_ACTION,
            TextLineColorAction.class), TEXT_LINE(
            ActionTypeConstants.META_TEXTLINE_ACTION), FLOAT_TRANSPARENT(
            ActionTypeConstants.META_FLOATTRANSPARENT_ACTION), GRADIENT_EX(
            ActionTypeConstants.META_GRADIENTEX_ACTION), LAYOUT_MODE(
            ActionTypeConstants.META_LAYOUTMODE_ACTION), TEXT_LANGUAGE(
            ActionTypeConstants.META_TEXTLANGUAGE_ACTION,
            TextLanguageAction.class), OVERLINE_COLOR(
            ActionTypeConstants.META_OVERLINECOLOR_ACTION,
            OverLineColorAction.class), COMMENT(
            ActionTypeConstants.META_COMMENT_ACTION, CommentAction.class);
    
    private static final ObjectTransformer<ActionType, Integer> CODE_KEY_GENERATOR = new ObjectTransformer<ActionType, Integer>() {
        
        @Override
        public Integer transform(ActionType value) {
            return value.code;
        }
    };
    
    private static final ObjectTransformer<ActionType, Class<? extends SVMAction>> CLASS_KEY_GENERATOR = new ObjectTransformer<ActionType, Class<? extends SVMAction>>() {
        
        @Override
        public Class<? extends SVMAction> transform(ActionType value) {
            return value.actionObjectClass;
        }
    };
    
    // TODO: reduce to 1 loop
    private static final Map<Integer, ActionType> BY_CODE_MAP = CollectionUtil
            .toHashMap(CODE_KEY_GENERATOR, values());
    private static final Map<Class<? extends SVMAction>, ActionType> BY_CLASS_MAP = CollectionUtil
            .toHashMap(CLASS_KEY_GENERATOR, values());
    
    public static ActionType getByCode(int code) {
        return BY_CODE_MAP.get(code);
    }
    
    public static SVMAction newByCode(int code) {
        return getByCode(code).newActionObject();
    }
    
    public static ActionType getByClass(Class<? extends SVMAction> clazz) {
        return BY_CLASS_MAP.get(clazz);
    }
    
    public static SVMAction newByClass(Class<? extends SVMAction> clazz) {
        return getByClass(clazz).newActionObject();
    }
    
    private final int code;
    private final Class<? extends SVMAction> actionObjectClass;
    
    // TODO: remove me (debugging only)
    private ActionType(int code) {
        this(code, UnsupportedAction.class);
    }
    
    private ActionType(int code, Class<? extends SVMAction> actionObjectClass) {
        this.code = code;
        this.actionObjectClass = actionObjectClass;
    }
    
    public int getCode() {
        return code;
    }
    
    public Class<? extends SVMAction> getActionObjectClass() {
        return actionObjectClass;
    }
    
    public SVMAction newActionObject() {
        try {
            SVMAction result = actionObjectClass.newInstance();
            // TODO: remove me (debugging only)
            if (result instanceof UnsupportedAction) ((UnsupportedAction) result)
                    .setActionType(this);
            return result;
        } catch (Exception e) {
            throw new InaccessibleSectionException();
        }
    }
    
}