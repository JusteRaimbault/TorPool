from pathlib import Path
import time


#'
#' Note : the application must be secure itself when using ports, the torpool can not manage external use and put the port back
#'  e.g. in case of a failure
#'

class TorPoolManager():

    port = 0
    hasConnexion=False

    #'
    #' Read the first line of locked file
    def readLineWithLock(portfile,lockfile):
        locked=True
        while locked:
            time.sleep(0.2)
            lock = Path(lockfile)
            locked = lock.is_file()

        # create the lock
        lock = Path(lockfile)
        lock.touch()
        ports = open(portfile,'r')
        res = ports.readline()
        lock.unlink()
        return(res)


    #'
    #' Remove a port in the locked file
    def removeInFileWithLock(s,portfile,lockfile):
        locked=True
        while locked:
            time.sleep(0.2)
            lock = Path(lockfile)
            locked = lock.is_file()

        # create the lock
        lock = Path(lockfile)
        lock.touch()
        ports = open(portfile,'r')
        newcontent = [l.replace('\n','') for l in ports.readlines() if not l.startswith(s)]
        ports = open(portfile,'w')
        for l in newcontent:
            ports.write(l+'\n')
        lock.unlink()

    def setupTorPoolConnexion(portExclusivity,self):
        self.hasConnexion = checkRunningPool()
        if self.hasConnexion:
            self.port = switchPort(portExclusivity)



    #'
    #' Check if there is a running pool locally
    def checkRunningPool():
        openports = Path('.tor_tmp/ports')
        return(openports.is_file())

    #'
    #' Primitive to get current socks proxy
    def proxies(self):
        print('Proxy address : socks5://127.0.0.1:'+str(self.port))
        return({'http':'socks5://127.0.0.1:'+str(self.port)})

    #'
    #' Switch the port
    #'
    def switchPort(self,portExclusivity):
        print('Switching port from port '+str(self.port))

        if self.port != 0:
            print('Killing task for port '+str(self.port))
            killsignal = Path('.tor_tmp/kill'+self.port)
            killsignal.touch()

        portfile = '.tor_tmp/ports'
        lockfile = '.tor_tmp/lock'
        newPort = ""
        while len(newPort)<4:
            newPort=TorPoolManager.readLineWithLock(portfile,lockfile)
            #print(newPort)
        print('Switching to new port: '+newPort)
        self.port = newPort.replace('\n','').replace('\r','')

        # FIXME : rq : if there is no port exclusivity, process can be killed by an other ?
        # should always use with exclusivity for now
        if portExclusivity:
            TorPoolManager.removeInFileWithLock(newPort,portfile,lockfile)

    #'
    #' Release the current port (in the c ase of an exclusive use), by switching without exclusivity
    def releasePort(self):
        print('Releasing port'+str(self.port))
        switchPort(self,False)
