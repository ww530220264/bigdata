### 修改ip地址为静态ip，且使用于宿主机同一网络

![image-20200608145200951](.\image\image-20200608145200951.png)

![image-20200608145256465](.\image\image-20200608145256465.png)

![image-20200608145351564](.\image\image-20200608145351564.png)

```bash
vim /etc/sysconfig/network-scripts/ifcfg-ens32
```

```bash
TYPE="Ethernet"
PROXY_METHOD="none"
BROWSER_ONLY="no"
BOOTPROTO="static"
DEFROUTE="yes"
IPV4_FAILURE_FATAL="no"
IPV6INIT="yes"
IPV6_AUTOCONF="yes"
IPV6_DEFROUTE="yes"
IPV6_FAILURE_FATAL="no"
IPV6_ADDR_GEN_MODE="stable-privacy"
NAME="ens32"
UUID="379d88da-4878-49d6-ae26-f089ca3bac4b"
DEVICE="ens32"
ONBOOT="yes"
IPV6_PRIVACY="no"
IPADDR="192.168.10.151"
NETMASK="255.255.255.0"
GATEWAY="192.168.10.1"
DNS1="8.8.8.8"
```

```bash
service network restart
```

