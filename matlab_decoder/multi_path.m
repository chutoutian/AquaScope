FS = 48000; % sampling frequency in Hz
BW = 2000;
FC = 2500;
SF = 5;
CR = 4;
M = 2^SF;
Ns = M / BW * FS;
center_freq = 0e3;
offset_freq = 1e3;


dirName = 'raw_data/SF_5_BW_2_FC_25_pool';
files = dir(fullfile(dirName, 'Bob-Rx_Raw_Symbols-*.txt'));
numFiles = length(files);

start = (195 / 1000) * 48000;
start = start + 960;


uc_1 = chirp(true, SF, BW, FS, 3, 0, 0);
numsym = calc_sym_num(80,CR,SF);
index = [];

for fileIdx = 1
    fileName = fullfile(dirName, files(fileIdx).name);
     
    raw_symbol = load(fileName)';
    
    raw_symbol = raw_symbol(start+1+135*Ns:start+  136* Ns)/32676.0;
    t = (0:1/FS:(length(raw_symbol)-1)/FS)';
    real_chirp = raw_symbol .* cos(2*pi* FC * t);
    imag_chirp = raw_symbol .* sin(2*pi *FC *t);
    
    real_cs = BPassFilter(real_chirp,center_freq,offset_freq,FS);
    imag_cs = BPassFilter(imag_chirp,center_freq,offset_freq,FS);
    
    %real_cs = lowpass(real_chirp,2e3,FS);
    %imag_cs = lowpass(imag_chirp,2e3,FS);
    
    downversion_symbol = real_cs - 1j*imag_cs;

    [corr, lags] = xcorr(downversion_symbol, uc_1); % Normalized cross-correlation
    [max_value, max_index] = max(abs(corr(Ns+1 :2* Ns-1)));
    index = [index;max_index]
    figure; % Create a new figure window
    plot(lags, abs(corr)); % Plot cross-correlation vs. time lags in seconds
    title('Cross-Correlation of Two Signals');
    xlabel('Time Lag (samples)');
    ylabel('Cross-Correlation');
    grid on; % Enable grid for easier visualization
end

