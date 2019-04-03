#!/usr/bin/env bash
set -e

HOME_DIR=/home/piqr
INSTALL_DIR=${HOME_DIR}/key_manager

export KEY_MANAGER_CONFIG=${INSTALL_DIR}/config.cfg
export FLASK_APP=app
export LC_ALL=en_US.UTF-8
export LANG=en_US.UTF-8

. ${HOME_DIR}/pfsense36/bin/activate

cd ${INSTALL_DIR}
exec flask run --host 0.0.0.0
