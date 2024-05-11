function dominantFrequencies = draw_frequency(signal, Fs, windowSize, overlap)
    % This function calculates the dominant frequency of a signal over time.
    %
    % Parameters:
    % signal    : The input signal (vector).
    % Fs        : Sampling frequency of the signal (scalar).
    % windowSize: The length of each segment for the FFT (scalar).
    % overlap   : The number of samples that consecutive segments overlap (scalar).
    %
    % Returns:
    % dominantFrequencies: A vector containing the dominant frequency at each time slice.
    
    % Number of segments
    step = windowSize - overlap;
    numSegments = floor((length(signal) - overlap) / step);
    
    % Initialize the vector to hold the dominant frequencies
    dominantFrequencies = zeros(1, numSegments);
    
    % Analyze each segment
    for i = 1:numSegments
        % Get the segment of the signal
        idxStart = (i-1) * step + 1;
        idxEnd = idxStart + windowSize - 1;
        segment = signal(idxStart:idxEnd);
        
        % Compute the FFT of the segment
        fftSegment = fft(segment, windowSize);
        [P, f] = max(abs(fftSegment(1:windowSize/2+1)));
        
        % Calculate the frequency
        dominantFrequencies(i) = (f-1) * (Fs / windowSize);
    end
    
    % Plot the result
    figure;
    plot((0:numSegments-1) * step / Fs, dominantFrequencies);
    title('Dominant Frequency Over Time');
    xlabel('Time (seconds)');
    ylabel('Frequency (Hz)');
end