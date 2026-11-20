package com.foobnix.ext.lit;

import java.util.HashMap;
import java.util.Map;

/**
 * The tag and attribute codes of Microsoft Reader's binary HTML and OPF, as ConvertLIT and
 * calibre (ebooks/lit/maps) have them. An attribute table is "code=name;code=name" with the codes
 * in hex; a name starting with % is one Reader keeps for itself and is dropped.
 */
final class LitMaps {

    private LitMaps() {
    }

    static final class Map3 {
        final String[] tags;
        final Map<Integer, String> attrs;
        final Map<Integer, String>[] tagAttrs;

        @SuppressWarnings("unchecked")
        Map3(String[] tags, String attrs, String[] tagAttrs) {
            this.tags = tags;
            this.attrs = parse(attrs);
            this.tagAttrs = new Map[tagAttrs == null ? 0 : tagAttrs.length];
            for (int i = 0; i < this.tagAttrs.length; i++) {
                this.tagAttrs[i] = tagAttrs[i] == null ? null : parse(tagAttrs[i]);
            }
        }
    }

    static Map<Integer, String> parse(String table) {
        final Map<Integer, String> map = new HashMap<Integer, String>();
        if (table == null || table.isEmpty()) {
            return map;
        }
        for (String pair : table.split(";")) {
            final int eq = pair.indexOf('=');
            map.put(Integer.parseInt(pair.substring(0, eq), 16), pair.substring(eq + 1));
        }
        return map;
    }

    static final String[] HTML_TAGS = {null, null, null, "a", "acronym", "address", "applet", "area", "b", "base", "basefont", "bdo", "bgsound", "big", "blink", "blockquote", "body", "br", "button", "caption", "center", "cite", "code", "col", "colgroup", null, null, "dd", "del", "dfn", "dir", "div", "dl", "dt", "em", "embed", "fieldset", "font", "form", "frame", "frameset", null, "h1", "h2", "h3", "h4", "h5", "h6", "head", "hr", "html", "i", "iframe", "img", "input", "ins", "kbd", "label", "legend", "li", "link", "tag61", "map", "tag63", "tag64", "meta", "nextid", "nobr", "noembed", "noframes", "noscript", "object", "ol", "option", "p", "param", "plaintext", "pre", "q", "rp", "rt", "ruby", "s", "samp", "script", "select", "small", "span", "strike", "strong", "style", "sub", "sup", "table", "tbody", "tc", "td", "textarea", "tfoot", "th", "thead", "title", "tr", "tt", "u", "ul", "var", "wbr", null};
    static final String HTML_ATTRS = "8010=tabindex;8046=title;804b=style;804d=disabled;83ea=class;83eb=id;83fe=datafld;83ff=datasrc;8400=dataformatas;87d6=accesskey;9392=lang;93ed=language;93fe=dir;9771=onmouseover;9772=onmouseout;9773=onmousedown;9774=onmouseup;9775=onmousemove;9776=onkeydown;9777=onkeyup;9778=onkeypress;9779=onclick;977a=ondblclick;977e=onhelp;977f=onfocus;9780=onblur;9783=onrowexit;9784=onrowenter;9786=onbeforeupdate;9787=onafterupdate;978a=onreadystatechange;9790=onscroll;9794=ondragstart;9795=onresize;9796=onselectstart;9797=onerrorupdate;9799=ondatasetchanged;979a=ondataavailable;979b=ondatasetcomplete;979c=onfilterchange;979f=onlosecapture;97a0=onpropertychange;97a2=ondrag;97a3=ondragend;97a4=ondragenter;97a5=ondragover;97a6=ondragleave;97a7=ondrop;97a8=oncut;97a9=oncopy;97aa=onpaste;97ab=onbeforecut;97ac=onbeforecopy;97ad=onbeforepaste;97af=onrowsdelete;97b0=onrowsinserted;97b1=oncellchange;97b2=oncontextmenu;97b6=onbeforeeditfocus";
    static final String[] HTML_TAG_ATTRS = {
            null,
            null,
            null,
            "1=href;3ec=target;3ee=rel;3ef=rev;3f0=urn;3f1=methods;8001=name;8046=title;804b=style;83ea=class;83eb=id",
            null,
            "9399=clear",
            "8001=name;8006=width;8007=height;804a=align;8bbb=classid;8bbc=data;8bbf=codebase;8bc0=codetype;8bc1=code;8bc2=type;8bc5=vspace;8bc6=hspace;978e=onerror",
            "1=href;3ea=shape;3eb=coords;3ed=target;3ee=alt;3ef=nohref;8046=title;804b=style;83ea=class;83eb=id",
            "8046=title;804b=style;83ea=class;83eb=id",
            "3ec=href;3ed=target",
            "938b=color;939b=face;93a3=size",
            null,
            "3ea=src;3eb=loop;3ec=volume;3ed=balance",
            "8046=title;804b=style;83ea=class;83eb=id",
            null,
            "8046=title;804b=style;83ea=class;83eb=id;9399=clear",
            "7db=link;7dc=alink;7dd=vlink;8046=title;804b=style;83ea=class;83eb=id;938a=background;938b=text;938e=nowrap;93ae=topmargin;93af=rightmargin;93b0=bottommargin;93b1=leftmargin;93b6=bgproperties;93d8=scroll;977b=onselect;9791=onload;9792=onunload;9798=onbeforeunload;97b3=onbeforeprint;97b4=onafterprint;fe0c=bgcolor",
            "8046=title;804b=style;83ea=class;83eb=id;9399=clear",
            "7d1=type;8001=name",
            "8046=title;8049=align;804b=style;83ea=class;83eb=id;93a8=valign",
            "8046=title;804b=style;83ea=class;83eb=id;9399=clear",
            "8046=title;804b=style;83ea=class;83eb=id",
            "8046=title;804b=style;83ea=class;83eb=id",
            "3ea=span;8006=width;8049=align;93a8=valign;fe0c=bgcolor",
            "3ea=span;8006=width;8049=align;93a8=valign;fe0c=bgcolor",
            null,
            null,
            "8046=title;804b=style;83ea=class;83eb=id;938e=nowrap",
            null,
            "8046=title;804b=style;83ea=class;83eb=id",
            null,
            "8046=title;8049=align;804b=style;83ea=class;83eb=id;938e=nowrap",
            "3ea=compact;8046=title;804b=style;83ea=class;83eb=id",
            "8046=title;804b=style;83ea=class;83eb=id;938e=nowrap",
            "8046=title;804b=style;83ea=class;83eb=id",
            "8001=name;8006=width;8007=height;804a=align;8bbd=palette;8bbe=pluginspage;8bbf=src;8bc1=units;8bc2=type;8bc3=hidden",
            "804a=align",
            "8046=title;804b=style;83ea=class;83eb=id;938b=color;939b=face;939c=size",
            "3ea=action;3ec=enctype;3ed=method;3ef=target;3f4=accept-charset;8001=name;977c=onsubmit;977d=onreset",
            "8000=align;8001=name;8bb9=src;8bbb=border;8bbc=frameborder;8bbd=framespacing;8bbe=marginwidth;8bbf=marginheight;8bc0=noresize;8bc1=scrolling;8fa2=bordercolor",
            "3e9=rows;3ea=cols;3eb=border;3ec=bordercolor;3ed=frameborder;3ee=framespacing;8001=name;9791=onload;9792=onunload;9798=onbeforeunload;97b3=onbeforeprint;97b4=onafterprint",
            null,
            "8046=title;8049=align;804b=style;83ea=class;83eb=id;9399=clear",
            "8046=title;8049=align;804b=style;83ea=class;83eb=id;9399=clear",
            "8046=title;8049=align;804b=style;83ea=class;83eb=id;9399=clear",
            "8046=title;8049=align;804b=style;83ea=class;83eb=id;9399=clear",
            "8046=title;8049=align;804b=style;83ea=class;83eb=id;9399=clear",
            "8046=title;8049=align;804b=style;83ea=class;83eb=id;9399=clear",
            null,
            "3ea=noshade;8006=width;8007=size;8046=title;8049=align;804b=style;83ea=class;83eb=id;938b=color",
            null,
            "8046=title;804b=style;83ea=class;83eb=id",
            "8001=name;8006=width;8007=height;804a=align;8bb9=src;8bbb=border;8bbc=frameborder;8bbd=framespacing;8bbe=marginwidth;8bbf=marginheight;8bc0=noresize;8bc1=scrolling;8fa2=vspace;8fa3=hspace",
            "3eb=alt;3ec=src;3ed=border;3ee=vspace;3ef=hspace;3f0=lowsrc;3f1=vrml;3f2=dynsrc;3f4=loop;3f6=start;7d3=ismap;7d9=usemap;8001=name;8006=width;8007=height;8046=title;804a=align;804b=style;83ea=class;83eb=id;978d=onabort;978e=onerror;9791=onload",
            "7d1=type;7d3=size;7d4=maxlength;7d6=readonly;7d8=indeterminate;7da=checked;7db=alt;7dc=src;7dd=border;7de=vspace;7df=hspace;7e0=lowsrc;7e1=vrml;7e2=dynsrc;7e4=loop;7e5=start;8001=name;8006=width;8007=height;804a=align;93ee=value;977b=onselect;978d=onabort;978e=onerror;978f=onchange;9791=onload",
            null,
            "8046=title;804b=style;83ea=class;83eb=id",
            "3e9=for",
            "804a=align",
            "3ea=value;8046=title;804b=style;83ea=class;83eb=id;939a=type",
            "3ee=href;3ef=rel;3f0=rev;3f1=type;3f9=media;3fa=target;8046=title;804b=style;83ea=class;83eb=id;978e=onerror;9791=onload",
            "9399=clear",
            "8001=name;8046=title;804b=style;83ea=class;83eb=id",
            "1771=scrolldelay;1772=direction;1773=behavior;1774=scrollamount;1775=loop;1776=vspace;1777=hspace;1778=truespeed;8006=width;8007=height;9785=onbounce;978b=onfinish;978c=onstart;fe0c=bgcolor",
            null,
            "3ea=http-equiv;3eb=content;3ec=url;3f6=charset;8001=name",
            "3f5=n",
            null,
            null,
            null,
            null,
            "8000=usemap;8001=name;8006=width;8007=height;8046=title;804a=align;804b=style;83ea=class;83eb=id;8bbb=classid;8bbc=data;8bbf=codebase;8bc0=codetype;8bc1=code;8bc2=type;8bc5=vspace;8bc6=hspace;978e=onerror",
            "3eb=compact;3ec=start;8046=title;804b=style;83ea=class;83eb=id;939a=type",
            "3ea=selected;3eb=value",
            "8046=title;8049=align;804b=style;83ea=class;83eb=id;9399=clear",
            "8000=type",
            "9399=clear",
            "8046=title;804b=style;83ea=class;83eb=id;9399=clear",
            "8046=title;804b=style;83ea=class;83eb=id",
            null,
            null,
            null,
            "8046=title;804b=style;83ea=class;83eb=id",
            "8046=title;804b=style;83ea=class;83eb=id",
            "3ea=src;3ed=for;3ee=event;3f0=defer;3f2=type;978e=onerror",
            "3eb=size;3ec=multiple;8000=align;8001=name;978f=onchange",
            "8046=title;804b=style;83ea=class;83eb=id",
            "8046=title;804b=style;83ea=class;83eb=id",
            "8046=title;804b=style;83ea=class;83eb=id",
            "8046=title;804b=style;83ea=class;83eb=id",
            "3eb=type;3ef=media;8046=title;978e=onerror;9791=onload",
            "8046=title;804b=style;83ea=class;83eb=id",
            "8046=title;804b=style;83ea=class;83eb=id",
            "3ea=cols;3eb=border;3ec=rules;3ed=frame;3ee=cellspacing;3ef=cellpadding;3fa=datapagesize;8006=width;8007=height;8046=title;804a=align;804b=style;83ea=class;83eb=id;938a=background;93a5=bordercolor;93a6=bordercolorlight;93a7=bordercolordark;fe0c=bgcolor",
            "8049=align;93a8=valign;fe0c=bgcolor",
            "8049=align;93a8=valign",
            "7d2=rowspan;7d3=colspan;8006=width;8007=height;8046=title;8049=align;804b=style;83ea=class;83eb=id;938a=background;938e=nowrap;93a5=bordercolor;93a6=bordercolorlight;93a7=bordercolordark;93a8=valign;fe0c=bgcolor",
            "1b5a=rows;1b5b=cols;1b5c=wrap;1b5d=readonly;8001=name;977b=onselect;978f=onchange",
            "8049=align;93a8=valign;fe0c=bgcolor",
            "7d2=rowspan;7d3=colspan;8006=width;8007=height;8046=title;8049=align;804b=style;83ea=class;83eb=id;938a=background;938e=nowrap;93a5=bordercolor;93a6=bordercolorlight;93a7=bordercolordark;93a8=valign;fe0c=bgcolor",
            "8049=align;93a8=valign;fe0c=bgcolor",
            null,
            "8007=height;8046=title;8049=align;804b=style;83ea=class;83eb=id;93a5=bordercolor;93a6=bordercolorlight;93a7=bordercolordark;93a8=valign;fe0c=bgcolor",
            "8046=title;804b=style;83ea=class;83eb=id",
            "8046=title;804b=style;83ea=class;83eb=id",
            "3eb=compact;8046=title;804b=style;83ea=class;83eb=id;939a=type",
            "8046=title;804b=style;83ea=class;83eb=id",
            null,
            null,
    };
    static final String[] OPF_TAGS = {null, "package", "dc:Title", "dc:Creator", null, null, null, null, null, null, null, null, null, null, null, null, "manifest", "item", "spine", "itemref", "metadata", "dc-metadata", "dc:Subject", "dc:Description", "dc:Publisher", "dc:Contributor", "dc:Date", "dc:Type", "dc:Format", "dc:Identifier", "dc:Source", "dc:Language", "dc:Relation", "dc:Coverage", "dc:Rights", "x-metadata", "meta", "tours", "tour", "site", "guide", "reference", null};
    static final String OPF_ATTRS = "1=href;2=%never-used;3=%guid;4=%minimum_level;5=%attr5;6=id;7=href;8=media-type;9=fallback;a=idref;b=xmlns:dc;c=xmlns:oebpackage;d=role;e=file-as;f=event;10=scheme;11=title;12=type;13=unique-identifier;14=name;15=content;16=xml:lang";

    static final Map3 HTML = new Map3(HTML_TAGS, HTML_ATTRS, HTML_TAG_ATTRS);
    static final Map3 OPF = new Map3(OPF_TAGS, OPF_ATTRS, null);
}
