import io
from queue import Queue
from select import select
from threading import Thread

import evdev

key_mapping = {
    evdev.ecodes.KEY_A: ord('a'),
    evdev.ecodes.KEY_B: ord('b'),
    evdev.ecodes.KEY_C: ord('c'),
    evdev.ecodes.KEY_D: ord('d'),
    evdev.ecodes.KEY_E: ord('e'),
    evdev.ecodes.KEY_F: ord('f'),
    evdev.ecodes.KEY_G: ord('g'),
    evdev.ecodes.KEY_H: ord('h'),
    evdev.ecodes.KEY_I: ord('i'),
    evdev.ecodes.KEY_J: ord('j'),
    evdev.ecodes.KEY_K: ord('k'),
    evdev.ecodes.KEY_L: ord('l'),
    evdev.ecodes.KEY_M: ord('m'),
    evdev.ecodes.KEY_N: ord('n'),
    evdev.ecodes.KEY_O: ord('o'),
    evdev.ecodes.KEY_P: ord('p'),
    evdev.ecodes.KEY_Q: ord('q'),
    evdev.ecodes.KEY_R: ord('r'),
    evdev.ecodes.KEY_S: ord('s'),
    evdev.ecodes.KEY_T: ord('t'),
    evdev.ecodes.KEY_U: ord('u'),
    evdev.ecodes.KEY_V: ord('v'),
    evdev.ecodes.KEY_W: ord('w'),
    evdev.ecodes.KEY_X: ord('x'),
    evdev.ecodes.KEY_Y: ord('y'),
    evdev.ecodes.KEY_Z: ord('z'),
    evdev.ecodes.KEY_0: ord('0'),
    evdev.ecodes.KEY_1: ord('1'),
    evdev.ecodes.KEY_2: ord('2'),
    evdev.ecodes.KEY_3: ord('3'),
    evdev.ecodes.KEY_4: ord('4'),
    evdev.ecodes.KEY_5: ord('5'),
    evdev.ecodes.KEY_6: ord('6'),
    evdev.ecodes.KEY_7: ord('7'),
    evdev.ecodes.KEY_8: ord('8'),
    evdev.ecodes.KEY_9: ord('9'),
    evdev.ecodes.KEY_MINUS: ord('-'),
    evdev.ecodes.KEY_EQUAL: ord('='),
    evdev.ecodes.KEY_LEFTBRACE: ord('['),
    evdev.ecodes.KEY_RIGHTBRACE: ord('{'),
    evdev.ecodes.KEY_SEMICOLON: ord(';'),
    evdev.ecodes.KEY_APOSTROPHE: ord("'"),
    evdev.ecodes.KEY_COMMA: ord(','),
    evdev.ecodes.KEY_DOT: ord('.'),
    evdev.ecodes.KEY_SLASH: ord('/'),
    evdev.ecodes.KEY_SPACE: ord(' '),
    evdev.ecodes.KEY_ENTER: ord('\n')
}

key_mapping_shifted = {
    evdev.ecodes.KEY_A: ord('A'),
    evdev.ecodes.KEY_B: ord('B'),
    evdev.ecodes.KEY_C: ord('C'),
    evdev.ecodes.KEY_D: ord('D'),
    evdev.ecodes.KEY_E: ord('E'),
    evdev.ecodes.KEY_F: ord('F'),
    evdev.ecodes.KEY_G: ord('G'),
    evdev.ecodes.KEY_H: ord('H'),
    evdev.ecodes.KEY_I: ord('I'),
    evdev.ecodes.KEY_J: ord('J'),
    evdev.ecodes.KEY_K: ord('K'),
    evdev.ecodes.KEY_L: ord('L'),
    evdev.ecodes.KEY_M: ord('M'),
    evdev.ecodes.KEY_N: ord('N'),
    evdev.ecodes.KEY_O: ord('O'),
    evdev.ecodes.KEY_P: ord('P'),
    evdev.ecodes.KEY_Q: ord('Q'),
    evdev.ecodes.KEY_R: ord('R'),
    evdev.ecodes.KEY_S: ord('S'),
    evdev.ecodes.KEY_T: ord('T'),
    evdev.ecodes.KEY_U: ord('U'),
    evdev.ecodes.KEY_V: ord('V'),
    evdev.ecodes.KEY_W: ord('W'),
    evdev.ecodes.KEY_X: ord('X'),
    evdev.ecodes.KEY_Y: ord('Y'),
    evdev.ecodes.KEY_Z: ord('Z'),
    evdev.ecodes.KEY_0: ord(')'),
    evdev.ecodes.KEY_1: ord('!'),
    evdev.ecodes.KEY_2: ord('@'),
    evdev.ecodes.KEY_3: ord('#'),
    evdev.ecodes.KEY_4: ord('$'),
    evdev.ecodes.KEY_5: ord('%'),
    evdev.ecodes.KEY_6: ord('^'),
    evdev.ecodes.KEY_7: ord('&'),
    evdev.ecodes.KEY_8: ord('*'),
    evdev.ecodes.KEY_9: ord('('),
    evdev.ecodes.KEY_MINUS: ord('_'),
    evdev.ecodes.KEY_EQUAL: ord('+'),
    evdev.ecodes.KEY_LEFTBRACE: ord('{'),
    evdev.ecodes.KEY_RIGHTBRACE: ord('}'),
    evdev.ecodes.KEY_SEMICOLON: ord(':'),
    evdev.ecodes.KEY_APOSTROPHE: ord('"'),
    evdev.ecodes.KEY_COMMA: ord('<'),
    evdev.ecodes.KEY_DOT: ord('>'),
    evdev.ecodes.KEY_SLASH: ord('?')
}


class KeyboardIO(io.TextIOBase):
    def __init__(self, device_name):
        self._dev = evdev.InputDevice(device_name)
        self._queue = Queue(maxsize=100)
        self._thread = Thread(target=self._read_keyboard, daemon=True)
        self._thread.start()
        self._shift_pressed = False

    def _read_keyboard(self):
        print("Starting read thread")
        while True:
            r, w, x = select([self._dev], [], [])
            for event in self._dev.read():
                if event.type == evdev.ecodes.EV_KEY:
                    # print(evdev.categorize(event))

                    if event.code == evdev.ecodes.KEY_LEFTSHIFT:
                        self._shift_pressed = event.value == 1  # key down

                    if event.value == 1:  # key down
                        key = key_mapping.get(event.code)
                        if key is None:
                            continue

                        if self._shift_pressed:
                            shifted = key_mapping_shifted.get(event.code)
                            if shifted is not None:
                                key = shifted

                        self._queue.put(key)

    def readable(self):
        return True

    def read1(self, n=None):
        ch = self._queue.get()
        # print("Read %d" % ch)
        return bytes([ch])
