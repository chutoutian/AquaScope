function autocorrelation_plot(signal)
    % Ensure the signal is a row vector
    if size(signal, 1) > 1
        signal = signal.';
    end

    % Compute the length of the signal
    n = length(signal);

    % Preallocate the autocorrelation array
    R = zeros(1, n);

    % Compute the autocorrelation
    for lag = 0:n-1
        R(lag+1) = sum(signal(1:n-lag) .* signal(lag+1:n));
    end

    % Time shifts corresponding to lags
    lags = 0:n-1;

    % Create the figure
    figure;

    % Plot autocorrelation
    stem(lags, R, 'filled', 'MarkerSize', 4);
    title('Autocorrelation of the Signal');
    xlabel('Lag');
    ylabel('Autocorrelation Value');

    % Enhance display
    grid on;
    axis tight;  % Adjust the axis limits to fit the data
end