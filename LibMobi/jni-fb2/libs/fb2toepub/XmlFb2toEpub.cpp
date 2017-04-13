#include "hdr.h"

#include <sstream>
#include "converter.h"

namespace Fb2ToEpub
{

	static char* errMsg = NULL;

	//-----------------------------------------------------------------------
	int XmlConvert(InStm *pin, const strvector &css, const strvector &fonts, const strvector &mfonts,
		XlitConv *xlitConv, OutPackStm *pout)
	{
		// perform pass 1 to determine fb2 document structure and to collect all cross-references inside the fb2 file
		UnitArray units;
		// The input file name is pin->UIFileName();
		XMLDocument doc;
		doc.LoadFile(pin->UIFileName().c_str());
		XMLHandle hDoc(&doc);
		XMLHandle fb = hDoc.FirstChildElement("FictionBook");
		XMLHandle desc = fb.FirstChildElement("description");
		XMLHandle titleInfo = desc.FirstChildElement("title-info");
		XMLHandle genre = titleInfo.FirstChildElement("genre");
		XMLHandle genreInfo = genre.FirstChild();
		const char* txt = genreInfo.ToNode()->Value(); // "Ciencia-Ficción"

		// Now build from the above the damn epub!
		// Go directly to DoConvertionPass2 and substitute XML calls to make epub.

		// CONVERTION PASS 1 (DETERMINE DOCUMENT STRUCTURE AND COLLECT ALL CROSS-REFERENCES INSIDE THE FB2 FILE)
		Ptr<ConverterPass1> conv = new ConverterPass1(&units);
		conv->XmlScan(hDoc);
		//DoConvertionPass1(CreateScanner(pin), &units);
		//pin->Rewind();

		// sanity check
		if (units.size() == 0)
			InternalError(__FILE__, __LINE__, "I don't know why but it happened that there is no content in input file!");

		// perform pass 2 to create epub document
		//XmlConversionPass2(hDoc, css, fonts, mfonts, xlitConv, &units, pout);
		//DoConvertionPass2(CreateScanner(pin), css, fonts, mfonts, xlitConv, &units, pout);
		return 0;
	}

	void ConverterPass1::XmlScan(XMLHandle hDoc)
	{
		XmlFictionBook(hDoc);
	}


	//-----------------------------------------------------------------------
	bool ConverterPass1::XmlFictionBook(XMLHandle hDoc)
	{
		XMLHandle fb = hDoc.FirstChildElement("FictionBook");
		XMLElement* fbEl;
		if (!(fbEl = fb.ToElement())) {
			errMsg = "FictionBook element not found.";
			return false;
		}

		if (!fbEl->Attribute("xmlns", "http://www.gribuser.ru/xml/fictionbook/2.0")) {
			errMsg = "Missing FictionBook namespace definition.";
			return false;
		}
		if (!fbEl->Attribute("xmlns:l", "http://www.w3.org/1999/xlink") &&
			!fbEl->Attribute("xmlns:xlink", "http://www.w3.org/1999/xlink")) {
			errMsg = "Bad FictionBook namespace definition or bad xlink namespace definition.";
			return false;
		}

		//<description>
		XMLElement *desc = fbEl->FirstChildElement("description");
		if (!desc) {
			errMsg = "description element not found.";
			return false;
		}
		XMLElement *titleInfo = desc->FirstChildElement("title-info");
		if (!titleInfo) {
			errMsg = "title-info element not found.";
			return false;
		}
		// needs to process "annotation" and "coverpage" elements here
		XmlAnnotation(titleInfo->FirstChildElement("annotation"), true);

		//</description>

		//<body>
		body(Unit::MAIN);
		if (s_->IsNextElement("body"))
			body(Unit::NOTES);
		if (s_->IsNextElement("body"))
			body(Unit::COMMENTS);
		//</body>
	}

	//-----------------------------------------------------------------------
	void ConverterPass1::XmlAnnotation(XMLElement *annotation, bool startUnit)
	{
		if (!annotation)
			return;
		if (startUnit)
			units_->push_back(Unit(bodyType_, Unit::ANNOTATION, 0, -1));
		XmlAddId(annotation);

		for (XMLNode *ch = annotation->FirstChild(); ch; ch = ch->NextSibling())
		{
			//<p>, <poem>, <cite>, <subtitle>, <empty-line>, <table>
			const char *tag = ch->Value();
			if (!tag)
				continue;
			if (strcmp(tag, "p") == 0)
				p();
			else if (strcmp(tag, "poem") == 0)
				poem();
			else if (strcmp(tag, "cite") == 0)
				cite();
			else if (strcmp(tag, "subtitle") == 0)
				subtitle();
			else if (strcmp(tag, "empty-line") == 0)
				; // do nothing here, was: empty_line();
			else if (strcmp(tag, "table") == 0)
				table();
			//</p>, </poem>, </cite>, </subtitle>, </empty-line>, </table>
		}
	}

	//-----------------------------------------------------------------------
	const const char* ConverterPass1::XmlAddId(const XMLElement *el)
	{
		const char *id = el->Attribute("id");
		if (!id)
			return NULL;

		units_->back().refIds_.push_back(id);
		return id;
	}


};  //namespace Fb2ToEpub
