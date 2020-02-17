import os
import sys

from datetime import datetime
from io import BytesIO

import dateutil.parser
import uuid
import qrcode

from flask import request, jsonify, make_response
from PIL import Image

from .db import AccessKey, db


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


def new_access_key():
    params = request.get_json()

    try:
        if 'expires_at' not in params:
            raise AttributeError("Expected 'expires_at' parameter")

        try:
            expires_at = dateutil.parser.parse(params['expires_at'])
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
                access_key.expires_at = dateutil.parser.parse(params['expires_at'])
            except ValueError:
                raise ValueError("Invalid format for 'expires_at'")

        if 'metadata' in params:
            access_key.metadata = params['metadata']

        db.session.commit()

        return jsonify(key=key, metadata=access_key.metadata, expires_at=access_key.expires_at)
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


def get_valid_access_keys():
    try:
        keys = [dict(key=ak.key, expires_at=ak.expires_at, metadata=ak.meta)
                for ak in AccessKey.query.all()
                if ak.expires_at > datetime.utcnow()]
        return jsonify(keys=keys)
    except Exception as e:
        return jsonify(msg="Error: %s" % e), 400


def check_access_key(key):
    try:
        access_key = AccessKey.query.filter_by(key=key).first()
        valid = access_key is not None and access_key.expires_at > datetime.utcnow()
        return jsonify(key=key, metadata=access_key.metadata, valid=valid)
    except Exception as e:
        return jsonify(msg="Error: %s" % e), 400


def get_key_qr_code(key):
    qr = qrcode.QRCode(error_correction=qrcode.ERROR_CORRECT_H, box_size=10)

    # logo = Image.open('logo-transp.png')
    logo = Image.open(_rel_file_path('../static/img/logo.jpg'))

    uu = uuid.UUID(hex=key)
    qr.add_data(uu.bytes)

    # qr_img = qr.make_image(back_color='transparent')
    qr_img = qr.make_image(fill_color='#010101')

    qr_w, qr_h = qr_img.size

    logo = logo.resize((qr_w // 4, qr_h // 4), resample=Image.LANCZOS)

    if False:
        logo_big = Image.new('RGB', size=(qr_w, qr_h))
        logo_big.paste(logo, box=(qr_w // 3, qr_h // 3))

        qr_img = Image.alpha_composite(qr_img, logo_big)
    else:
        qr_img.paste(logo, box=(qr_w * 3 // 8, qr_h * 3 // 8))

    with BytesIO() as f:
        qr_img.save(f, format='png')
        resp = make_response(f.getvalue())

    resp.headers.set('Content-Type', 'image/png')
    resp.headers.set('Content-Disposition', 'attachment',
                     filename='hubbahubba-access-code.png')

    return resp

