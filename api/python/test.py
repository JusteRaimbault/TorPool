
from manager import TorPoolManager
from pathlib import Path
from lxml import html,etree
import time
import requests

lock = Path('.tor_tmp/lock')
if lock.is_file(): lock.unlink()

pool = TorPoolManager()
pool.switchPort(True)

while True:
    time.sleep(10)
    try:
        result = requests.get('http://ipecho.net/plain',proxies=pool.proxies(), timeout=10)
        print(result.text)

        #pdata = requests.get('https://patents.google.com/patent/US9000616B2',proxies=pool.proxies(), timeout=10)
        #try :
        #    tree = html.fromstring(pdata.content)
        #    if len(tree.find_class("abstract")) > 0 :
        #        t = tree.find_class("abstract")[0]
        #        print(t.text)
        #except Exception :
        #    print('Error parsing html')
    except Exception as e:
        print(e)
        pool.switchPort(True)
    pool.switchPort(True)
