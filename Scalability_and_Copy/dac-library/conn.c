#include "conn.h"
#include "debug.h"
#include <assert.h>
#include <errno.h>
#include <netdb.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <string.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <unistd.h>
#include <pthread.h>
#include <stdint.h>

#define CONNAC_CONN_READ_MAX (1024 * 1024 * 1024)
typedef signed char byte;

static int conn_readRawVarint32(int conn);
static int action_readRawVarint32(int conn);


/**
  * Get a socket address for a connection.
  * @param hostname Hostname or IP address for the connection
  * @param port Port for the connection
  * @param addr Location to store the address
  * @return The socket address structure supplied as argument; NULL on error
  */ 
static struct sockaddr *conn_get_sockaddr(const char* hostname, 
        unsigned short port, struct sockaddr* addr)
{
    struct addrinfo hints;
    struct addrinfo* results;

    /* Setup hints structure for getaddrinfo */
    bzero(&hints,sizeof(struct addrinfo));
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_DGRAM;
    hints.ai_protocol = 0;

    /* Attempt to get address info */
    getaddrinfo(hostname, NULL, &hints, &results);
    if (results == NULL) 
    {
        return NULL;
    }
    
    /* Copy first matching address into addr structure */
    memcpy(addr, results->ai_addr, sizeof(struct sockaddr));
    ((struct sockaddr_in *)addr)->sin_port = htons(port);

    /* Free address info */
    freeaddrinfo(results);
    return addr;
}

/**
  * Open a socket.
  * @return Connected socket file descriptor; -1 on error
  */
int conn_active_open(const char *host, unsigned short port)
{
    int sock = -1;
    struct sockaddr dst;

	DEBUG_PRINT("Opening socket");
    sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0)
    {
        ERROR_PRINT("Could not get socket: %s", strerror(errno));
        return -1;
    }

    /* added so that state channel does not buffer to be full; disable
     * Nagle algorithm
     */
    int i = 1;
    setsockopt(sock, IPPROTO_TCP, TCP_NODELAY, (void*)&i, sizeof(i));

    if (NULL == conn_get_sockaddr(host, port, &dst))
    { 
        ERROR_PRINT("Could not get sockaddr"); 
        return -1; 
    }
    if (connect(sock, &dst, sizeof(struct sockaddr)) < 0)
    {
        ERROR_PRINT("Could not connect to %s:%d:  %s", host, port,
			strerror(errno)); 
        return -1; 
    }

    /* added so that state channel does not buffer to be full; disable
     * Nagle algorithm
     */
    /*int i = 1;
    setsockopt(sock, IPPROTO_TCP, TCP_NODELAY, (void*)&i, sizeof(i));
	*/
	DEBUG_PRINT("Opened socket");
    return sock;
}

/**
  * Listen for a socket connection and accept it. 
  * @return Connected socket file descriptor; -1 on error
  */
int conn_passive_open(unsigned short port)
{
    int server = -1, client = -1;
    struct sockaddr_in addr;
    socklen_t addrlen = sizeof(struct sockaddr);
    server = socket(AF_INET, SOCK_STREAM, 0);
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    addr.sin_addr.s_addr = INADDR_ANY;
    if (bind(server, (struct sockaddr *)&addr, addrlen) < 0)
    {
        ERROR_PRINT("Could not bind: %s", strerror(errno)); 
        return -1; 
    }
    DEBUG_PRINT("Listening for connection");
    listen(server, 0);
    client = accept(server, (struct sockaddr *)&addr, &addrlen);
    close(server);
    return client;
}

int conn_close(int conn)
{
    if (close(conn) < 0)
    { return -1; }
    return 0;
}

ProtoObject conn_read(int conn)
{
    int readlen = 0, result = 0;

    
    //INFO_PRINT("conn_read.........\n");

    int length;
    length = conn_readRawVarint32(conn);

    //printf("length : %d", length);
   
    // Limit read size 
    
    assert(length < CONNAC_CONN_READ_MAX);

    // Allocate buffer for string
    char *buf = NULL;
    //first buf to store the length
    buf = (char *)malloc(length);
  
    assert(buf != NULL);

    //Read string
    while (readlen < length)
    {
	//INFO_PRINT("conn_read_in.....string.........\n");        
	// Read from connection
        result = read(conn, buf, length - readlen);
	//printf("result %d\n",result);
        // Encountered error
        if (result < 0)
        { 
            ERROR_PRINT("Error reading from socket: %d", result);
            free(buf);
        }
	
        // Data was read
        buf += result;
        readlen += result;
    }
    buf = buf -length;
    uint8_t * intbuf = (uint8_t*)(buf);

    ProtoObject protoObject;
    protoObject.object = intbuf;
    protoObject.length = length;
    
    return protoObject;

}

ProtoObject action_read(int conn)
{
    int readlen = 0, result = 0;

    
    //INFO_PRINT("action_read.........\n");

    int length;
    length = action_readRawVarint32(conn);

    //printf("length : %d", length);
   
    // Limit read size 
    
    assert(length < CONNAC_CONN_READ_MAX);

    // Allocate buffer for string
    char *buf = NULL;
    //first buf to store the length
    buf = (char *)malloc(length);
  
    assert(buf != NULL);

    //Read string
    while (readlen < length)
    {
	//INFO_PRINT("conn_read_in.....string.........\n");        
	// Read from connection
        result = read(conn, buf, length - readlen);
	//printf("result %d\n",result);
        // Encountered error
        if (result < 0)
        { 
            ERROR_PRINT("Error reading from socket: %d", result);
            free(buf);
        }
	
        // Data was read
        buf += result;
        readlen += result;
    }
    buf = buf -length;
    uint8_t * intbuf = (uint8_t*)(buf);

    ProtoObject protoObject;
    protoObject.object = intbuf;
    protoObject.length = length;
    
    return protoObject;

}


int conn_write(int conn, uint8_t *buf, int len)
{
    //pthread_mutex_lock(&connac_lock_conn);
    int result = write(conn, buf, len);
    //pthread_mutex_unlock(&connac_lock_conn);
    
    return result;
}

int action_write(int conn, uint8_t *buf, int len)
{
    //pthread_mutex_lock(&connac_lock_action);
    int result = write(conn, buf, len);
    //pthread_mutex_unlock(&connac_lock_action);
    
    return result;
}

/*int conn_write_append_newline(int conn, char *buf, int len)
{
    int tmplen = htonl(len);
    pthread_mutex_lock(&sdmbn_lock_conn);
    write(conn, &tmplen, sizeof(tmplen));
    int result = write(conn, (void *)buf, len);
    //write(conn, "\n", 1);
    pthread_mutex_unlock(&sdmbn_lock_conn);
    return result;
}*/



static int conn_readRawVarint32(int conn) {
        byte tmp;
        if (read(conn, &tmp, 1) < 0) {
            return 0;
        } else {
            if (tmp >= 0) {
		//printf("1:tmp>=0 tmp:%x \n",tmp);
                return tmp;
            } else { 
                int result = tmp & 127;
		//printf("2:tmp&127 tmp:%x \n",result);
                if (read(conn, &tmp, 1) < 0) {	   
                    return 0;
                } else {
                    if (tmp >= 0) {
                        result |= tmp << 7;
			//printf("3:tmp<<7 tmp:%x \n",result);
                    } else {
                        result |= (tmp & 127) << 7;
			//printf("4:(tmp & 127) << 7 tmp:%x \n",result);
                        if (read(conn, &tmp, 1) < 0) { 
                            return 0;
                        }

                        if (tmp >= 0) {
	                    
                            result |= tmp << 14;
                        } else {
	                    
                            result |= (tmp & 127) << 14;
                            if (read(conn, &tmp, 1) < 0) {
	                        
                                return 0;
                            }

                            if (tmp >= 0) {                 
                                result |= tmp << 21;
                            } else {
	                        
                                result |= (tmp & 0) << 21;
                                if (read(conn, &tmp, 1) < 0) {
	                   
                                    return 0;
                                }

                                result |= tmp  << 28;
                                if (tmp < 0) {
				   return 0;
                                }
                            }
                        }
                    }

                    return result;
                }
            }
        }
    }
  
static int action_readRawVarint32(int conn) {
        byte tmp;
        if (read(conn, &tmp, 1) < 0) {
            return 0;
        } else {
            if (tmp >= 0) {
		//printf("1:tmp>=0 tmp:%x \n",tmp);
                return tmp;
            } else { 
                int result = tmp & 127;
		//printf("2:tmp&127 tmp:%x \n",result);
                if (read(conn, &tmp, 1) < 0) {	   
                    return 0;
                } else {
                    if (tmp >= 0) {
                        result |= tmp << 7;
			//printf("3:tmp<<7 tmp:%x \n",result);
                    } else {
                        result |= (tmp & 127) << 7;
			//printf("4:(tmp & 127) << 7 tmp:%x \n",result);
                        if (read(conn, &tmp, 1) < 0) { 
                            return 0;
                        }

                        if (tmp >= 0) {
	                    
                            result |= tmp << 14;
                        } else {
	                    
                            result |= (tmp & 127) << 14;
                            if (read(conn, &tmp, 1) < 0) {
	                        
                                return 0;
                            }

                            if (tmp >= 0) {                 
                                result |= tmp << 21;
                            } else {
	                        
                                result |= (tmp & 0) << 21;
                                if (read(conn, &tmp, 1) < 0) {
	                   
                                    return 0;
                                }

                                result |= tmp  << 28;
                                if (tmp < 0) {
				   return 0;
                                }
                            }
                        }
                    }

                    return result;
                }
            }
        }
    }
 


