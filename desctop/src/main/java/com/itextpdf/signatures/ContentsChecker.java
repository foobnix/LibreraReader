package com.itextpdf.signatures;

import java.io.IOException;

import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.io.source.IRandomAccessSource;
import com.itextpdf.io.source.PdfTokenizer;
import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfNull;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.signatures.exceptions.SignExceptionMessageConstant;

public class ContentsChecker extends PdfReader {

	private long contentsStart;
	private long contentsEnd;

	private int currentLevel = 0;
	private int contentsLevel = 1;
	private boolean searchInV = true;

	private boolean rangeIsCorrect = false;


	public ContentsChecker(IRandomAccessSource byteSource) throws IOException {
		super(byteSource, null);
	}

	public boolean checkWhetherSignatureCoversWholeDocument(PdfFormField signatureField) {
		rangeIsCorrect = false;
		PdfDictionary signature = (PdfDictionary) signatureField.getValue();
		int[] byteRange = ((PdfArray) signature.get(PdfName.ByteRange)).toIntArray();
		if (4 != byteRange.length || 0 != byteRange[0] || tokens.getSafeFile().length() != byteRange[2] + byteRange[3]) {
			return false;
		}

		contentsStart = byteRange[1];
		contentsEnd = byteRange[2];

		long signatureOffset;
		if (null != signature.getIndirectReference()) {
			signatureOffset = signature.getIndirectReference().getOffset();
			searchInV = true;
		} else {
			signatureOffset = signatureField.getPdfObject().getIndirectReference().getOffset();
			searchInV = false;
			contentsLevel++;
		}

		try {
			tokens.seek(signatureOffset);
			tokens.nextValidToken();
			readObject(false, false);
		} catch (IOException e) {
			// That's not expected because if the signature is invalid, it should have already failed
			return false;
		}

		return rangeIsCorrect;
	}

	@Override
	// The method copies the logic of PdfReader's method.
	// Only Contents related checks have been introduced.
	protected PdfDictionary readDictionary(boolean objStm) throws IOException {
		currentLevel++;
		PdfDictionary dic = new PdfDictionary();
		while (!rangeIsCorrect) {
			tokens.nextValidToken();
			if (tokens.getTokenType() == PdfTokenizer.TokenType.EndDic) {
				currentLevel--;
				break;
			}
			if (tokens.getTokenType() != PdfTokenizer.TokenType.Name) {
				tokens.throwError(
						SignExceptionMessageConstant.DICTIONARY_THIS_KEY_IS_NOT_A_NAME, tokens.getStringValue());
			}
			PdfName name = readPdfName(true);
			PdfObject obj;
			if (PdfName.Contents.equals(name) && searchInV && contentsLevel == currentLevel) {
				long startPosition = tokens.getPosition();
				int ch;
				int whiteSpacesCount = -1;
				do {
					ch = tokens.read();
					whiteSpacesCount++;
				} while (ch != -1 && PdfTokenizer.isWhitespace(ch));
				tokens.seek(startPosition);
				obj = readObject(true, objStm);
				long endPosition = tokens.getPosition();
				if (endPosition == contentsEnd && startPosition + whiteSpacesCount == contentsStart) {
					rangeIsCorrect = true;
				}
			} else if (PdfName.V.equals(name) && !searchInV && 1 == currentLevel) {
				searchInV = true;
				obj = readObject(true, objStm);
				searchInV = false;
			} else {
				obj = readObject(true, objStm);
			}
			if (obj == null) {
				if (tokens.getTokenType() == PdfTokenizer.TokenType.EndDic)
					tokens.throwError(SignExceptionMessageConstant.UNEXPECTED_GT_GT);
				if (tokens.getTokenType() == PdfTokenizer.TokenType.EndArray)
					tokens.throwError(SignExceptionMessageConstant.UNEXPECTED_CLOSE_BRACKET);
			}
			dic.put(name, obj);
		}
		return dic;
	}

	@Override
	protected PdfObject readReference(boolean readAsDirect) {
		return new PdfNull();
	}
}
