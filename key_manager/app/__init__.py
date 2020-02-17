from flask import Flask

from .access_keys import *


def create_app(test_config=None):
    """Create and configure an instance of the Flask application."""
    app = Flask(__name__, instance_relative_config=True)
    app.config.from_mapping(
        API_TOKEN='123',
        SQLALCHEMY_DATABASE_URI='sqlite:///' + os.path.join(app.instance_path, 'auth-keys.db')
    )

    if test_config is None:
        app.config.from_envvar('KEY_MANAGER_CONFIG')
    else:
        # load the test config if passed in
        app.config.from_mapping(test_config)

    # ensure the instance folder exists
    try:
        os.makedirs(app.instance_path)
    except OSError:
        pass

    # Register the database commands
    from .db import init_app
    init_app(app)

    # Apply the blueprints to the app
    from .auth import token_required
    app.before_request(token_required)

    app.add_url_rule('/keys', 'new', new_access_key, methods=('POST',))
    app.add_url_rule('/keys', 'keys', get_all_access_keys, methods=('GET',))
    app.add_url_rule('/keys/valid', 'valid_keys', get_valid_access_keys, methods=('GET',))
    app.add_url_rule('/keys/<string:key>', 'revoke', revoke_access_key, methods=('DELETE',))
    app.add_url_rule('/keys/<string:key>', 'check', check_access_key, methods=('GET',))
    app.add_url_rule('/keys/<string:key>', 'update', update_access_key, methods=('PUT',))
    app.add_url_rule('/keys/<string:key>/qrcode', 'qr_code_image', get_key_qr_code, methods=('GET',))

    return app
