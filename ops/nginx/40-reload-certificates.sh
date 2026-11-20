#!/bin/sh
# Mounted into /docker-entrypoint.d/ of the nginx image: reloads nginx every 6h
# so certificates renewed by the certbot service are picked up without downtime.
(
    while :; do
        sleep 21600
        nginx -s reload
    done
) &
