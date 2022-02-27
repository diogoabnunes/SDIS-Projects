#! /usr/bin/bash

# Script for running a peer
# To be run in the root of the build tree
# No jar files used
# Assumes that Peer is the main class
#  and that it belongs to the peer package
# Modify as appropriate, so that it can be run
#  from the root of the compiled tree

# Check number input arguments
argc=$#

if ((argc != 13)); then
  echo "Usage: $0 <version> <peer_id> <svc_access_point> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port> <chord_addr> <chord_port> <ask_chord_addr> <ask_chord_port>"
  exit 1
fi

# Assign input arguments to nicely named variables

ver=$1
id=$2
sap=$3
mc_addr=$4
mc_port=$5
mdb_addr=$6
mdb_port=$7
mdr_addr=$8
mdr_port=$9
chord_addr=${10}
chord_port=${11}
ask_chord_addr=${12}
ask_chord_port=${13}

# Execute the program
# Should not need to change anything but the class and its package, unless you use any jar file

# echo "java peer.Peer ${ver} ${id} ${sap} ${mc_addr} ${mc_port} ${mdb_addr} ${mdb_port} ${mdr_addr} ${mdr_port}"
cd ../src/build
java main.Peer ${ver} ${id} ${sap} ${mc_addr} ${mc_port} ${mdb_addr} ${mdb_port} ${mdr_addr} ${mdr_port} ${chord_addr} ${chord_port} ${ask_chord_addr} ${ask_chord_port}
cmd \k
# EXECUTAR PEER EXEMPLO
# .\peer.sh 1 1 1 230.0.0.0 4445 231.0.0.0 4446 232.0.0.0 4447 localhost 4448 null -1 -> primeiro no
# .\peer.sh 1 1 1 230.0.0.0 4445 231.0.0.0 4446 232.0.0.0 4447 localhost 4449 localhost 4448 -> n e o primeiro no
