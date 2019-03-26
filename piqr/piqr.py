from imutils.video import VideoStream
from pyzbar import pyzbar
import argparse
import datetime
import imutils
import time
import requests
import os
import re
import cv2

_KEY_RE = re.compile('^[0-9a-z]{32}$')


def _verify_key(key_api_url, key):
    if not _KEY_RE.match(key):
        print('Invalid key format')
        return False

    api_token = os.getenv('API_TOKEN', 'foo')
    res = requests.get("%s/keys/%s" % (key_api_url, key),
        headers={'Authorization': "Token %s" % api_token})
    print("Got response: %s %s" % (res.status_code, res.text))

    resp = res.json()
    return resp['valid']


def _open_door():
    print("OPENING DOOR")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--key-api-url", default="http://localhost:5000",
        help="URL of key verification service")
    parser.add_argument("-v", "--video-src", type=int, default=0,
        help="Video source index")
    args = parser.parse_args()

    print(cv2.getBuildInformation())

    print("Starting video stream...")
    vs = VideoStream(src=args.video_src, resolution=(320, 240), framerate=10).start()

    # Warm up camera sensor
    time.sleep(2.0)

    t0 = time.time()
    frame_count = 0

    while True:
        frame = vs.read()
        # cv2.imwrite("frame-%05d.jpg" % frame_count, frame)

        barcodes = pyzbar.decode(frame)

        if True:
            for barcode in barcodes:
                # The barcode data is a bytes object
                barcode_data = barcode.data.decode("utf-8")
                barcode_type = barcode.type
                print("Got barcode '%s' of type '%s'" % (barcode_data, barcode_type))

                if _verify_key(args.key_api_url, barcode_data):
                    _open_door()

        frame_count += 1
        print("\rFPS: %.02f" % (frame_count / (time.time() - t0)))



if __name__ == '__main__':
    main()
