#!/bin/bash

# Exit on error
set -e

# Install requirements
composer install

# Start the server with router
PORT="${PORT:-8000}"
php -S 0.0.0.0:$PORT router.php
