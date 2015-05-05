#!/bin/sh

for i in `ps -aux | grep Standalone | gawk '{ print $2 }'`; do kill -9 $i; done
for i in `ps -aux | grep standalone | gawk '{ print $2 }'`; do kill -9 $i; done
for i in `ps -aux | grep Domain | gawk '{ print $2 }'`; do kill -9 $i; done
for i in `ps -aux | grep domain | gawk '{ print $2 }'`; do kill -9 $i; done
