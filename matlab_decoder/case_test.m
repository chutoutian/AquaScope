FS = 48000; % sampling frequency in Hz
BW = 2000;
FC = 2500;
SF = 7;
CR = 4;
M = 2^SF;
Ns = M / BW * FS;
center_freq = 0e3;
offset_freq = 1e3;

gt_embedding = [125, 84, 548, 237, 608, 972, 875, 184, 577, 498, 887, 906, 172, 528, 539, 928, 508, 914, 382, 217, 730, 593, 589, 683, 533, 810, 821, 92, 171, 700, 982, 579, 788, 601, 589, 226, 624, 813, 903, 610, 937, 970, 109, 609, 539, 417, 308, 835, 761, 471, 358, 749, 469, 865, 1012, 150, 500, 805, 261, 40, 503, 548, 965, 713]';
if SF == 7
    gt_transmitted_bytes = [33,21,97,69,5,113,17,29,82,68,30,39,6,15,53,21,100,82,92,88,102,44,10,76,76,126,0,81,108,13,105,51,119,70,45,69,108,0,80,29,109,113,76,112,6,43,24,57,27,121,60,119,48,94,71,68,43,88,14,45,101,17,52,0,51,44,34,101,31,33,122,110,5,117,97,24,119,82,90,58,105,62,68,116,15,72,23,3,21,63,26,113,0,116,33,12,64,78,95,109,3,28,121,36,127,92,120,25,95,83,20,100,28,92,77,43,7,90,1,124,78,108,93,67,79,62,31,44,120,78,104,57,120,23,93,41,79,82,36,39,23,121,68,14,69,50,16,21,63,9,14,38,2,43,4,77,43,2,100,107,12,106,98,7,22,101,45,65,123,73,95,6,119,18,120,48,81,35,101,84,57,54,30,90,86,43,107,86,86,86,87,85]';
end
if SF == 6
    gt_transmitted_bytes =[33,21,61,37,1,5,9,1,36,7,28,36,5,13,53,44,14,6,51,51,53,46,17,24,39,43,26,38,42,49,40,11,23,17,8,7,38,32,7,36,30,47,53,55,50,15,10,4,55,8,27,25,62,54,6,15,27,57,28,58,24,47,36,31,44,18,5,41,31,20,51,0,55,47,26,36,56,63,16,27,36,38,7,18,57,63,28,37,1,34,12,63,55,53,54,20,27,49,0,52,32,9,30,1,11,33,52,25,32,30,9,62,0,51,34,52,3,13,61,18,4,55,18,17,35,46,20,29,20,46,19,55,1,54,1,10,18,27,60,44,37,5,1,20,10,46,20,24,10,46,18,62,35,45,10,7,14,30,53,38,40,41,15,52,60,32,17,61,60,15,16,12,31,5,7,46,3,44,8,55,45,1,30,22,48,38,5,25,41,52,39,2,48,58,11,32,41,51,34,3,16,39,13,44,56,50,25,51,44,56,58,53,6,39,29,53]';
end
if SF == 5
    gt_transmitted_bytes = [29,9,29,17,1,29,29,1,7,20,25,26,8,13,21,28,11,9,12,28,24,25,2,17,15,24,14,12,19,6,31,23,23,27,15,23,11,7,21,23,12,9,4,29,23,8,2,25,30,18,28,26,26,8,28,2,20,17,21,25,2,11,6,2,21,4,15,13,22,30,8,13,29,17,17,27,1,11,11,3,22,12,26,7,14,3,7,1,28,24,29,10,13,17,4,23,29,27,7,10,29,0,14,14,2,30,8,3,10,11,11,13,24,3,20,29,3,18,6,17,20,7,8,13,1,16,2,5,3,9,28,3,4,20,31,16,0,26,17,15,31,4,17,12,4,23,15,9,18,23,10,2,25,6,21,14,4,11,1,10,7,23,29,19,15,11,1,9,14,21,20,19,19,16,8,27,30,21,29,2,16,22,3,9,18,10,28,29,30,25,19,23,25,24,2,28,29,16,9,15,10,29,16,16,29,5,7,30,12,20,0,21,30,1,5,21,1,22,0,28,19,16,7,23,12,23,31,2,6,26,21,1,24,29,27,8,12,20,9,17,16,26,4,21,28,25,13,23,24,18,14,10,8,26,4,28,22,21,11,22,22,22,21,11]';
end





dirName = 'raw_data/SF_7_BW_2_FC_25_pool';
files = dir(fullfile(dirName, 'Bob-Rx_Raw_Symbols-*.txt'));
numFiles = length(files);



% remove the preamble 
start = (195 / 1000) * 48000;
start = start + 960;
numsym = calc_sym_num(80,CR,SF);

symbol_error_rate = [];


for fileIdx = 1:numFiles
    pks = [];
    tos = [];
    embedding_error_rate = [];
    for k = 0
        fileName = fullfile(dirName, files(fileIdx).name);
        %fprintf('Processing file: %s\n', fileName);
     
        raw_symbol = load(fileName)';
        
        raw_symbol = raw_symbol(start+1+k:start+k+ (numsym+4) * Ns)/ 32767.0;
        %crosscorrelation_plot(raw_symbol(100*Ns+1:104*Ns),raw_symbol(100*Ns+1:101*Ns))
        sound_file_name =sprintf('chirpSound_%d.wav',fileIdx);
        audiowrite(sound_file_name, raw_symbol, FS, 'BitsPerSample', 16);
        t = (0:1/FS:(length(raw_symbol)-1)/FS)';
        %draw_spectrum(raw_symbol,FS);
        %%%%%%%% downversion
        %raw_symbol = BPassFilter(raw_symbol,2.5e3,1e3,FS);
        %raw_symbol = BPassFilter(raw_symbol,2.5e3,1e3,FS);
        real_chirp = raw_symbol .* cos(2*pi* FC * t);
        imag_chirp = raw_symbol .* sin(2*pi *FC *t);
        
        real_cs = BPassFilter(real_chirp,center_freq,offset_freq,FS);
        imag_cs = BPassFilter(imag_chirp,center_freq,offset_freq,FS);
        
        %real_cs = lowpass(real_chirp,2e3,FS);
        %imag_cs = lowpass(imag_chirp,2e3,FS);
        
        downversion_symbol = real_cs - 1j*imag_cs;
        %%%%%%%% downsample  
        received_symbol_sampled_2bw = resample(downversion_symbol,2*BW, 48000);
        
        uc_1 = chirp(false, SF, BW, BW * 2, 0, 0, 0);
        dc_1 = chirp(true, SF, BW, BW * 2, 0, 0, 0);
    
        
        ft_combine = [];
        bin_index_ori = [];
        ft_ori = [];
        
        pk = 0;
        for i = 1:2
            segment = received_symbol_sampled_2bw((i-1) * 2* M + 1 :i  * 2*M);
            fft_output = abs(fft(segment .* uc_1, M*2 * 10));
            %figure
            %plot(fft_output)
            ft_ori = [ft_ori; fft_output];
            ft_ = abs(fft_output(1:M*10)) + abs(fft_output(M*10+1:2*M*10));
            ft_combine = [ft_combine;ft_];
            %figure
            %plot(ft_)
            [max_value,max_index] = max(ft_);
            pk = pk + max_value;
            %index = mod(floor(max_index / 10),M) ;
            index = mod(max_index / 10,M) ;
            bin_index_ori = [bin_index_ori; index];
        end
        
        
        for i = 3:4
            segment = received_symbol_sampled_2bw((i-1) * 2 * M + 1 :i * 2 * M);
            %figure;
            %spectrogram(segment, 128, 120, Ns, FS, 'yaxis');
            %ylim([0 5]);
            fft_output = abs(fft(segment .* dc_1, 2 *M * 10));
            %figure
            %plot(fft_output)
            ft_ori = [ft_ori; fft_output];
            ft_ = abs(fft_output(1:M*10)) + abs(fft_output(M*10+1:2*M*10));
            %figure
            %plot(ft_)
            ft_combine = [ft_combine;ft_];
            [max_value,max_index] = max(ft_);
            pk = pk + max_value;
            %index = mod(floor(max_index / 10),M);
            index = mod(max_index / 10,M) ;
            bin_index_ori = [bin_index_ori; index];
        end
        pks = [pks; pk];
        
        [cfo,to] = synchronization(SF,bin_index_ori(2),bin_index_ori(4));
        tau = abs(round(to * Ns / M)) ;
        tos = [tos; abs(round(to * Ns / M))];
        %tau = 0;
        raw_symbol_shift = load(fileName)' ;
        raw_symbol_shift = raw_symbol_shift(start+1+k+tau:start+k+tau+ (numsym+4) * Ns);
        %raw_symbol_shift = BPassFilter(raw_symbol_shift,2.5e3,1e3,FS);
        %raw_symbol_shift = BPassFilter(raw_symbol_shift,2.5e3,1e3,FS);
        %t = (tau:length(raw_symbol)+tau-1/FS)';
        real_chirp = raw_symbol_shift .* cos(2*pi* FC * t);
        imag_chirp = raw_symbol_shift .* sin(2*pi *FC *t);
        real_cs = BPassFilter(real_chirp,center_freq,offset_freq,FS);
        imag_cs = BPassFilter(imag_chirp,center_freq,offset_freq,FS);
        %real_cs = lowpass(real_chirp,2e3,FS);
        %imag_cs = lowpass(imag_chirp,2e3,FS);
        
        downversion_symbol = real_cs - 1j*imag_cs;
        received_symbol_sampled_2bw = resample(downversion_symbol, 2 * BW, 48000);
        
        
        for i = 5:numsym+4
            segment = received_symbol_sampled_2bw((i-1) * 2 * M + 1 :i *2 * M);
            fft_output = abs(fft(segment .* uc_1, 2 * M * 10));
            %figure
            %plot(fft_output)
            
            ft_ori = [ft_ori; fft_output];
            ft_ = abs(fft_output(1:M*10)) + abs(fft_output(M*10+1:2*M*10));
            %figure
            %plot(ft_)
            %filename = sprintf('figures/fft_results_%d_%d.jpg',i,fileIdx);
            %saveas(gcf, filename, 'jpg');
            ft_combine = [ft_combine;ft_];
            [max_value,max_index] = max(ft_);
            index = mod(max_index / 10,M) ;
            index = index - cfo;
            index = mod(round(index),M);
            bin_index_ori = [bin_index_ori; index];
        end
        bin_index_ori;
           
        diff_bin_index = bin_index_ori(5:length(bin_index_ori)) - gt_transmitted_bytes;
        %figure
        %plot(diff)
    
        diff_num = nnz(bin_index_ori(5:length(bin_index_ori)) - gt_transmitted_bytes);
        symbol_error_rate = [symbol_error_rate; diff_num];
        bin_index_ori_remove_preamble = bin_index_ori(5:length(bin_index_ori));
    
        symbols_g = gray_coding(bin_index_ori_remove_preamble,SF);
        codewords = diag_deinterleave(symbols_g(1:8), SF-2);
        nibbles = hamming_decode(codewords, 8);
        
        rdd = CR + 4;
        for ii = 9:rdd:length(symbols_g)-rdd+1
            codewords = diag_deinterleave(symbols_g(ii:ii+rdd-1), SF);
            % hamming decode
            nibbles = [nibbles; hamming_decode(codewords, rdd)];
        end
        
        bytes = uint8(zeros(min(255, floor(length(nibbles)/2)), 1));
        for ii = 1:length(bytes)
            a = uint8(nibbles(2*ii-1));
            b = 16*uint8(nibbles(2*ii));
            bytes(ii) = bitor(a, b);
        end
        
        len = 80;
        
        data = dewhiten(bytes(1:len));
        
        
        embedding = Bytes2Embedding(data)';
        
        diff = nnz(gt_embedding - embedding);
        embedding_error_rate = [embedding_error_rate;diff]
    end
    %{
    figure
    plot(embedding_error_rate);
    filename = sprintf('figures/error_rate_vs_time_shift_%d.jpg',fileIdx);
    saveas(gcf, filename, 'jpg');
    figure
    plot(pks);
    filename = sprintf('figures/pks_vs_time_shift_%d.jpg',fileIdx);
    saveas(gcf, filename, 'jpg');
    figure
    plot(tos);
    filename = sprintf('figures/tos_vs_time_shift_%d.jpg',fileIdx);
    saveas(gcf, filename, 'jpg');
    %}
end





