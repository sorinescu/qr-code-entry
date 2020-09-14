import os
import sys
import uuid
from datetime import datetime
from io import BytesIO

import dateutil.parser
import qrcode
from PIL import Image
from flask import request, jsonify, make_response

from .db import AccessKey, db
from .uuid_qr import uuid_to_qr_alphanum


# See https://stackoverflow.com/questions/16771894/python-nameerror-global-name-file-is-not-defined/41546830#41546830
def _rel_file_path(filename):
    if getattr(sys, 'frozen', False):
        # The application is frozen
        datadir = os.path.dirname(sys.executable)
    else:
        # The application is not frozen
        # Change this bit to match where you store your data files:
        datadir = os.path.dirname(__file__)

    return os.path.join(datadir, filename)


def get_all_access_keys():
    try:
        keys = [dict(key=ak.key, expires_at=ak.expires_at, metadata=ak.meta) for ak in AccessKey.query.all()]
        return jsonify(keys=keys)
    except Exception as e:
        return jsonify(msg="Error: %s" % e), 400


def get_all_access_keys():
    try:
        keys = [dict(key=ak.key, expires_at=ak.expires_at, metadata=ak.meta) for ak in AccessKey.query.all()]
        return jsonify(keys=keys)
    except Exception as e:
        return jsonify(msg="Error: %s" % e), 400


def _not_expired(expires_at):
    return expires_at is None or expires_at > datetime.utcnow()


def get_valid_access_keys():
    try:
        keys = [dict(key=ak.key, expires_at=ak.expires_at, metadata=ak.meta)
                for ak in AccessKey.query.all()
                if _not_expired(ak.expires_at)]
        return jsonify(keys=keys)
    except Exception as e:
        return jsonify(msg="Error: %s" % e), 400


def check_access_key(key):
    try:
        access_key = AccessKey.query.filter_by(key=key).first()
        if access_key is None:
            return jsonify(msg='Unknown access key'), 404

        return jsonify(key=key, metadata=access_key.meta,
                       expires_at=access_key.expires_at,
                       valid=_not_expired(access_key.expires_at))
    except Exception as e:
        return jsonify(msg="Error: %s" % e), 400


def new_access_key():
    params = request.get_json()

    try:
        expires_at = params.get('expires_at')
        if expires_at is not None:
            try:
                expires_at = dateutil.parser.parse(expires_at)
            except ValueError:
                raise ValueError("Invalid format for 'expires_at'")

        metadata = params.get('metadata')

        key = uuid.uuid4().hex

        access_key = AccessKey(key=key, expires_at=expires_at, meta=metadata)
        db.session.add(access_key)
        db.session.commit()
        return jsonify(key=key, metadata=metadata, expires_at=expires_at)
    except Exception as e:
        return jsonify(msg="Error: %s" % e), 400


def update_access_key(key):
    try:
        access_key = AccessKey.query.filter_by(key=key).first()
        params = request.get_json()

        if access_key is None:
            return jsonify(msg='Unknown access key'), 404

        if 'expires_at' in params:
            try:
                expires_at = params['expires_at']
                if expires_at is not None:
                    expires_at = dateutil.parser.parse(expires_at)

                access_key.expires_at = expires_at
            except ValueError:
                raise ValueError("Invalid format for 'expires_at'")

        if 'metadata' in params:
            access_key.meta = params['metadata']

        db.session.commit()

        return jsonify(key=key, metadata=access_key.meta, expires_at=access_key.expires_at)
    except Exception as e:
        return jsonify(msg="Error: %s" % e), 400


def revoke_access_key(key):
    try:
        access_key = AccessKey.query.filter_by(key=key).first()

        if access_key is None:
            return jsonify(msg='Unknown access key'), 404

        db.session.delete(access_key)
        db.session.commit()
        return jsonify(msg='Ok')
    except Exception as e:
        return jsonify(msg="Error: %s" % e), 400


def get_key_qr_code(key):
    qr = qrcode.QRCode(error_correction=qrcode.ERROR_CORRECT_H, box_size=10)

    # logo = Image.open('logo-transp.png')
    logo = Image.open(_rel_file_path('../static/img/logo.jpg'))

    u = uuid.UUID(key)
    enc_key = uuid_to_qr_alphanum(u)
    qr.add_data(enc_key)

    # qr_img = qr.make_image(back_color='transparent')
    qr_img = qr.make_image(fill_color='#010101')

    qr_w, qr_h = qr_img.size

    logo = logo.resize((qr_w // 4, qr_h // 4), resample=Image.LANCZOS)

    qr_img.paste(logo, box=(qr_w * 3 // 8, qr_h * 3 // 8))

    with BytesIO() as f:
        qr_img.save(f, format='png')
        resp = make_response(f.getvalue())

    resp.headers.set('Content-Type', 'image/png')
    resp.headers.set('Content-Disposition', 'attachment',
                     filename='hubbahubba-access-code.png')

    return resp

