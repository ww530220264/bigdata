```bash
#max file descriptors [4096] for elasticsearch process is too low, increase to at least [65535]
vim /etc/security/limits.conf
#增加如下配置
*               soft    nofile          65536
*               hard    nofile          65536
```

```bash
#max virtual memory areas vm.max_map_count [65530] is too low, increase to at least [262144]
vim /etc/sysctl.conf
#增加配置
vm.max_map_count=262144
#生效命令
sysctl -p
```