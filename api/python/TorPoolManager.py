
Class TorPoolManager():

    port = 0

    def setupTorPoolConnexion():
        checkRunningPool()

    def checkRunningPool():
        open('.tor_tmp/ports')

    def proxies:
         return({'http':'socks5://localhost:'+str(port)})

    def switchPort(portExclusivity):
