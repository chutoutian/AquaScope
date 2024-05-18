function max_index = synchronization_xcorr(downversion_symbol,uc_1,Ns,is_start)

[corr, lags] = xcorr(downversion_symbol, uc_1); 
if is_start
[max_value, max_index] = max(abs(corr(3*Ns +1:4*Ns)));
else
[max_value, max_index] = max(abs(corr(Ns+1:2*Ns -1)));
end
end

