#!/usr/bin/env bash

# Change this to your netid
netid=kxp210004

# Root directory of your project
PROJECT_DIR=/home/013/k/kx/kxp210004/LeaderElection

# Directory where the config file is located on your local system
CONFIG_LOCAL=$HOME/Desktop/LeaderElection/config.txt

# Directory your java classes are in
BINARY_DIR=$PROJECT_DIR/bin

# Your main project class
PROGRAM=Main

i=0

declare -A uidHostMap

cat $CONFIG_LOCAL | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    mapfile -t configFile 

    totalNodes=${configFile[0]}
    counter=0

    while [[ $counter -lt $totalNodes ]]
    do
        line=(${configFile[$((counter))]})
        uid="${line[0]}"
        host="${line[1]}"
        port="${line[2]}"

        uidHostMap[$uid]="$host,$port"

        counter=$((counter + 1))
    done

    # Uncomment to debug the values in the uidHostMap hashmap.
    # for key in ${!uidHostMap[@]}; do
    #     echo ${key} ${uidHostMap[${key}]}
    # done
    
    i=1
    while [[ $i -lt $totalNodes ]]
    do
    	line=(${configFile[$((i))]})
        uid="${line[0]}"
        host="${line[1]}"
        port="${line[2]}"

        echo $uid $host $port

        neighborsArray=(${configFile[$i + totalNodes]})
        neighCounter=0

        for neighborUID in ${neighborsArray[@]}; do
            neighborsArray[$neighCounter]=${uidHostMap[${neighborUID}]}
            neighCounter=$(( neighCounter+1 ))
        done
        
        neighbors=$(printf "_%s" "${neighborsArray[@]}")

        echo "Neighbors ${neighbors}"
	
        osascript -e '
                tell app "Terminal"
                    do script "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no '$netid@$host' java -cp '$BINARY_DIR' '$PROGRAM' '$uid' '$host' '$port' '$neighbors'"
                end tell'

        i=$(( i + 1 ))
    done
)
