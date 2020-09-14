import uuid

QR_ALNUM = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:'
QR_ALNUM_INDEX = {c: i for i, c in enumerate(QR_ALNUM)}


def uuid_to_qr_alphanum(u):
    x = u.int
    s = ''
    while x:
        i = x % 45
        x //= 45
        s += QR_ALNUM[i]
    return s


def qr_alphanum_to_uuid(s):
    x = 0
    for c in s[::-1]:
        i = QR_ALNUM_INDEX[c]
        x = x * 45 + i
    return uuid.UUID(int=x)
