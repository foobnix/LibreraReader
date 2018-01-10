/** @file xmlwriter.h
 *
 * Copyright (c) 2016 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#ifndef mobi_minixml_h
#define mobi_minixml_h

#include <stdio.h>
#include "buffer.h"
#include "structure.h"

#define BAD_CAST (xmlChar *)
#define LIBXML_TEST_VERSION

#define xmlCleanupParser()

typedef unsigned char xmlChar;

/**
 @brief Buffer for xml output.
 For libxml2 compatibility, it is a wrapper for MOBIBuffer
 */
typedef struct {
    xmlChar *content; /**< Points to mobibuffer->data */
    MOBIBuffer *mobibuffer; /**< Dynamic buffer */
} xmlBuffer;
typedef xmlBuffer *xmlBufferPtr;

/** 
 @brief Xml writer states
 */
typedef enum {
    MOBI_XMLMODE_NONE = 0,
    MOBI_XMLMODE_NAME,
    MOBI_XMLMODE_ATTR,
    MOBI_XMLMODE_TEXT
} MOBI_XML_MODE;

/**
 @brief Xml writer states list structure
 First element in the list is currently processed element.
 Last element is root of the document
 */
typedef struct MOBIXmlState {
    char *name; /**< Element name */
    MOBI_XML_MODE mode; /**< State mode */
    struct MOBIXmlState *next; /**< Next list item */
} MOBIXmlState;

/**
 @brief Xml TextWriter structure
 */
typedef struct {
    xmlBufferPtr xmlbuf; /**< XML buffer */
    MOBIXmlState *states; /**< TextWriter states list */
    char *nsname; /**< Namespace attribute name */
    char *nsvalue; /**< Namespace attribute value */
    bool indent_enable; /**< Enable indentation */
    bool indent_next; /**< Indentation needed */
} xmlTextWriter;
typedef xmlTextWriter *xmlTextWriterPtr;

xmlBufferPtr xmlBufferCreate(void);
void xmlBufferFree(xmlBufferPtr buf);
xmlTextWriterPtr xmlNewTextWriterMemory(xmlBufferPtr xmlbuf, int compression);
void xmlFreeTextWriter(xmlTextWriterPtr writer);
int xmlTextWriterStartDocument(xmlTextWriterPtr writer, const char *version,
                               const char *encoding, const char *standalone);
int xmlTextWriterEndDocument(xmlTextWriterPtr writer);
int xmlTextWriterStartElement(xmlTextWriterPtr writer, const xmlChar *name);
int xmlTextWriterEndElement(xmlTextWriterPtr writer);
int xmlTextWriterWriteAttribute(xmlTextWriterPtr writer, const xmlChar *name,
                                const xmlChar *content);
int xmlTextWriterEndAttribute(xmlTextWriterPtr writer);
int xmlTextWriterWriteAttributeNS(xmlTextWriterPtr writer,
                                  const xmlChar * prefix, const xmlChar * name,
                                  const xmlChar * namespaceURI,
                                  const xmlChar * content);
int xmlTextWriterStartElementNS(xmlTextWriterPtr writer,
                                const xmlChar *prefix, const xmlChar *name,
                                const xmlChar * namespaceURI);
int xmlTextWriterWriteElementNS(xmlTextWriterPtr writer, const xmlChar *prefix,
                                const xmlChar *name, const xmlChar *namespaceURI,
                                const xmlChar *content);
int xmlTextWriterWriteString(xmlTextWriterPtr writer, const xmlChar *content);
int xmlTextWriterSetIndent(xmlTextWriterPtr writer, int indent);

#endif
