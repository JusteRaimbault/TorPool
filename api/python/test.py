
from manager import TorPoolManager
import time
import requests


pool = TorPoolManager()
pool.switchPort(True)

while True:
    result = requests.get('http://ipecho.net/plain',proxies=pool.proxies())
    print(result)
    pool.switchPort(True)
    time.sleep(5)
