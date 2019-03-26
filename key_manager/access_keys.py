from datetime import datetime

import dateutil.parser
import uuid

from flask import request, jsonify

from .db import AccessKey, db


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
        return jsonify(key=key, expires_at=expires_at)
    except Exception as e:
        return jsonify(msg="Error: %s" % e), 400


def revoke_access_key(key):
    try:
        access_key = AccessKey.query.filter_by(key=key).first()
        if access_key is not None:
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
        return jsonify(key=key, valid=valid)
    except Exception as e:
        return jsonify(msg="Error: %s" % e), 400
