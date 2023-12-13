#ifdef __cplusplus
extern "C" {
#endif

#ifndef _ConnacConfig_H_
#define _ConnacConfig_H_

#include <stdint.h>
#include <sys/time.h>
#include <pcap.h>

///// DEFINES ////////////////////////////////////////////////////////////////
#define CONFIG_FILE "config.conf"
#define CONF_KEY_LEN    32
#define CONF_VALUE_LEN  32

#define CONF_CTRL_IP_DEFAULT            "127.0.0.1"
#define CONF_CTRL_PORT_CONN_DEFAULT    18080


#define CONF_CTRL_IP_LEN  16

#define CONF_CTRL_IP            "ctrl_ip"
#define CONF_CTRL_PORT_CONN    "ctrl_port_conn"
///// STRUCTURES /////////////////////////////////////////////////////////////
typedef struct {
    char ctrl_ip[CONF_CTRL_IP_LEN];
    uint16_t ctrl_port_conn; 
} ConnacConfig;

///// FUNCTION PROTOTYPES ////////////////////////////////////////////////////
int connac_parse_config();

#endif

#ifdef __cplusplus
}
#endif
