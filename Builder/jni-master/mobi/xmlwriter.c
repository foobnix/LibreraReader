/** @file xmlwriter.c
 *  @brief Implements a simplified subset of libxml2 functions used in libmobi.
 *
 * Copyright (c) 2016 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#define _GNU_SOURCE 1
#ifndef __USE_BSD
#define __USE_BSD /* for strdup on linux/glibc */
#endif

#include <stdlib.h>
#include <string.h>
#include "xmlwriter.h"
#include "debug.h"
#include "util.h"
#include "parse_rawml.h"

#define MOBI_XML_BUFFERSIZE 4096
#define MOBI_XML_STATESSIZE 200
#define XML_ERROR -1
#define XML_OK 0

/**
 @brief Initiate xml states with first name and mode pair
 
 @param[in] name MOBIRawml State element name
 @param[in] mode MOBIRawml State mode
 @return New state
 */
static MOBIXmlState * mobi_xml_state_init(const char *name, const MOBI_XML_MODE mode) {
    MOBIXmlState *curr = calloc(1, sizeof(MOBIXmlState));
    if (curr == NULL) {
        return NULL;
    }
    curr->name = strdup(name);
    if (curr->name == NULL) {
        free(curr);
        return NULL;
    }
    curr->mode = mode;
    return curr;
}

/**
 @brief Get current active state from the list
 
 @param[in] writer xmlTextWriter
 @return State
 */
static MOBIXmlState * mobi_xml_state_current(const xmlTextWriterPtr writer) {
    return writer->states;
}

/**
 @brief Add new state to the list
 
 @param[in,out] writer xmlTextWriter
 @param[in] name MOBIRawml State element name
 @param[in] mode MOBIRawml State mode
 @return Added state
 */
static MOBIXmlState * mobi_xml_state_push(xmlTextWriterPtr writer, const char *name, const MOBI_XML_MODE mode) {
    MOBIXmlState *new = mobi_xml_state_init(name, mode);
    MOBIXmlState *first = writer->states;
    if (!first) {
        writer->states = new;
    } else {
        new->next = first;
        writer->states = new;
    }
    return writer->states;
}

/**
 @brief Remove state from the list
 
 @param[in] state State structure that will be deleted
 @return Next state in the list or NULL if not present
 */
static MOBIXmlState * mobi_xml_state_del(MOBIXmlState *state) {
    MOBIXmlState *del = state;
    state = state->next;
    free(del->name);
    free(del);
    del = NULL;
    return state;
}

/**
 @brief Remove state from the beginning of the list
 
 @param[in,out] writer xmlTextWriter
 @return Next state or NULL if not present
 */
static MOBIXmlState * mobi_xml_state_pop(xmlTextWriterPtr writer) {
    writer->states = mobi_xml_state_del(writer->states);
    return writer->states;
}

/**
 @brief Remove all states from the list
 
 @param[in,out] first First state from the list
 */
static void mobi_xml_state_delall(MOBIXmlState *first) {
    while (first) {
        first = mobi_xml_state_del(first);
    }
}

/**
 @brief Get current level of nested element
 
 @param[in] writer xmlTextWriter
 @return Level
 */
static size_t mobi_xml_level(const xmlTextWriterPtr writer) {
    MOBIXmlState *curr = writer->states;
    size_t level = 0;
    while (curr) {
        level++;
        if (curr->next == NULL) {
            break;
        }
        curr = curr->next;
    }
    return level;
}

/**
 @brief Write string to xml buffer
 
 @param[in,out] writer xmlTextWriter
 @param[in] string String
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_xml_buffer_addstring(xmlTextWriterPtr writer, const char *string) {
    if (writer == NULL || writer->xmlbuf == NULL || writer->xmlbuf->mobibuffer == NULL || string == NULL) {
        return MOBI_INIT_FAILED;
    }
    MOBIBuffer *buf = writer->xmlbuf->mobibuffer;
    buffer_addstring(buf, string);
    if (buf->error == MOBI_BUFFER_END) {
        buffer_resize(buf, buf->maxlen * 2);
        if (buf->error != MOBI_SUCCESS) {
            return buf->error;
        }
        /* update xmlbuf->content */
        writer->xmlbuf->content = writer->xmlbuf->mobibuffer->data;
        mobi_xml_buffer_addstring(writer, string);
    }
    return buf->error;
}

/**
 @brief Write character to xml buffer
 
 @param[in,out] writer xmlTextWriter
 @param[in] c Character
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_xml_buffer_addchar(xmlTextWriterPtr writer, const unsigned char c) {
    if (writer == NULL || writer->xmlbuf == NULL || writer->xmlbuf->mobibuffer == NULL) {
        return MOBI_INIT_FAILED;
    }
    MOBIBuffer *buf = writer->xmlbuf->mobibuffer;
    buffer_add8(buf, c);
    if (buf->error == MOBI_BUFFER_END) {
        buffer_resize(buf, buf->maxlen * 2);
        if (buf->error != MOBI_SUCCESS) {
            return buf->error;
        }
        /* update xmlbuf->content */
        writer->xmlbuf->content = writer->xmlbuf->mobibuffer->data;
        mobi_xml_buffer_addchar(writer, c);
    }
    return buf->error;
}

/**
 @brief Write terminating null character to xml buffer
 
 @param[in,out] writer xmlTextWriter
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_xml_buffer_flush(xmlTextWriterPtr writer) {
    return mobi_xml_buffer_addchar(writer, '\0');
}

/**
 @brief Write string with encoded reserved characters to xml buffer
 
 @param[in,out] writer xmlTextWriter
 @param[in] string String
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_xml_buffer_addencoded(xmlTextWriterPtr writer, const char *string) {
    if (string == NULL) {
        return MOBI_INIT_FAILED;
    }
    MOBI_RET ret = MOBI_SUCCESS;
    unsigned char *p = (unsigned char *)string;
    unsigned char c;
    while ((c = *p++)) {
        switch (c) {
            case '<':
                ret = mobi_xml_buffer_addstring(writer, "&lt;");
                break;
            case '>':
                ret = mobi_xml_buffer_addstring(writer, "&gt;");
                break;
            case '&':
                ret = mobi_xml_buffer_addstring(writer, "&amp;");
                break;
            case '"':
                ret = mobi_xml_buffer_addstring(writer, "&quot;");
                break;
            case '\r':
                ret = mobi_xml_buffer_addstring(writer, "&#13;");
                break;
            default:
                ret = mobi_xml_buffer_addchar(writer, c);
                break;
        }
        if (ret != MOBI_SUCCESS) {
            break;
        }
    }
    return ret;
}

/**
 @brief Write attribute value with encoded reserved characters to xml buffer
 
 @param[in,out] writer xmlTextWriter
 @param[in] string String
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_xml_buffer_addencoded_attr(xmlTextWriterPtr writer, const char *string) {
    if (string == NULL) {
        return MOBI_INIT_FAILED;
    }
    MOBI_RET ret = MOBI_SUCCESS;
    unsigned char *p = (unsigned char *)string;
    unsigned char c;
    while ((c = *p++)) {
        switch (c) {
            case '<':
                ret = mobi_xml_buffer_addstring(writer, "&lt;");
                break;
            case '>':
                ret = mobi_xml_buffer_addstring(writer, "&gt;");
                break;
            case '&':
                ret = mobi_xml_buffer_addstring(writer, "&amp;");
                break;
            case '"':
                ret = mobi_xml_buffer_addstring(writer, "&quot;");
                break;
            case '\r':
                ret = mobi_xml_buffer_addstring(writer, "&#13;");
                break;
            case '\n':
                ret = mobi_xml_buffer_addstring(writer, "&#10;");
                break;
            case '\t':
                ret = mobi_xml_buffer_addstring(writer, "&#9;");
                break;
            default:
                ret = mobi_xml_buffer_addchar(writer, c);
                break;
        }
        if (ret != MOBI_SUCCESS) {
            break;
        }
    }
    return ret;
}

/**
 @brief Write indent to xml buffer
 
 @param[in,out] writer xmlTextWriter
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_xml_write_indent(xmlTextWriterPtr writer) {
    if (writer == NULL) {
        debug_print("%s\n", "XML writer init failed");
        return MOBI_INIT_FAILED;
    }
    size_t levels_count = mobi_xml_level(writer);
    if (levels_count > 0) {
        /* don't indent first level */
        levels_count--;
    }
    MOBI_RET ret = MOBI_SUCCESS;
    while (levels_count--) {
        ret = mobi_xml_buffer_addchar(writer, ' ');
        if (ret != MOBI_SUCCESS) {
            break;
        }
    }
    return ret;
}

/**
 @brief Write namespace declaration if needed
 
 @param[in,out] writer xmlTextWriter
 */
static void mobi_xml_write_ns(xmlTextWriterPtr writer) {
    if (writer && writer->nsname && writer->nsvalue) {
        xmlTextWriterWriteAttribute(writer, (unsigned char *) writer->nsname, (unsigned char *) writer->nsvalue);
        free(writer->nsname);
        writer->nsname = NULL;
        free(writer->nsvalue);
        writer->nsvalue = NULL;
    }
}

/**
 @brief Save namespace declaration parameters
 
 @param[in,out] writer xmlTextWriter
 @param[in] nsname NS attribute name
 @param[in] nsvalue NS attribute value
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_xml_save_ns(xmlTextWriterPtr writer, const char *nsname, const char *nsvalue) {
    /* Only one declaration should be enough for libmobi */
    if (writer && writer->nsname == NULL && writer->nsvalue == NULL) {
        writer->nsname = strdup(nsname);
        if (writer->nsname == NULL) {
            return MOBI_MALLOC_FAILED;
        }
        writer->nsvalue = strdup(nsvalue);
        if (writer->nsvalue == NULL) {
            return MOBI_MALLOC_FAILED;
        }
    }
    return MOBI_SUCCESS;
}


/*
 
 Libxml2 compatible functions
 
 */

/**
 @brief Create xml buffer
 
 Libxml2 compatibility wrapper for MOBIBuffer structure.
 Must be deallocated with xmlBufferFree
 
 @return Buffer pointer
 */
xmlBufferPtr xmlBufferCreate(void) {
    xmlBufferPtr xmlbuf = NULL;
    xmlbuf = malloc(sizeof(xmlBuffer));
    if (xmlbuf == NULL) {
        debug_print("%s", "Buffer allocation failed\n");
        return NULL;
    }
    unsigned int size = MOBI_XML_BUFFERSIZE;
    MOBIBuffer *buf = buffer_init(size);
    if (buf == NULL) {
        free(xmlbuf);
        return NULL;
    }
    xmlbuf->content = buf->data;
    xmlbuf->mobibuffer = buf;
    return xmlbuf;
}

/**
 @brief Free xml buffer
  */
void xmlBufferFree(xmlBufferPtr buf) {
    if (buf == NULL) { return; }
    if (buf->mobibuffer != NULL) {
        buffer_free(buf->mobibuffer);
    }
    free(buf);
}

/**
 @brief Initialize TextWriter structure
 
 @param[in] xmlbuf Initialized xml output buffer
 @param[in] compression Unused
 @return TextWriter pointer
 */
xmlTextWriterPtr xmlNewTextWriterMemory(xmlBufferPtr xmlbuf, int compression) {
    UNUSED(compression);
    if (xmlbuf == NULL) {
        debug_print("%s", "XML buffer not initialized\n");
        return NULL;
    }
    xmlTextWriterPtr writer = NULL;
    writer = malloc(sizeof(xmlTextWriter));
    if (writer == NULL) {
        debug_print("%s", "XML writer allocation failed\n");
        return NULL;
    }
    writer->xmlbuf = xmlbuf;
    writer->states = NULL;
    writer->nsname = NULL;
    writer->nsvalue = NULL;
    writer->indent_enable = false;
    writer->indent_next = false;
    return writer;
}

/**
 @brief Deallocate TextWriter instance and all its resources
 
 @param[in,out] writer TextWriter
 */
void xmlFreeTextWriter(xmlTextWriterPtr writer) {
    if (writer == NULL) { return; }
    if (writer->states != NULL) {
        mobi_xml_state_delall(writer->states);
        writer->states = NULL;
    }
    free(writer->nsname);
    free(writer->nsvalue);
    free(writer);
}

/**
 @brief Start xml document
 
 Only utf-8 encoding supported.
 
 @param[in] writer TextWriter
 @param[in] version Value of version attribute, "1.0" if NULL
 @param[in] encoding Unused, defaults to utf-8
 @param[in] standalone Unused, omitted in declaration
 @return TextWriter pointer
 */
int xmlTextWriterStartDocument(xmlTextWriterPtr writer, const char *version,
                               const char *encoding, const char *standalone) {
    UNUSED(encoding);
    UNUSED(standalone);
    if (writer == NULL) {
        debug_print("%s\n", "XML writer init failed");
        return XML_ERROR;
    }
    if (mobi_xml_level(writer) > 0) {
        debug_print("%s\n", "XML document already started");
        return XML_ERROR;
    }
    MOBI_RET ret = mobi_xml_buffer_addstring(writer, "<?xml version=");
    if (ret != MOBI_SUCCESS) { return XML_ERROR; }
    if (version == NULL) {
        ret = mobi_xml_buffer_addstring(writer, "\"1.0\"");
    } else {
        ret = mobi_xml_buffer_addstring(writer, version);
    }
    if (ret != MOBI_SUCCESS) { return XML_ERROR; }
    ret = mobi_xml_buffer_addstring(writer, "?>\n");
    if (ret != MOBI_SUCCESS) { return XML_ERROR; }
    return XML_OK;
}

/**
 @brief End xml document
 
 All open elements will be closed.
 xmlBuffer will be flushed.
 
 @param[in] writer TextWriter
 @return XML_OK (0) on success, XML_ERROR (-1) on failure
 */
int xmlTextWriterEndDocument(xmlTextWriterPtr writer) {
    if (writer == NULL) {
        debug_print("%s\n", "XML writer init failed");
        return XML_ERROR;
    }
    MOBIXmlState *state = NULL;
    while((state = mobi_xml_state_current(writer))) {
        switch (state->mode) {
            case MOBI_XMLMODE_NAME:
            case MOBI_XMLMODE_ATTR:
            case MOBI_XMLMODE_TEXT:
                xmlTextWriterEndElement(writer);
                break;
                
            default:
                break;
        }
    }
    MOBI_RET ret;
    if (!writer->indent_enable) {
        ret = mobi_xml_buffer_addstring(writer, "\n");
        if (ret != MOBI_SUCCESS) { return XML_ERROR; }
    }
    ret = mobi_xml_buffer_flush(writer);
    if (ret != MOBI_SUCCESS) { return XML_ERROR; }
    return XML_OK;
}

/**
 @brief Start xml element
 
 @param[in,out] writer TextWriter
 @param[in] name Element name
 @return XML_OK (0) on success, XML_ERROR (-1) on failure
 */
int xmlTextWriterStartElement(xmlTextWriterPtr writer, const xmlChar *name) {
    if (writer == NULL || name == NULL || *name == '\0') {
        debug_print("%s\n", "XML writer init failed");
        return XML_ERROR;
    }
    MOBI_RET ret = MOBI_SUCCESS;
    MOBIXmlState *state = mobi_xml_state_current(writer);
    if (state) {
        switch (state->mode) {
            case MOBI_XMLMODE_ATTR:
                if (xmlTextWriterEndAttribute(writer) == XML_ERROR) { return XML_ERROR; }
                /* fallthrough */
            case MOBI_XMLMODE_NAME:
                /* TODO: output ns declarations */
                mobi_xml_write_ns(writer);
                ret = mobi_xml_buffer_addstring(writer, ">");
                if (ret != MOBI_SUCCESS) { return XML_ERROR; }
                if (writer->indent_enable) {
                    ret = mobi_xml_buffer_addstring(writer, "\n");
                    if (ret != MOBI_SUCCESS) { return XML_ERROR; }
                }
                state->mode = MOBI_XMLMODE_TEXT;
                if (ret != MOBI_SUCCESS) { return XML_ERROR; }
                break;
                
            default:
                break;
        }
    }
    mobi_xml_state_push(writer, (char *) name, MOBI_XMLMODE_NAME);
    if (writer->indent_enable) {
        ret = mobi_xml_write_indent(writer);
        if (ret != MOBI_SUCCESS) { return XML_ERROR; }
    }
    ret = mobi_xml_buffer_addstring(writer, "<");
    if (ret != MOBI_SUCCESS) { return XML_ERROR; }
    ret = mobi_xml_buffer_addstring(writer, (const char *) name);
    if (ret != MOBI_SUCCESS) { return XML_ERROR; }
    return XML_OK;
}

/**
 @brief End current element
 
 @param[in] writer TextWriter
 @return XML_OK (0) on success, XML_ERROR (-1) on failure
 */
int xmlTextWriterEndElement(xmlTextWriterPtr writer) {
    if (writer == NULL) {
        debug_print("%s\n", "XML writer init failed");
        return XML_ERROR;
    }
    MOBI_RET ret = MOBI_SUCCESS;
    MOBIXmlState *state = mobi_xml_state_current(writer);
    if (state == NULL) { return XML_ERROR; }
    switch (state->mode) {
        case MOBI_XMLMODE_ATTR:
            if (xmlTextWriterEndAttribute(writer) == XML_ERROR) { return XML_ERROR; }
            mobi_xml_state_pop(writer);
            /* fallthrough */
        case MOBI_XMLMODE_NAME:
            /* output namespace declarations */
            mobi_xml_write_ns(writer);
            if (writer->indent_enable) {
                writer->indent_next = true;
            }
            ret = mobi_xml_buffer_addstring(writer, "/>");
            if (ret != MOBI_SUCCESS) { return XML_ERROR; }
            break;
        case MOBI_XMLMODE_TEXT:
            if (writer->indent_enable && writer->indent_next) {
                ret = mobi_xml_write_indent(writer);
                if (ret != MOBI_SUCCESS) { return XML_ERROR; }
                writer->indent_next = true;
            } else {
                writer->indent_next = true;
            }
            ret = mobi_xml_buffer_addstring(writer, "</");
            if (ret != MOBI_SUCCESS) { return XML_ERROR; }
            ret = mobi_xml_buffer_addstring(writer, state->name);
            if (ret != MOBI_SUCCESS) { return XML_ERROR; }
            ret = mobi_xml_buffer_addstring(writer, ">");
            if (ret != MOBI_SUCCESS) { return XML_ERROR; }
            break;
            
        default:
            break;
    }
    if (writer->indent_enable) {
        ret = mobi_xml_buffer_addstring(writer, "\n");
        if (ret != MOBI_SUCCESS) { return XML_ERROR; }
    }
    mobi_xml_state_pop(writer);
    return XML_OK;
}

/**
 @brief Start attribute for current xml element
 
 @param[in,out] writer TextWriter
 @param[in] name Attribute name
 @return XML_OK (0) on success, XML_ERROR (-1) on failure
 */
int xmlTextWriterStartAttribute(xmlTextWriterPtr writer, const xmlChar *name) {
    if (writer == NULL) {
        debug_print("%s\n", "XML writer init failed");
        return XML_ERROR;
    }
    if (name == NULL || *name == '\0') {
        debug_print("%s\n", "XML writer init failed");
        return XML_ERROR;
    }
    MOBI_RET ret = MOBI_SUCCESS;
    MOBIXmlState *state = mobi_xml_state_current(writer);
    if (state) {
        switch (state->mode) {
            case MOBI_XMLMODE_ATTR:
                if (xmlTextWriterEndAttribute(writer) == XML_ERROR) { return XML_ERROR; }
                /* fallthrough */
            case MOBI_XMLMODE_NAME:
                ret = mobi_xml_buffer_addstring(writer, " ");
                if (ret != MOBI_SUCCESS) { return XML_ERROR; }
                ret = mobi_xml_buffer_addstring(writer, (const char *) name);
                if (ret != MOBI_SUCCESS) { return XML_ERROR; }
                ret = mobi_xml_buffer_addstring(writer, "=\"");
                if (ret != MOBI_SUCCESS) { return XML_ERROR; }
                state->mode = MOBI_XMLMODE_ATTR;
                break;
                
            default:
                return XML_ERROR;
        }
    }
    return XML_OK;
}

/**
 @brief End current attribute
 
 @param[in,out] writer TextWriter
 @return XML_OK (0) on success, XML_ERROR (-1) on failure
 */
int xmlTextWriterEndAttribute(xmlTextWriterPtr writer) {
    if (writer == NULL) {
        debug_print("%s\n", "XML writer init failed");
        return XML_ERROR;
    }
    MOBI_RET ret = MOBI_SUCCESS;
    MOBIXmlState *state = mobi_xml_state_current(writer);
    if (state) {
        switch (state->mode) {
            case MOBI_XMLMODE_ATTR:
                state->mode = MOBI_XMLMODE_NAME;
                ret = mobi_xml_buffer_addstring(writer, "\"");
                if (ret != MOBI_SUCCESS) { return XML_ERROR; }
                break;
                
            default:
                return XML_ERROR;
        }
    }
    return XML_OK;
}

/**
 @brief Write attribute with given name and content
 
 @param[in,out] writer TextWriter
 @param[in] name Attribute name
 @param[in] content Attribute content
 @return XML_OK (0) on success, XML_ERROR (-1) on failure
 */
int xmlTextWriterWriteAttribute(xmlTextWriterPtr writer, const xmlChar *name,
                            const xmlChar * content) {
    if (xmlTextWriterStartAttribute(writer, name) == XML_ERROR) { return XML_ERROR; }
    if (xmlTextWriterWriteString(writer, content) == XML_ERROR) { return XML_ERROR; }
    if (xmlTextWriterEndAttribute(writer) == XML_ERROR) { return XML_ERROR; }
    return XML_OK;
}

/**
 @brief Start attribute with namespace support for current xml element
 
 @param[in,out] writer TextWriter
 @param[in] prefix Namespace prefix or NULL
 @param[in] name Attribute name
 @param[in] namespaceURI Namespace uri or NULL
 @return XML_OK (0) on success, XML_ERROR (-1) on failure
 */
int xmlTextWriterStartAttributeNS(xmlTextWriterPtr writer,
                                  const xmlChar *prefix, const xmlChar *name,
                                  const xmlChar *namespaceURI) {
    if (writer == NULL || name == NULL || *name == '\0') {
        debug_print("%s\n", "XML writer init failed");
        return XML_ERROR;
    }
    if (namespaceURI != NULL) {
        char namespace[] =  "xmlns";
        if (prefix != NULL) {
            size_t length = sizeof(namespace) - 1 + strlen((char *) prefix) + 1; /* add one for ":" */
            char *prefixed = malloc(length + 1);
            if (prefixed == NULL) {
                debug_print("%s\n", "Memory allocation failed");
                return XML_ERROR;
            }
            sprintf(prefixed, "%s:%s", namespace, prefix);
            MOBI_RET ret = mobi_xml_save_ns(writer, prefixed, (char *) namespaceURI);
            free(prefixed);
            if (ret != MOBI_SUCCESS) {
                return XML_ERROR;
            }
        } else {
            MOBI_RET ret = mobi_xml_save_ns(writer, namespace, (char *) namespaceURI);
            if (ret != MOBI_SUCCESS) {
                return XML_ERROR;
            }
        }
        
    }
    if (prefix != NULL) {
        size_t length = strlen((char *) prefix) + strlen((char *) name) + 1; /* add one for ":" */
        char *prefixed = malloc(length + 1);
        if (prefixed == NULL) {
            debug_print("%s\n", "Memory allocation failed");
            return XML_ERROR;
        }
        sprintf(prefixed, "%s:%s", prefix, name);
        int ret = xmlTextWriterStartAttribute(writer, (xmlChar *)prefixed);
        free(prefixed);
        if (ret == XML_ERROR) {
            return XML_ERROR;
        }
    } else {
        int ret = xmlTextWriterStartAttribute(writer, name);
        if (ret == XML_ERROR) {
            return XML_ERROR;
        }
    }
    return XML_OK;
}

/**
 @brief Write attribute with namespace support
 
 @param[in,out] writer TextWriter
 @param[in] prefix Namespace prefix or NULL
 @param[in] name Attribute name
 @param[in] namespaceURI Namespace uri or NULL
 @param[in] content Attribute content
 @return XML_OK (0) on success, XML_ERROR (-1) on failure
 */
int xmlTextWriterWriteAttributeNS(xmlTextWriterPtr writer,
                                  const xmlChar *prefix, const xmlChar *name,
                                  const xmlChar *namespaceURI,
                                  const xmlChar *content) {

    if (xmlTextWriterStartAttributeNS(writer, prefix, name, namespaceURI) == XML_ERROR) { return XML_ERROR; }
    if (xmlTextWriterWriteString(writer, content) == XML_ERROR) { return XML_ERROR; }
    if (xmlTextWriterEndAttribute(writer) == XML_ERROR) { return XML_ERROR; }
    return XML_OK;
}

/**
 @brief Start element with namespace support
 
 @param[in,out] writer TextWriter
 @param[in] prefix Namespace prefix or NULL
 @param[in] name Element name
 @param[in] namespaceURI Namespace uri or NULL
 @return XML_OK (0) on success, XML_ERROR (-1) on failure
 */
int xmlTextWriterStartElementNS(xmlTextWriterPtr writer,
                                const xmlChar *prefix, const xmlChar *name,
                                const xmlChar *namespaceURI) {
    if (writer == NULL || name == NULL || *name == '\0') {
        debug_print("%s\n", "XML writer init failed");
        return XML_ERROR;
    }
    if (prefix != NULL) {
        size_t length = strlen((char *) prefix) + strlen((char *) name) + 1;
        char *prefixed = malloc(length + 1);
        if (prefixed == NULL) {
            debug_print("%s\n", "Memory allocation failed");
            return XML_ERROR;
        }
        sprintf(prefixed, "%s:%s", prefix, name);
        int ret = xmlTextWriterStartElement(writer, (xmlChar *)prefixed);
        free(prefixed);
        if (ret == XML_ERROR) { return XML_ERROR; }
    } else {
        if (xmlTextWriterStartElement(writer, name) == XML_ERROR) { return XML_ERROR; }
    }
    if (namespaceURI != NULL) {
        char namespace[] =  "xmlns";
        if (prefix != NULL) {
            size_t length = sizeof(namespace) - 1 + strlen((char *) prefix) + 1;
            char *prefixed = malloc(length + 1);
            if (prefixed == NULL) {
                debug_print("%s\n", "Memory allocation failed");
                return XML_ERROR;
            }
            sprintf(prefixed, "%s:%s", namespace, prefix);
            MOBI_RET ret = mobi_xml_save_ns(writer, prefixed, (char *) namespaceURI);
            free(prefixed);
            if (ret != MOBI_SUCCESS) {
                return XML_ERROR;
            }
        } else {
            MOBI_RET ret = mobi_xml_save_ns(writer, namespace, (char *) namespaceURI);
            if (ret != MOBI_SUCCESS) {
                return XML_ERROR;
            }
        }
    }
    return XML_OK;
}

/**
 @brief Write element with namespace support
 
 @param[in,out] writer TextWriter
 @param[in] prefix Namespace prefix or NULL
 @param[in] name Element name
 @param[in] namespaceURI Namespace uri or NULL
 @param[in] content Element content
 @return XML_OK (0) on success, XML_ERROR (-1) on failure
 */
int xmlTextWriterWriteElementNS(xmlTextWriterPtr writer, const xmlChar *prefix,
                                const xmlChar *name, const xmlChar *namespaceURI,
                                const xmlChar *content) {
    if (xmlTextWriterStartElementNS(writer, prefix, name, namespaceURI) == XML_ERROR) { return XML_ERROR; }
    if (xmlTextWriterWriteString(writer, content) == XML_ERROR) { return XML_ERROR; }
    if (xmlTextWriterEndElement(writer) == XML_ERROR) { return XML_ERROR; }
    return XML_OK;
}

/**
 @brief Write xml string
 
 @param[in,out] writer TextWriter
 @param[in] content Attribute content
 @return XML_OK (0) on success, XML_ERROR (-1) on failure
 */
int xmlTextWriterWriteString(xmlTextWriterPtr writer, const xmlChar *content) {
    if (writer == NULL || content == NULL) {
        debug_print("%s\n", "XML writer init failed");
        return XML_ERROR;
    }
    MOBI_RET ret = MOBI_SUCCESS;
    MOBI_XML_MODE mode = MOBI_XMLMODE_NONE;
    MOBIXmlState *state = mobi_xml_state_current(writer);
    if (state != NULL) {
        mode = state->mode;
    }
    switch (mode) {
        case MOBI_XMLMODE_NAME:
            // output namespace decl
            mobi_xml_write_ns(writer);
            ret = mobi_xml_buffer_addstring(writer, ">");
            if (ret != MOBI_SUCCESS) { return XML_ERROR; }
            state->mode = MOBI_XMLMODE_TEXT;
            /* falltrough */
        case MOBI_XMLMODE_TEXT:
            ret = mobi_xml_buffer_addencoded(writer, (const char *) content);
            if (writer->indent_enable) {
                writer->indent_next = false;
            }
            break;
        case MOBI_XMLMODE_ATTR:
            ret = mobi_xml_buffer_addencoded_attr(writer, (const char *) content);
            break;
            
        default:
            ret = mobi_xml_buffer_addstring(writer, (const char *) content);
            if (writer->indent_enable) {
                writer->indent_next = false;
            }
            break;
    }
    if (ret != MOBI_SUCCESS) { return XML_ERROR; }
    return XML_OK;
}

/**
 @brief Set indentation option
 
 @param[in,out] writer TextWriter
 @param[in] indent Indent output if value greater than zero
 @return XML_OK (0) on success, XML_ERROR (-1) on failure
 */
int xmlTextWriterSetIndent(xmlTextWriterPtr writer, int indent) {
    if (writer == NULL) {
        debug_print("%s\n", "XML writer init failed");
        return XML_ERROR;
    }
    writer->indent_enable = (indent != 0);
    writer->indent_next = true;
    return XML_OK;
}
