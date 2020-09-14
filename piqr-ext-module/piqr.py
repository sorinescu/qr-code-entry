#!/usr/bin/env python3
import argparse
import io
import os
import re
from time import sleep

import requests
from gpiozero import LED
from kbscanner import KeyboardIO
from uuid_qr import qr_alphanum_to_uuid

_KEY_RE = re.compile('^[0-9a-f]{32}$')


def _verify_key(key_api_url, key):
    if not _KEY_RE.match(key):
        print('Invalid key format')
        return False

    api_token = os.getenv('API_TOKEN', 'foo')
    res = requests.get("%s/keys/%s" % (key_api_url, key),
                       headers={'Authorization': "Bearer %s" % api_token})
    print("Got response: %s %s" % (res.status_code, res.text))

    resp = res.json()
    return resp.get('valid', False)


def _open_door(pin):
    print("OPENING DOOR")
    pin.on()
    sleep(0.5)
    pin.off()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--key-api-url", default="http://localhost:5000",
                        help="URL of key verification service")
    parser.add_argument("--door-pin", default="GPIO21",
                        help="Door relay GPIO pin (run `pinout` to see the board pin layout)")
    args = parser.parse_args()

    fp = io.TextIOWrapper(KeyboardIO('/dev/input/by-id/usb-Manufacturer_Barcode_Reader-event-kbd'))
    door_pin = LED(args.door_pin)

    while True:
        line = fp.readline().strip('\r\n')
        print("LINE: '%s'" % line)
        try:
            u = qr_alphanum_to_uuid(line)
            print("Got UUID: '%s'" % u)
            key = qr_alphanum_to_uuid(line).hex
            print("Got key: '%s'" % key)
            if _verify_key(args.key_api_url, key):
                _open_door(door_pin)
        except Exception as e:
            print("Got exception: %s" % e)


if __name__ == '__main__':
    main()
