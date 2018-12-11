
from manager import TorPoolManager
from pathlib import Path
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
    except Exception as e:
        print(e)
    pool.switchPort(True)
