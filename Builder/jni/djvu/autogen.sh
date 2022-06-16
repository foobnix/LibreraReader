#! /bin/sh

autoreconf -f -i -v --warnings=all || exit 1

if [ -z "$NOCONFIGURE" ]; then
	./configure "$@"
fi
