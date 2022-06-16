
MINIEXPDIR=../../libdjvu
MINIEXPC=${MINIEXPDIR}/miniexp.cpp
MINIEXPH=${MINIEXPDIR}/miniexp.h


minilisp: minilisp.o miniexp.o
	${CXX} ${CXXFLAGS} -o minilisp  minilisp.o miniexp.o -lm

minilisp.o: minilisp.cpp ${MINIEXPH}
	${CXX} ${CXXFLAGS} -I${MINIEXPDIR} -o minilisp.o -c minilisp.cpp

miniexp.o: ${MINIEXPC} ${MINIEXPH}
	${CXX} ${CXXFLAGS} -I${MINIEXPDIR} -o miniexp.o -c ${MINIEXPC}

distclean clean: 
	-rm minilisp minilisp.o miniexp.o 2>/dev/null

