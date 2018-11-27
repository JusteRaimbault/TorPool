from pathlib import Path
import time

class TorPoolManager():

    port = 0
    hasConnexion=False

    def setupTorPoolConnexion(portExclusivity):
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
    def proxies():
         return({'http':'socks5://localhost:'+str(port)})

    #'
    #' Switch the port
    def switchPort(portExclusivity):
        if self.port != 0:
            killsignal = Path('.tor_tmp/kill'+self.port)
            killsignal.touch()

        portfile = '.tor_tmp/ports'
        lockfile = '.tor_tmp/lock'
        newPort = ''
        while len(newPort)<4:
            newPort=readLineWithLock(portfile,lockfile)
        print('Switching to new port: '+newPort)
        self.port = newPort

        # FIXME : rq : if there is no port exclusivity, process can be killed by an other ?
        # should always use with exclusivity for now
        if portExclusivity:
            removeInFileWithLock(newPort,portfile,lockfile)

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
        ports = open(portfile,'rw')
        newcontent = [l.replace('\n','') if not l.startsWith(s) for l in ports.readlines()]
        for l in newcontent:
            ports.write(l+'\n')
        lock.unlink()
