
# Matlab code
Matlab codes for processing the raw data in an offline manner.


| System component               | main files and APIs                                                                                                                 |
|--------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| Protocol sequence logic        | SendChirpAsyncTask.java/work<br/>   SendChirpAsyncTask.java/send_data_helper <br/>                                                  |
| Preamble generation (Alice)    | SymbolGeneration.java/generateDataSymbols_LoRa (convert encoded data to symbol)                                                     |
| Encoding data packet (Alice)   | SymbolGeneration.java/encode_LoRa (encode data)                                                                                     |
| Demodulating data packet (Bob) | Decoder.java/demodulate (demodulate the received packet)                                                                            |
| Decoding packet (Bob)          | Decoder.java/decoding (decode the demodulated symbols)                                                                              |
| Other key components           | Utils.java/waitForData (logics of listening sounds and detecting the preamble)<br/> Utils.java/GenerateChirp_LoRa (generate chirps) |

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
