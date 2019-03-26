from flask import current_app, request, jsonify


def token_required():
    expected = "Token %s" % current_app.config['API_TOKEN']
    if expected != request.headers.get('Authorization'):
        return jsonify(msg='Invalid token'), 403
