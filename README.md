
# Smartphone code
Real time underwater imaging application using Android studio. The project was originally built with Android Studio Arctic Fox 2022.3.1 Patch 1, and has been tested on Samsung Galaxy S9 phones.

When change the parameters in the interface, please restart the app at the receiver or wait for several seconds.


| System component                            | Link to code |
|---------------------------------------------| ----------- |
| Protocol sequence logic                     | [Code](OceanRealDemo/app/src/main/java/com/example/root/ffttest2/SendChirpAsyncTask.java)       |
| Preamble generation (Alice)                 | [Code](smartphone/OceanRealDemo/app/src/main/java/com/example/root/ffttest2/SymbolGeneration.java)    |
| Encoding data packet (Alice)                | [Code](smartphone/OceanRealDemo/app/src/main/java/com/example/root/ffttest2/SymbolGeneration.java)    |
| Demodulating and Decoding data packet (Bob) | [Code](smartphone/OceanRealDemo/app/src/main/java/com/example/root/ffttest2/Decoder.java)       |

| PHY Parameters | Explanations                                                                                                                                                                                                                             |
|----------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Volume         | The volume of transmitting sound (0-1)                                                                                                                                                                                                   |
| Send Delay     | Delay (default 3000ms) after pushing the "Send" button which is used to overcome the difficulty of sending message underwater                                                                                                            |
| BW             | Bandwidth (suggested 2000-4000Hz)                                                                                                                                                                                                        |
| FC             | Carrier Frequency (suggested 2500 for 2-3khz, and 3000 for 4khz)                                                                                                                                                                         |
| SF             | Spread Factor (suggested 6 or below, SF = 7 takes a long time)                                                                                                                                                                           |
| # measurements | Number of measurements in one experiments (push the "Send" button once)                                                                                                                                                                  |
| Code rate      | Error correction code (None: no correction code; 4/8 add 4 bits for every 4 data bits; 6/8; add 2 bits for every 4 bits)                                                                                                                 |
| Tx protocol    | Different schemes Proposed: chirp-based transmission; OFDMwiadapt: OFDM with frequency adaptation; OFDMwoadapt: OFDM without frequency adaptation; Noise: just use one phone to measure the environment noise; Chirp: send a 500ms chirp |
