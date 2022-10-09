#!/bin/bash

sleep 1

sudo systemctl daemon-reload
sudo systemctl stop blog
sudo systemctl start blog

