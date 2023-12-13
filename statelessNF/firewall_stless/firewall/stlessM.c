#include "stlessM.h"

redisContext *conn;

STLESSStatistics stless_stats;

char configPath[] = "/home/godbestway/stateless/prads_stless/src/config.txt";


void stless_notify_packet_received(char *type, struct timeval *recv_time)
{
    stless_stats.pkts_received++;
    
    // Output count statistics 
    if (0 == (stless_stats.pkts_received % 10))
    { printf("pkts_received=%d, flows_active=%d\n",
            stless_stats.pkts_received, stless_stats.flows_active); }
}



void stless_notify_flow_created()
{
    stless_stats.flows_active++;
}

void stless_notify_flow_destroyed()
{
    stless_stats.flows_active--;
}



struct configItem
{
    char key[20];
    char value[50];
};


struct configItem configList[] = {
	{"RedisServerIP", 0},
	{"RedisServerPort", 0},
	{"RedisServerPass", 0},
	{"IsRedisBackup", -1}
};


/*
* Read Key Value
* src -- srcChar
* key -- matchKey
* value -- matchValue
*/
int strkv(char *src, char *key, char *value)
{
    char *p, *q;
    int len;
    p = strchr(src, '=');
    q = strchr(src, '\n');
    
    if (p != NULL && q != NULL)
    {
        *q = '\0'; 
        strncpy(key, src, p-src); 
        strcpy(value, p+1);
        return 1;
    }
    return 0;
}

/*
*  Read Redis Config
*
*/
void Config(char * configFilePath, struct configItem* configVar, int configNum)
{
    int i;
    FILE * pfile;
    char buf[50] = ""; 
    char key[50] = "";  
    char value[50] = "";

    pfile = fopen(configFilePath, "r");

    if (pfile == NULL)
    {
        printf("open RedisConfigFile error!\n");
        exit(-1);
    }

    while (fgets(buf, 50, pfile))
    {
        if (strkv(buf, key, value))
        {
            for (i = 0; i < configNum; i++)
            {
                if (strcmp(key, configVar[i].key) == 0)
                {
					strcpy(configVar[i].value, value);
                }
            }
            memset(key, 0, strlen(key));
        }

    }
    fclose(pfile);
}

int statisticSet(){
    const char *v[3];
    size_t vlen[3];
    v[0] = "SET";
    vlen[0] = strlen("SET");
 
    v[1] = "statistic";
    vlen[1] = strlen("statistic");

    v[2] = (const char *)&stless_stats;
    vlen[2] = sizeof(struct _STLESSStatistics);
 
    redisReply *r = (redisReply *)redisCommandArgv(conn, sizeof(v) / sizeof(v[0]), v, vlen);
 
    if (!r) {
        printf(">>>>>>>>>>>>>>>>>>>>>>>>>>>empty reply\n");
        return -1;
    }
	
    
	if(strcmp(r->str, "OK") == 0){
		freeReplyObject(r);
		return 1;
	}else{
		printf(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>configSetError:%s!\n", r->str);
		freeReplyObject(r);
		return -1;
	}
}


int statisticGet()
{
    redisReply *reply;
    redisReply *r = (redisReply *)redisCommand(conn, "GET statistic");
	
    if (!r) {
        printf("empty reply\n");
        return -1;
    }
 	
    if (r->len != sizeof(struct _STLESSStatistics)) {
        printf("reply len error\n");
		freeReplyObject(r);
        return -1;
    }
	struct _STLESSStatistics *p = (struct _STLESSStatistics *)malloc(sizeof(struct _STLESSStatistics));
    if (!p) {
        printf("malloc fail\n");
		freeReplyObject(r);
        return -1;
    }
    memcpy(p, r->str, r->len);
	
	stless_stats.pkts_received = p->pkts_received;
	stless_stats.flows_active = p->flows_active;
	
	free(p);
    freeReplyObject(r);
	return 1;
}


int initRedisConfig(){
	char *hostname;
    int port = 0, i, isBackup = -1;
    char *password;
	char systemcmd[200] = {0};

	Config(configPath, configList, sizeof(configList)/sizeof(struct configItem));

	for (i = 0; i < sizeof(configList)/sizeof(struct configItem); i++)
    {

        if( strcmp(configList[i].key, "RedisServerIP" ) == 0){
        	hostname = configList[i].value;
			continue;
		}
		if( strcmp(configList[i].key, "RedisServerPort" ) == 0){
        	port = atoi(configList[i].value);
			continue;
		}
		if( strcmp(configList[i].key, "RedisServerPass" ) == 0){
        	password = configList[i].value;
			continue;
		}
		if( strcmp(configList[i].key, "IsRedisBackup" ) == 0){
        	isBackup = atoi(configList[i].value); 
			continue;
		}
    }
	


    redisReply *reply;
    struct timeval timeout = {1, 500000};
    conn = redisConnectWithTimeout(hostname, port, timeout);
    // conn erro
    if (conn == NULL || conn->err) {
        if (conn) {
            printf("connection error %s\n", conn->errstr);
            return -1;
        } else {
            printf("cannot alloc redis context\n");
            return -1;
        }
    }
	

    // auth
    reply = redisCommand(conn, "AUTH %s", password);
    printf("auth is %s\n", reply->str);
    freeReplyObject(reply);

	printf("isBackup is %d\n", isBackup);

	if(isBackup == 0){
		printf("Start init Redis...\n");

	    int systemRet;

		memset(systemcmd, 0, sizeof(systemcmd));

		sprintf(systemcmd, "redis-cli -h %s -p %d -a %s keys \"bucket_*\" | xargs redis-cli -h %s -p %d  -a %s del", hostname, port, password, hostname, port, password);	
		systemRet = system(systemcmd);
				
		if(systemRet == -1){
		  printf("system exec bucket failed !\n");
		}

		memset(systemcmd, 0, sizeof(systemcmd));
		sprintf(systemcmd, "redis-cli -h %s -p %d -a %s keys \"pradsBack\" | xargs redis-cli -h %s -p %d  -a %s del", hostname, port, password, hostname, port, password);
		systemRet = system(systemcmd);
				
		if(systemRet == -1){
		  printf("system exec pradsBack failed !\n");
		}
	}
	

    //redisFree(conn);
    return 1;
}

int isBackup(){
	
    int i, isBackup = -1;

	
	Config(configPath, configList, sizeof(configList)/sizeof(struct configItem));

	for (i = 0; i < sizeof(configList)/sizeof(struct configItem); i++)
    {

		if( strcmp(configList[i].key, "IsRedisBackup" ) == 0){
        	isBackup = atoi(configList[i].value); 
			break;
		}
    }
	
    return isBackup;
}


int bucketSet(uint32_t hash,struct _state_node *bucketValue){
	const char *v[3];
    size_t vlen[3];
	
    v[0] = "SET";
    vlen[0] = strlen("SET");
	
	char tmp[20]={0};
	sprintf(tmp, "bucket_%d", hash);

	v[1] = tmp;
    vlen[1] = strlen(tmp);

    v[2] = (const char *)bucketValue;
    vlen[2] = sizeof(struct _state_node);
 
    redisReply *r = (redisReply *)redisCommandArgv(conn, sizeof(v) / sizeof(v[0]), v, vlen);
 
    if (!r) {
        printf(">>>>>>>>>>>>>>>>>>>>>>>>>>>empty reply\n");
        return -1;
    }
	
    
	if(strcmp(r->str, "OK") == 0){
		freeReplyObject(r);
		return 1;
	}else{
		printf(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>configSetError:%s!\n", r->str);
		freeReplyObject(r);
		return -1;
	}
}


int bucketGet(uint32_t hash, struct _state_node *bucketValue)
{
    redisReply *reply;

	char tmp[20]={0};
	sprintf(tmp, "bucket_%d", hash);

	
    redisReply *r = (redisReply *)redisCommand(conn, "GET %s", tmp);
	
    if (!r) {
        printf("empty reply\n");
        return -1;
    }
 	
    if (r->len != sizeof(struct _state_node)) {
        printf("get %s\n", tmp);
		freeReplyObject(r);
        return -1;
    }
	
    memcpy(bucketValue, r->str, r->len);
	
    freeReplyObject(r);
	return 1;
}



int NoticeSet(const char *content){
	const char *v[3];
    size_t vlen[3];
    v[0] = "SET";
    vlen[0] = strlen("SET");
 
    v[1] = "pradsBack";
    vlen[1] = strlen("pradsBack");

	
    v[2] = content;
    vlen[2] = sizeof(strlen(v[2]));
 
    redisReply *r = (redisReply *)redisCommandArgv(conn, sizeof(v) / sizeof(v[0]), v, vlen);
 
    if (!r) {
        printf(">>>>>>>>>>>>>>>>>>>>>>>>>>>empty reply\n");
        return -1;
    }
	
    
	if(strcmp(r->str, "OK") == 0){
		freeReplyObject(r);
		return 1;
	}else{
		printf(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>configSetError:%s!\n", r->str);
		freeReplyObject(r);
		return -1;
	}
}


int NoticeGet()
{
    redisReply *reply;
    redisReply *r = (redisReply *)redisCommand(conn, "GET pradsBack");
	
    if (!r) {
        printf("empty reply\n");
        return -1;
    }
	//wait for the reply
	//printf("get is %s\n", r->str);

	if(r->str == NULL){
		freeReplyObject(r);
		return -1;
	}

	
 	
    /*
	struct _prads_stat *p = (struct _prads_stat *)malloc(sizeof(struct _prads_stat));
    if (!p) {
        printf("malloc fail\n");
		freeReplyObject(r);
        return -1;
    }
    memcpy(p, r->str, r->len);
	
	
	
	free(p);*/
    freeReplyObject(r);
	return 1;
}

