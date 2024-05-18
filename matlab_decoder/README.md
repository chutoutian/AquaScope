
# Matlab code
Matlab codes for processing the raw data in an offline manner.

`raw_data` contains collected data from real experiments

Run `demodulation.m` or `demodulation_xcorr.m` to get the number of error bytes (calculate the time-offset using derived formation or cross-correlation, respectively)

| Key functions       | explanations                                       |
|---------------------|----------------------------------------------------|
| BPassFilter.m       | Band pass filter                                   |
| Bytes2Embedding.m   | Convert bytes to embedding                         |
| LoRaPHY.m           | standard LoRa protocol (I use some apis from them) |
| chirp.m             | generate chirps                                    |
| underwater_noise.m  | process and analysis noise                         |
| underwater_snr.m    | get channel frequency                              |

