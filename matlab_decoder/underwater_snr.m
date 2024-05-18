FS = 48000; % sampling frequency in Hz


dirName = 'raw_data/chirps';
files = dir(fullfile(dirName, 'Bob-DataChirp-*.txt'));

numFiles = length(files);


% remove the preamble 
start = (195 / 1000) * 48000;
start = start + 960;

symbol_error_rate = [];

embedding_error_rate = [];
symbol_gt = load('raw_data\chirps\Alice-DataChirp-0.txt');
symbol_gt = symbol_gt(start+1:start+ FS/2)/32676.0;

fft_gt = fft(symbol_gt);

%uc = chirp2(true, SF);
for fileIdx = 1:numFiles
        fileName = fullfile(dirName, files(fileIdx).name);
        
        raw_symbol = load(fileName)';
        
        
        
        raw_symbol = raw_symbol(start+1:start+ FS/2)/32676.0;
        draw_spectrum(raw_symbol,FS,128,120);
        sound_file_name =sprintf('chirp_sound/chirp_%d.wav',fileIdx);
        % Compute the FFT of the noise data
        N = length(raw_symbol);
        fft_data = fft(raw_symbol);
        
        
        % Compute the frequency axis
        f = (0:N-1) * Fs / N;
        
        % Compute the power spectrum (magnitude squared)
        power_spectrum = (abs(fft_data).^2) ./ (abs(fft_gt).^2);
        
        % Convert power spectrum to decibels (dB)
        power_spectrum_dB = 10*log10(power_spectrum);
        
        % Plot the frequency spectrum
        figure;
        plot(f, power_spectrum_dB);
        title('Frequency Spectrum of Noise Data');
        xlabel('Frequency (Hz)');
        ylabel('Power (dB)');
        xlim([0 6000]); % Limit the x-axis to the positive frequencies
        filename = sprintf('figures/chirp_%d.jpg',fileIdx);
        saveas(gcf, filename, 'jpg');
        grid on;
        
        audiowrite(sound_file_name, raw_symbol, FS, 'BitsPerSample', 16);
end
