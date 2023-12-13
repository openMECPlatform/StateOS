SOURCES= connac_core.c conn.c config.c connac_state.c myConnMessage.pb-c.c protoObject.c
CC = gcc

ifeq (${PREFIX},)
PREFIX=/usr/local/
endif
LIBDIR=${PREFIX}/lib
INCLUDEDIR=${PREFIX}/include
CONFDIR=${PREFIX}/etc

CFLAGS += -Wall -g -DCONFIG_DIR='"${CONFDIR}/"'
LDFLAGS += -lpthread -lpcap -lprotobuf-c -Wl,--no-undefined

build: libstless.so

libstless.so: ${SOURCES:.c=.o}
	$(CC) -o $@ -shared $^ $(LDFLAGS)

%.o: %.c
	$(CC) $(CFLAGS) -fpic -c $^

install: build 
	install -d ${LIBDIR}
	install libstless.so ${LIBDIR}/libstless.so
	install -d ${INCLUDEDIR}
	install STLESS.h ${INCLUDEDIR}/STLESS.h
	install -d ${CONFDIR}
	if [ ! -f ${CONFDIR}/config.conf ]; then install config.conf ${CONFDIR}/config.conf; fi

clean:
	rm *.o *.so *.gch

