## Leader Election 

This repository contains the implementation of Peleg's leader election algorithm. 

## Running the code

Before running the code, ensure that you have modified the netid in the launcher and cleaner scripts. Ensure that you specify the path to the project correctly as well. To run the code, follow the steps given below:
1. Copy this repository from your local machine to any of the dc machines. This can be done using the following command:
```
        scp -r /path/to/repository/in/local netid@dcxx.utdallas.edu:/home/find/path/to/your/netid/
```
2. Once done, cd into the directory in the dc machines and run the following command:
```
        javac -d bin/ src/*.java
```
3. Finally, to start executing the code, run `./launcher-mac.sh` from your terminal. To ensure that the socket connections are closed properly, run `./cleaner.sh` after checking the output.

Please remember that you need to perform these steps each time you make changes to the code :/ .
