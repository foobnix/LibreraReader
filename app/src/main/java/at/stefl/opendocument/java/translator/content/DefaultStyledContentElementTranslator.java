package at.stefl.opendocument.java.translator.content;

import java.io.IOException;

import at.stefl.commons.lwxml.LWXMLUtil;
import at.stefl.commons.lwxml.reader.LWXMLPushbackReader;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.opendocument.java.translator.Retranslator;
import at.stefl.opendocument.java.translator.context.TranslationContext;

public class DefaultStyledContentElementTranslator<C extends TranslationContext>
		extends DefaultStyledElementTranslator<C> {

	public DefaultStyledContentElementTranslator(String elementName,
			StyleAttribute... attributes) {
		super(elementName, attributes);
	}

	@Override
	public void translateAttributeList(LWXMLPushbackReader in, LWXMLWriter out,
			C context) throws IOException {
		super.translateAttributeList(in, out, context);

		if (context.getSettings().isBackTranslateable()) {
			// TODO: out-source literal
			out.writeAttribute("contenteditable", "true");
		}
	}

	// TODO: fix me (whitespace?)
	@Override
	public void translateChildren(LWXMLPushbackReader in, LWXMLWriter out,
			TranslationContext context) throws IOException {
		if (LWXMLUtil.isEmptyElement(in)) {
			out.writeEmptyElement("br");
		} else {
			in.unreadEvent();
		}
	}

	@Override
	public void translateContent(LWXMLPushbackReader in, LWXMLWriter out,
			C context) throws IOException {
		if (context.getSettings().isBackTranslateable()) {
			Retranslator.writeEventNumber(in, out);
		}

		super.translateContent(in, out, context);
	}

}