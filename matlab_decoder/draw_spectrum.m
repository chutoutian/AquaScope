function draw_spectrum(signal, Fs,window,overlap)
    nfft = length(signal);            % Number of FFT points, adjusted for better frequency resolution
    [S, F, T] = spectrogram(signal, window, overlap, nfft, Fs);
    
    % Shift zero-frequency to center and scale frequency vector to kHz
    S_shifted = fftshift(S, 1);  % Shift along the frequency dimension
    F_shifted = (-nfft/2:nfft/2-1) * (Fs / nfft) / 1000;  % Frequency vector in kHz
    
    % Plot the shifted spectrogram
    figure;
    imagesc(T, F_shifted, 20*log10(abs(S_shifted)));  % Display in dB for better dynamic range visualization
    axis xy;  % Correct the axis orientation
    colormap jet;  % Use a good color map for visibility
    title('Spectrogram of the Downversioned Signal');
    xlabel('Time (s)');
    ylabel('Frequency (kHz)');
    ylim([-2 2]);  % Set frequency limits to show -1 kHz to 1 kHz
    colorbar;
    clim([-100 0]);  % Adjust based on your signal's dynamics for visibility
end

