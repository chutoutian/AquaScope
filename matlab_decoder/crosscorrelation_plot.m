function crosscorrelation_plot(signal1, signal2)
    % Ensure both signals are row vectors
    if size(signal1, 1) > 1
        signal1 = signal1.';
    end
    if size(signal2, 1) > 1
        signal2 = signal2.';
    end

    % Calculate the cross-correlation using the xcorr function
    [R, lags] = xcorr(signal1, signal2, 'none');  % Use 'none' for no scaling

    % Create the figure
    figure;

    % Plot cross-correlation
    plot(lags, abs(R), '-', 'MarkerSize', 4, 'LineWidth', 1.5);
    title('Cross-Correlation between Two Signals');
    xlabel('Lag');
    ylabel('Cross-Correlation');

    % Enhance display
    grid on;
    axis tight;  % Adjust the axis limits to fit the data
end