import click
from flask.cli import with_appcontext
from flask_sqlalchemy import SQLAlchemy

db = SQLAlchemy()


class AccessKey(db.Model):
    key = db.Column(db.String(32), primary_key=True)
    expires_at = db.Column(db.DateTime(), nullable=False)
    meta = db.Column(db.String(256))


@click.command('init-db')
@with_appcontext
def init_db_command():
    """Clear existing data and create new tables."""
    db.create_all()
    click.echo('Initialized the database.')


def init_app(app):
    """Register database functions with the Flask app. This is called by
    the application factory.
    """
    db.init_app(app)
    app.cli.add_command(init_db_command)
