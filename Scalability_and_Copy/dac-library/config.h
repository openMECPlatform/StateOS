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
#define CONF_CTRL_PORT_ACTION_DEFAULT    18081
//1 is share mode
#define CONF_CTRL_SHARE_DEFAULT    0
//1 is sfc mode
#define CONF_CTRL_SFC_DEFAULT    0
//for all the NFs
#define CONF_CTRL_ADNATFIRE_DEFAULT	0
#define CONF_CTRL_COPY_DEFAULT	0


#define CONF_CTRL_IP_LEN  16

#define CONF_CTRL_IP            "ctrl_ip"
#define CONF_CTRL_PORT_CONN    "ctrl_port_conn"
#define CONF_CTRL_PORT_ACTION    "ctrl_port_action"
#define CONF_CTRL_SHARE    "ctrl_share"
#define CONF_CTRL_SFC    "ctrl_sfc"
#define CONF_CTRL_ADNATFIRE "ctrl_adnatfire"
#define CONF_CTRL_COPY	"ctrl_copy"	
///// STRUCTURES /////////////////////////////////////////////////////////////
typedef struct {
    char ctrl_ip[CONF_CTRL_IP_LEN];
    uint16_t ctrl_port_conn; 
    uint16_t ctrl_port_action;
    uint16_t ctrl_share;
    uint16_t ctrl_sfc;
    uint16_t ctrl_adnatfire;
    uint16_t ctrl_copy;
} ConnacConfig;

///// FUNCTION PROTOTYPES ////////////////////////////////////////////////////
int connac_parse_config();

#endif

#ifdef __cplusplus
}
#endif
