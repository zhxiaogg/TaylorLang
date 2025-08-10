#!/bin/bash

MESSAGE=$(cat | jq -r '.message')
curl -d "${MESSAGE}" ntfy.sh/${NTFY_TOPIC}