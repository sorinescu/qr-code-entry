import os
import yaml

from flask import Flask

from .access_keys import check_access_key, new_access_key, get_all_access_keys, get_valid_access_keys, revoke_access_key


def create_app(test_config=None):
    """Create and configure an instance of the Flask application."""
    app = Flask(__name__, instance_relative_config=True, )
    app.config.from_mapping(
        # a default secret that should be overridden by instance config
        SECRET_KEY='dev',
        API_TOKEN='123',
        SQLALCHEMY_DATABASE_URI='sqlite:///' + os.path.join(app.instance_path, 'auth-keys.db')
    )

    if test_config is None:
        # load the instance config, if it exists, when not testing
        try:
            with open("config.yml", 'r') as f:
                config = yaml.load(f)
                # print(config)
                app.config.from_mapping(config)
        except FileNotFoundError:
            pass
    else:
        # load the test config if passed in
        app.config.update(test_config)

    # ensure the instance folder exists
    try:
        os.makedirs(app.instance_path)
    except OSError:
        pass

    # Register the database commands
    from key_manager import db
    db.init_app(app)

    # Apply the blueprints to the app
    from key_manager import auth
    app.before_request(auth.token_required)

    app.add_url_rule('/keys', 'new', new_access_key, methods=('POST',))
    app.add_url_rule('/keys', 'keys', get_all_access_keys, methods=('GET',))
    app.add_url_rule('/keys/valid', 'valid_keys', get_valid_access_keys, methods=('GET',))
    app.add_url_rule('/keys/<string:key>', 'revoke', revoke_access_key, methods=('DELETE',))
    app.add_url_rule('/keys/<string:key>', 'check', check_access_key, methods=('GET',))

    return app
