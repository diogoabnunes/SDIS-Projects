> How to compile: (Inside proj1/scripts/ folder)
    - Run compile.sh (without arguments).

> How to clean generated dirs/ subfolders: (Inside proj1/scripts/ folder)
    - Run cleanup.sh <peer_ap>
        (E.g.: cleanup.sh 1 -> Deletes proj1/dirs/1/ directory, that had all peer 1 information)

How to run the application: (Inside proj1/scripts/folder)
> Peer:
    - Run peer.sh <version> <peer_id> <svc_access_point> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port>
        (E.g.: peer.sh 1 1 1 230.0.0.0 4445 231.0.0.0 4446 232.0.0.0 4447
               peer.sh 1 2 2 230.0.0.0 4445 231.0.0.0 4446 232.0.0.0 4447
               peer.sh 1 3 3 230.0.0.0 4445 231.0.0.0 4446 232.0.0.0 4447 -> Initiates 3 peers)

> Backup:
    - Run test.sh <peer_ap> BACKUP <file_path> <desired_rep_degree>
        (E.g.: test.sh 1 BACKUP ..\originals\file.txt 2 -> From peer 1, makes a backup of file.txt with replication degree of 2)

> Delete:
    - Run test.sh <peer_ap> DELETE <file_path>
        (E.g.: test.sh 1 DELETE ..\originals\file.txt -> Deletes all the chunks of this file on other peers)

> Restore:
    - Run test.sh <peer_ap> RESTORE <file_path>
        (E.g.: test.sh 1 RESTORE ..\originals\file.txt -> Replicates the file on peer 1 directory)

> Reclaim:
    - Run test.sh <peer_ap> RECLAIM <max_disk_space_to_store_chunks_in_KB>
        (E.g.: test.sh 2 RECLAIM 0 -> Reclaims all the disk space being used (no chunks can be saved by this peer, unless you reclaim some space later))

> State:
    - Run test.sh <peer_ap> STATE
        (E.g.: test.sh 2 STATE -> Informs about files backed up, chunks stored and peer's capacity)