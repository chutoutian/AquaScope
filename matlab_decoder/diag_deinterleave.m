function codewords = diag_deinterleave(symbols, ppm)
            % diag_deinterleave  Diagonal deinterleaving
            %
            % input:
            %     symbols: Symbols after gray coding
            %     ppm: Size with parity bits
            % output:
            %     codewords: Codewords after deinterleaving

            b = de2bi(symbols, double(ppm), 'left-msb');
            temp_1 = cell2mat(arrayfun(@(x) ...
                circshift(b(x,:), [1 1-x]), (1:length(symbols))', 'un', 0))';
            temp_2 = bi2de(temp_1);
            codewords = flipud(temp_2);
        end