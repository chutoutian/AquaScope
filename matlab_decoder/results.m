gt_embedding = [125, 84, 548, 237, 608, 972, 875, 184, 577, 498, 887, 906, 172, 528, 539, 928, 508, 914, 382, 217, 730, 593, 589, 683, 533, 810, 821, 92, 171, 700, 982, 579, 788, 601, 589, 226, 624, 813, 903, 610, 937, 970, 109, 609, 539, 417, 308, 835, 761, 471, 358, 749, 469, 865, 1012, 150, 500, 805, 261, 40, 503, 548, 965, 713]';
gt_transmitted_bytes = [33,21,97,69,5,113,17,29,82,68,30,39,6,15,53,21,100,82,92,88,102,44,10,76,76,126,0,81,108,13,105,51,119,70,45,69,108,0,80,29,109,113,76,112,6,43,24,57,27,121,60,119,48,94,71,68,43,88,14,45,101,17,52,0,51,44,34,101,31,33,122,110,5,117,97,24,119,82,90,58,105,62,68,116,15,72,23,3,21,63,26,113,0,116,33,12,64,78,95,109,3,28,121,36,127,92,120,25,95,83,20,100,28,92,77,43,7,90,1,124,78,108,93,67,79,62,31,44,120,78,104,57,120,23,93,41,79,82,36,39,23,121,68,14,69,50,16,21,63,9,14,38,2,43,4,77,43,2,100,107,12,106,98,7,22,101,45,65,123,73,95,6,119,18,120,48,81,35,101,84,57,54,30,90,86,43,107,86,86,86,87,85]';

a = [125,856,549,740,612,972,323,219,89,499,869,954,684,533,539,929,892,146,894,105,730,604,591,683,532,876,885,108,1000,1021,966,610,788,599,617,82,580,907,967,519,937,970,121,721,521,161,308,846,761,506,230,649,854,609,612,118,492,801,965,56,1015,558,773,995]';
nnz(a - gt_embedding)

baseDir = 'raw_data/SF_5_BW_2_FC_25_bathtub/';
FileName_embedding = 'Bob-Rx_Embedding-';
FileName_symbol = 'Bob-Rx_Symbols-';
numFiles = 10; % Total number of files

% Initialize an empty array for concatenation
received_embedding = [];
received_symbol = [];

% Loop through each file indexnnz
for idx = 0:numFiles-1
    % Construct the file name
    fileName_embedding = sprintf('%s%s%d.txt', baseDir, FileName_embedding, idx);
    fileName_symbol = sprintf('%s%s%d.txt', baseDir, FileName_symbol, idx);
    embedding = load(fileName_embedding)';
    symbol = load(fileName_symbol)';
    received_embedding = [received_embedding, embedding];
end




for i = 1:numFiles
    rx_emb = received_embedding(:,i);
    nnz(rx_emb - gt_embedding);
    %nnz(received_symbol(5:length(received_symbol),i) - gt_transmitted_bytes);
end