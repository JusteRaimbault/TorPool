#ps -ef | grep /opt/local/bin/tor | grep -v grep | awk -F" " '{print "kill -9 "$2}' | sh
ps -ef | grep SOCKSPort | grep -v grep | awk -F" " '{print "kill -9 "$2}' | sh
ps -ef | grep torpool | grep -v grep | awk -F" " '{print "kill -9 "$2}' | sh

