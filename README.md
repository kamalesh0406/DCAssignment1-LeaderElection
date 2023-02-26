## Leader Election and BFS Tree

This repository contains the implementation of Peleg's leader election algorithm and the construction of a BFS tree with the elected leader node as the root.

Team members: Kamalesh Palanisamy(kxp210004), Vignesh Thirunavukkarasu(vxt200003)

## Testing the code
- Modify the launcher file with your net id first and copy this entire directory to the dc server. 
- If you are running it on the dc server directly from your linux machine, use the ```launcher-linux.sh``` file. Otherwise, use the ```launcher-macos.sh``` file if you are using macos. Please note that the launcher-linux.sh might fail since we did not have access to linux machines to test it.
- Although the bin files are included, they might not work due to version conflicts and platform differences.
- To compile it, please run ```javac -d ./bin @compilepaths.txt``` and then run ```./launcher-mac.sh``` or ```./launcher-linux.sh```. This should have everything up and running.

If you want to test this on your local machine, please replce the DC in config with localhost so that you can simulate a distributed network over your local host.

## Developer Guide

Before modifying the code, ensure that you have modified the netid in the launcher and cleaner scripts. Ensure that you specify the path to the project correctly as well. To run the code, follow the steps given below (Note that this guide is specifically for Macs):
- Copy this repository from your local machine to any of the dc machines. This can be done using the following command:
```
scp -r /path/to/repository/in/local netid@dcxx.utdallas.edu:/home/find/path/to/your/netid/
```
- Once done, login to the directory of any one of the dc machines and run the following command:
```
javac -d ./bin @compilepaths.txt
```
- Finally, to start executing the code, run `./launcher-mac.sh` from your terminal. To ensure that the socket connections are closed properly, run `./cleaner.sh` after checking the output.

Please remember that you need to perform these steps each time you make changes to the code :/ .
