# Instructions for compiling and running
Run from scripts:
## Clean peers
    cleanup.sh <id_peer>

## Compile
    compile.sh

## Start Peers
    peer.sh <version> <id_peer> <access_point> <mc_address> <mc_port> <mdb_address> <mdb_port> <mdr_address> <mdr_port> <peer_chord_address> <peer_chord_port> <chord_ask_address> <chord_ask_port> 

### Examples:
#### Peer 1
    peer.sh 1 1 1 localhost 4445 localhost 4446 localhost 4447 localhost 4448 null -1

#### Peer 2
    peer.sh 1 2 2 localhost 4449 localhost 4450 localhost 4451 localhost 4452 localhost 4448

#### Peer 3
    peer.sh 1 3 3 localhost 4453 localhost 4454 localhost 4455 localhost 4456 localhost 4448

## Run BACKUP 
    test.sh <id_peer> BACKUP <file_path> <rep_degree>

Example:

    test.sh 1 BACKUP ../originals/file.txt 2

## Run DELETE
    test.sh <id_peer> DELETE <file_path>
    
Example:

    test.sh 1 DELETE ../originals/file.txt

## Run RESTORE
    test.sh <id_peer> RESTORE <file_path> 
    
Example:

    test.sh 1 RESTORE ../originals/file.txt

## Run RECLAIM
    test.sh <id_peer> RECLAIM <max_space_size>
    
Example:

    test.sh 1 RECLAIM 0
