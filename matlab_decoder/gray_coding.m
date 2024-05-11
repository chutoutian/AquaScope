function symbols = gray_coding(din,SF)
            % gray_coding  Gray coding
            %              `gray_coding` is used in the DECODING process
            %
            % input:
            %     data: Symbols with bin drift
            % output:
            %     symbols: Symbols after bin calibration

            din(1:8) = floor(din(1:8)/4);
            din(9:end) = mod(din(9:end)-1, 2^SF);
            s = uint16(din);
            symbols = bitxor(s, bitshift(s, -1));
        end

