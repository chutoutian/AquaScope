Fs = 48000;

dirName = 'raw_data/noise';
files = dir(fullfile(dirName, 'Bob-DataNoise-*.txt'));
numFiles = length(files);

for fileIdx = 1:numFiles
    % Compute the FFT of the noise data
    fileName = fullfile(dirName, files(fileIdx).name);
    noise = load(fileName);
    N = length(noise_data);
    fft_data = fft(noise);
    
    % Compute the frequency axis
    f = (0:N-1) * Fs / N;
    
    % Compute the power spectrum (magnitude squared)
    power_spectrum = abs(fft_data).^2 / N;
    
    % Convert power spectrum to decibels (dB)
    power_spectrum_dB = 10*log10(power_spectrum);
    
    % Plot the frequency spectrum
    figure;
    plot(f, power_spectrum_dB);
    title('Frequency Spectrum of Noise Data');
    xlabel('Frequency (Hz)');
    ylabel('Power (dB)');
    xlim([0 6000]); % Limit the x-axis to the positive frequencies
    filename = sprintf('figures/noise_%d.jpg',fileIdx);
    saveas(gcf, filename, 'jpg');
    grid on;
end
    