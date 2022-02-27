cleanup.sh 1
cleanup.sh 2
cleanup.sh 3
compile.sh
peer.sh 1 1 1 localhost 4445 localhost 4446 localhost 4447 localhost 4448 null -1
peer.sh 1 2 2 localhost 4449 localhost 4450 localhost 4451 localhost 4452 localhost 4448
peer.sh 1 3 3 localhost 4453 localhost 4454 localhost 4455 localhost 4456 localhost 4448
test.sh 1 BACKUP ..\originals\large_file.jpg 2
test.sh 1 RESTORE ..\originals\large_file.jpg
test.sh 1 DELETE ..\originals\large_file.jpg
test.sh 1 RECLAIM 10