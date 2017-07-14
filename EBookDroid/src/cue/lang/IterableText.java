package cue.lang;

import java.util.Iterator;

abstract class IterableText implements Iterator<String>, Iterable<String> {
	public Iterator<String> iterator() {
		return this;
	}
}
